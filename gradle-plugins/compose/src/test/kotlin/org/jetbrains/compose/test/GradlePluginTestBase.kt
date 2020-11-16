package org.jetbrains.compose.test

import org.junit.jupiter.api.io.TempDir
import java.io.File

abstract class GradlePluginTestBase {
    @TempDir
    lateinit var testWorkDir: File

    fun testProject(name: String): TestProject =
        TestProject(name, workingDir = testWorkDir)
}
