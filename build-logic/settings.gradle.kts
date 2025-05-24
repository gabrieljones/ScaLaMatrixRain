dependencyResolutionManagement {
  repositories {
    mavenCentral()
  }
}

buildscript {
  dependencyLocking {
    lockAllConfigurations()
  }
}

rootProject.name = "build-logic"
