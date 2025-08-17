pluginManagement {
  includeBuild("build-logic")
}

buildscript {
  dependencyLocking {
    lockAllConfigurations()
    //these are expected to show up in settings-gradle.lockfile, but for some unknown reason it is empty
    ignoredDependencies.add("org.gradle.toolchains.foojay-resolver-convention:*")
    ignoredDependencies.add("org.gradle.toolchains:foojay-resolver")
  }
}

plugins {
  id("org.gradle.toolchains.foojay-resolver-convention") version "latest.release"
}

dependencyResolutionManagement {
  repositories {
    mavenCentral()
  }
}

rootProject.name = "ScaLaMatrixRain"

include(
  "slmatrix",
)
