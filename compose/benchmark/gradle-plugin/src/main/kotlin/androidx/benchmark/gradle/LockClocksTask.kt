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

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction
import org.gradle.kotlin.dsl.property
import org.gradle.work.DisableCachingByDefault
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardCopyOption

@Suppress("UnstableApiUsage")
@DisableCachingByDefault(
    because = "LockClocks affects device state, and may be modified/reset outside of this task"
)
open class LockClocksTask : DefaultTask() {
    init {
        group = "Android"
        description = "locks clocks of connected, supported, rooted device"
    }

    @Input
    val adbPath: Property<String> = project.objects.property()

    @Suppress("unused")
    @TaskAction
    fun exec() {
        val adb = Adb(adbPath.get(), logger)

        adb.execSync("root", silent = true, shouldThrow = false)

        val isAdbdRoot = adb.isAdbdRoot()
        val isRooted = isAdbdRoot || adb.isSuInstalled()

        if (!isRooted) {
            throw GradleException("Your device must be rooted to lock clocks.")
        }

        val dest = "/data/local/tmp/lockClocks.sh"
        val source = javaClass.classLoader.getResource("scripts/lockClocks.sh")
        val tmpSource = Files.createTempFile("lockClocks.sh", null).toString()
        Files.copy(
            source.openStream(),
            Paths.get(tmpSource),
            StandardCopyOption.REPLACE_EXISTING
        )
        adb.execSync("push $tmpSource $dest")

        // Files pushed by adb push don't always preserve file permissions.
        adb.execSync("shell chmod 700 $dest")

        // Forward gradle arguments to lockClocks.sh.
        val coresArg = project.findProperty("androidx.benchmark.lockClocks.cores")

        if (!isAdbdRoot) {
            // Default shell is not running as root, escalate with su 0. Although the root group is
            // su's default, using syntax different from "su gid cmd", can cause the adb shell
            // command to hang on some devices.
            adb.execSync("shell su 0 $dest ${coresArg ?: ""}")
        } else {
            adb.execSync("shell $dest ${coresArg ?: ""}")
        }
        adb.execSync("shell rm $dest")
    }
}
