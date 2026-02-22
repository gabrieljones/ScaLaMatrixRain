package org.gabrieljones.scalarain

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class ColorContextTest {

  @Test
  def testResolveNamedColors(): Unit = {
    val green = ColorContext.resolve("green")
    assertEquals(46, green.baseIndex)
    assertEquals(6, green.step)

    val red = ColorContext.resolve("red")
    assertEquals(196, red.baseIndex)
    assertEquals(36, red.step)

    val blue = ColorContext.resolve("blue")
    assertEquals(21, blue.baseIndex)
    assertEquals(1, blue.step)

    val cyan = ColorContext.resolve("cyan")
    assertEquals(51, cyan.baseIndex)
    assertEquals(7, cyan.step)

    val magenta = ColorContext.resolve("magenta")
    assertEquals(201, magenta.baseIndex)
    assertEquals(37, magenta.step)

    val yellow = ColorContext.resolve("yellow")
    assertEquals(226, yellow.baseIndex)
    assertEquals(42, yellow.step)

    val white = ColorContext.resolve("white")
    assertEquals(231, white.baseIndex)
    assertEquals(43, white.step)
  }

  @Test
  def testResolveCaseInsensitivity(): Unit = {
    val green = ColorContext.resolve("GrEeN")
    assertEquals(46, green.baseIndex)
    assertEquals(6, green.step)
  }

  @Test
  def testResolveCustomFormat(): Unit = {
    val custom = ColorContext.resolve("100,10")
    assertEquals(100, custom.baseIndex)
    assertEquals(10, custom.step)
  }

  @Test
  def testResolveSingleInteger(): Unit = {
    val single = ColorContext.resolve("123")
    assertEquals(123, single.baseIndex)
    assertEquals(6, single.step)
  }

  @Test
  def testResolveFallback(): Unit = {
    // Unknown name
    val unknown = ColorContext.resolve("unknown")
    assertEquals(46, unknown.baseIndex)
    assertEquals(6, unknown.step)

    // Invalid format
    val invalid = ColorContext.resolve("abc")
    assertEquals(46, invalid.baseIndex)
    assertEquals(6, invalid.step)

    // Incomplete custom format
    val incomplete = ColorContext.resolve("100,")
    assertEquals(46, incomplete.baseIndex)
    assertEquals(6, incomplete.step)
  }

  @Test
  def testResolveClamping(): Unit = {
    val under = ColorContext.resolve("-1")
    assertEquals(0, under.baseIndex)

    val over = ColorContext.resolve("300")
    assertEquals(255, over.baseIndex)

    val customUnder = ColorContext.resolve("-10,5")
    assertEquals(0, customUnder.baseIndex)

    val customOver = ColorContext.resolve("500,5")
    assertEquals(255, customOver.baseIndex)
  }
}
