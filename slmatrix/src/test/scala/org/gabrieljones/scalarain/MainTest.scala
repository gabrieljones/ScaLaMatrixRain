package org.gabrieljones.scalarain

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions._
import com.googlecode.lanterna.TextColor

class MainTest {

  @Test
  def testFade(): Unit = {
    // Verify fade logic for known colors
    val white = TextColor.ANSI.WHITE_BRIGHT
    val fadedWhite = Main.fade(white)
    assertNotNull(fadedWhite)
    assertNotEquals(TextColor.ANSI.RED, fadedWhite, "White should fade to something other than RED")

    // Verify fade logic for unknown colors
    val unknown = TextColor.ANSI.CYAN
    val fadedUnknown = Main.fade(unknown)
    assertEquals(TextColor.ANSI.RED, fadedUnknown, "Unknown color should return RED")
  }

  @Test
  def benchmarkFade(): Unit = {
    // Warm up
    val white = TextColor.ANSI.WHITE_BRIGHT
    for (_ <- 0 until 10000) {
      Main.fade(white)
    }

    val iterations = 10_000_000
    val start = System.nanoTime()
    var i = 0
    while (i < iterations) {
      Main.fade(white)
      i += 1
    }
    val end = System.nanoTime()
    val durationMs = (end - start) / 1_000_000.0
    println(s"Fade benchmark: $durationMs ms for $iterations iterations")
  }
}
