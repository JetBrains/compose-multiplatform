/*
 * Copyright 2020-2022 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.test.tests.integration

import org.gradle.internal.impldep.junit.framework.TestCase.assertEquals
import org.gradle.internal.impldep.junit.framework.TestCase.assertTrue
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
import kotlin.concurrent.thread
import kotlin.test.assertContentEquals
import kotlin.test.assertFalse

class GradlePluginTest : GradlePluginTestBase() {
    @Test
    fun skikoWasm() = with(
        testProject(
            "misc/skikoWasm",
            // TODO: enable the configuration cache after moving all test projects to kotlin 2.0 or newer
            defaultTestEnvironment.copy(useGradleConfigurationCache = false)
        )
    ) {
        gradle(":build").checks {
            check.taskSuccessful(":unpackSkikoWasmRuntime")
            check.taskSuccessful(":processSkikoRuntimeForKWasm")
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

    // Note: we can't test non-jvm targets with Kotlin older than 2.2.0, because of klib abi version bump in 2.2.0
    private val oldestSupportedKotlinVersion = "2.2.0"
    @Test
    fun testOldestKotlinMpp() = with(
        testProject(
            "application/mpp",
            testEnvironment = defaultTestEnvironment.copy(kotlinVersion = oldestSupportedKotlinVersion)
        )
    ) {
        val logLine = "Kotlin MPP app is running!"
        gradle("run").checks {
            check.taskSuccessful(":run")
            check.logContains(logLine)
        }
    }

    @Test
    fun testOldestKotlinJsMpp() = with(
        testProject(
            "application/jsMpp",
            testEnvironment = defaultTestEnvironment.copy(kotlinVersion = oldestSupportedKotlinVersion)
        )
    ) {
        gradle(":compileKotlinJs").checks {
            check.taskSuccessful(":compileKotlinJs")
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
