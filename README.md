This is the buildSrc project.
Gradle builds (and tests) this project before the other projects, and Gradle adds its artifacts into the classpath of the other projects when configuring them.

Tests for the buildSrc project are located in the buildSrc-tests project, so that the build doesn't need to wait for those tests to complete

To run these tests you can run `./gradlew :buildSrc-tests:test`

For information about Gradle's configuration caching, see:
  *  https://medium.com/androiddevelopers/configuration-caching-deep-dive-bcb304698070
  *  https://docs.gradle.org/current/userguide/configuration_cache.html#config_cache:requirements:use_project_during_execution
  *  https://github.com/gradle/gradle/issues/17813
