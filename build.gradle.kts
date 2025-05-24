buildscript {
  dependencyLocking {
    lockAllConfigurations()
  }
}

plugins {
  id("buildlogic.dependency-milestone-fix")
  id("buildlogic.dependency-locking")
  id("pl.allegro.tech.build.axion-release") version "latest.release"
}

scmVersion {
}

version = scmVersion.version

allprojects {
  project.version = rootProject.version
}

dependencyLocking {
  lockAllConfigurations()
}
