package org.jetbrains.compose.test

import org.junit.jupiter.api.DisplayNameGenerator

class GradleTestNameGenerator : DisplayNameGenerator.Standard() {
    private val gradleVersion = "[Gradle '${TestProperties.gradleVersionForTests}']"

    override fun generateDisplayNameForClass(testClass: Class<*>?): String =
        super.generateDisplayNameForClass(testClass) + gradleVersion
}