package org.jetbrains.compose.test

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
