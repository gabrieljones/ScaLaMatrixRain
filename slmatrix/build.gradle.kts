buildscript {
  dependencyLocking {
    lockAllConfigurations()
  }
}

plugins {
  id("buildlogic.scala-application-conventions")
  id("com.gradleup.shadow") version "9.+"
  id("org.graalvm.buildtools.native") version "0.11.2"
}

dependencies {
  implementation("com.googlecode.lanterna:lanterna:3.1.3")
  implementation("com.github.alexarchambault:case-app_3:2.+")
}

application {
  mainClass = "org.gabrieljones.scalarain.Main"
}

graalvmNative {
  binaries {
    named("main") {
      buildArgs.add("-O3")
      buildArgs.add("-march=native")
      // Oracle GraalVM specific optimizations for comparison:
      // buildArgs.add("--gc=G1") // Use G1 garbage collector for improved latency and throughput
      // buildArgs.add("--pgo") // Profile-Guided Optimizations for improved throughput
    }
  }
}

dependencyLocking {
  lockAllConfigurations()
}
