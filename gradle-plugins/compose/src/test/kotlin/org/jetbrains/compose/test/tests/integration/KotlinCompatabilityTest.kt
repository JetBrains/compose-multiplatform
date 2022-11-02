/*
 * Copyright 2020-2022 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.test.tests.integration

import org.gradle.testkit.runner.TaskOutcome
import org.gradle.testkit.runner.UnexpectedBuildFailure
import org.jetbrains.compose.test.utils.GradlePluginTestBase
import org.jetbrains.compose.test.utils.TestProjects
import org.jetbrains.compose.test.utils.checks
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class KotlinCompatabilityTest : GradlePluginTestBase() {
    @Test
    fun testKotlinMpp_1_7_10() = testMpp("1.7.10")

    @Test
    fun testKotlinJsMpp_1_7_10() = testJsMpp("1.7.10")

    @Test
    fun testKotlinMpp_1_7_20() = testMpp("1.7.20")

    @Test
    fun testKotlinJsMpp_1_7_20() = testJsMpp("1.7.20")

    private fun testMpp(kotlinVersion: String) = with(
        testProject(
            TestProjects.mpp,
            testEnvironment = defaultTestEnvironment.copy(kotlinVersion = kotlinVersion)
        )
    ) {
        val logLine = "Kotlin MPP app is running!"
        gradle("run").build().checks { check ->
            check.taskOutcome(":run", TaskOutcome.SUCCESS)
            check.logContains(logLine)
        }
    }

    private fun testJsMpp(kotlinVersion: String) = with(
        testProject(
            TestProjects.jsMpp,
            testEnvironment = defaultTestEnvironment.copy(kotlinVersion = kotlinVersion)
        )
    ) {
        gradle(":compileKotlinJs").build().checks { check ->
            check.taskOutcome(":compileKotlinJs", TaskOutcome.SUCCESS)
        }
    }
}
