package org.gabrieljones.scalarain

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.util.concurrent.ThreadLocalRandom
import org.gabrieljones.scalarain.CodePointSyntax.*

class CodePointSyntaxTest {

  @Test
  def testGetDisplayWidth(): Unit = {
    // Zero-width characters
    assertEquals(0, 0x0.asSetsOfCodePoints.get(0).getDisplayWidth)
    assertEquals(0, 0x0300.asSetsOfCodePoints.get(0).getDisplayWidth) // Combining Grave Accent (NON_SPACING_MARK)
    assertEquals(0, 0x200B.asSetsOfCodePoints.get(0).getDisplayWidth) // Zero Width Space

    // Wide characters
    assertEquals(2, 0x3041.asSetsOfCodePoints.get(0).getDisplayWidth) // Hiragana Letter A
    assertEquals(2, 0x1F600.asSetsOfCodePoints.get(0).getDisplayWidth) // Grinning Face Emoji
    assertEquals(2, 0xFF01.asSetsOfCodePoints.get(0).getDisplayWidth) // Fullwidth Exclamation Mark

    // Standard width
    assertEquals(1, 'A'.toInt.asSetsOfCodePoints.get(0).getDisplayWidth)
    assertEquals(1, ' '.toInt.asSetsOfCodePoints.get(0).getDisplayWidth)
  }

  @Test
  def testSetsOfCodePointsMethods(): Unit = {
    val array = Array('A'.toInt, 'B'.toInt, 'C'.toInt, 0x3041)
    val sets = array.asInstanceOf[SetsOfCodePoints]

    assertEquals('A'.toInt, sets.get(0))
    assertEquals('B'.toInt, sets.getAsInt(1))
    assertEquals(4, sets.length)
    assertArrayEquals(array, sets.unwrap)

    assertTrue(sets.contains('A'.toInt))
    assertFalse(sets.contains('Z'.toInt))

    assertEquals(1, sets.count(_ == 0x3041))
    assertEquals(3, sets.count(_ < 0x3041))

    // maxDisplayWidth: A(1), B(1), C(1), 0x3041(2) -> max 2
    assertEquals(2, sets.maxDisplayWidth())
  }

  @Test
  def testRandomChar(): Unit = {
    val array = Array('A'.toInt, 'B'.toInt, 'C'.toInt)
    val sets = array.asInstanceOf[SetsOfCodePoints]
    given rng: ThreadLocalRandom = ThreadLocalRandom.current()

    val random = sets.randomChar
    assertTrue(array.contains(random.toInt))
  }

  @Test
  def testAsSetsOfCodePoints(): Unit = {
    val array = Array(1, 2, 3)
    val sets = array.asInstanceOf[SetsOfCodePoints]
    assertEquals(3, sets.length)
  }

  extension (i: Int) {
    def asSetsOfCodePoints: SetsOfCodePoints = Array(i).asInstanceOf[SetsOfCodePoints]
  }
}
