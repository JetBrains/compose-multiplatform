/*
 * Copyright 2019 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.benchmark.gradle

import org.gradle.api.GradleException
import org.gradle.api.logging.LogLevel
import org.gradle.api.logging.Logger
import java.util.concurrent.TimeUnit

/**
 * Helper class wrapping the adb cli tool.
 *
 * Provides an interface to execute adb commands in a way that automatically handles directing
 * stdout and stderr to gradle output. Typical usage of this class is as input into gradle tasks or
 * plugins that need to interact with adb.
 */
class Adb {
    data class ProcessResult(
        val exitValue: Int,
        val stdout: String,
        val stderr: String
    )

    private val adbPath: String
    private val logger: Logger

    constructor(adbPath: String, logger: Logger) {
        this.adbPath = adbPath
        this.logger = logger
    }

    /**
     * Check if adb shell runs as root by default.
     *
     * The most common case for this is when adbd is running as root.
     *
     * @return `true` if adb shell runs as root by default, `false` otherwise.
     */
    fun isAdbdRoot(): Boolean {
        val defaultUser = execSync("shell id").stdout
        return defaultUser.contains("uid=0(root)")
    }

    /**
     * Check if the `su` binary is installed.
     */
    fun isSuInstalled(): Boolean {
        // Not all devices / methods of rooting support su -c, but sh -c is usually supported.
        // Although the root group is su's default, using syntax different from "su gid cmd", can
        // cause the adb shell command to hang on some devices.
        return execSync("shell su 0 sh -c exit", shouldThrow = false).exitValue == 0
    }

    fun execSync(
        adbCmd: String,
        deviceId: String? = null,
        shouldThrow: Boolean = true,
        silent: Boolean = false
    ): ProcessResult {
        val subCmd = adbCmd.trim().split(Regex("\\s+")).toTypedArray()
        val adbArgs = if (!deviceId.isNullOrEmpty()) arrayOf("-s", deviceId) else emptyArray()
        val cmd = arrayOf(adbPath, *adbArgs, *subCmd)

        if (!silent) {
            logger.log(LogLevel.INFO, cmd.joinToString(" "))
        }
        val process = Runtime.getRuntime().exec(cmd)

        if (!process.waitFor(5, TimeUnit.SECONDS)) {
            throw GradleException("Timeout waiting for ${cmd.joinToString(" ")}")
        }

        val stdout = process.inputStream.bufferedReader().use { it.readText() }
        val stderr = process.errorStream.bufferedReader().use { it.readText() }

        if (!stdout.isBlank() && !silent) {
            logger.log(LogLevel.QUIET, stdout.trim())
        }

        if (!stderr.isBlank() && shouldThrow && !silent) {
            logger.log(LogLevel.ERROR, stderr.trim())
        }

        if (shouldThrow && process.exitValue() != 0) {
            throw GradleException(stderr)
        }

        return ProcessResult(process.exitValue(), stdout, stderr)
    }
}