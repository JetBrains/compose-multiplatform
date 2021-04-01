/*
 * Copyright 2020-2021 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.gradle

import org.gradle.testkit.runner.TaskOutcome
import org.jetbrains.compose.test.GradlePluginTestBase
import org.jetbrains.compose.test.TestKotlinVersion
import org.jetbrains.compose.test.TestProjects
import org.jetbrains.compose.test.checks
import org.junit.jupiter.api.Test

class GradlePluginTest : GradlePluginTestBase() {
    @Test
    fun jsMppIsNotBroken() =
        with(
            testProject(
                TestProjects.jsMpp,
                testEnvironment = defaultTestEnvironment.copy(kotlinVersion = TestKotlinVersion.V1_5_20_dev_3226)
            )
        ) {
            gradle(":compileKotlinJs").build().checks { check ->
                check.taskOutcome(":compileKotlinJs", TaskOutcome.SUCCESS)
            }
        }
}