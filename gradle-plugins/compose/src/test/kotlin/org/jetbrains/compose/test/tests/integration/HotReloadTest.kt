package org.jetbrains.compose.test.tests.integration

import org.gradle.testkit.runner.BuildResult
import org.jetbrains.compose.ComposeBuildConfig
import org.jetbrains.compose.test.utils.GradlePluginTestBase
import org.jetbrains.compose.test.utils.checks
import org.jetbrains.compose.test.utils.modify
import org.junit.jupiter.api.Test
import kotlin.concurrent.thread

class HotReloadTest : GradlePluginTestBase() {
    @Test
    fun smokeTestHotRunTask() = with(testProject("application/jvm")) {
        file("build.gradle").modify {
            it + """
                afterEvaluate {
                    tasks.getByName("hotRun").doFirst {
                        throw new StopExecutionException("Skip hotRun task")
                    }
                }
            """.trimIndent()
        }
        gradle("hotRun").checks {
            check.taskSuccessful(":hotRun")
        }
    }

    @Test
    fun testHotReload() = with(testProject("application/hotReload")) {
        var result: BuildResult? = null
        val hotRunThread = thread {
            result = gradle("hotRunJvm")
        }

        while (!file("started").exists()) {
            Thread.sleep(200)
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
    }

    @Test
    fun testExternalHotReload() = with(testProject("application/mpp")) {
        val externalHotReloadVersion = "1.0.0-beta04"
        modifyText("settings.gradle") {
            it.replace(
                "plugins {", "plugins {\n" +
                        """
                        id 'org.jetbrains.compose.hot-reload' version '$externalHotReloadVersion'
                """.trimIndent()
            )
        }
        modifyText("build.gradle") {
            it.replace(
                "plugins {", "plugins {\n" +
                        """
                        id "org.jetbrains.compose.hot-reload"
                """.trimIndent()
            )
        }
        gradle("hotRunJvm").checks {
            check.taskSuccessful(":hotRunJvm")
            check.logContains("Compose Hot Reload ($externalHotReloadVersion)")
            check.logContains("Kotlin MPP app is running!")
        }
    }
}