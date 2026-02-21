package org.gabrieljones.scalarain

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.gabrieljones.scalarain.Physics.Vector2
import org.gabrieljones.scalarain.Physics.Vector2.*
import org.gabrieljones.scalarain.Physics.Acceleration.Gravity
import org.gabrieljones.scalarain.Physics.Acceleration.GravityCenter
import org.gabrieljones.scalarain.Physics.Acceleration.Rain
import org.gabrieljones.scalarain.Physics.Acceleration
import java.util.concurrent.ThreadLocalRandom
import com.googlecode.lanterna.terminal.Terminal

class TestFrameContext(width: Int, height: Int) extends FrameContext(null, 1) {
  override def update(terminal: Terminal): Unit = {
    this.cols = width
    this.rows = height
  }
  // Re-initialize after super constructor, since width/height are 0 during super's update call
  cols = width
  rows = height
}


class PhysicsTest {

  @Test
  def benchmarkRainApply(): Unit = {
    given frameContext: FrameContext = new TestFrameContext(100, 100)
    given rng: ThreadLocalRandom = ThreadLocalRandom.current()
    val rain = Rain
    val vX = 0
    val vY = 10
    val x = 50
    val y = 50

    // Warmup
    for (_ <- 0 until 100000) {
      rain.apply(vX, vY, x, y)
    }

    val start = System.nanoTime()
    var i = 0
    while (i < 10000000) {
      rain.apply(vX, vY, x, y)
      i += 1
    }
    val end = System.nanoTime()
    println(s"Rain.apply benchmark: ${(end - start) / 1000000.0} ms")
  }

  @Test
  def testGravityApply(): Unit = {
    given frameContext: FrameContext = new TestFrameContext(100, 100)
    given rng: ThreadLocalRandom = ThreadLocalRandom.current()
    val gravity = Gravity(strength = 2)

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
    given frameContext: FrameContext = new TestFrameContext(100, 100)
    given rng: ThreadLocalRandom = ThreadLocalRandom.current()
    val gravity = Gravity()
    val v = gravity.startVector
    assertEquals(0, v.x)
    assertEquals(32, v.y)
  }

  @Test
  def testGravityStartPosition(): Unit = {
    val w = 100
    val h = 100
    given frameContext: FrameContext = new TestFrameContext(w, h)
    given rng: ThreadLocalRandom = ThreadLocalRandom.current()
    val gravity = Gravity()
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
    given frameContext: FrameContext = new TestFrameContext(w, h)
    given rng: ThreadLocalRandom = ThreadLocalRandom.current()
    val gravity = Gravity()
    for (_ <- 0 until 100) {
      val p = gravity.newPosition(0, 0)
      assertTrue(p.x >= 0 && p.x < w)
      assertEquals(0, p.y)
    }
  }

  @Test
  def testGravityOutOfBounds(): Unit = {
    given frameContext: FrameContext = new TestFrameContext(100, 100)
    val gravity = Gravity()
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

  @Test
  def testGravityCenterApply(): Unit = {
    given frameContext: FrameContext = new TestFrameContext(100, 100)
    given rng: ThreadLocalRandom = ThreadLocalRandom.current()

    // Test Attraction (Hole)
    val hole = GravityCenter(strength = 1)
    // Place point to the left of center (x=40, y=50). Center is (50, 50).
    // Vector towards center is (+10, 0).
    // Expect positive velocity (moving right).
    val vHole = hole.apply(0, 0, 40, 50)
    assertTrue(vHole.x > 0, s"Expected positive x velocity for hole attraction, got ${vHole.x}")

    // Test Repulsion (Repel)
    val repel = GravityCenter(strength = -1)
    // Place point to the left of center (x=40, y=50). Center is (50, 50).
    // Vector towards center is (+10, 0).
    // Expect negative velocity (moving left, away from center).
    val vRepel = repel.apply(0, 0, 40, 50)
    assertTrue(vRepel.x < 0, s"Expected negative x velocity for repel repulsion, got ${vRepel.x}")
  }

  @Test
  def testResolvePhysics(): Unit = {
    // Existing options
    assertTrue(Acceleration.fromName("rain").isInstanceOf[Acceleration.Rain.type])
    assertTrue(Acceleration.fromName("warp").isInstanceOf[Acceleration.Warp.type])
    val spiral = Acceleration.fromName("spiral").asInstanceOf[Acceleration.Spiral]
    assertEquals(-1.4, spiral.angle, 0.001)

    // New options
    val gravity = Acceleration.fromName("gravity").asInstanceOf[Acceleration.Gravity]
    assertEquals(1, gravity.strength)

    val hole = Acceleration.fromName("hole").asInstanceOf[Acceleration.GravityCenter]
    assertEquals(1, hole.strength)

    val repel = Acceleration.fromName("repel").asInstanceOf[Acceleration.GravityCenter]
    assertEquals(-1, repel.strength)

    val swirl = Acceleration.fromName("swirl").asInstanceOf[Acceleration.Spiral]
    assertEquals(0.5, swirl.angle, 0.001)

    val vortex = Acceleration.fromName("vortex").asInstanceOf[Acceleration.Spiral]
    assertEquals(3.0, vortex.angle, 0.001)
  }

  @Test
  def testInvalidPhysics(): Unit = {
    assertThrows(classOf[IllegalArgumentException], () => {
      Acceleration.fromName("invalid_physics_option")
    })
  }
}
