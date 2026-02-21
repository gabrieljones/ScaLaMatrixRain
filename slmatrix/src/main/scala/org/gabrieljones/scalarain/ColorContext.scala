package org.gabrieljones.scalarain

import com.googlecode.lanterna.TextColor

class ColorContext(val baseIndex: Int, val step: Int) {
  // Pre-computed state transition tables for fast lookups.
  // Represents color lifecycle: White -> Faded... -> Empty (-1).

  // Calculate fade states.
  // Original logic: (colorBase to (colorBase - 30) by -6)
  // New logic: 0 to 5 mapped to base - i*step
  // Ensure indices are within [0, 255]
  val fadeStates: IndexedSeq[TextColor] = (0 to 5).map { i =>
    val index = baseIndex - (i * step)
    val safeIndex = if (index < 0) 0 else if (index > 255) 255 else index
    new TextColor.Indexed(safeIndex)
  }
  val maxState: Int = fadeStates.length // should be 6

  val fadeTable: Array[Int] = new Array[Int](maxState + 1)
  val stateToColor: Array[TextColor] = new Array[TextColor](maxState + 1)

  // Initialize tables
  // State 0 is White Bright (Head)
  stateToColor(0) = TextColor.ANSI.WHITE_BRIGHT
  fadeTable(0) = 1 // Transitions to State 1

  fadeStates.zipWithIndex.foreach { case (color, idx) =>
    val state = idx + 1
    stateToColor(state) = color

    // Calculate transition to next state
    if (state < maxState) {
      fadeTable(state) = state + 1
    } else {
      fadeTable(state) = -1 // End of lifecycle (Empty)
    }
  }

  def baseColor: TextColor = new TextColor.Indexed(baseIndex)
}

object ColorContext {
  def resolve(colorName: String): ColorContext = {
     val (base, step) = colorName.toLowerCase match {
       case "green" => (46, 6)
       case "red" => (196, 36)
       case "blue" => (21, 1)
       case "cyan" => (51, 7)
       case "magenta" => (201, 37)
       case "yellow" => (226, 42)
       case "white" => (231, 43)
       case other =>
          try {
            // Try parsing "base,step"
            if (other.contains(",")) {
              val parts = other.split(",")
              if (parts.length >= 2) {
                (parts(0).trim.toInt, parts(1).trim.toInt)
              } else {
                // If parsing fails or not enough parts, default to green
                (46, 6)
              }
            } else {
               // Try parsing single integer as base, default step 6
               (other.trim.toInt, 6)
            }
          } catch {
            case _: Exception => (46, 6)
          }
     }
     val clampedBase = if (base < 0) 0 else if (base > 255) 255 else base
     new ColorContext(clampedBase, step)
   }
}
