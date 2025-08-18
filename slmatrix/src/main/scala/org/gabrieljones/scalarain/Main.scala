package org.gabrieljones.scalarain

import caseapp._
import com.googlecode.lanterna.input.KeyStroke
import com.googlecode.lanterna.terminal.{DefaultTerminalFactory, Terminal}
import com.googlecode.lanterna.*
import Options._

import java.util
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.atomic.AtomicReference
import scala.util.Random
import scala.util.chaining.scalaUtilChainingOps

object Main extends CaseApp[Options] {
  val dropQuantityFactor: Double = 1
  val frameInterval = 50
  val fadeProbability = 25
  val glitchProbability = 25
  val sets: Array[Int] = Array(
    0x30A0 to 0x30FF, //katakana unicode range
    0xFF10 to 0xFF19, //full width numbers
//    0x1F000 to 0x1FAFF, //emoji unicode range
//    0x41 to 0x5A, //ascii capital letters
//    0x2200 to 0x22FF, //math symbols
//    0x21 to 0x2F, //ascii punctuation
//    0xFF66 to 0xFF9D, //half width katakana
//    0x30 to 0x39, //ascii numbers
  )
    .flatten

  def charFromSet: Char = sets(Random.nextInt(sets.length)).toChar

  def run(options: Options, remaining: RemainingArgs): Unit = {

    //lanterna copy screen
    val defaultTerminalFactory = new DefaultTerminalFactory()
    val terminal = defaultTerminalFactory.createTerminal()
    import terminal._
    enterPrivateMode()


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
    val dropQuantity = (dropQuantityFactor * terminalSizeColumns).toInt
    val drops: Array[Array[Int]] = Array.fill(dropQuantity) {
      newDrop(new Array[Int](5), terminalSizeColumns)//.tap(_(1) = Random.nextInt(terminalsSizeRows))
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
      val terminalSize = getTerminalSize
      val terminalSizeColumns = terminalSize.getColumns
      val terminalSizeRows = terminalSize.getRows
      var fx = 0
      var fy = 0
      while (fy < terminalSizeRows) {
        while (fx < terminalSizeColumns) {
          val charCur = rainGraphics.getCharacter(fx, fy)
          if (charCur != null && charCur != TextCharacter.DEFAULT_CHARACTER && Random.nextInt(100) < fadeProbability) {
            val colorCur = charCur.getForegroundColor
            val glitchInsteadOfFade = Random.nextInt(100) < glitchProbability
            val colorNew = if (glitchInsteadOfFade) colorCur else fade(colorCur)
            if (colorNew.getGreen > 1) {
              val charGlitched = if (glitchInsteadOfFade) charFromSet else charCur.getCharacter
              val charNew = new TextCharacter(charGlitched, colorNew, charCur.getBackgroundColor)
              rainGraphics.setCharacter(fx, fy, charNew)
            } else {
              rainGraphics.setCharacter(fx, fy, TextCharacter.DEFAULT_CHARACTER)
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
        val char = charFromSet
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
            rainGraphics.setCharacter(pXC, pYC, new TextCharacter(char, fade(TextColor.ANSI.WHITE_BRIGHT), TextColor.ANSI.DEFAULT))
          }
        }
        {// randomly accelerate
          val vvY   = Random.between(-32, 32) / 31 //accelerate = -1, 0, or 1, make changes less likely
          val vYNew = vY + vvY
          if (vYNew > 0 && vYNew < 32) { //if new velocity is in bounds update
            drop(3) = vYNew
          }
//          {// accelerate in x dimension, wind
//            val vvX   = Random.between(-32, 32) / 31 //accelerate = -1, 0, or 1, make changes less likely
//            val vXNew = vX + vvX
//            if (vXNew > -3 && vXNew < 0) {
//              drop(2) = vXNew
//            }
//          }
        }
        {//if drop is off-screen then replace with new drop
          if (drop(0) < 0 || drop(1) < 0 || drop(0) > terminalSizeColumns || drop(1) > terminalSizeRows) {
            newDrop(drop, terminalSizeColumns)
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

  def newDrop(drop: Array[Int], terminalSizeColumns: Int): Array[Int] = {
    drop(0) = Random.nextInt(terminalSizeColumns - 1)/ 2 * 2
    drop(1) = 0
    drop(2) = 0 //Random.nextInt(5) - 3
    drop(3) = Math.min(Random.nextInt(8) + 1, Random.nextInt(8) + 1)
    drop(4) = Random.nextInt(2)
    drop
  }

  val colorMap = new util.HashMap[TextColor, TextColor]()
  val colorBase = 46
//  val colorBase = 226
  ((15 to 15) ++ (colorBase to (colorBase - 30) by -6)).map(i => new TextColor.Indexed(i)).sliding(2).foreach { case Seq(a, b) => colorMap.put(a, b) }
  colorMap.put(TextColor.ANSI.WHITE_BRIGHT, new TextColor.Indexed(colorBase))
  def fade(color: TextColor): TextColor = {
    if (colorMap.containsKey(color)) {
      colorMap.get(color)
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
