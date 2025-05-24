tasks.register("dependenciesResolve") {
  notCompatibleWithConfigurationCache("Filters configurations at execution time")
  description = "will fail if `depdendencies --write-locks` did not work as expected. `dependencies --write-locks` will result in successful even if some dependencies failed to resolve"
  doLast {
    configurations.filter { it.isCanBeResolved }
      .forEach { it.resolve() }
  }
}

//adapted from https://docs.gradle.org/current/userguide/dependency_locking.html#lock_all_configurations_in_one_build_execution
tasks.register("dependenciesResolveAndLockAll") {
  notCompatibleWithConfigurationCache("Filters configurations at execution time")
  doFirst {
    require(gradle.startParameter.isWriteDependencyLocks) { "$path must be run from the command line with the `--write-locks` flag" }
  }
  doLast {
    configurations.filter { it.isCanBeResolved }
      .forEach { it.resolve() }
  }
}

dependencyLocking {
  lockAllConfigurations()
}
