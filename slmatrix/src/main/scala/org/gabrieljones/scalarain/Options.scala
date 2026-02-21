package org.gabrieljones.scalarain

import org.gabrieljones.scalarain.CodePointSyntax.*
import caseapp.*
import caseapp.core.argparser.{ArgParser, SimpleArgParser}

@AppName("ScaLaMatrixRain")
@ProgName("slmatrix")
@AppVersion("0.1.0") //TODO get from axion-release-plugin
case class Options(
  @HelpMessage("Which scenes to run")
  @ValueDescription("cursorBlink,trace,wakeUp,traceFail,rain")
  scenes: Seq[String] = Seq(
    "cursorBlink",
    "trace",
    "wakeUp",
    "traceFail",
    "rain",
  ),
  @HelpMessage("Which physics to use")
  @ValueDescription("rain,warp,spiral,gravity,hole,repel,swirl,vortex")
  physics: String = "rain",
  @HelpMessage("Display the test pattern during rain scene")
  testPattern: Boolean = false,
  @HelpMessage("Sets of unicode chars and their weights")
  @ValueDescription("ranges, named sets, and weights, e.g. '0x30A0-0x30FF:2,ascii_uppercase:1'")
  unicodeChars: String = "0x30A0-0x30FF,0xFF10-0xFF19",
  @HelpMessage("Max frames to run (for benchmarking, -1 for infinite)")
  @ValueDescription("Number of frames")
  maxFrames: Int = -1,
  @HelpMessage("Frame interval in milliseconds")
  @ValueDescription("milliseconds")
  frameInterval: Int = 50,
  @HelpMessage("Fade color (green, red, blue, cyan, magenta, yellow, white) or base,step")
  @ValueDescription("color name or base,step")
  fadeColor: String = "green",
)

object Options {
  implicit val commaSeparatedSeqArgParser: ArgParser[Seq[String]] =
    SimpleArgParser.from("comma-separated list") { str =>
      Right(str.split(",").map(_.trim).filter(_.nonEmpty).toSeq)
    }

  private val namedSets: Map[String, Range] = Map(
    "katakana" -> (0x30A0 to 0x30FF),
    "digits_full_width" -> (0xFF10 to 0xFF19),
    "emoji" -> (0x1F000 to 0x1FAFF),
    "ascii_uppercase" -> (0x41 to 0x5A),
    "ascii_lowercase" -> (0x61 to 0x7A),
    "math_symbols" -> (0x2200 to 0x22FF),
    "katakana_half_width" -> (0xFF66 to 0xFF9D),
    "ascii_digits" -> (0x30 to 0x39),
    "ascii_punctuation" -> (0x21 to 0x2F)
  )

  def parseWeightedSets(input: String): SetsOfCodePoints = {
    input.split(",").flatMap { part =>
      scala.util.Try {
        val (identifier, weight) = part.split(":") match {
          case Array(r, w) => (r.trim, w.trim.toInt)
          case Array(r) => (r.trim, 1)
          case _ => throw new IllegalArgumentException(s"Invalid weighted set format: $part")
        }

        if (weight < 0) throw new IllegalArgumentException(s"Negative weight: $weight")

        val codes: Seq[Int] = if (namedSets.contains(identifier)) {
          namedSets(identifier)
        } else if (identifier.contains("-")) {
          identifier.split("-") match {
            case Array(startStr, endStr) =>
              val start = parseChar(startStr.trim)
              val end = parseChar(endStr.trim)
              (start to end)
            case _ => throw new IllegalArgumentException(s"Invalid range format: $identifier")
          }
        } else {
          Seq(parseChar(identifier))
        }

        Iterator.fill(weight)(codes).flatten
      }.getOrElse(Nil)
    }
      .asSetsOfCodePoints
  }

  def parseChar(s: String): Int = {
    if (s.startsWith("0x")) {
      Integer.parseInt(s.substring(2), 16)
    } else if (s.length == 1) {
      s.head.toInt
    } else {
      throw new IllegalArgumentException(s"Invalid character format: $s")
    }
  }
}
