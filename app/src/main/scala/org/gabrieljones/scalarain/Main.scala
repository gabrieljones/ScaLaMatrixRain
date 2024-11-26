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
  val dropQuantityFactor = 1
  val frameInterval = 50
  val fadeProbability = 25
  val glitchProbability = 25
  val sets      = Array(
    0x30A0 -> 0x30FF, //katakana unicode range
//    0x1F000 -> 0x1FAFF, //emoji unicode range
//    0x41 -> 0x5A, //ascii capital letters
//    0x2200 -> 0x22FF, //math symbols
//    0x21 -> 0x2F, //ascii punctuation
//    0x30 -> 0x39, //ascii numbers
  )

  def charFromSet = {
    val set = sets(Random.nextInt(sets.length))
    (Random.nextInt(set._2 - set._1) + set._1).toChar
  }
  def main(args: Array[String]): Unit = {

    //lanterna copy screen
    val defaultTerminalFactory = new DefaultTerminalFactory()
    val terminal = defaultTerminalFactory.createTerminal()
    terminal.enterPrivateMode()
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
    val drops: Array[Array[Int]] = Array.fill(dropQuantityFactor * terminal.getTerminalSize.getColumns)(newDrop(new Array[Int](5), terminal.getTerminalSize.getColumns).tap(_(1) = Random.nextInt(terminal.getTerminalSize.getRows)))
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
          if (charCur != null && charCur.getCharacter != ' ' && Random.nextInt(100) < fadeProbability) {
            val colorCur = charCur.getForegroundColor
            val glitchInsteadOfFade = Random.nextInt(100) < glitchProbability
            val colorNew = if (glitchInsteadOfFade) colorCur else fade(colorCur)
            if (colorNew.getGreen > 1) {
              val charGlitched = if (glitchInsteadOfFade) charFromSet else charCur.getCharacter
              val charNew = new TextCharacter(charGlitched, colorNew, charCur.getBackgroundColor)
              rainGraphics.setCharacter(fx, fy, charNew)
            } else {
              rainGraphics.setCharacter(fx, fy, ' ')
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
        /*
        if (c == 1) {
          rainGraphics.setCharacter(pX, pY, char)
        } else {
          rainGraphics.setCharacter(pX, pY, ' ')
        }
        */
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
          if (pXN != pYC && pYN != pYC) {
            rainGraphics.setCharacter(pXC, pYC, new TextCharacter(char, fade(TextColor.ANSI.WHITE_BRIGHT), TextColor.ANSI.DEFAULT))
          }
        }
        {// randomly accelerate
          val vvY   = Random.between(-32, 32) / 31 //accelerate = -1, 0, or 1, make changes less likely
          val vYNew = vY + vvY
          if (vYNew > 0 && vYNew < 32) { //if new velocity is in bounds update
            drop(3) = vYNew
          }
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
  ((15 to 15) ++ (46 to 16 by -6)).map(i => new TextColor.Indexed(i)).sliding(2).foreach { case Seq(a, b) => colorMap.put(a, b) }
  colorMap.put(TextColor.ANSI.WHITE_BRIGHT, new TextColor.Indexed(46))
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
