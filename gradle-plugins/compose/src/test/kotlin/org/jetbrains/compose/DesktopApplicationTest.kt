package org.jetbrains.compose

import org.gradle.testkit.runner.TaskOutcome
import org.jetbrains.compose.desktop.application.internal.OS
import org.jetbrains.compose.desktop.application.internal.currentOS
import org.jetbrains.compose.test.*
import org.junit.Assert.assertEquals
import org.junit.Test

class DesktopApplicationTest : GradlePluginTestBase() {
    @Test
    fun smokeTestRunTask() = with(testProject(TestProjects.jvm)) {
        file("build.gradle").modify {
            it + """
                afterEvaluate {
                    tasks.getByName("run").doFirst {
                        throw new StopExecutionException("Skip run task")
                    }
                }
            """.trimIndent()
        }
        val result = gradle("run").build()
        assertEquals(TaskOutcome.SUCCESS, result.task(":run")?.outcome)
    }

    @Test
    fun kotlinDsl(): Unit = with(testProject(TestProjects.jvmKotlinDsl)) {
        gradle(":package", "--dry-run").build()
    }

    @Test
    fun packageJvm() = with(testProject(TestProjects.jvm)) {
        testPackage()
    }

    @Test
    fun packageMpp() = with(testProject(TestProjects.mpp)) {
        testPackage()
    }

    private fun TestProject.testPackage() {
        val result = gradle(":package").build()
        val ext = when (currentOS) {
            OS.Linux -> "deb"
            OS.Windows -> "msi"
            OS.MacOS -> "dmg"
        }
        file("build/compose/binaries/main/$ext/simple-1.0.$ext")
            .checkExists()
        assertEquals(TaskOutcome.SUCCESS, result.task(":package${ext.capitalize()}")?.outcome)
        assertEquals(TaskOutcome.SUCCESS, result.task(":package")?.outcome)
    }
}
