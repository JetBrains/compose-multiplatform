/*
 * Copyright 2020-2022 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.test.tests.integration

import org.jetbrains.compose.desktop.ui.tooling.preview.rpc.PreviewLogger
import org.jetbrains.compose.desktop.ui.tooling.preview.rpc.RemoteConnection
import org.jetbrains.compose.desktop.ui.tooling.preview.rpc.receiveConfigFromGradle
import org.jetbrains.compose.internal.utils.OS
import org.jetbrains.compose.internal.utils.currentOS
import org.jetbrains.compose.test.utils.*
import org.junit.jupiter.api.Assumptions

import java.net.ServerSocket
import java.net.Socket
import java.net.SocketTimeoutException
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import kotlin.concurrent.thread
import org.junit.jupiter.api.Test
import java.io.File

class GradlePluginTest : GradlePluginTestBase() {
    private data class IosTestEnv(
        val targetBuildDir: File,
        val appDir: File,
        val envVars: Map<String, String>
    )

    enum class IosPlatform(val id: String) {
        SIMULATOR("iphonesimulator"), IOS("iphoneos")
    }
    enum class IosArch(val id: String) {
        X64("x86_64"), ARM64("arm64")
    }

    enum class IosBuildConfiguration(val id: String) {
        DEBUG("Debug"), RELEASE("Release")
    }

    private fun iosTestEnv(
        platform: IosPlatform = IosPlatform.SIMULATOR,
        arch: IosArch = IosArch.X64,
        configuration: IosBuildConfiguration = IosBuildConfiguration.DEBUG
    ): IosTestEnv {
        val targetBuildDir = testWorkDir.resolve("build/ios/${configuration.id}-${platform.id}").apply { mkdirs() }
        val appDir = targetBuildDir.resolve("App.app").apply { mkdirs() }
        val envVars = mapOf(
            "PLATFORM_NAME" to platform.id,
            "ARCHS" to arch.id,
            "BUILT_PRODUCTS_DIR" to targetBuildDir.canonicalPath,
            "CONTENTS_FOLDER_PATH" to appDir.name,
        )
        return IosTestEnv(
            targetBuildDir = targetBuildDir,
            appDir = appDir,
            envVars = envVars
        )
    }

    @Test
    fun iosResources() {
        Assumptions.assumeTrue(currentOS == OS.MacOS)
        val iosTestEnv = iosTestEnv()
        val testEnv = defaultTestEnvironment.copy(
            // for some reason configuration cache + test kit + custom vars does not work
            useGradleConfigurationCache = false,
            additionalEnvVars = iosTestEnv.envVars
        )

        with(testProject(TestProjects.iosResources, testEnv)) {
            gradle(":embedAndSignAppleFrameworkForXcode", "--dry-run").checks {
                // This test is not intended to actually run embedAndSignAppleFrameworkForXcode.
                // Instead, it should check that embedAndSign depends on syncComposeResources using dry run
                check.taskSkipped(":syncComposeResourcesForIos")
                check.taskSkipped(":embedAndSignAppleFrameworkForXcode")
            }
            gradle(":syncComposeResourcesForIos").checks {
                check.taskSuccessful(":syncComposeResourcesForIos")
                iosTestEnv.appDir.resolve("compose-resources/compose-multiplatform.xml").checkExists()
            }
        }
    }

    @Test
    fun iosMokoResources() {
        Assumptions.assumeTrue(currentOS == OS.MacOS)
        val iosTestEnv = iosTestEnv()
        val testEnv = defaultTestEnvironment.copy(
            // for some reason configuration cache + test kit + custom vars does not work
            useGradleConfigurationCache = false,
            additionalEnvVars = iosTestEnv.envVars
        )
        with(testProject(TestProjects.iosMokoResources, testEnv)) {
            gradle(
                ":embedAndSignAppleFrameworkForXcode",
                ":copyFrameworkResourcesToApp",
                "--dry-run",
                "--info"
            ).checks {
                // This test is not intended to actually run embedAndSignAppleFrameworkForXcode.
                // Instead, it should check that the sync disables itself.
                check.logContains("Compose Multiplatform resource management for iOS is disabled")
                check.logDoesntContain(":syncComposeResourcesForIos")
            }
        }
    }

    @Test
    fun nativeCacheKind() {
        Assumptions.assumeTrue(currentOS == OS.MacOS)
        fun nativeCacheKindProject(kotlinVersion: String) = testProject(
            TestProjects.nativeCacheKind,
            defaultTestEnvironment.copy(kotlinVersion = kotlinVersion, useGradleConfigurationCache = false)
        )

        val task = ":linkDebugFrameworkIosX64"
        with(nativeCacheKindProject(kotlinVersion = TestKotlinVersions.v1_8_20)) {
            gradle(task, "--info").checks {
                check.taskSuccessful(task)
                check.logDoesntContain("-Xauto-cache-from=")
            }
        }
        testWorkDir.deleteRecursively()
        testWorkDir.mkdirs()
        with(nativeCacheKindProject(kotlinVersion = TestKotlinVersions.v1_9_0) ) {
            gradle(task, "--info").checks {
                check.taskSuccessful(task)
                check.logDoesntContain("-Xauto-cache-from=")
            }
        }
    }

    @Test
    fun nativeCacheKindWarning() {
        Assumptions.assumeTrue(currentOS == OS.MacOS)
        fun nativeCacheKindWarningProject(kotlinVersion: String) = testProject(
            TestProjects.nativeCacheKindWarning,
            defaultTestEnvironment.copy(kotlinVersion = kotlinVersion)
        )

        val cacheKindWarning = "'kotlin.native.cacheKind' is explicitly set to `none`"

        val args = arrayOf("build", "--dry-run", "-Pkotlin.native.cacheKind=none")
        with(nativeCacheKindWarningProject(kotlinVersion = TestKotlinVersions.v1_8_20)) {
            gradle(*args).checks {
                check.logContains(cacheKindWarning)
            }
        }
        testWorkDir.deleteRecursively()
        testWorkDir.mkdirs()
        with(nativeCacheKindWarningProject(kotlinVersion = TestKotlinVersions.v1_9_0) ) {
            gradle(*args).checks {
                check.logContains(cacheKindWarning)
            }
        }
    }

    @Test
    fun skikoWasm() = with(
        testProject(
            TestProjects.skikoWasm,
            // configuration cache is disabled as a temporary workaround for KT-58057
            // todo: enable once KT-58057 is fixed
            testEnvironment = defaultTestEnvironment.copy(useGradleConfigurationCache = false)
        )
    ) {
        fun jsCanvasEnabled(value: Boolean) {
            modifyGradleProperties { put("org.jetbrains.compose.experimental.jscanvas.enabled", value.toString()) }

        }

        jsCanvasEnabled(false)
        gradleFailure(":build").checks {
            check.logContains("ERROR: Compose targets '[jscanvas]' are experimental and may have bugs!")
        }

        jsCanvasEnabled(true)
        gradle(":build").checks {
            check.taskSuccessful(":unpackSkikoWasmRuntimeJs")
            check.taskSuccessful(":compileKotlinJs")
        }
    }

    @Test
    fun jsMppIsNotBroken() =
        with(
            testProject(
                TestProjects.jsMpp,
                testEnvironment = defaultTestEnvironment.copy(
                    kotlinVersion = TestProperties.composeJsCompilerCompatibleKotlinVersion
                )
            )
        ) {
            gradle(":compileKotlinJs").checks {
                check.taskSuccessful(":compileKotlinJs")
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

        val expectedReceivedConfigCount = 2
        val actualReceivedConfigCount = receivedConfigCount.get()
        check(actualReceivedConfigCount == 2) {
            "Expected to receive $expectedReceivedConfigCount preview configs, got $actualReceivedConfigCount"
        }
    }

    private fun testConfigureDesktopPreviewImpl(port: Int) {
        check(port > 0) { "Invalid port: $port" }
        with(testProject(TestProjects.jvmPreview)) {
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