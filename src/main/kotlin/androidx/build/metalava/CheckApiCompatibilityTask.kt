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

package androidx.build.metalava

import androidx.build.checkapi.ApiBaselinesLocation
import androidx.build.checkapi.ApiLocation
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputFiles
import org.gradle.api.tasks.TaskAction
import org.gradle.workers.WorkerExecutor
import java.io.File
import javax.inject.Inject

/**
 * This task validates that the API described in one signature txt file is compatible with the API
 * in another.
 */
abstract class CheckApiCompatibilityTask @Inject constructor(
    workerExecutor: WorkerExecutor
) : MetalavaTask(workerExecutor) {
    // Text file from which the API signatures will be obtained.
    @get:Input
    abstract val referenceApi: Property<ApiLocation>

    // Text file representing the current API surface to check.
    @get:Input
    abstract val api: Property<ApiLocation>

    // Text file listing violations that should be ignored.
    @get:Input
    abstract val baselines: Property<ApiBaselinesLocation>

    @InputFiles
    fun getTaskInputs(): List<File> {
        val apiLocation = api.get()
        val referenceApiLocation = referenceApi.get()
        val baselineApiLocation = baselines.get()
        return listOf(
            apiLocation.publicApiFile,
            apiLocation.restrictedApiFile,
            referenceApiLocation.publicApiFile,
            referenceApiLocation.restrictedApiFile,
            baselineApiLocation.publicApiFile,
            baselineApiLocation.restrictedApiFile
        )
    }

    // Declaring outputs prevents Gradle from rerunning this task if the inputs haven't changed
    @OutputFiles
    fun getTaskOutputs(): List<File> {
        return listOf(referenceApi.get().publicApiFile)
    }

    @TaskAction
    fun exec() {
        check(bootClasspath.isNotEmpty()) { "Android boot classpath not set." }

        val apiLocation = api.get()
        val referenceApiLocation = referenceApi.get()
        val baselineApiLocation = baselines.get()

        checkApiFile(
            apiLocation.publicApiFile,
            referenceApiLocation.publicApiFile,
            baselineApiLocation.publicApiFile
        )

        if (referenceApiLocation.restrictedApiFile.exists()) {
            checkApiFile(
                apiLocation.restrictedApiFile,
                referenceApiLocation.restrictedApiFile,
                baselineApiLocation.restrictedApiFile
            )
        }
    }

    // Confirms that <api> is compatible with <oldApi> except for any baselines listed in <baselineFile>
    fun checkApiFile(api: File, oldApi: File, baselineFile: File) {
        var args = listOf(
            "--classpath",
            (bootClasspath + dependencyClasspath.files).joinToString(File.pathSeparator),

            "--source-files",
            api.toString(),

            "--check-compatibility:api:released",
            oldApi.toString(),

            "--warnings-as-errors",
            "--format=v3"
        )
        if (baselineFile.exists()) {
            args = args + listOf("--baseline", baselineFile.toString())
        }
        runWithArgs(args)
    }
}
