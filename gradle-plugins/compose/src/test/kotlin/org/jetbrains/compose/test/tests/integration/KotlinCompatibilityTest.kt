/*
 * Copyright 2020-2022 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.test.tests.integration

import org.jetbrains.compose.test.utils.GradlePluginTestBase
import org.jetbrains.compose.test.utils.TestProjects
import org.jetbrains.compose.test.utils.checks
import org.junit.jupiter.api.Test

class KotlinCompatibilityTest : GradlePluginTestBase() {
    @Test
    fun testKotlinMpp_1_7_10() = testMpp("1.7.10")

    @Test
    fun testKotlinJsMpp_1_7_10() = testJsMpp("1.7.10")

    @Test
    fun testKotlinMpp_1_7_20() = testMpp("1.7.20")

    @Test
    fun testKotlinJsMpp_1_7_20() = testJsMpp("1.7.20")

    @Suppress("RedundantUnitExpression")
    @Test
    fun testKotlinJs_shows_warning_for_androidx_compose_compiler() = testProject(
        TestProjects.customCompilerArgs, defaultTestEnvironment.copy(
            kotlinVersion = "1.8.22",
            composeCompilerPlugin = "\"androidx.compose.compiler:compiler:1.4.8\"",
            composeCompilerArgs = "\"suppressKotlinVersionCompatibilityCheck=1.8.22\""
        )
    ).let {
        it.gradle(":compileKotlinJs").checks {
            check.taskSuccessful(":compileKotlinJs")
            check.logContains("WARNING: You are using the 'androidx.compose.compiler' compiler plugin in your Kotlin multiplatform project.")
        }
        Unit
    }

    private fun testMpp(kotlinVersion: String) = with(
        testProject(
            TestProjects.mpp,
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
            TestProjects.jsMpp,
            testEnvironment = defaultTestEnvironment.copy(kotlinVersion = kotlinVersion)
        )
    ) {
        gradle(":compileKotlinJs").checks {
            check.taskSuccessful(":compileKotlinJs")
        }
    }
}
