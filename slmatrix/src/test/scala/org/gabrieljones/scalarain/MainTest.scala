package org.gabrieljones.scalarain

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions._
import com.googlecode.lanterna.TextColor

class MainTest {

  @Test
  def testStateTables(): Unit = {
    val context = ColorContext.resolve("green")

    // Verify state 0 is White
    assertEquals(TextColor.ANSI.WHITE_BRIGHT, context.stateToColor(0))

    // Verify transition 0 -> 1
    assertEquals(1, context.fadeTable(0))

    // Verify state 1 is mapped correctly
    val fadedWhite = context.stateToColor(1)
    assertNotNull(fadedWhite)

    // Verify last state transition to -1
    val lastState = context.maxState
    assertEquals(-1, context.fadeTable(lastState))
  }
}
