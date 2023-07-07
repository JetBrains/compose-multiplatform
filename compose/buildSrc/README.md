This is the buildSrc project.
Gradle builds (and tests) this project before the other projects, and Gradle adds its artifacts into the classpath of the other projects when configuring them.

Tests for the buildSrc project are located in the buildSrc-tests project, so that the build doesn't need to wait for those tests to complete

To run these tests you can run `./gradlew :buildSrc-tests:test`

For information about Gradle's configuration caching, see:
  *  https://medium.com/androiddevelopers/configuration-caching-deep-dive-bcb304698070
  *  https://docs.gradle.org/current/userguide/configuration_cache.html#config_cache:requirements:use_project_during_execution
  *  https://github.com/gradle/gradle/issues/17813

The buildSrc directory is split into multiple projects based on what needs to be available on the classpath when parsing build.gradle files outside of buildSrc.
Any classes that Gradle puts on the classpath for parsing build.gradle files can theoretically overwrite the implementation of tasks in those projects.
So, if a class is on that classpath and it changes, then Gradle is not confident that the task is necessarily up-to-date and Gradle will rerun it.
So, we move as many classes as possible off of this classpath by applying them from within a separate .gradle script instead.

To verify that classes in private/ don't unnecessarily affect the up-to-datedness status of tasks from outside plugins, try something like this:

```
  # run a kotlin compilation task
  ./gradlew :core:core:compileDebugKotlin
  # make some unrelated changes in buildSrc:
  sed -i 's/ignoreCase = true/ignoreCase = false/g' buildSrc/private/src/main/kotlin/androidx/build/ErrorProneConfiguration.kt
  # rerun same kotlin compilation task
  ./gradlew :core:core:compileDebugKotlin | cat
  # see that the tasks were up-to-date
```

See also b/140265324 for more information.
