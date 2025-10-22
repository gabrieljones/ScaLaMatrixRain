tasks.register("dependenciesResolve") {
  description = "will fail if `depdendencies --write-locks` did not work as expected. `dependencies --write-locks` will result in successful even if some dependencies failed to resolve"
  val resolvableConfigurations = configurations.filter { it.isCanBeResolved }
  doLast {
    resolvableConfigurations.forEach { it.resolve() }
  }
}

//adapted from https://docs.gradle.org/current/userguide/dependency_locking.html#lock_all_configurations_in_one_build_execution
tasks.register("dependenciesResolveAndLockAll") {
  val resolvableConfigurations = configurations.filter { it.isCanBeResolved }
  val isWriteDependencyLocks = gradle.startParameter.isWriteDependencyLocks
  doFirst {
    require(isWriteDependencyLocks) { "$path must be run from the command line with the `--write-locks` flag" }
  }
  doLast {
    resolvableConfigurations.forEach { it.resolve() }
  }
}

dependencyLocking {
  lockAllConfigurations()
}
