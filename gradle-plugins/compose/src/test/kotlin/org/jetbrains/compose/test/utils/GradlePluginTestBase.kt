/*
 * Copyright 2020-2022 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.test.utils

import org.junit.jupiter.api.io.TempDir
import java.io.File

abstract class GradlePluginTestBase {
    @TempDir
    lateinit var testWorkDir: File

    val defaultTestEnvironment: TestEnvironment
        get() = TestEnvironment(workingDir = testWorkDir)

    fun testProject(
        name: String,
        testEnvironment: TestEnvironment = defaultTestEnvironment
    ): TestProject =
        TestProject(name, testEnvironment = testEnvironment)
}
