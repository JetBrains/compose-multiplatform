/*
 * Copyright 2020-2022 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.test.tests.integration

import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.gradle.util.GradleVersion
import org.jetbrains.compose.desktop.ui.tooling.preview.rpc.PreviewLogger
import org.jetbrains.compose.desktop.ui.tooling.preview.rpc.RemoteConnection
import org.jetbrains.compose.desktop.ui.tooling.preview.rpc.receiveConfigFromGradle
import org.jetbrains.compose.internal.Version
import org.jetbrains.compose.newComposeCompilerError
import org.jetbrains.compose.test.utils.GradlePluginTestBase
import org.jetbrains.compose.test.utils.checkExists
import org.jetbrains.compose.test.utils.checks
import org.junit.jupiter.api.Assumptions
import org.junit.jupiter.api.Test
import java.net.ServerSocket
import java.net.Socket
import java.net.SocketTimeoutException
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.zip.ZipFile
import kotlin.concurrent.thread
import kotlin.test.assertFalse

class GradlePluginTest : GradlePluginTestBase() {
    // https://youtrack.jetbrains.com/issue/CMP-7479
    @Test
    fun skikoRuntimeIsNotPublished() = with(
        testProject("misc/wasmTestSkiko")
    ) {
        // Preserve unrelated library resources that share a runtime filename at a different path.
        file("src/commonMain/resources/nested/skiko.wasm").apply {
            parentFile.mkdirs()
            writeText("A library resource with the same name as a Skiko runtime file.\n")
        }

        gradle(
            "publishAllPublicationsToTestRepository",
            "jsTestProcessResources",
            "-Pexecutable=false"
        ).checks {
            val runtimeDir = file("build/compose/skiko-js-runtime")
            val runtimeFiles = runtimeDir.walkTopDown()
                .filter { it.isFile && it.relativeTo(runtimeDir).invariantSeparatorsPath.substringBefore('/') != "META-INF" }
                .toList()
            assertTrue(runtimeFiles.any { it.extension == "wasm" }, "Skiko runtime must be resolved")

            for (runtimeFile in runtimeFiles) {
                val path = runtimeFile.relativeTo(runtimeDir).invariantSeparatorsPath
                for (compilation in listOf("main", "test")) {
                    assertArrayEquals(
                        runtimeFile.readBytes(),
                        file("build/processedResources/js/$compilation/$path").readBytes(),
                        "Skiko runtime resource $path must be preserved for $compilation"
                    )
                }
            }

            val klibs = file("build/repo").walkTopDown().filter { it.extension == "klib" }.toList()
            assertEquals(2, klibs.size, "Both JS and Wasm libraries must be published")
            val resourcesDir = file("src/commonMain/resources")
            for (klib in klibs) {
                ZipFile(klib).use { archive ->
                    assertTrue(archive.getEntry("default/manifest") != null, "Compiler output must be preserved")
                    for (runtimeFile in runtimeFiles) {
                        val path = runtimeFile.relativeTo(runtimeDir).invariantSeparatorsPath
                        assertFalse(archive.getEntry(path) != null, "$path must not be published in ${klib.name}")
                    }
                    for (resource in resourcesDir.walkTopDown().filter { it.isFile }) {
                        val path = resource.relativeTo(resourcesDir).invariantSeparatorsPath
                        val entry = requireNotNull(archive.getEntry(path)) { "Missing library resource $path in ${klib.name}" }
                        assertArrayEquals(resource.readBytes(), archive.getInputStream(entry).use { it.readBytes() })
                    }
                }
            }
        }
    }

    @Test
    fun skikoRuntimeFromTestCompilation() = with(
        testProject("misc/wasmTestSkiko")
    ) {
        // Keep this case limited to UI dependencies from test sources.
        modifyText("build.gradle") {
            it.replace("implementation(\"org.jetbrains.compose.ui:ui:${defaultTestEnvironment.composeVersion}\")", "")
        }

        gradle(":unpackTestSkikoRuntimeForJs").checks {
            val jsRuntimeFiles = file("./build/compose/skiko-js-test-runtime")
                .listFiles()!!.map { it.name }
            assertTrue(jsRuntimeFiles.contains("skiko.wasm"), "JS runtime files: $jsRuntimeFiles")
        }
        gradle(":unpackTestSkikoRuntimeForWasmJs").checks {
            val wasmRuntimeFiles = file("./build/compose/skiko-wasmJs-test-runtime")
                .listFiles()!!.map { it.name }
            assertTrue(wasmRuntimeFiles.contains("skiko.wasm"), "Wasm runtime files: $wasmRuntimeFiles")
            assertTrue(wasmRuntimeFiles.contains("skiko.mjs"), "Wasm runtime files: $wasmRuntimeFiles")
        }
    }

    @Test
    fun skikoWasm() = with(
        testProject("misc/skikoWasm")
    ) {
        gradle(":build").checks {
            check.taskSuccessful(":unpackSkikoRuntimeForJs")
            check.taskSuccessful(":unpackSkikoRuntimeForWasmJs")
            check.taskSuccessful(":compileKotlinJs")
            check.taskSuccessful(":compileKotlinWasmJs")
            check.taskSuccessful(":wasmJsBrowserDistribution")
            check.taskSuccessful(":jsBrowserDistribution")

            file("./build/dist/wasmJs/productionExecutable").apply {
                checkExists()
                assertTrue(isDirectory)
                val distributionFiles = listFiles()!!.map { it.name }.toList()
                assertFalse(
                    distributionFiles.contains("skiko.wasm"),
                    "skiko.wasm is probably a duplicate"
                )
                // one file is the app wasm file and another one is skiko wasm file with a mangled name
                assertEquals(2, distributionFiles.filter { it.endsWith(".wasm") }.size)
            }

            file("./build/dist/js/productionExecutable").apply {
                checkExists()
                assertTrue(isDirectory)
                val distributionFiles = listFiles()!!.map { it.name }.toList()
                assertTrue(distributionFiles.contains("skiko.wasm"))
                assertTrue(distributionFiles.contains("skiko.mjs"))
            }
        }
    }

    @Test
    fun newAndroidTarget() {
        Assumptions.assumeTrue(defaultTestEnvironment.parsedGradleVersion >= GradleVersion.version("8.10.2"))
        Assumptions.assumeTrue(Version.fromString(defaultTestEnvironment.agpVersion) >= Version.fromString("8.8.0-alpha08"))
        with(testProject("application/newAndroidTarget")) {
            gradle("build", "--dry-run").checks {
            }
        }
    }

    @Test
    fun jsMppIsNotBroken() =
        with(
            testProject("misc/jsMpp")
        ) {
            gradle(":compileKotlinJs").checks {
                check.taskSuccessful(":compileKotlinJs")
            }
        }

    // Note: older version is fine for k/jvm, k/android projects
    private val oldestSupportedKotlinJvmVersion = "2.3.0"

    @Test
    fun testOldestKotlinMpp() = with(
        testProject(
            "application/mpp",
            testEnvironment = defaultTestEnvironment.copy(kotlinVersion = oldestSupportedKotlinJvmVersion)
        )
    ) {
        val logLine = "Kotlin MPP app is running!"
        gradle("run").checks {
            check.taskSuccessful(":run")
            check.logContains(logLine)
        }
    }

    // Note: we can't test non-jvm targets with Kotlin older than 2.4.0, because of klib abi version bump in 2.4.0
    private val oldestSupportedKotlinMultiplatformVersion = "2.4.0"

    @Test
    fun testOldestKotlinJsMpp() = with(
        testProject(
            "application/jsMpp",
            testEnvironment = defaultTestEnvironment.copy(kotlinVersion = oldestSupportedKotlinMultiplatformVersion)
        )
    ) {
        gradle(":compileKotlinJs").checks {
            check.taskSuccessful(":compileKotlinJs")
        }
    }

    //https://youtrack.jetbrains.com/issue/CMP-4906
    @Test
    fun testJsNoExecutableTests() = with(
        testProject("misc/jsNoExecutableTests")
    ) {
        // The project depends on Compose UI (Skiko) but does not declare `binaries.executable()`,
        // so the tests are not bundled with webpack and cannot run. The check task must fail with
        // an actionable message instead of letting the tests fail in a confusing way.
        gradleFailure("jsBrowserTest").checks {
            check.taskFailed(":checkComposeUiTestConfigurationForJs")
            check.logContains(
                "Compose UI tests for the 'js' target are not bundled with webpack: " +
                        "no executable binary is declared, so the Skiko runtime required by Compose UI " +
                        "cannot be loaded and the tests may fail. Add `binaries.executable()` to the " +
                        "'js' target. See https://youtrack.jetbrains.com/issue/CMP-4906"
            )
        }
    }

    @Test
    fun testOldComposePluginError() = with(testProject("misc/oldComposePlugin")) {
        gradleFailure("tasks").checks {
            check.logContains(newComposeCompilerError)
        }
    }

    @Test
    fun configurePreview() {
        val isAlive = AtomicBoolean(true)
        val receivedConfigCount = AtomicInteger(0)
        val port = AtomicInteger(-1)
        val connectionThread = thread {
            val serverSocket = ServerSocket(0).apply {
                soTimeout = 10_000
            }
            port.set(serverSocket.localPort)
            serverSocket.use {
                while (isAlive.get()) {
                    try {
                        val socket = serverSocket.accept()
                        val connection = RemoteConnectionImpl(socket, TestPreviewLogger("SERVER"))
                        val previewConfig = connection.receiveConfigFromGradle()
                        if (previewConfig != null) {
                            receivedConfigCount.incrementAndGet()
                        }
                    } catch (e: Exception) {
                        if (!isAlive.get()) break

                        if (e !is SocketTimeoutException) {
                            e.printStackTrace()
                            throw e
                        }
                    }
                }
            }
        }

        val startTimeNs = System.nanoTime()
        while (port.get() <= 0) {
            val elapsedTimeNs = System.nanoTime() - startTimeNs
            val elapsedTimeMs = elapsedTimeNs / 1_000_000L
            if (elapsedTimeMs > 10_000) {
                error("Server socket initialization timeout!")
            }
            Thread.sleep(200)
        }

        try {
            testConfigureDesktopPreviewImpl(port.get())
        } finally {
            isAlive.set(false)
            connectionThread.interrupt()
            connectionThread.join(5000)
        }

        val expectedReceivedConfigCount = 3
        val actualReceivedConfigCount = receivedConfigCount.get()
        check(actualReceivedConfigCount == expectedReceivedConfigCount) {
            "Expected to receive $expectedReceivedConfigCount preview configs, got $actualReceivedConfigCount"
        }
    }

    //https://youtrack.jetbrains.com/issue/CMP-10608
    @Test
    fun testEarlyTaskMaterialization(): Unit = with(testProject("misc/skikoWasm")) {
        //whenTaskAdded materialize tasks in the configuration phase
        file("build.gradle").appendText("\ntasks.whenTaskAdded { }")

        gradle("tasks").checks {
            check.taskSuccessful(":tasks")
        }
    }

    private fun testConfigureDesktopPreviewImpl(port: Int) {
        check(port > 0) { "Invalid port: $port" }
        with(testProject("misc/jvmPreview")) {
            val portProperty = "-Pcompose.desktop.preview.ide.port=$port"
            val previewTargetProperty = "-Pcompose.desktop.preview.target=PreviewKt.ExamplePreview"
            val jvmTask = ":jvm:configureDesktopPreview"
            gradle(jvmTask, portProperty, previewTargetProperty).checks {
                check.taskSuccessful(jvmTask)
            }

            val mppTask = ":mpp:configureDesktopPreviewDesktop"
            gradle(mppTask, portProperty, previewTargetProperty).checks {
                check.taskSuccessful(mppTask)
            }

            val commonTask = ":common:configureDesktopPreviewDesktop"
            gradle(commonTask, portProperty, previewTargetProperty).checks {
                check.taskSuccessful(commonTask)
            }
        }
    }

    private class TestPreviewLogger(private val prefix: String) : PreviewLogger() {
        override val isEnabled: Boolean
            get() = true

        override fun log(s: String) {
            println("$prefix: $s")
        }
    }

    private fun RemoteConnectionImpl(
        socket: Socket, logger: PreviewLogger
    ): RemoteConnection {
        val connectionClass = Class.forName("org.jetbrains.compose.desktop.ui.tooling.preview.rpc.RemoteConnectionImpl")
        val constructor = connectionClass.constructors.first {
            it.parameterCount == 3
        }
        return constructor.newInstance(socket, logger, {}) as RemoteConnection
    }
}
