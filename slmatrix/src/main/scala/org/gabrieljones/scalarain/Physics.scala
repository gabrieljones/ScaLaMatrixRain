package org.gabrieljones.scalarain

import scala.util.Random

object Physics {
  opaque type Vector2 = Long
  object Vector2 {
    def apply(x: Int, y: Int): Vector2 = {
      ((y.toLong & 0xFFFFFFFFL) << 32) | (x.toLong & 0xFFFFFFFFL)
    }
    extension (v: Vector2) {
      def x: Int = (v & 0xFFFFFFFFL).toInt
      def y: Int = ((v >> 32) & 0xFFFFFFFFL).toInt
    }
  }

  sealed trait Acceleration {
    def apply(vX: Int, vY: Int, x: Int, y: Int): Vector2
    def startVector: Vector2
    def startPosition: Vector2
    def newPosition: Vector2
    def outOfBounds(x: Int, y: Int): Boolean
  }

  object Acceleration {
    //    case object None extends Acceleration {
    //      override def apply(vX: Int, vY: Int, x: Int, y: Int): Vector2 = Vector2(vX, vY)
    //    }

    case class Gravity(w: Int, h: Int, strength: Int = 1) extends Acceleration {
      val centerX = w / 2
      val centerY = h / 2
      override def apply(vX: Int, vY: Int, x: Int, y: Int): Vector2 = Vector2(vX, Math.max(1, vY - strength))

      override def startVector: Vector2 = Vector2(0, 32)

      override def startPosition: Vector2 = Vector2(Random.nextInt(w), Random.nextInt(h))

      override def newPosition: Vector2 = Vector2(Random.nextInt(w), 0)

      override def outOfBounds(x: Int, y: Int): Boolean = {
        math.abs(x - centerX) < 2 && math.abs(y - centerY) < 2
      }
    }

    case class Rain(w: Int, h: Int) extends Acceleration {

      override def apply(vX: Int, vY: Int, x: Int, y: Int): Vector2 = {
        val deltaY = Random.between(-32, 32) / 31 //accelerate = -1, 0, or 1, make changes less likely
        val vYNew = vY + deltaY
        val vYClamped = if (vYNew > 0 && vYNew < 32) { //if new velocity is in bounds update
          vYNew
        } else {
          vY
        }
        Vector2(vX, vYClamped)
      }

      override def startVector: Vector2 = Vector2(0, Math.min(Random.nextInt(8) + 1, Random.nextInt(8) + 1)) //Vector2(0, Random.nextInt(32))

      override def startPosition: Vector2 = Vector2(Random.nextInt(w), Random.nextInt(h))

      override def newPosition: Vector2 = Vector2(Random.nextInt(w), 0)

      override def outOfBounds(x: Int, y: Int): Boolean = y > h
    }

    case class GravityCenter(w: Int, h: Int, strength: Int) extends Acceleration {
      val centerX = w / 2
      val centerY = h / 2
      override def apply(vX: Int, vY: Int, x: Int, y: Int): Vector2 = {
        val velX = if (vX == 0) 0.0 else 1.0 / vX
        val velY = if (vY == 0) 0.0 else 1.0 / vY
        val dirX = centerX - x
        val dirY = centerY - y
        val dist = math.sqrt(dirX * dirX + dirY * dirY)
        val velXAdj = (dirX.toDouble / dist) * strength
        val velYAdj = (dirY.toDouble / dist) * strength
        val vXNew = (1.0 / velXAdj).toInt
        val vYNew = (1.0 / velYAdj).toInt
        Vector2(
          if (vXNew == 0) math.signum(dirX) else vXNew,
          if (vYNew == 0) math.signum(dirY) else vYNew
        )
      }

      override def startVector: Vector2 = Vector2(0, 0)

      override def startPosition: Vector2 = edgeOfScreen(w, h)

      override def newPosition: Vector2 = startPosition

      override def outOfBounds(x: Int, y: Int): Boolean = {
        math.abs(x - centerX) < 2 && math.abs(y - centerY) < 2
      }
    }

    case class Warp(w: Int, h: Int) extends Acceleration {
      override def apply(vX: Int, vY: Int, x: Int, y: Int): Vector2 = Vector2(vX, vY)

      override def startVector: Vector2 = Vector2(Random.between(-27, 27) / 2, Random.between(-59, 59) / 2)

      override def startPosition: Vector2 = Vector2(w / 2, h / 2)

      override def newPosition: Vector2 = startPosition

      override def outOfBounds(x: Int, y: Int): Boolean = {
        x < 0 || y < 0 || x > w - 4 || y > h - 2
      }
    }

    case class Spiral(w: Int, h: Int, angle: Double = 1.4) extends Acceleration {
      val centerX = w / 2
      val centerY = h / 2

      val maxRadius = Math.max(centerX, centerY)

      override def apply(vX: Int, vY: Int, x: Int, y: Int): Vector2 = {
        val dirX = centerX - x
        val dirY = centerY - y
        val dist = math.sqrt(dirX * dirX + dirY * dirY)
        val angleOrth = math.atan2(dirY, dirX)
        val angleNew = angleOrth + angle
        val vXNew = (1.0 / math.cos(angleNew)).toInt
        val vYNew = (2.0 / math.sin(angleNew)).toInt
        Vector2(
          if (vXNew == 0) math.signum(dirX) else vXNew,
          if (vYNew == 0) math.signum(dirY) else vYNew
        )
      }

      override def startVector: Vector2 = Vector2(0, 0)

      override def startPosition: Vector2 = Vector2(Random.nextInt(w), Random.nextInt(h))

      override def newPosition: Vector2 = edgeOfScreen(w, h)

      override def outOfBounds(x: Int, y: Int): Boolean = {
        math.abs(x - centerX) < 4         && math.abs(y - centerY) < 4 ||
        math.abs(x - centerX) > maxRadius && math.abs(y - centerY) > maxRadius
      }
    }

    def edgeOfScreen(w: Int, h: Int): Vector2 = {
      val side = Random.nextInt(4)
      side match {
        case 0 => Vector2(Random.nextInt(w), 0) //top
        case 1 => Vector2(w - 4, Random.nextInt(h)) //right
        case 2 => Vector2(Random.nextInt(w), h - 2) //bottom
        case 3 => Vector2(0, Random.nextInt(h)) //left
      }
    }
  }
}
