plugins {
  id("buildlogic.scala-application-conventions")
  id("com.gradleup.shadow") version "latest.release"
}

dependencies {
  implementation("com.googlecode.lanterna:lanterna:latest.release")
}

application {
  mainClass = "org.gabrieljones.scalarain.Main"
}
