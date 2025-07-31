package org.jetbrains.compose.test.tests.integration

import org.gradle.internal.impldep.junit.framework.TestCase.assertTrue
import org.jetbrains.compose.test.utils.GradlePluginTestBase
import org.jetbrains.compose.test.utils.checkExists
import org.jetbrains.compose.test.utils.checks
import org.junit.jupiter.api.Test
import kotlin.test.assertContentEquals

class CompatibilityDistributionTest : GradlePluginTestBase() {
    @Test
    fun testWebJsWasm() = with(
        testProject(
            "application/webJsWasm",
            testEnvironment = defaultTestEnvironment.copy()
        )
    ) {
        gradle(":composeApp:composeCompatibilityBrowserDistribution").checks {
            check.taskSuccessful(":composeApp:composeCompatibilityBrowserDistribution")
            check.taskSuccessful(":composeApp:jsBrowserDistribution")
            check.taskSuccessful(":composeApp:wasmJsBrowserDistribution")

            file("./composeApp/build/dist/composeWebCompatibility/productionExecutable").apply {
                checkExists()
                assertTrue(isDirectory)
                val distributionFiles = listFiles()!!.map { it.name }.toList().sorted()

                assertTrue(distributionFiles.any { it.endsWith(".wasm") })

                assertContentEquals(distributionFiles.filter { !it.endsWith(".wasm") }, listOf(
                    "composeApp.js", "composeResources", "index.html", "originJsComposeApp.js", "originJsComposeApp.js.map", "originWasmComposeApp.js", "originWasmComposeApp.js.map", "styles.css"
                ))
            }
        }
    }


    @Test
    fun testWebJsWasmNonStandard() = with(
        testProject(
            "application/webJsWasmNonStandard",
            testEnvironment = defaultTestEnvironment.copy()
        )
    ) {
        gradle(":composeApp:composeCompatibilityBrowserDistribution").checks {
            check.taskSuccessful(":composeApp:composeCompatibilityBrowserDistribution")
            check.taskSuccessful(":composeApp:webJsBrowserDistribution")
            check.taskSuccessful(":composeApp:webWasmBrowserDistribution")

            file("./composeApp/build/dist/composeWebCompatibility/productionExecutable").apply {
                checkExists()
                assertTrue(isDirectory)
                val distributionFiles = listFiles()!!.map { it.name }.toList().sorted()

                assertTrue(distributionFiles.any { it.endsWith(".wasm") })

                println(distributionFiles.filter { !it.endsWith(".wasm") }.sorted().joinToString(", ") { "\"$it\"" })

                assertContentEquals(distributionFiles.filter { !it.endsWith(".wasm") }, listOf(
                    "composeApp.js", "composeResources", "index.html", "originJsComposeApp.js", "originJsComposeApp.js.map", "originWasmComposeApp.js", "originWasmComposeApp.js.map", "styles.css"
                ))
            }
        }
    }

}