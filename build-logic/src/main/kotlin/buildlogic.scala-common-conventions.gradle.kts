plugins {
  id("buildlogic.dependency-milestone-fix")
  id("buildlogic.dependency-locking")
  scala
}

scala {
  scalaVersion = "3.8.2"
}

//workaround for https://github.com/gradle/gradle/issues/6854
configurations.all {
  if (name.startsWith("incrementalScalaAnalysis")) {
    setExtendsFrom(emptyList())
  }
}

testing {
  suites {
    val test by getting(JvmTestSuite::class) {
      useJUnitJupiter("5.11.3")
    }
  }
}

java {
  toolchain {
    languageVersion = JavaLanguageVersion.of(25)
  }
}

tasks {
  compileJava {
    options.release = 17
  }
  compileScala {
    scalaCompileOptions.additionalParameters.add("-release:17")
  }
}
