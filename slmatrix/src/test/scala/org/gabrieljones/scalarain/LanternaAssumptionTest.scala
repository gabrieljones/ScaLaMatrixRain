package org.gabrieljones.scalarain

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions._
import com.googlecode.lanterna.TextColor

class LanternaAssumptionTest {

  @Test
  def testIndexedColorHashCode(): Unit = {
    // Verify the assumption that TextColor.Indexed.hashCode() is linear
    // This optimization depends on this internal implementation detail of Lanterna 3.1.3
    val offset = new TextColor.Indexed(0).hashCode()
    for (i <- 0 until 256) {
      val color = new TextColor.Indexed(i)
      assertEquals(i + offset, color.hashCode(), s"HashCode for Indexed($i) should be ${i + offset}")
    }
  }
}
