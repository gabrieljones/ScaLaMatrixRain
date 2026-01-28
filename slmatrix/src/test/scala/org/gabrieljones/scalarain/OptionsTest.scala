package org.gabrieljones.scalarain

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class OptionsTest {

  @Test
  def testDefaultLikeInput(): Unit = {
    val input = "0x30A0-0x30FF,0xFF10-0xFF19"
    val result = Options.parseWeightedSets(input)
    // 0x30A0 to 0x30FF is 96 chars.
    // 0xFF10 to 0xFF19 is 10 chars.
    // Total 106.
    assertEquals(106, result.length)
    assertTrue(result.contains(0x30A0))
    assertTrue(result.contains(0x30FF))
    assertTrue(result.contains(0xFF10))
    assertTrue(result.contains(0xFF19))
  }

  @Test
  def testWeights(): Unit = {
    val input = "ascii_uppercase:1,ascii_digits:2"
    val result = Options.parseWeightedSets(input)
    // ascii_uppercase (A-Z) is 26 chars. Weight 1.
    // ascii_digits (0-9) is 10 chars. Weight 2.
    // Total 26 + 20 = 46.
    assertEquals(46, result.length)

    val countA = result.count(_ == 'A'.toInt)
    assertEquals(1, countA)

    val count0 = result.count(_ == '0'.toInt)
    assertEquals(2, count0)
  }

  @Test
  def testSingleChars(): Unit = {
    val input = "A,B,C"
    val result = Options.parseWeightedSets(input)
    assertEquals(3, result.length)
    assertTrue(result.contains('A'.toInt))
    assertTrue(result.contains('B'.toInt))
    assertTrue(result.contains('C'.toInt))
  }

  @Test
  def testNamedSets(): Unit = {
    val input = "ascii_uppercase,ascii_digits"
    val result = Options.parseWeightedSets(input)
    assertEquals(26 + 10, result.length)
    assertTrue(result.contains('A'.toInt))
    assertTrue(result.contains('Z'.toInt))
    assertTrue(result.contains('0'.toInt))
    assertTrue(result.contains('9'.toInt))
  }

  @Test
  def testSpecificNamedSets(): Unit = {
    val katakana = Options.parseWeightedSets("katakana")
    assertEquals(96, katakana.length)
    assertTrue(katakana.contains(0x30A0))

    val digitsFullWidth = Options.parseWeightedSets("digits_full_width")
    assertEquals(10, digitsFullWidth.length)
    assertTrue(digitsFullWidth.contains(0xFF10))

    val emoji = Options.parseWeightedSets("emoji")
    assertTrue(emoji.length > 0)
    assertTrue(emoji.contains(0x1F000))

    val math = Options.parseWeightedSets("math_symbols")
    assertTrue(math.contains(0x2200))
  }

  @Test
  def testSingleCharsWithWeights(): Unit = {
    val input = "A:2,B:3"
    val result = Options.parseWeightedSets(input)
    assertEquals(5, result.length)
    assertEquals(2, result.count(_ == 'A'.toInt))
    assertEquals(3, result.count(_ == 'B'.toInt))
  }

  @Test
  def testMixed(): Unit = {
    val input = "ascii_uppercase,0:2"
    val result = Options.parseWeightedSets(input)
    // ascii_uppercase -> 26
    // 0:2 -> 2
    // Total 28
    assertEquals(28, result.length)
    assertTrue(result.contains('A'.toInt))
    assertEquals(2, result.count(_ == '0'.toInt))
  }
}
