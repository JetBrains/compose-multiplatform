package org.jetbrains.compose

import org.gradle.testkit.runner.TaskOutcome
import org.jetbrains.compose.desktop.application.internal.OS
import org.jetbrains.compose.desktop.application.internal.currentOS
import org.jetbrains.compose.desktop.application.internal.currentTarget
import org.jetbrains.compose.test.*
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.util.jar.JarFile

class DesktopApplicationTest : GradlePluginTestBase() {
    @Test
    fun smokeTestRunTask() = with(testProject(TestProjects.jvm)) {
        file("build.gradle").modify {
            it + """
                afterEvaluate {
                    tasks.getByName("run").doFirst {
                        throw new StopExecutionException("Skip run task")
                    }
                    
                    tasks.getByName("runDistributable").doFirst {
                        throw new StopExecutionException("Skip runDistributable task")
                    }
                }
            """.trimIndent()
        }
        gradle("run").build().let { result ->
            assertEquals(TaskOutcome.SUCCESS, result.task(":run")?.outcome)
        }
        gradle("runDistributable").build().let { result ->
            assertEquals(TaskOutcome.SUCCESS, result.task(":createDistributable")!!.outcome)
            assertEquals(TaskOutcome.SUCCESS, result.task(":runDistributable")?.outcome)
        }
    }

    @Test
    fun kotlinDsl(): Unit = with(testProject(TestProjects.jvmKotlinDsl)) {
        gradle(":package", "--dry-run").build()
    }

    @Test
    fun packageJvm() = with(testProject(TestProjects.jvm)) {
        testPackageNativeExecutables()
    }

    @Test
    fun packageMpp() = with(testProject(TestProjects.mpp)) {
        testPackageNativeExecutables()
    }

    private fun TestProject.testPackageNativeExecutables() {
        val result = gradle(":package").build()
        val ext = when (currentOS) {
            OS.Linux -> "deb"
            OS.Windows -> "msi"
            OS.MacOS -> "dmg"
        }
        file("build/compose/binaries/main/$ext/TestPackage-1.0.$ext")
            .checkExists()
        assertEquals(TaskOutcome.SUCCESS, result.task(":package${ext.capitalize()}")?.outcome)
        assertEquals(TaskOutcome.SUCCESS, result.task(":package")?.outcome)
    }

    @Test
    fun packageUberJarForCurrentOSJvm() = with(testProject(TestProjects.jvm)) {
        testPackageUberJarForCurrentOS()
    }

    @Test
    fun packageUberJarForCurrentOSMpp() = with(testProject(TestProjects.mpp)) {
        testPackageUberJarForCurrentOS()
    }

    private fun TestProject.testPackageUberJarForCurrentOS() {
        gradle(":packageUberJarForCurrentOS").build().let { result ->
            assertEquals(TaskOutcome.SUCCESS, result.task(":packageUberJarForCurrentOS")?.outcome)

            val resultJarFile = file("build/compose/jars/TestPackage-${currentTarget.id}-1.0.jar")
            resultJarFile.checkExists()

            JarFile(resultJarFile).use { jar ->
                val mainClass = jar.manifest.mainAttributes.getValue("Main-Class")
                assertEquals("MainKt", mainClass, "Unexpected main class")

                jar.entries().toList().mapTo(HashSet()) { it.name }.apply {
                    checkContains("MainKt.class", "org/jetbrains/skiko/SkiaWindow.class")
                }
            }
        }
    }
}
