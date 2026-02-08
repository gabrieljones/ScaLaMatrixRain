package org.gabrieljones.scalarain

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.gabrieljones.scalarain.Physics.Vector2
import org.gabrieljones.scalarain.Physics.Vector2.*
import org.gabrieljones.scalarain.Physics.Acceleration.Gravity

class PhysicsTest {

  @Test
  def testGravityApply(): Unit = {
    val gravity = Gravity(100, 100, strength = 2)

    val v1 = gravity.apply(5, 10, 50, 50)
    assertEquals(5, v1.x)
    assertEquals(8, v1.y)

    val v2 = gravity.apply(5, 2, 50, 50)
    assertEquals(5, v2.x)
    assertEquals(1, v2.y)

    val v3 = gravity.apply(5, 1, 50, 50)
    assertEquals(5, v3.x)
    assertEquals(1, v3.y)
  }

  @Test
  def testGravityStartVector(): Unit = {
    val gravity = Gravity(100, 100)
    val v = gravity.startVector
    assertEquals(0, v.x)
    assertEquals(32, v.y)
  }

  @Test
  def testGravityStartPosition(): Unit = {
    val w = 100
    val h = 100
    val gravity = Gravity(w, h)
    for (_ <- 0 until 100) {
      val p = gravity.startPosition
      assertTrue(p.x >= 0 && p.x < w)
      assertTrue(p.y >= 0 && p.y < h)
    }
  }

  @Test
  def testGravityNewPosition(): Unit = {
    val w = 100
    val h = 100
    val gravity = Gravity(w, h)
    for (_ <- 0 until 100) {
      val p = gravity.newPosition
      assertTrue(p.x >= 0 && p.x < w)
      assertEquals(0, p.y)
    }
  }

  @Test
  def testGravityOutOfBounds(): Unit = {
    val gravity = Gravity(100, 100)
    // centerX = 50, centerY = 50

    assertTrue(gravity.outOfBounds(50, 50))
    assertTrue(gravity.outOfBounds(49, 49))
    assertTrue(gravity.outOfBounds(51, 51))
    assertTrue(gravity.outOfBounds(50, 51))
    assertTrue(gravity.outOfBounds(51, 50))

    assertFalse(gravity.outOfBounds(48, 50))
    assertFalse(gravity.outOfBounds(52, 50))
    assertFalse(gravity.outOfBounds(50, 48))
    assertFalse(gravity.outOfBounds(50, 52))
    assertFalse(gravity.outOfBounds(0, 0))
    assertFalse(gravity.outOfBounds(99, 99))
  }
}
