package org.gabrieljones.scalarain

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions._
import com.googlecode.lanterna.TextColor

class MainTest {

  @Test
  def testStateTables(): Unit = {
    // Verify state 0 is White
    assertEquals(TextColor.ANSI.WHITE_BRIGHT, Main.stateToColor(0))
    assertEquals(0, Main.colorToState.get(TextColor.ANSI.WHITE_BRIGHT).intValue())

    // Verify transition 0 -> 1
    assertEquals(1, Main.fadeTable(0))

    // Verify state 1 is mapped correctly
    val fadedWhite = Main.stateToColor(1)
    assertNotNull(fadedWhite)
    assertEquals(1, Main.colorToState.get(fadedWhite).intValue())

    // Verify last state transition to -1
    val lastState = Main.maxState
    assertEquals(-1, Main.fadeTable(lastState))

    // Verify unknown colors map to -1 (via getOrDefault or logic)
    // Main.colorToState contains DEFAULT and RED mapping to -1
    assertEquals(-1, Main.colorToState.get(TextColor.ANSI.DEFAULT).intValue())
    assertEquals(-1, Main.colorToState.get(TextColor.ANSI.RED).intValue())

    // Verify truly unknown color returns null (map behavior)
    assertNull(Main.colorToState.get(TextColor.ANSI.CYAN))
  }
}
