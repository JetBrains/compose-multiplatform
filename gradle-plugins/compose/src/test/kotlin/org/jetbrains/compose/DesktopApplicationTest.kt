package org.jetbrains.compose

import org.gradle.internal.impldep.org.testng.Assert
import org.gradle.testkit.runner.TaskOutcome
import org.jetbrains.compose.desktop.application.internal.OS
import org.jetbrains.compose.desktop.application.internal.currentOS
import org.jetbrains.compose.desktop.application.internal.currentTarget
import org.jetbrains.compose.test.*
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
        val packageDir = file("build/compose/binaries/main/$ext")
        val packageDirFiles = packageDir.listFiles() ?: arrayOf()
        check(packageDirFiles.size == 1) {
            "Expected single package in $packageDir, got [${packageDirFiles.joinToString(", ") { it.name }}]"
        }
        val packageFile = packageDirFiles.single()

        if (currentOS == OS.Linux) {
            val expectedName = "test-package_1.0.0-1_amd64.$ext"
            check(packageFile.name.equals(expectedName, ignoreCase = true)) {
                "Expected '$expectedName' package in $packageDir, got '${packageFile.name}'"
            }
        } else {
            Assert.assertEquals(packageFile.name, "TestPackage-1.0.0.$ext", "Unexpected package name")
        }
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

            val resultJarFile = file("build/compose/jars/TestPackage-${currentTarget.id}-1.0.0.jar")
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

    @Test
    fun testModuleClash() = with(testProject(TestProjects.moduleClashCli)) {
        gradle(":app:runDistributable").build().checks { check ->
            check.taskOutcome(":app:createDistributable", TaskOutcome.SUCCESS)
            check.taskOutcome(":app:runDistributable", TaskOutcome.SUCCESS)
            check.logContains("Called lib1#util()")
            check.logContains("Called lib2#util()")
        }
    }

    @Test
    fun testJavaLogger() = with(testProject(TestProjects.javaLogger)) {
        gradle(":runDistributable").build().checks { check ->
            check.taskOutcome(":runDistributable", TaskOutcome.SUCCESS)
            check.logContains("Compose Gradle plugin test log warning!")
        }
    }
}
