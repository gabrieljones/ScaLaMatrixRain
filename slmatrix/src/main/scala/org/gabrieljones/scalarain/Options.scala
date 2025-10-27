package org.gabrieljones.scalarain

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
  @ValueDescription("rain,warp,spiral")
  physics: String = "rain",
  @HelpMessage("Display the test pattern during rain scene")
  testPattern: Boolean = false,
)

object Options {
  implicit val commaSeparatedSeqArgParser: ArgParser[Seq[String]] =
    SimpleArgParser.from("comma-separated list") { str =>
      Right(str.split(",").map(_.trim).filter(_.nonEmpty).toSeq)
    }
}

