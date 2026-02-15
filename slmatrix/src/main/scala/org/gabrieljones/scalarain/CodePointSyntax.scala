package org.gabrieljones.scalarain

import java.util.concurrent.ThreadLocalRandom

object CodePointSyntax {

  opaque type CodePoint = Int

  opaque type SetsOfCodePoints = Array[CodePoint]

  extension (codePoint: CodePoint) {
    def getDisplayWidth: Int = {
      // 1. Check for Zero Width characters (like combiners)
      if (codePoint == 0 || Character.getType(codePoint) == Character.NON_SPACING_MARK || Character.getType(codePoint) == Character.ENCLOSING_MARK || (codePoint >= 0x200B && codePoint <= 0x200F)) return 0
      // 2. Check for Wide characters (CJK, Full-width, Emojis)
      // Basic CJK ranges and East Asian Full-width forms
      if ((codePoint >= 0x1100 && codePoint <= 0x115F) || (codePoint >= 0x2329 && codePoint <= 0x232A) || (codePoint >= 0x2E80 && codePoint <= 0x303E) || // CJK Radicals
        (codePoint >= 0x3040 && codePoint <= 0xA4CF) || // Hiragana, Katakana, CJK Unified
        (codePoint >= 0xAC00 && codePoint <= 0xD7A3) || // Hangul
        (codePoint >= 0xF900 && codePoint <= 0xFAFF) || // CJK Compatibility
        (codePoint >= 0xFE10 && codePoint <= 0xFE19) || // Vertical forms
        (codePoint >= 0xFE30 && codePoint <= 0xFE6F) || // CJK Compatibility Forms
        (codePoint >= 0xFF00 && codePoint <= 0xFF60) || // Fullwidth Forms
        (codePoint >= 0xFFE0 && codePoint <= 0xFFE6) || (codePoint >= 0x1F300 && codePoint <= 0x1F64F) || (codePoint >= 0x1F900 && codePoint <= 0x1F9FF) // Miscellaneous Symbols and Pictographs (Emojis)
      ) { // Supplemental Symbols and Pictographs
        return 2
      }
      1
    }
  }

  extension (sets: SetsOfCodePoints) {
    @inline def get(i: Int) = sets(i)
    @inline def getAsInt(i: Int): Int = sets(i)
    def maxDisplayWidth(): Int = sets.map(_.getDisplayWidth).max
    def randomChar(using rng: ThreadLocalRandom): Char = sets.get(rng.nextInt(sets.length)).toChar
    def length = sets.length
    def unwrap: Array[Int] = sets
    def contains(elem: Int): Boolean = sets.contains(elem)
    def count(p: Int => Boolean): Int = sets.count(p)
  }

  extension (sets: Array[Int]) {
    def asSetsOfCodePoints: SetsOfCodePoints = sets
  }
}
