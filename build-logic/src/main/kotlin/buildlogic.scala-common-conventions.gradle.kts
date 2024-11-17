plugins {
    scala
}

dependencies {
    constraints {
        implementation("org.scala-lang:scala3-library_3:latest.release")
    }
    implementation("org.scala-lang:scala3-library_3")
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
        languageVersion = JavaLanguageVersion.of(21)
    }
}
