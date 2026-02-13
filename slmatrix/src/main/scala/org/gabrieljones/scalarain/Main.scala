package org.gabrieljones.scalarain

import caseapp.*
import com.googlecode.lanterna.input.KeyStroke
import com.googlecode.lanterna.terminal.{DefaultTerminalFactory, Terminal}
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
  val frameInterval = 50
  val fadeProbability = 25
  val glitchProbability = 25
  def run(options: Options, remaining: RemainingArgs): Unit = {
    val sets: Array[Int] = Options.parseWeightedSets(options.unicodeChars)
    // Optimization: Precompute faded white color and trail characters to avoid repeated allocations and lookups
    val fadedWhite = fade(TextColor.ANSI.WHITE_BRIGHT)
    val trailChars = sets.map(code => new TextCharacter(code.toChar, fadedWhite, TextColor.ANSI.DEFAULT))

    def charFromSet(rng: ThreadLocalRandom): Char = sets(rng.nextInt(sets.length)).toChar

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
      //continue
    } else {
      return
    }

    //run rain scene until 'q' or 'c' is pressed

    setForegroundColor(TextColor.ANSI.WHITE_BRIGHT)

    terminal.cursorHide()

    //frame interval with scheduler
    val scheduler = new java.util.concurrent.ScheduledThreadPoolExecutor(1)
    val testPatternGraphics = newTextGraphics()
    testPatternGraphics.setForegroundColor(TextColor.ANSI.RED_BRIGHT)
    val rainGraphics = newTextGraphics()
    rainGraphics.setForegroundColor(TextColor.ANSI.WHITE_BRIGHT)
    rainGraphics.setModifiers(util.EnumSet.of(SGR.BOLD))
    val lastInput = new AtomicReference[KeyStroke](KeyStroke.fromString("|"))
    var frameCounter: Int = 0
    val terminalSize: TerminalSize = getTerminalSize
    val terminalSizeColumns = terminalSize.getColumns
    val terminalsSizeRows   = terminalSize.getRows

    // Optimization: Cache terminal size to avoid expensive native calls every frame
    var cachedTerminalSize: TerminalSize = terminalSize
    var cachedTerminalSizeColumns = terminalSizeColumns
    var cachedTerminalSizeRows = terminalsSizeRows

    val acceleration: Physics.Acceleration = options.physics match {
      case "rain"   => Physics.Acceleration.Rain(terminalSizeColumns, terminalsSizeRows)
      case "spiral" => Physics.Acceleration.Spiral(terminalSizeColumns, terminalsSizeRows, -1.4)
      case "warp"   => Physics.Acceleration.Warp(terminalSizeColumns, terminalsSizeRows)
    }

    val dropQuantity = (dropQuantityFactor * terminalSizeColumns).toInt
    val drops: Array[Array[Int]] = Array.fill(dropQuantity) {
      val rng = ThreadLocalRandom.current()
      newDrop(new Array[Int](5), acceleration.startPosition(rng), acceleration.startVector(rng), terminalSizeColumns, terminalsSizeRows, rng)//.tap(_(1) = ThreadLocalRandom.current().nextInt(terminalsSizeRows))
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
    //Array(positionX, positionY, velocityX, velocityY, color)
    val frameFn: Runnable = () => {
      val input = terminal.pollInput()
      terminal.checkExit(input)

      // Optimization: Only update terminal size every 10 frames
      if (frameCounter % 10 == 0) {
        cachedTerminalSize = getTerminalSize
        cachedTerminalSizeColumns = cachedTerminalSize.getColumns
        cachedTerminalSizeRows = cachedTerminalSize.getRows
      }
      val terminalSizeColumns = cachedTerminalSizeColumns
      val terminalSizeRows = cachedTerminalSizeRows

      val rng = ThreadLocalRandom.current()
      var fx = 0
      var fy = 0
      val fadeThreshold = (fadeProbability * 128) / 100
      val glitchThreshold = (glitchProbability * 128) / 100
      while (fy < terminalSizeRows) {
        while (fx < terminalSizeColumns) {
          // Optimization: Use bitwise mask (0..127) to approximate probability check
          // significantly faster than nextInt(100) which involves modulo
          if ((rng.nextInt() & 127) < fadeThreshold) {
            val charCur = rainGraphics.getCharacter(fx, fy)
            if (charCur != null && charCur != TextCharacter.DEFAULT_CHARACTER) {
              val colorCur = charCur.getForegroundColor
              val glitchInsteadOfFade = (rng.nextInt() & 127) < glitchThreshold
              val colorNew = if (glitchInsteadOfFade) colorCur else fade(colorCur)
              if (colorNew.getGreen > 1) {
                val charGlitched = if (glitchInsteadOfFade) charFromSet(rng) else charCur.getCharacter
                val charNew = new TextCharacter(charGlitched, colorNew, charCur.getBackgroundColor)
                rainGraphics.setCharacter(fx, fy, charNew)
              } else {
                rainGraphics.setCharacter(fx, fy, TextCharacter.DEFAULT_CHARACTER)
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
        val char = sets(charIndex).toChar
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
          rainGraphics.setCharacter(pXN, pYN, char)
        }
        {//paint drop faded first step at current position
          val pXN = drop(0)
          val pYN = drop(1)
          if (pXN != pXC || pYN != pYC) {
            rainGraphics.setCharacter(pXC, pYC, trailChars(charIndex))
          }
        }
        {
          val vec = acceleration.apply(vX, vY, pXC, pYC, rng)
          drop(2) = vec.x
          drop(3) = vec.y
        }
        {//if drop is off-screen then replace with new drop
          if (acceleration.outOfBounds(drop(0), drop(1))) {
            newDrop(drop, acceleration.newPosition(rng), acceleration.startVector(rng), terminalSizeColumns, terminalsSizeRows, rng)
          }
        }
        dI += 1
      }
      testPatternFn(terminal, input)
      flush()
      frameCounter += 1
    }
    val animationLoop: ScheduledFuture[?] = scheduler.scheduleAtFixedRate(frameFn, 0, frameInterval, java.util.concurrent.TimeUnit.MILLISECONDS)
    //shutdown handler
    Runtime.getRuntime.addShutdownHook(new Thread(() => {
      scheduler.shutdown()
      setCursorVisible(true)
    }))
    try {
      animationLoop.get()
    } catch {
      case e: Exception =>
        close()
        e.printStackTrace()
        System.exit(1)
    }
  }

  def newDrop(drop: Array[Int], pos: Vector2, vel: Vector2, terminalSizeColumns: Int, terminalSizeRows: Int, rng: ThreadLocalRandom): Array[Int] = {
    drop(0) = pos.x
    drop(1) = pos.y
    drop(2) = vel.x
    drop(3) = vel.y
    drop(4) = rng.nextInt(2)
    drop
  }

  val colorMap = new util.HashMap[TextColor, TextColor]()
  val colorBase = 46
//  val colorBase = 226
  ((15 to 15) ++ (colorBase to (colorBase - 30) by -6)).map(i => new TextColor.Indexed(i)).sliding(2).foreach { case Seq(a, b) => colorMap.put(a, b) }
  colorMap.put(TextColor.ANSI.WHITE_BRIGHT, new TextColor.Indexed(colorBase))
  def fade(color: TextColor): TextColor = {
    // Optimization: Avoid double hash lookup by using get() and checking for null
    val nextColor = colorMap.get(color)
    if (nextColor != null) {
      nextColor
    } else {
      TextColor.ANSI.RED //unexpected color map entry, return red
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
