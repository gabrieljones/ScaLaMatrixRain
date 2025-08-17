buildscript {
  dependencyLocking {
    lockAllConfigurations()
  }
}

plugins {
  id("buildlogic.scala-application-conventions")
  id("com.gradleup.shadow") version "8.3.5"
  id("org.graalvm.buildtools.native") version "0.10.3"
}

dependencies {
  implementation("com.googlecode.lanterna:lanterna:3.+")
  implementation("com.github.alexarchambault:case-app_3:2.+")
}

application {
  mainClass = "org.gabrieljones.scalarain.Main"
}

dependencyLocking {
  lockAllConfigurations()
}
