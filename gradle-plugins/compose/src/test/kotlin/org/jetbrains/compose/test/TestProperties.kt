package org.jetbrains.compose.test

object TestProperties {
    val composeVersion: String
        get() = System.getProperty("compose.plugin.version")!!

    val gradleVersionForTests: String
        get() = System.getProperty("gradle.version.for.tests")!!
}