package org.jetbrains.compose.test.tests.integration

import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.TaskOutcome
import org.jetbrains.compose.ComposeBuildConfig
import org.jetbrains.compose.desktop.application.internal.ComposeProperties
import org.jetbrains.compose.test.utils.GradlePluginTestBase
import org.jetbrains.compose.test.utils.TestEnvironment
import org.jetbrains.compose.test.utils.checks
import org.junit.jupiter.api.fail
import org.junit.jupiter.api.Test
import kotlin.concurrent.thread

class HotReloadTest : GradlePluginTestBase() {
    @Test
    fun testHotReloadTaskRegisteredInJvmProject() = with(testProject("application/jvm")) {
        gradle("hotRun", "--dry-run").checks {
            check.taskSkipped(":hotRun")
        }
    }

    @Test
    fun testHotReloadTaskRegisteredInKmpProject() = with(testProject("application/mpp")) {
        gradle("hotRunJvm", "--dry-run").checks {
            check.taskSkipped(":hotRunJvm")
        }
    }

    @Test
    fun testDisableHotReload() = with(testProject("application/jvm")) {
        gradleFailure("hotRun", "-P${ComposeProperties.DISABLE_HOT_RELOAD}=true").checks {
            check.logContains("Task 'hotRun' not found")
        }
    }

    @Test
    fun testIncompatibleKotlin() = with(testProject("application/jvm",
        TestEnvironment(defaultTestEnvironment.workingDir, kotlinVersion = "2.1.0")))
    {
        gradleFailure("hotRun").checks {
            check.logContains("w: Compose Hot Reload is disabled")
            check.logContains("Task 'hotRun' not found")
        }
    }

    @Test
    fun testNonJvmProject() = with(testProject("application/nonJvm")) {
        gradleFailure("hotRun").checks {
            check.logContains("Task 'hotRun' not found")
        }
    }

    private fun gradleRunnerWorkaround() {
        // Gradle seems not waiting for spawned processes to complete (see related https://github.com/gradle/gradle/issues/7603).
        // which block test directory on Windows, and a test may fail because of it.
        // We use this dirty workaround to handle this.
        Thread.sleep(1000)
    }

    @Test
    fun testHotReload() = with(testProject("application/hotReload")) {
        var result: BuildResult? = null
        val hotRunThread = thread {
            result = gradle("hotRunJvm", "-Pcompose.reload.headless=true")
        }

        val timeoutMs = 300000L
        val startTimeMs = System.currentTimeMillis()
        // wait until the test is ready for hot reload
        while (!file("started").exists()) {
            if (!hotRunThread.isAlive) {
                if (result?.task(":hotRunJvm")?.outcome != TaskOutcome.SUCCESS) {
                    fail("hotRunJvm task failed")
                } else {
                    fail("hotRunJvm task completed unexpectedly")
                }
            }
            Thread.sleep(200)
            if (System.currentTimeMillis() - startTimeMs > timeoutMs) {
                hotRunThread.interrupt()
                fail("timeout: hotRunJvm task did not start within $timeoutMs ms")
            }
        }

        modifyText("src/jvmMain/kotlin/main.kt") {
            it.replace("Kotlin MPP", "KMP")
        }

        gradle("reload").checks {
            check.taskSuccessful(":reload")
            check.logContains("MainKt.class: modified")
        }

        hotRunThread.join()
        check(result != null)
        result.checks {
            check.taskSuccessful(":hotRunJvm")
            check.logContains("Kotlin MPP app is running!")
            check.logContains("KMP app is running!")
            check.logContains("Compose Hot Reload (${ComposeBuildConfig.composeHotReloadVersion})")
        }
        gradleRunnerWorkaround()
    }

    @Test
    fun testExternalHotReload() = with(testProject("application/mpp")) {
        val externalHotReloadVersion = "1.0.0-rc01"
        modifyText("settings.gradle") {
            //  Set the explicit version of Compose Hot Reload in the "pluginManagement {" block
            it.replace(
                "plugins {",
                """
                         plugins {
                             id 'org.jetbrains.compose.hot-reload' version '$externalHotReloadVersion'
                """.trimIndent()
            )
        }
        modifyText("build.gradle") {
            //  Apply hot reload plugin explicitly
            it.replace(
                "plugins {",
                """
                        plugins {
                            id "org.jetbrains.compose.hot-reload"
                """.trimIndent()
            )
        }
        gradle("hotRunJvm", "-Pcompose.reload.headless=true").checks {
            check.taskSuccessful(":hotRunJvm")
            check.logContains("Compose Hot Reload ($externalHotReloadVersion)")
            check.logContains("Kotlin MPP app is running!")
        }
        gradleRunnerWorkaround()
    }
}