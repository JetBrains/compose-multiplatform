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

    /**
     * Kotlin/JS and Kotlin/Wasm support of the current KGP is not compatible with Gradle isolated projects:
     * - JS/Wasm incremental compilation accesses tasks of the whole task graph:
     *   https://youtrack.jetbrains.com/issue/KT-80262
     * - `NpmResolverPlugin` accesses the root project from subprojects:
     *   https://youtrack.jetbrains.com/issue/KT-75899
     *
     * TODO Use [defaultTestEnvironment] instead after updating Kotlin to a version with the fixes
     */
    val defaultTestEnvironmentWithoutIsolatedProjects: TestEnvironment
        get() = defaultTestEnvironment.copy(useGradleIsolatedProjects = false)

    fun testProject(
        name: String,
        testEnvironment: TestEnvironment = defaultTestEnvironment
    ): TestProject =
        TestProject(name, testEnvironment = testEnvironment)
}
