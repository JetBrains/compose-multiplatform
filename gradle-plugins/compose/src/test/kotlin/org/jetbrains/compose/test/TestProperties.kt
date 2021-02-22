package org.jetbrains.compose.test

object TestProperties {
    // __KOTLIN_COMPOSE_VERSION__
    val kotlinVersion: String = "1.4.30"

    val composeVersion: String
        get() = System.getProperty("compose.plugin.version")!!

    val gradleVersionForTests: String
        get() = System.getProperty("gradle.version.for.tests")!!
}
