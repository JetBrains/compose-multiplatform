package org.jetbrains.compose.test.tests.integration

import org.jetbrains.compose.test.utils.GradlePluginTestBase
import org.jetbrains.compose.test.utils.TestProject
import org.jetbrains.compose.test.utils.checkExists
import org.jetbrains.compose.test.utils.checks
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class WebCompatibilityDistributionTest : GradlePluginTestBase() {
    private val defaultDistDir = "./composeApp/build/dist/composeWebCompatibility/productionExecutable"

    private fun TestProject.assertCompatibilityDistribution(
        dirPath: String,
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

    private fun runCompatibilityTest(
        projectPath: String,
        successfulTasks: List<String>,
        distDir: String = defaultDistDir,
        expectedFiles: Set<String>
    ) = with(
        testProject(
            projectPath,
            testEnvironment = defaultTestEnvironment.copy()
        )
    ) {
        gradle(":composeApp:composeCompatibilityBrowserDistribution").checks {
            check.taskSuccessful(":composeApp:composeCompatibilityBrowserDistribution")
            successfulTasks.forEach { check.taskSuccessful(it) }
            assertCompatibilityDistribution(dirPath = distDir, expectedFileNames = expectedFiles)
        }
    }

    @Test
    fun testWebJsWasm() = runCompatibilityTest(
        projectPath = "application/webJsWasm",
        successfulTasks = listOf(
            ":composeApp:jsBrowserDistribution",
            ":composeApp:wasmJsBrowserDistribution"
        ),
        expectedFiles = setOf(
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

    @Test
    fun testWebJsWasmNonStandard() = runCompatibilityTest(
        projectPath = "application/webJsWasmNonStandard",
        successfulTasks = listOf(
            ":composeApp:webJsBrowserDistribution",
            ":composeApp:webWasmBrowserDistribution"
        ),
        expectedFiles = setOf(
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

    @Test
    fun testWebJsWasmReconfigured() = runCompatibilityTest(
        projectPath = "application/webJsWasmReconfigured",
        successfulTasks = listOf(
            ":composeApp:wasmRepack",
            ":composeApp:jsRepack"
        ),
        expectedFiles = setOf(
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