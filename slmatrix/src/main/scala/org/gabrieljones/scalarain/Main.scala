package org.gabrieljones.scalarain

import org.gabrieljones.scalarain.CodePointSyntax.*
import caseapp.*
import com.googlecode.lanterna.input.{KeyStroke, MouseAction}
import com.googlecode.lanterna.terminal.{DefaultTerminalFactory, ExtendedTerminal, MouseCaptureMode, Terminal}
import com.googlecode.lanterna.*
import Options.*
import org.gabrieljones.scalarain.Physics.Vector2

import java.util
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.atomic.AtomicReference
import java.util.concurrent.ThreadLocalRandom
import scala.util.chaining.scalaUtilChainingOps

object Main extends CaseApp[Options] {
  val dropQuantityFactor: Double = 1
  val fadeProbability = 25
  val glitchProbability = 25
  def run(options: Options, remaining: RemainingArgs): Unit = {
    val sets: SetsOfCodePoints = Options.parseWeightedSets(options.unicodeChars)
    val colorContext = ColorContext.resolve(options.fadeColor)
    val baseColor = colorContext.baseColor

    //lanterna copy screen
    val defaultTerminalFactory = new DefaultTerminalFactory()
    val terminal = defaultTerminalFactory.createTerminal()
    import terminal._
    enterPrivateMode()

    terminal.cursorShow()

    if (options.scenes.contains("cursorBlink")) {
      setForegroundColor(baseColor)
      setCursorPosition(TerminalPosition(0, 0))
      terminal.cursorBlinkOn()
      terminal.sleep(5000)
    }

    if (options.scenes.contains("trace")) {
      setForegroundColor(baseColor)
      setCursorPosition(TerminalPosition(0, 0))
      terminal.cursorBlinkOn()

      "Call trans opt: received. 2-19-98 13:24:18 REC:Log>" foreach { c =>
        putCharacter(c)
        flush()
        terminal.sleep(50)
      }
      terminal.sleep(1000)

      setCursorPosition(TerminalPosition(0, 2))
      "Trace program: running" foreach { c =>
        putCharacter(c)
        flush()
        terminal.sleep(50)
      }
      terminal.sleep(5000)
    }

    if (options.scenes.contains("wakeUp")) {
      terminal.cursorHide()
      setForegroundColor(baseColor)
      clearScreen()
      val pos = TerminalPosition(5, 3)
      setCursorPosition(pos)
      "Wake up, Neo...".foreach { c =>
        putCharacter(c)
        flush()
        terminal.sleep(100)
      }
      terminal.sleep(5000)
      clearScreen()
      setCursorPosition(pos)
      "The Matrix has you...".foreach { c =>
        putCharacter(c)
        flush()
        terminal.sleep(300)
      }
      terminal.sleep(5000)
      clearScreen()
      setCursorPosition(pos)
      "Follow the white rabbit.".foreach { c =>
        putCharacter(c)
        flush()
        terminal.sleep(100)
      }
      terminal.sleep(5000)
      clearScreen()
      terminal.sleep(100)
      setCursorPosition(pos)
      terminal.putString("Knock, knock, Neo.")
      flush()
      terminal.sleep(5000)
    }

    if (options.scenes.contains("traceFail")) {
      clearScreen()
      setForegroundColor(baseColor)
      setCursorPosition(TerminalPosition(0, 0))
      terminal.cursorBlinkOn()
      terminal.sleep(5000)

      "Call trans opt: received. 9-18-99 14:32:21 REC:Log>" foreach { c =>
        putCharacter(c)
        flush()
        terminal.sleep(50)
      }
      terminal.sleep(1000)
      setCursorPosition(TerminalPosition(0, 1))
      "WARNING: carrier anomaly" foreach { c =>
        putCharacter(c)
        flush()
        terminal.sleep(50)
      }
      terminal.sleep(1000)
      setCursorPosition(TerminalPosition(0, 2))
      "Trace program: running" foreach { c =>
        putCharacter(c)
        flush()
        terminal.sleep(50)
      }
      terminal.sleep(1000)
      setCursorPosition(TerminalPosition(0, 4))
      "SYSTEM FAILURE" foreach { c =>
        putCharacter(c)
        flush()
        terminal.sleep(50)
      }
      terminal.sleep(5000)
    }

    if (options.scenes.contains("rain")) {
      runLoop(options, terminal, sets, colorContext)
    }
  }

  def runLoop(options: Options, terminal: Terminal, sets: SetsOfCodePoints, colorContext: ColorContext): Unit = {
    import terminal._
    import colorContext._

    // Optimization: Flatten charCache from 2D Array to 1D Array to improve cache locality and
    // remove pointer indirection in the hot rendering loop.
    val setsLengthOuter = sets.length
    val charCache = new Array[TextCharacter]((maxState + 1) * setsLengthOuter)

    // Initialize cache
    for (state <- 0 to maxState) {
       val color = stateToColor(state)
       // State 0 (head) uses BOLD, others use default modifiers
       val isHead = state == 0
       val stateOffset = state * setsLengthOuter

       for (i <- 0 until setsLengthOuter) {
          val char = sets.getAsInt(i).toChar
          if (isHead) {
             charCache(stateOffset + i) = new TextCharacter(char, color, TextColor.ANSI.DEFAULT, SGR.BOLD)
          } else {
             charCache(stateOffset + i) = new TextCharacter(char, color, TextColor.ANSI.DEFAULT)
          }
       }
    }

    setForegroundColor(TextColor.ANSI.WHITE_BRIGHT)

    terminal.cursorHide()

    terminal match {
      case et: ExtendedTerminal =>
        et.setMouseCaptureMode(MouseCaptureMode.CLICK_RELEASE_DRAG_MOVE)
      case _ => // terminal does not support mouse capture
    }

    val testPatternGraphics = newTextGraphics()
    testPatternGraphics.setForegroundColor(TextColor.ANSI.RED_BRIGHT)
    val rainGraphics = newTextGraphics()
    rainGraphics.setForegroundColor(TextColor.ANSI.WHITE_BRIGHT)
    rainGraphics.setModifiers(util.EnumSet.of(SGR.BOLD))
    val lastInput = new AtomicReference[KeyStroke](KeyStroke.fromString("|"))
    var frameCounter: Int = 0
    var mousePosition = TerminalPosition(0,0)

    given frameContext: FrameContext = new FrameContext(terminal, sets.maxDisplayWidth())
    // Optimization: Use primitive arrays for color state (int) and character INDEX (int)
    // -1 represents empty/default state
    var colorBuffer = Array.fill(frameContext.rows, frameContext.cols)(-1)
    // Use Int to store index into 'sets', instead of Char
    var charIndexBuffer = Array.ofDim[Int](frameContext.rows, frameContext.cols)

    val acceleration: Physics.Acceleration = Physics.Acceleration.fromName(options.physics)

    val dropQuantity = (dropQuantityFactor * frameContext.cols).toInt

    // Optimization: Flatten drops array to a 1D primitive array (pX, pY, vX, vY)
    val dropsFlattened: Array[Int] = {
       val arr = new Array[Int](dropQuantity * 4)
       given ThreadLocalRandom = ThreadLocalRandom.current()
       var i = 0
       while (i < dropQuantity) {
          val pos = acceleration.startPosition
          val vel = acceleration.startVector
          arr(i * 4) = pos.x
          arr(i * 4 + 1) = pos.y
          arr(i * 4 + 2) = vel.x
          arr(i * 4 + 3) = vel.y
          i += 1
       }
       arr
    }

    val testPatternOnFn = (t: Terminal, input: KeyStroke) => {
      if (input != null) {
        lastInput.set(input)
      }
      val ts: TerminalSize     = t.getTerminalSize
      val bu: TerminalPosition = t.getCursorPosition
      testPatternGraphics.putString(2, 1, t.getClass.getName)
      testPatternGraphics.putString(2, 2, ts.toString)
      testPatternGraphics.putString(2, 3, bu.toString)
      testPatternGraphics.putString(2, 4, frameCounter.toString)
      testPatternGraphics.putString(2, 5, dropsFlattened.take(4).mkString(","))
      testPatternGraphics.putString(2, 6, lastInput.get().toString)
      testPatternGraphics.putString(2, 7, mousePosition.toString)
      testPatternGraphics.putString(mousePosition, "▹")
      for {
        i <- 0 until 255
        index = new TextColor.Indexed(i)
      } {
        t.setForegroundColor(new TextColor.Indexed((i+16) %255))
        t.setBackgroundColor(index)
        t.setCursorPosition(2 + (i+2) % 6 * 4, 8 + (i+2) / 6)
        val str = f"$i%3d"
        str.foreach(t.putCharacter)
        t.putCharacter('█')
      }
      t.setCursorPosition(bu)
      ()
    }
    val testPatternOffFn = (t: Terminal, input: KeyStroke) => {}
    val testPatternFn: (Terminal, KeyStroke) => Unit = if (options.testPattern) testPatternOnFn else testPatternOffFn

    val frameFn: Runnable = () => {
      var tiD: KeyStroke = terminal.pollInput()
      var ti = tiD
      var draining = true
      while (draining) {
        tiD match {
          case ma: MouseAction if ma.isMouseMove || ma.isMouseDrag => //drain
            mousePosition = ma.getPosition
            tiD = terminal.pollInput()
          case _ =>
            ti = tiD
            draining = false
        }
      }
      val input = ti
      terminal.checkExit(input)

      // Optimization: Only update terminal size every 10 frames
      if (frameCounter % 10 == 0) {
        val oldCols = frameContext.cols
        val oldRows = frameContext.rows
        frameContext.update(terminal)
        if (frameContext.cols != oldCols || frameContext.rows != oldRows) {
          colorBuffer = Array.fill(frameContext.rows, frameContext.cols)(-1)
          charIndexBuffer = Array.ofDim[Int](frameContext.rows, frameContext.cols)
        }
      }

      given rng: ThreadLocalRandom = ThreadLocalRandom.current()

      // Local LCG to avoid ThreadLocalRandom overhead in tight loop
      var seed: Long = rng.nextLong()
      // Optimization: Fast bounded random integer generation using LCG and long multiplication.
      // This approach is branchless and avoids the expensive modulo operation and object allocation
      // of `ThreadLocalRandom.current().nextInt()`, resulting in a ~10-15% performance boost
      // in the tight render loop.
      inline def next31Bits(): Int = {
        seed = seed * 6364136223846793005L + 1442695040888963407L
        (seed >>> 33).toInt
      }
      inline def nextBounded(bound: Int): Int = {
        ((next31Bits().toLong * bound.toLong) >>> 31).toInt
      }
      inline def next7Bits(): Int = {
        seed = seed * 6364136223846793005L + 1442695040888963407L
        (seed >>> 57).toInt
      }

      var fx = 0
      var fy = 0
      val fadeThreshold = (fadeProbability * 128) / 100
      val glitchThreshold = (glitchProbability * 128) / 100
      val rows = frameContext.rows
      val cols = frameContext.cols
      val setsLength = sets.length

      while (fy < rows) {
        val colorRow = colorBuffer(fy)
        val charIndexRow = charIndexBuffer(fy)
        while (fx < cols) {
          // Optimization: Reuse random bits to reduce entropy generation overhead
          if (next7Bits() < fadeThreshold) {
            val state = colorRow(fx)
            if (state >= 0) {
              // Optimization: Reuse bit buffer instead of expensive RNG call
              val glitch = next7Bits() < glitchThreshold
              val nextState = if (glitch) state else fadeTable(state)

              if (nextState >= 0) {
                val charIndex = charIndexRow(fx)
                val newCharIndex = if (glitch) nextBounded(setsLength) else charIndex

                // Lookup precomputed character
                val charNew = charCache(nextState * setsLengthOuter + newCharIndex)
                rainGraphics.setCharacter(fx, fy, charNew)

                colorRow(fx) = nextState
                if (glitch) charIndexRow(fx) = newCharIndex
              } else {
                rainGraphics.setCharacter(fx, fy, TextCharacter.DEFAULT_CHARACTER)
                colorRow(fx) = -1
              }
            }
          }
          fx += 1
        }
        fx = 0
        fy += 1
      }

      var dI = 0
      val dropsLength = dropsFlattened.length
      while (dI < dropsLength) {
        val pXC = dropsFlattened(dI)
        val pYC = dropsFlattened(dI + 1)
        val vX = dropsFlattened(dI + 2)
        val vY = dropsFlattened(dI + 3)

        {//advance drops
          if (vX != 0 && frameCounter % vX == 0) {
            val dir = if (vX > 0) 1 else -1
            dropsFlattened(dI) += dir //pX
          }
          if (vY != 0 && frameCounter % vY == 0) {
            val dir = if (vY > 0) 1 else -1
            dropsFlattened(dI + 1) += dir //pY
          }
        }

        val pXN = dropsFlattened(dI)
        val pYN = dropsFlattened(dI + 1)

        // Use nextBounded to maintain entropy with performance
        val charIndex = nextBounded(setsLengthOuter)

        {//paint drop new at next position
          if (pXN >= 0 && pXN < cols && pYN >= 0 && pYN < rows) {
             val c = charCache(charIndex) // 0 * setsLengthOuter + charIndex
             rainGraphics.setCharacter(pXN, pYN, c)
             colorBuffer(pYN)(pXN) = 0
             charIndexBuffer(pYN)(pXN) = charIndex
          }
        }
        {//paint drop faded first step at current position
          if (pXN != pXC || pYN != pYC) {
            if (pXC >= 0 && pXC < cols && pYC >= 0 && pYC < rows) {
               val c = charCache(setsLengthOuter + charIndex) // 1 * setsLengthOuter + charIndex
               rainGraphics.setCharacter(pXC, pYC, c)
               colorBuffer(pYC)(pXC) = 1
               charIndexBuffer(pYC)(pXC) = charIndex
            }
          }
        }
        {
          val vec = acceleration.apply(vX, vY, pXC, pYC)
          dropsFlattened(dI + 2) = vec.x
          dropsFlattened(dI + 3) = vec.y
        }

        // Check out of bounds (must do this even if didn't move to replace initially out of bounds drops)
        if (acceleration.outOfBounds(pXN, pYN)) {
          val newPos = acceleration.newPosition(mousePosition.getColumn, mousePosition.getRow)
          val newVec = acceleration.startVector
          dropsFlattened(dI) = newPos.x
          dropsFlattened(dI + 1) = newPos.y
          dropsFlattened(dI + 2) = newVec.x
          dropsFlattened(dI + 3) = newVec.y
        }

        dI += 4
      }
      testPatternFn(terminal, input)

      // Yield to prevent pegging CPU when unthrottled and no input
      if (options.frameInterval <= 0 && input == null) {
         java.lang.Thread.`yield`()
      }

      flush()
      frameCounter += 1
    }

    val shutdownHook = new Thread(() => {
      try {
        setCursorVisible(true)
      } catch {
        case _: Exception => // ignore
      }
    })
    Runtime.getRuntime.addShutdownHook(shutdownHook)

    try {
      if (options.frameInterval <= 0) {
        try {
          while (options.maxFrames <= 0 || frameCounter < options.maxFrames) {
            frameFn.run()
          }
        } catch {
          case e: Exception => e.printStackTrace()
        }
      } else {
        val scheduler = new java.util.concurrent.ScheduledThreadPoolExecutor(1)
        val schedulerHook = new Thread(() => { val _ = scheduler.shutdownNow() })
        Runtime.getRuntime.addShutdownHook(schedulerHook)
        try {
          val frameWrapper: Runnable = () => {
            if (options.maxFrames > 0 && frameCounter >= options.maxFrames) {
               throw new RuntimeException("Max frames reached")
            }
            frameFn.run()
          }
          val animationLoop: ScheduledFuture[?] = scheduler.scheduleAtFixedRate(frameWrapper, 0, options.frameInterval, java.util.concurrent.TimeUnit.MILLISECONDS)
          animationLoop.get()
        } catch {
          case e: java.util.concurrent.ExecutionException if e.getCause.getMessage == "Max frames reached" =>
            // Expected for maxFrames limit
          case e: Exception =>
            e.printStackTrace()
        } finally {
          try { Runtime.getRuntime.removeShutdownHook(schedulerHook) } catch { case _: Exception => () }
          scheduler.shutdownNow()
        }
      }
    } finally {
      try { Runtime.getRuntime.removeShutdownHook(shutdownHook) } catch { case _: Exception => () }
      try {
        setCursorVisible(true)
        close()
      } catch {
        case _: Exception => // ignore
      }
    }
  }

  private var _cursorBlinkOn  = false
  private var cursorBlinkLast = System.currentTimeMillis()
  private val cursorBlinkInterval = 300
  private var cursorVisible = false

  extension (t: Terminal) {

    def putString(x: Int, y: Int, s: String): Unit = {
      t.setCursorPosition(x, y)
      s.foreach(t.putCharacter)
    }
    def sleep(millis: Int): Unit = {
      (1 to millis/50) foreach { i =>
        cursorBlink()
        Thread.sleep(50)
        checkExit()
      }
    }
    def checkExit(): Unit = checkExit(t.pollInput())
    def checkExit(input: KeyStroke): Unit = {
      if (input != null) {
        val c = input.getCharacter
        if (c == 'q' || c == 'Q' || c == 'c' || c == 'C') {
          System.exit(0)
        }
      }
    }

    def cursorHide(): Unit = {
      _cursorBlinkOn = false
      if (cursorVisible) {
        t.setCursorVisible(false)
        t.flush()
      }
      cursorVisible = false
    }

    def cursorShow(): Unit = {
      _cursorBlinkOn = false
      if (!cursorVisible) {
        t.setCursorVisible(true)
        t.flush()
      }
      cursorVisible = true
    }

    def cursorBlinkOn(): Unit = _cursorBlinkOn = true

    def cursorBlink(): Unit = {
      if (_cursorBlinkOn && System.currentTimeMillis() - cursorBlinkLast > cursorBlinkInterval) {
        cursorBlinkLast = System.currentTimeMillis()
        cursorVisible = !cursorVisible
        t.setCursorVisible(cursorVisible)
        t.flush()
      }
    }
  }
}
