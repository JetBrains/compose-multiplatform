/*
 * Copyright 2020-2021 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.gradle

import org.gradle.testkit.runner.TaskOutcome
import org.jetbrains.compose.desktop.ui.tooling.preview.rpc.PreviewLogger
import org.jetbrains.compose.desktop.ui.tooling.preview.rpc.RemoteConnection
import org.jetbrains.compose.desktop.ui.tooling.preview.rpc.receiveConfigFromGradle
import org.jetbrains.compose.test.GradlePluginTestBase
import org.jetbrains.compose.test.TestKotlinVersion
import org.jetbrains.compose.test.TestProjects
import org.jetbrains.compose.test.checks
import org.junit.jupiter.api.Test
import java.net.ServerSocket
import java.net.Socket
import java.net.SocketTimeoutException
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import kotlin.concurrent.thread

class GradlePluginTest : GradlePluginTestBase() {
    @Test
    fun jsMppIsNotBroken() =
        with(
            testProject(
                TestProjects.jsMpp,
                testEnvironment = defaultTestEnvironment.copy(kotlinVersion = TestKotlinVersion.V1_5_20)
            )
        ) {
            gradle(":compileKotlinJs").build().checks { check ->
                check.taskOutcome(":compileKotlinJs", TaskOutcome.SUCCESS)
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
            try {
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

            } finally {
                serverSocket.close()
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
            testConfigureDesktopPreivewImpl(port.get())
        } finally {
            isAlive.set(false)
            connectionThread.interrupt()
            connectionThread.join(5000)
        }

        val expectedReceivedConfigCount = 2
        val actualReceivedConfigCount = receivedConfigCount.get()
        check(actualReceivedConfigCount == 2) {
            "Expected to receive $expectedReceivedConfigCount preview configs, got $actualReceivedConfigCount"
        }
    }

    private fun testConfigureDesktopPreivewImpl(port: Int) {
        check(port > 0) { "Invalid port: $port" }
        with(testProject(TestProjects.jvmPreview)) {
            val portProperty = "-Pcompose.desktop.preview.ide.port=$port"
            val previewTargetProperty = "-Pcompose.desktop.preview.target=PreviewKt.ExamplePreview"
            val jvmTask = ":jvm:configureDesktopPreview"
            gradle(jvmTask, portProperty, previewTargetProperty)
                .build()
                .checks { check ->
                    check.taskOutcome(jvmTask, TaskOutcome.SUCCESS)
                }

            val mppTask = ":mpp:configureDesktopPreviewDesktop"
            gradle(mppTask, portProperty, previewTargetProperty)
                .build()
                .checks { check ->
                    check.taskOutcome(mppTask, TaskOutcome.SUCCESS)
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