package org.jetbrains.compose.test

import org.junit.Rule
import org.junit.rules.TemporaryFolder

abstract class GradlePluginTestBase {
    @Rule
    @JvmField
    val testDir: TemporaryFolder = TemporaryFolder()

    fun testProject(name: String): TestProject =
        TestProject(name, workingDir = testDir.root)
}
