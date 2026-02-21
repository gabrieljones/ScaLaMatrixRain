package org.gabrieljones.scalarain

import java.util.concurrent.ThreadLocalRandom

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
    def apply(vX: Int, vY: Int, x: Int, y: Int)(using frameContext: FrameContext, rng: ThreadLocalRandom): Vector2
    def startVector(using frameContext: FrameContext, rng: ThreadLocalRandom): Vector2
    def startPosition(using frameContext: FrameContext, rng: ThreadLocalRandom): Vector2
    def newPosition(mouseX: Int, mouseY: Int)(using frameContext: FrameContext, rng: ThreadLocalRandom): Vector2
    def outOfBounds(x: Int, y: Int)(using frameContext: FrameContext): Boolean
  }

  object Acceleration {
    def fromName(name: String): Acceleration = name.toLowerCase match {
      case "rain"    => Rain
      case "warp"    => Warp
      case "spiral"  => Spiral(-1.4)
      case "gravity" => Gravity(1)
      case "hole"    => GravityCenter(1)
      case "repel"   => GravityCenter(-1)
      case "swirl"   => Spiral(0.5)
      case "vortex"  => Spiral(3.0)
      case "meteor"  => Meteor
      case "bounce"  => Bounce
      case "snow"    => Snow
      case _         => throw new IllegalArgumentException(s"Unknown physics: $name")
    }

    //    case object None extends Acceleration {
    //      override def apply(vX: Int, vY: Int, x: Int, y: Int): Vector2 = Vector2(vX, vY)
    //    }

    case class Gravity(strength: Int = 1) extends Acceleration {
      override def apply(vX: Int, vY: Int, x: Int, y: Int)(using frameContext: FrameContext, rng: ThreadLocalRandom): Vector2 = Vector2(vX, Math.max(1, vY - strength))

      override def startVector(using frameContext: FrameContext, rng: ThreadLocalRandom): Vector2 = Vector2(0, 32)

      override def startPosition(using frameContext: FrameContext, rng: ThreadLocalRandom): Vector2 = Vector2(rng.nextInt(frameContext.w), rng.nextInt(frameContext.h))

      override def newPosition(mouseX: Int, mouseY: Int)(using frameContext: FrameContext, rng: ThreadLocalRandom): Vector2 = Vector2(rng.nextInt(frameContext.w), 0)

      override def outOfBounds(x: Int, y: Int)(using frameContext: FrameContext): Boolean = {
        val centerX = frameContext.w / 2
        val centerY = frameContext.h / 2
        math.abs(x - centerX) < 2 && math.abs(y - centerY) < 2
      }
    }

    case object Rain extends Acceleration {

      override def apply(vX: Int, vY: Int, x: Int, y: Int)(using frameContext: FrameContext, rng: ThreadLocalRandom): Vector2 = {
        val deltaY = rng.nextInt(-32, 32) / 31 //accelerate = -1, 0, or 1, make changes less likely
        val vYNew = vY + deltaY
        val vYClamped = if (vYNew > 0 && vYNew < 32) { //if new velocity is in bounds update
          vYNew
        } else {
          vY
        }
        Vector2(vX, vYClamped)
      }

      override def startVector(using frameContext: FrameContext, rng: ThreadLocalRandom): Vector2 = Vector2(0, Math.min(rng.nextInt(8) + 1, rng.nextInt(8) + 1)) //Vector2(0, ThreadLocalRandom.current().nextInt(32))

      override def startPosition(using frameContext: FrameContext, rng: ThreadLocalRandom): Vector2 = Vector2(rng.nextInt(frameContext.w), rng.nextInt(frameContext.h))

      override def newPosition(mouseX: Int, mouseY: Int)(using frameContext: FrameContext, rng: ThreadLocalRandom): Vector2 = Vector2(rng.nextInt(frameContext.w), 0)

      override def outOfBounds(x: Int, y: Int)(using frameContext: FrameContext): Boolean = y > frameContext.h
    }

    case class GravityCenter(strength: Int) extends Acceleration {
      override def apply(vX: Int, vY: Int, x: Int, y: Int)(using frameContext: FrameContext, rng: ThreadLocalRandom): Vector2 = {
        val centerX = frameContext.w / 2
        val centerY = frameContext.h / 2
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

      override def startVector(using frameContext: FrameContext, rng: ThreadLocalRandom): Vector2 = Vector2(0, 0)

      override def startPosition(using frameContext: FrameContext, rng: ThreadLocalRandom): Vector2 = edgeOfScreen

      override def newPosition(mouseX: Int, mouseY: Int)(using frameContext: FrameContext, rng: ThreadLocalRandom): Vector2 = startPosition

      override def outOfBounds(x: Int, y: Int)(using frameContext: FrameContext): Boolean = {
        val centerX = frameContext.w / 2
        val centerY = frameContext.h / 2
        math.abs(x - centerX) < 2 && math.abs(y - centerY) < 2
      }
    }

    case object Warp extends Acceleration {
      override def apply(vX: Int, vY: Int, x: Int, y: Int)(using frameContext: FrameContext, rng: ThreadLocalRandom): Vector2 = Vector2(vX, vY)

      override def startVector(using frameContext: FrameContext, rng: ThreadLocalRandom): Vector2 = Vector2(rng.nextInt(-27, 27) / 2, rng.nextInt(-59, 59) / 2)

      override def startPosition(using frameContext: FrameContext, rng: ThreadLocalRandom): Vector2 = Vector2(frameContext.w / 2, frameContext.h / 2)

      override def newPosition(mouseX: Int, mouseY: Int)(using frameContext: FrameContext, rng: ThreadLocalRandom): Vector2 = startPosition

      override def outOfBounds(x: Int, y: Int)(using frameContext: FrameContext): Boolean = {
        x < 0 || y < 0 || x > frameContext.w - 4 || y > frameContext.h - 2
      }
    }

    case class Spiral(angle: Double = 1.4) extends Acceleration {
      override def apply(vX: Int, vY: Int, x: Int, y: Int)(using frameContext: FrameContext, rng: ThreadLocalRandom): Vector2 = {
        val centerX = frameContext.w / 2
        val centerY = frameContext.h / 2
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

      override def startVector(using frameContext: FrameContext, rng: ThreadLocalRandom): Vector2 = Vector2(0, 0)

      override def startPosition(using frameContext: FrameContext, rng: ThreadLocalRandom): Vector2 = Vector2(rng.nextInt(frameContext.w), rng.nextInt(frameContext.h))

      override def newPosition(mouseX: Int, mouseY: Int)(using frameContext: FrameContext, rng: ThreadLocalRandom): Vector2 = edgeOfScreen

      override def outOfBounds(x: Int, y: Int)(using frameContext: FrameContext): Boolean = {
        val centerX = frameContext.w / 2
        val centerY = frameContext.h / 2
        val maxRadius = Math.max(centerX, centerY)
        math.abs(x - centerX) < 4         && math.abs(y - centerY) < 4 ||
        math.abs(x - centerX) > maxRadius && math.abs(y - centerY) > maxRadius
      }
    }

    case object Meteor extends Acceleration {
      override def apply(vX: Int, vY: Int, x: Int, y: Int)(using frameContext: FrameContext, rng: ThreadLocalRandom): Vector2 = {
        val vYNew = Math.max(1, vY - 1)
        Vector2(vX, vYNew)
      }

      override def startVector(using frameContext: FrameContext, rng: ThreadLocalRandom): Vector2 = {
        val dirX = if (rng.nextBoolean()) 1 else -1
        val vX = rng.nextInt(2, 6) * dirX
        Vector2(vX, 32)
      }

      override def startPosition(using frameContext: FrameContext, rng: ThreadLocalRandom): Vector2 = {
        Vector2(rng.nextInt(frameContext.w), 0)
      }

      override def newPosition(mouseX: Int, mouseY: Int)(using frameContext: FrameContext, rng: ThreadLocalRandom): Vector2 = {
        startPosition
      }

      override def outOfBounds(x: Int, y: Int)(using frameContext: FrameContext): Boolean = {
        x < 0 || x >= frameContext.w || y >= frameContext.h
      }
    }

    case object Bounce extends Acceleration {
      override def apply(vX: Int, vY: Int, x: Int, y: Int)(using frameContext: FrameContext, rng: ThreadLocalRandom): Vector2 = {
        var newVX = vX
        var newVY = vY

        if ((x <= 0 && vX < 0) || (x >= frameContext.w - 1 && vX > 0)) {
          newVX = -vX
        }

        if ((y <= 0 && vY < 0) || (y >= frameContext.h - 1 && vY > 0)) {
          newVY = -vY
        }

        Vector2(newVX, newVY)
      }

      override def startVector(using frameContext: FrameContext, rng: ThreadLocalRandom): Vector2 = {
        def randomVelocity: Int = {
           val speed = rng.nextInt(1, 10)
           val dir = if (rng.nextBoolean()) 1 else -1
           speed * dir
        }
        Vector2(randomVelocity, randomVelocity)
      }

      override def startPosition(using frameContext: FrameContext, rng: ThreadLocalRandom): Vector2 = {
        Vector2(rng.nextInt(frameContext.w), rng.nextInt(frameContext.h))
      }

      override def newPosition(mouseX: Int, mouseY: Int)(using frameContext: FrameContext, rng: ThreadLocalRandom): Vector2 = {
        startPosition
      }

      override def outOfBounds(x: Int, y: Int)(using frameContext: FrameContext): Boolean = false
    }

    case object Snow extends Acceleration {
      override def apply(vX: Int, vY: Int, x: Int, y: Int)(using frameContext: FrameContext, rng: ThreadLocalRandom): Vector2 = {
        var newVX = vX
        if (rng.nextInt(20) == 0) {
           val drift = rng.nextInt(5, 20)
           val dir = if (rng.nextBoolean()) 1 else -1
           newVX = drift * dir
        }
        Vector2(newVX, vY)
      }

      override def startVector(using frameContext: FrameContext, rng: ThreadLocalRandom): Vector2 = {
         val vY = rng.nextInt(4, 9)
         val vX = rng.nextInt(5, 20) * (if (rng.nextBoolean()) 1 else -1)
         Vector2(vX, vY)
      }

      override def startPosition(using frameContext: FrameContext, rng: ThreadLocalRandom): Vector2 = {
        Vector2(rng.nextInt(frameContext.w), 0)
      }

      override def newPosition(mouseX: Int, mouseY: Int)(using frameContext: FrameContext, rng: ThreadLocalRandom): Vector2 = {
         startPosition
      }

      override def outOfBounds(x: Int, y: Int)(using frameContext: FrameContext): Boolean = {
         y >= frameContext.h
      }
    }

    def edgeOfScreen(using frameContext: FrameContext, rng: ThreadLocalRandom): Vector2 = {
      val side = rng.nextInt(4)
      side match {
        case 0 => Vector2(rng.nextInt(frameContext.w), 0) //top
        case 1 => Vector2(frameContext.w - 4, rng.nextInt(frameContext.h)) //right
        case 2 => Vector2(rng.nextInt(frameContext.w), frameContext.h - 2) //bottom
        case 3 => Vector2(0, rng.nextInt(frameContext.h)) //left
      }
    }
  }
}
