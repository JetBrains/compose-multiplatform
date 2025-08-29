package org.jetbrains.compose.test.tests.integration

import org.jetbrains.compose.test.utils.GradlePluginTestBase
import org.jetbrains.compose.test.utils.TestProject
import org.jetbrains.compose.test.utils.checkExists
import org.jetbrains.compose.test.utils.checks
import org.junit.jupiter.api.Test
import kotlin.test.Ignore
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class WebCompatibilityDistributionTest : GradlePluginTestBase() {

    private fun TestProject.assertCompatibilityDistribution(
        dirPath: String = "./build/dist/composeWebCompatibility/productionExecutable",
        expectedFileNames: Set<String>
    ) {
        file(dirPath).apply {
            checkExists()
            assertTrue(isDirectory, "Expected $dirPath to be a directory")

            val listed = listFiles()
                ?: error("Expected to list files in $dirPath but got null")
            val distributionNames = listed.map { it.name }

            assertTrue(
                distributionNames.any { it.endsWith(".wasm") },
                "Expected at least one .wasm file in $dirPath, found: $distributionNames"
            )

            val actualFiles = distributionNames.filterNot { it.endsWith(".wasm") }.toSet()
            assertEquals(expectedFileNames, actualFiles, "files mismatch in $dirPath")
        }
    }

    private fun TestProject.applyBuildConfig(caseName: String) {
        file("build.gradle.kts.${caseName}").renameTo(file("build.gradle.kts"))
    }

    @Test
    fun checkWebCompatibilityDistribution() {
        with(testProject("application/webApp")) {
            applyBuildConfig("WebApp")
            gradle(":composeCompatibilityBrowserDistribution").checks {
                check.taskSuccessful(":composeCompatibilityBrowserDistribution")
                check.taskSuccessful(":jsBrowserDistribution")
                check.taskSuccessful(":wasmJsBrowserDistribution")
            }
            assertCompatibilityDistribution(
                expectedFileNames = setOf(
                    "composeApp.js",
                    "composeResources",
                    "index.html",
                    "originJsComposeApp.js",
                    "originJsComposeApp.js.map",
                    "originWasmComposeApp.js",
                    "originWasmComposeApp.js.map",
                    "styles.css"
                )
            )
        }
    }

    @Test
    fun testWebJsOnly() {
        with(testProject("application/webApp")) {
            applyBuildConfig("WebJsOnly")
            gradle(":composeCompatibilityBrowserDistribution").checks {
                check.taskSkipped(":composeCompatibilityBrowserDistribution")
                check.taskSuccessful(":jsBrowserDistribution")
                check.logContains("no js and wasm distributions found, both are required for compatibility")
            }
        }
    }

    @Test
    fun testWebWasmOnly() {
        with(testProject("application/webApp")) {
            applyBuildConfig("WebWasmOnly")
            gradle(":composeCompatibilityBrowserDistribution").checks {
                check.taskSkipped(":composeCompatibilityBrowserDistribution")
                check.taskSuccessful(":wasmJsBrowserDistribution")
                check.logContains("no js and wasm distributions found, both are required for compatibility")
            }
        }
    }

    @Test
    fun testWebJsNonExecutable() {
        with(testProject("application/webApp")) {
            applyBuildConfig("WebJsNonExecutable")
            gradle(":composeCompatibilityBrowserDistribution").checks {
                check.taskSkipped(":composeCompatibilityBrowserDistribution")
                check.logContains("no js and wasm distributions found, both are required for compatibility")
            }
        }
    }

    @Test
    fun testWebSingleExecutable() {
        with(testProject("application/webApp")) {
            applyBuildConfig("WebSingleExecutable")
            gradle(":composeCompatibilityBrowserDistribution").checks {
                check.taskSkipped(":composeCompatibilityBrowserDistribution")
                check.logContains("no js and wasm distributions found, both are required for compatibility")
            }
        }
    }

    @Test
    fun testWebJsWasmNonStandardTargetNames() {
        with(testProject("application/webApp")) {
            applyBuildConfig("WebJsWasmNonStandardTargetNames")
            file("src/jsMain").renameTo(file("src/webJsMain"))
            file("src/wasmJsMain").renameTo(file("src/webWasmMain"))

            gradle(":composeCompatibilityBrowserDistribution").checks {
                check.taskSuccessful(":composeCompatibilityBrowserDistribution")
                check.taskSuccessful(":webJsBrowserDistribution")
                check.taskSuccessful(":webWasmBrowserDistribution")
            }
            assertCompatibilityDistribution(
                expectedFileNames = setOf(
                    "composeApp.js",
                    "composeResources",
                    "index.html",
                    "originJsComposeApp.js",
                    "originJsComposeApp.js.map",
                    "originWasmComposeApp.js",
                    "originWasmComposeApp.js.map",
                    "styles.css"
                )
            )
        }
    }

    @Test
    @Ignore("WebPack outputFileName doesn't reflect a real name of the bundle.")
    fun testWebJsWasmNonStandardBundleNames() {
        with(testProject("application/webApp")) {
            applyBuildConfig("WebJsWasmNonStandardBundleNames")
            gradle(":composeCompatibilityBrowserDistribution").checks {
                check.taskSuccessful(":composeCompatibilityBrowserDistribution")
                check.taskSuccessful(":jsBrowserDistribution")
                check.taskSuccessful(":wasmJsBrowserDistribution")
            }
            assertCompatibilityDistribution(
                expectedFileNames = setOf(
                    "myApp.js",
                    "composeResources",
                    "index.html",
                    "originJsMyApp.js",
                    "originJsMyApp.js.map",
                    "originWasmMyApp.js",
                    "originWasmMyApp.js.map",
                    "styles.css"
                )
            )
        }
    }

    @Test
    fun testWebJsWasmRepacked() {
        with(testProject("application/webApp")) {
            applyBuildConfig("WebJsWasmRepacked")
            gradle(":composeCompatibilityBrowserDistribution").checks {
                check.taskSuccessful(":composeCompatibilityBrowserDistribution")
                check.taskSuccessful(":jsBrowserDistribution")
                check.taskSuccessful(":wasmJsBrowserDistribution")
                check.taskSuccessful(":jsRepack")
                check.taskSuccessful(":wasmRepack")
            }
            assertCompatibilityDistribution(
                expectedFileNames = setOf(
                    "composeResources",
                    "index.html",
                    "originJsRepackedApp.js",
                    "originJsRepackedApp.js.map",
                    "originWasmRepackedApp.js",
                    "originWasmRepackedApp.js.map",
                    "repackedApp.js",
                    "styles.css"
                )
            )
        }
    }

    //https://youtrack.jetbrains.com/issue/CMP-8760
    @Test
    fun checkWebAppWithKmmBridge() {
        with(testProject("application/webApp")) {
            applyBuildConfig("WebAppWithKmmBridge")
            gradle(":composeCompatibilityBrowserDistribution").checks {
                check.taskSuccessful(":composeCompatibilityBrowserDistribution")
                check.taskSuccessful(":jsBrowserDistribution")
                check.taskSuccessful(":wasmJsBrowserDistribution")
            }
            assertCompatibilityDistribution(
                expectedFileNames = setOf(
                    "composeApp.js",
                    "composeResources",
                    "index.html",
                    "originJsComposeApp.js",
                    "originJsComposeApp.js.map",
                    "originWasmComposeApp.js",
                    "originWasmComposeApp.js.map",
                    "styles.css"
                )
            )
        }
    }
}