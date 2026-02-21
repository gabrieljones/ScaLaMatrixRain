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

    //lanterna copy screen
    val defaultTerminalFactory = new DefaultTerminalFactory()
    val terminal = defaultTerminalFactory.createTerminal()
    import terminal._
    enterPrivateMode()

    terminal.cursorShow()

    if (options.scenes.contains("cursorBlink")) {
      setForegroundColor(TextColor.ANSI.GREEN_BRIGHT)
      setCursorPosition(TerminalPosition(0, 0))
      terminal.cursorBlinkOn()
      terminal.sleep(5000)
    }

    if (options.scenes.contains("trace")) {
      setForegroundColor(TextColor.ANSI.GREEN_BRIGHT)
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
      setForegroundColor(TextColor.ANSI.GREEN_BRIGHT)
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
      setForegroundColor(TextColor.ANSI.GREEN_BRIGHT)
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
      runLoop(options, terminal, sets)
    }
  }

  def runLoop(options: Options, terminal: Terminal, sets: SetsOfCodePoints): Unit = {
    import terminal._

    // Optimization: Precompute faded white color and TextCharacters to avoid repeated allocations and lookups
    // Cache dimensions: [State][CharIndex]
    val charCache = Array.ofDim[TextCharacter](maxState + 1, sets.length)

    // Initialize cache
    for (state <- 0 to maxState) {
       val color = stateToColor(state)
       // State 0 (head) uses BOLD, others use default modifiers
       val isHead = state == 0

       for (i <- 0 until sets.length) {
          val char = sets.getAsInt(i).toChar
          if (isHead) {
             charCache(state)(i) = new TextCharacter(char, color, TextColor.ANSI.DEFAULT, SGR.BOLD)
          } else {
             charCache(state)(i) = new TextCharacter(char, color, TextColor.ANSI.DEFAULT)
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

    def updateChar(x: Int, y: Int, charIndex: Int, state: Int): Unit = {
      if (x >= 0 && x < frameContext.cols && y >= 0 && y < frameContext.rows) {
        val c = charCache(state)(charIndex)
        rainGraphics.setCharacter(x, y, c)

        colorBuffer(y)(x) = state
        if (state >= 0) {
           charIndexBuffer(y)(x) = charIndex
        }
      }
    }

    val acceleration: Physics.Acceleration = options.physics match {
      case "rain"   => Physics.Acceleration.Rain
      case "spiral" => Physics.Acceleration.Spiral(-1.4)
      case "warp"   => Physics.Acceleration.Warp
    }

    val dropQuantity = (dropQuantityFactor * frameContext.cols).toInt
    val drops: Array[Array[Int]] = Array.fill(dropQuantity) {
      given ThreadLocalRandom = ThreadLocalRandom.current()
      newDrop(
        new Array[Int](5),
        acceleration.startPosition,
        acceleration.startVector,
      )//.tap(_(1) = ThreadLocalRandom.current().nextInt(terminalsSizeRows))
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
      testPatternGraphics.putString(2, 5, drops(0).mkString(","))
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
      var fx = 0
      var fy = 0
      val fadeThreshold = (fadeProbability * 128) / 100
      val glitchThreshold = (glitchProbability * 128) / 100
      while (fy < frameContext.rows) {
        val colorRow = colorBuffer(fy)
        val charIndexRow = charIndexBuffer(fy)
        while (fx < frameContext.cols) {
          // Optimization: Use bitwise mask (0..127) to approximate probability check
          if ((rng.nextInt() & 127) < fadeThreshold) {
            val state = colorRow(fx)
            if (state >= 0) {
              val glitch = (rng.nextInt() & 127) < glitchThreshold
              val nextState = if (glitch) state else fadeTable(state)

              if (nextState >= 0) {
                val charIndex = charIndexRow(fx)
                val newCharIndex = if (glitch) rng.nextInt(sets.length) else charIndex

                // Lookup precomputed character
                val charNew = charCache(nextState)(newCharIndex)
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
      while (dI < drops.length) {
        val drop = drops(dI)
        val pXC = drop(0)
        val pYC = drop(1)
        val vX = drop(2)
        val vY = drop(3)
        val c  = drop(4)
        // Optimization: Generate index once to lookup both char and precomputed trail character
        val charIndex = rng.nextInt(sets.length)

        {//advance drops
          if (vX != 0 && frameCounter % vX == 0) {
            val dir = if (vX > 0) 1 else -1
            drop(0) += dir //pX
          }
          if (vY != 0 && frameCounter % vY == 0) {
            val dir = if (vY > 0) 1 else -1
            drop(1) += dir //pY
          }
        }
        {//paint drop new at next position
          val pXN = drop(0)
          val pYN = drop(1)
          updateChar(pXN, pYN, charIndex, 0)
        }
        {//paint drop faded first step at current position
          val pXN = drop(0)
          val pYN = drop(1)
          if (pXN != pXC || pYN != pYC) {
            updateChar(pXC, pYC, charIndex, 1)
          }
        }
        {
          val vec = acceleration.apply(vX, vY, pXC, pYC)
          drop(2) = vec.x
          drop(3) = vec.y
        }
        {//if drop is off-screen then replace with new drop
          if (acceleration.outOfBounds(drop(0), drop(1))) {
            newDrop(drop, acceleration.newPosition(mousePosition.getColumn, mousePosition.getRow), acceleration.startVector)
          }
        }
        dI += 1
      }
      testPatternFn(terminal, input)
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
      Runtime.getRuntime.removeShutdownHook(shutdownHook)
      try {
        setCursorVisible(true)
        close()
      } catch {
        case _: Exception => // ignore
      }
    }
  }

  def newDrop(drop: Array[Int], pos: Vector2, vel: Vector2)(using frameContext: FrameContext, rng: ThreadLocalRandom): Array[Int] = {
    drop(0) = pos.x
    drop(1) = pos.y
    drop(2) = vel.x
    drop(3) = vel.y
    drop(4) = rng.nextInt(2)
    drop
  }

  // Optimization: Pre-computed state transition tables for fast lookups.
  // Represents color lifecycle: White -> Faded... -> Empty (-1).
  val colorBase = 46
  val fadeStates: IndexedSeq[TextColor] = (colorBase to (colorBase - 30) by -6).map(i => new TextColor.Indexed(i))
  val maxState: Int = fadeStates.length

  val fadeTable: Array[Int] = new Array[Int](maxState + 1)
  val stateToColor: Array[TextColor] = new Array[TextColor](maxState + 1)
  val colorToState: java.util.IdentityHashMap[TextColor, java.lang.Integer] = new java.util.IdentityHashMap[TextColor, java.lang.Integer]()

  // Initialize state tables
  // State 0 is White Bright (Head)
  stateToColor(0) = TextColor.ANSI.WHITE_BRIGHT
  colorToState.put(TextColor.ANSI.WHITE_BRIGHT, 0)
  fadeTable(0) = 1 // Transitions to State 1

  // States 1..N are the fade sequence
  fadeStates.zipWithIndex.foreach { case (color, idx) =>
    val state = idx + 1
    stateToColor(state) = color
    colorToState.put(color, state)

    // Calculate transition to next state
    if (state < maxState) {
      fadeTable(state) = state + 1
    } else {
      fadeTable(state) = -1 // End of lifecycle (Empty)
    }
  }

  // Map clear/default colors to -1 (Empty)
  colorToState.put(TextColor.ANSI.DEFAULT, -1)
  colorToState.put(TextColor.ANSI.RED, -1)


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
