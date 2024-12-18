package org.gabrieljones.scalarain

import com.googlecode.lanterna.input.KeyStroke
import com.googlecode.lanterna.terminal.{DefaultTerminalFactory, Terminal}
import com.googlecode.lanterna.*

import java.util
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.atomic.AtomicReference
import scala.util.Random
import scala.util.chaining.scalaUtilChainingOps

object Main {
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

  object flags {
    val intro = false
  }
  def main(args: Array[String]): Unit = {

    //lanterna copy screen
    val defaultTerminalFactory = new DefaultTerminalFactory()
    val terminal = defaultTerminalFactory.createTerminal()
    terminal.enterPrivateMode()
    terminal.setCursorVisible(false)

    if (flags.intro) {
      val pos = TerminalPosition(5, 3)
      terminal.setForegroundColor(TextColor.ANSI.GREEN_BRIGHT)
      terminal.setCursorPosition(pos)
      "Wake up, Neo...".foreach { c =>
        terminal.putCharacter(c)
        terminal.flush()
        Thread.sleep(100)
      }
      Thread.sleep(5000)
      terminal.clearScreen()
      terminal.setCursorPosition(pos)
      "The Matrix has you...".foreach { c =>
        terminal.putCharacter(c)
        terminal.flush()
        Thread.sleep(300)
      }
      Thread.sleep(5000)
      terminal.clearScreen()
      terminal.setCursorPosition(pos)
      "Follow the white rabbit.".foreach { c =>
        terminal.putCharacter(c)
        terminal.flush()
        Thread.sleep(100)
      }
      Thread.sleep(5000)
      terminal.clearScreen()
      Thread.sleep(100)
      terminal.setCursorPosition(pos)
      terminal.putString("Knock, knock, Neo.")
      terminal.flush()
      Thread.sleep(5000)
      terminal.clearScreen()
    }

    terminal.setForegroundColor(TextColor.ANSI.WHITE_BRIGHT)
    terminal.setCursorVisible(false)

    //frame interval with scheduler
    val scheduler = new java.util.concurrent.ScheduledThreadPoolExecutor(1)
    val debugGraphics = terminal.newTextGraphics()
    debugGraphics.setForegroundColor(TextColor.ANSI.RED_BRIGHT)
    val rainGraphics = terminal.newTextGraphics()
    rainGraphics.setForegroundColor(TextColor.ANSI.WHITE_BRIGHT)
    rainGraphics.setModifiers(util.EnumSet.of(SGR.BOLD))
    val lastInput = new AtomicReference[KeyStroke](KeyStroke.fromString("|"))
    var frameCounter: Int = 0
    val terminalSize: TerminalSize = terminal.getTerminalSize
    val terminalSizeColumns = terminalSize.getColumns
    val terminalsSizeRows   = terminalSize.getRows
    val dropQuantity = (dropQuantityFactor * terminalSizeColumns).toInt
    val drops: Array[Array[Int]] = Array.fill(dropQuantity) {
      newDrop(new Array[Int](5), terminalSizeColumns).tap(_(1) = Random.nextInt(terminalsSizeRows))
    }
    val debugOn = (t: Terminal, input: KeyStroke) => {
      if (input != null) {
        lastInput.set(input)
      }
      val ts: TerminalSize     = t.getTerminalSize
      val bu: TerminalPosition = t.getCursorPosition
      debugGraphics.putString(2, 1, t.getClass.getName)
      debugGraphics.putString(2, 2, ts.toString)
      debugGraphics.putString(2, 3, bu.toString)
      debugGraphics.putString(2, 4, frameCounter.toString)
      debugGraphics.putString(2, 5, drops(0).mkString(","))
      debugGraphics.putString(2, 6, lastInput.get().toString)
      for {
        i <- 0 until 255
        index = new TextColor.Indexed(i)
      } {
        t.setForegroundColor(index)
        t.setCursorPosition(2 + (i+2) % 6 * 4, 8 + (i+2) / 6)
        t.putCharacter('█')
        val str = f"$i%3d"
        str.foreach(t.putCharacter)
      }
      t.setCursorPosition(bu)
      ()
    }
    val debugOff = (t: Terminal, input: KeyStroke) => {}
    val debug: (Terminal, KeyStroke) => Unit  = debugOn
    //Array(positionX, positionY, velocityX, velocityY, color)
    val frameFn: Runnable = () => {
      val input = terminal.pollInput()
      checkExit(input)
      val terminalSize = terminal.getTerminalSize
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
//      debug(terminal, input)
      terminal.flush()
      frameCounter += 1
    }
    val animationLoop: ScheduledFuture[?] = scheduler.scheduleAtFixedRate(frameFn, 0, frameInterval, java.util.concurrent.TimeUnit.MILLISECONDS)
    //shutdown handler
    Runtime.getRuntime.addShutdownHook(new Thread(() => {
      scheduler.shutdown()
      terminal.setCursorVisible(true)
    }))
    try {
      animationLoop.get()
    } catch {
      case e: Exception =>
        terminal.close()
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

  def checkExit(input: KeyStroke): Unit = {
    if (input != null) {
      val c = input.getCharacter
      if (c == 'q' || c == 'Q' || c == 'c' || c == 'C') {
        System.exit(0)
      }
    }
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

  extension (t: Terminal) {
    def putString(x: Int, y: Int, s: String): Unit = {
      t.setCursorPosition(x, y)
      s.foreach(t.putCharacter)
    }
  }
}
