package org.jetbrains.compose.test

object TestProperties {
    val java14Home: String
        get() = System.getProperty("jdk.14.home") ?: error("Run test using JDK 14 or set JDK_14 env var")

    val composeVersion: String
        get() = System.getProperty("compose.plugin.version")!!

    val defaultGradleVersionForTests: String
        get() = System.getProperty("gradle.version.for.tests")!!
}