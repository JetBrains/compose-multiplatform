/*
 * Copyright 2021 The Android Open Source Project
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

import org.gradle.api.DefaultTask
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault
import java.io.File
import java.lang.management.ManagementFactory
import java.lang.Runtime
import com.sun.management.OperatingSystemMXBean

@DisableCachingByDefault(because = "Simply creates a small file and doesn't benefit from caching")
/**
 * Saves system stats (cpu, memory) to a file
 */
abstract class SaveSystemStatsTask : DefaultTask() {
    @Input
    fun getNumProcessors(): Int {
        return Runtime.getRuntime().availableProcessors()
    }

    @Input
    @Suppress("DEPRECATION")
    fun getTotalMemory(): Long {
        val bean = ManagementFactory.getOperatingSystemMXBean() as OperatingSystemMXBean
        return bean.totalPhysicalMemorySize
    }

    @OutputFile
    val outputFile: Property<File> = project.objects.property(File::class.java)
    @TaskAction
    fun exec() {
        val outputFile = outputFile.get()
        if (outputFile.exists()) {
            // b/196115864 : make backup of file so we can know what changed
            outputFile.copyTo(target = File(outputFile.path + ".prev"), overwrite = true)
        }
        val statsText = "num processors = ${getNumProcessors()}, total memory = ${getTotalMemory()}"
        outputFile.writeText(statsText)
    }
}
