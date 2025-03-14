dependencies {
  components.all {
    val lcVersion = id.version.lowercase()
    if (
      lcVersion.contains("alpha") ||
      lcVersion.contains("-b") ||
      lcVersion.contains("beta") ||
      lcVersion.contains("cr") ||
      lcVersion.contains("m") ||
      lcVersion.contains("rc") ||
      lcVersion.contains("snap")
    ) {
      // Tell Gradle to not treat pre-releases as 'release'
      status = "milestone"
    }
  }
}
