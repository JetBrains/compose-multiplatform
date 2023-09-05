package org.jetbrains.compose.test.tests.integration

import org.jetbrains.compose.createWarningAboutNonCompatibleCompiler
import org.jetbrains.compose.test.utils.GradlePluginTestBase
import org.jetbrains.compose.test.utils.TestProjects
import org.jetbrains.compose.test.utils.checks
import org.junit.jupiter.api.Test

class UnsupportedCompilerPluginWarningTest : GradlePluginTestBase() {

    private val androidxComposeCompilerGroupId = "androidx.compose.compiler"
    private val androidxComposeCompilerPlugin = "$androidxComposeCompilerGroupId:compiler:1.4.8"

    private fun testCustomCompilerUnsupportedPlatformsWarning(
        platforms: String,
        warningIsExpected: Boolean
    ) {
        testProject(
            TestProjects.customCompilerUnsupportedPlatformsWarning, defaultTestEnvironment.copy(
                kotlinVersion = "1.8.22",
                composeCompilerPlugin = "\"$androidxComposeCompilerPlugin\"",
            )
        ).apply {
            // repeat twice to check that configuration cache hit does not affect the result
            repeat(2) {
                gradle("-Pplatforms=$platforms").checks {
                    val warning = createWarningAboutNonCompatibleCompiler(androidxComposeCompilerGroupId)
                    if (warningIsExpected) {
                        check.logContainsOnce(warning)
                    } else {
                        check.logDoesntContain(warning)
                    }
                }
            }
        }
    }

    @Test
    fun testJs() {
        testCustomCompilerUnsupportedPlatformsWarning("js", warningIsExpected = true)
    }

    @Test
    fun testIos() {
        testCustomCompilerUnsupportedPlatformsWarning("ios", warningIsExpected = true)
    }

    @Test
    fun testJvm() {
        testCustomCompilerUnsupportedPlatformsWarning("jvm", warningIsExpected = false)
    }
}