/*
 * Copyright 2020-2022 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.test.tests.integration

import org.jetbrains.compose.newComposeCompilerError
import org.jetbrains.compose.test.utils.GradlePluginTestBase
import org.jetbrains.compose.test.utils.TestProject
import org.jetbrains.compose.test.utils.checks
import org.junit.jupiter.api.Test

class KotlinCompatibilityTest : GradlePluginTestBase() {
    @Test
    fun testKotlinMpp_1_9_10() = testMpp("1.9.10")

    @Test
    fun testKotlinJsMpp_1_9_24() = testJsMpp("1.9.24")

    @Test
    fun testKotlinMpp_1_9_20() = testMpp("1.9.20")

    @Test
    fun testKotlinJsMpp_1_9_20() = testJsMpp("1.9.20")

    private fun testMpp(kotlinVersion: String) = with(
        testProject(
            "beforeKotlin2/mpp",
            testEnvironment = defaultTestEnvironment.copy(kotlinVersion = kotlinVersion)
        )
    ) {
        val logLine = "Kotlin MPP app is running!"
        gradle("run").checks {
            check.taskSuccessful(":run")
            check.logContains(logLine)
        }
    }

    private fun testJsMpp(kotlinVersion: String) = with(
        testProject(
            "beforeKotlin2/jsMpp",
            testEnvironment = defaultTestEnvironment.copy(kotlinVersion = kotlinVersion)
        )
    ) {
        gradle(":compileKotlinJs").checks {
            check.taskSuccessful(":compileKotlinJs")
        }
    }

    @Test
    fun testNewCompilerPluginError() {
        val testProject = testProject(
            "beforeKotlin2/mpp",
            testEnvironment = defaultTestEnvironment.copy(kotlinVersion = "2.0.0")
        )
        testProject.gradleFailure("tasks").checks {
            check.logContains(newComposeCompilerError)
        }
    }

    /**
     * Test the version of Compose Compiler published by Google.
     * See https://developer.android.com/jetpack/androidx/releases/compose-kotlin
     */
    @Test
    fun testAndroidxCompiler() = testProject(
        "beforeKotlin2/custom-compiler", defaultTestEnvironment.copy(
            kotlinVersion = "1.8.0",
            composeCompilerPlugin = "\"androidx.compose.compiler:compiler:1.4.0\""
        )
    ).checkCustomComposeCompiler()

    @Test
    fun testSettingLatestCompiler() = testProject(
        "beforeKotlin2/custom-compiler", defaultTestEnvironment.copy(
            kotlinVersion = "1.8.20",
            composeCompilerPlugin = "dependencies.compiler.forKotlin(\"1.8.20\")",
        )
    ).checkCustomComposeCompiler()

    @Test
    fun testSettingAutoCompiler() = testProject(
        "beforeKotlin2/custom-compiler", defaultTestEnvironment.copy(
            kotlinVersion = "1.8.10",
            composeCompilerPlugin = "dependencies.compiler.auto",
        )
    ).checkCustomComposeCompiler()

    @Test
    fun testKotlinCheckDisabled() = testProject(
        "beforeKotlin2/custom-compiler-args", defaultTestEnvironment.copy(
            kotlinVersion = "1.9.21",
            composeCompilerPlugin = "dependencies.compiler.forKotlin(\"1.9.20\")",
            composeCompilerArgs = "\"suppressKotlinVersionCompatibilityCheck=1.9.21\""
        )
    ).checkCustomComposeCompiler(checkKJS = true)

    private fun TestProject.checkCustomComposeCompiler(checkKJS: Boolean = false) {
        gradle(":runDistributable").checks {
            val actualMainImage = file("main-image.actual.png")
            val expectedMainImage = file("main-image.expected.png")
            assert(actualMainImage.readBytes().contentEquals(expectedMainImage.readBytes())) {
                "The actual image '$actualMainImage' does not match the expected image '$expectedMainImage'"
            }
        }
        if (checkKJS) {
            gradle(":jsBrowserProductionWebpack").checks {
                check.taskSuccessful(":jsBrowserProductionWebpack")
            }
        }
    }
}
