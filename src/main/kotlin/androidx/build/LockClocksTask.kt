/*
 * Copyright 2018 The Android Open Source Project
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

package androidx.build

import org.apache.commons.io.IOUtils
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.logging.LogLevel
import org.gradle.api.tasks.TaskAction
import java.io.InputStream
import java.util.concurrent.TimeUnit

open class ClockTask : DefaultTask() {
    init {
        group = "Android"
    }

    fun runAdb(adbCommand: Array<String>, errorString: String) {
        val adbPath = getSdkPath(project.projectDir).path + "/platform-tools/adb"

        val logCommand = "'adb ${adbCommand.joinToString(" ")}'"
        logger.log(LogLevel.QUIET, "running: $logCommand")
        val command = arrayOf(adbPath, *adbCommand)

        val process = Runtime.getRuntime().exec(command)

        if (!process.waitFor(5, TimeUnit.SECONDS)) {
            throw GradleException("Timeout waiting for $logCommand")
        }

        logStream(LogLevel.QUIET, process.inputStream)
        logStream(LogLevel.WARN, process.errorStream)

        if (process.exitValue() != 0) {
            throw GradleException(errorString)
        }
    }

    private fun logStream(level: LogLevel, stream: InputStream) {
        val string = IOUtils.toString(stream)
        if (string.isNotEmpty()) {
            logger.log(level, string)
        }
    }
}

open class LockClocksTask : ClockTask() {
    init {
        description = "locks clocks of connected, supported, rooted device"
    }

    @Suppress("unused")
    @TaskAction
    fun exec() {
        val source = project.projectDir.path +
                "/benchmark/gradle-plugin/src/main/resources/scripts/lockClocks.sh"
        val dest = "/data/local/tmp/lockClocks.sh"
        runAdb(arrayOf("root"), "Failed to run 'adb root'")
        runAdb(arrayOf("push", source, dest), "Failed to push locking script")
        runAdb(arrayOf("shell", dest), "Failed to run clock locking script")
        runAdb(arrayOf("shell", "rm", dest), "Failed to remove clock locking script")
    }
}

open class UnlockClocksTask : ClockTask() {
    init {
        description = "unlocks clocks of device by rebooting"
    }

    @Suppress("unused")
    @TaskAction
    fun exec() {
        project.logger.log(LogLevel.LIFECYCLE, "Rebooting device to reset clocks")
        runAdb(arrayOf("reboot"), "Failed to reboot device")
    }
}

fun Project.createClockLockTasks() {
    tasks.create("lockClocks", LockClocksTask::class.java)
    tasks.create("unlockClocks", UnlockClocksTask::class.java)
}
