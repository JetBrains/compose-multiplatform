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
import androidx.build.logging.TERMINAL_RED
import androidx.build.logging.TERMINAL_RESET
import org.gradle.api.provider.Property
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.workers.WorkerExecutor
import java.io.File
import javax.inject.Inject

/**
 * This task validates that the API described in one signature txt file is compatible with the API
 * in another.
 */
@CacheableTask
abstract class CheckApiCompatibilityTask @Inject constructor(
    workerExecutor: WorkerExecutor
) : MetalavaTask(workerExecutor) {
    // Text file from which the API signatures will be obtained.
    @get:Internal // already expressed by getTaskInputs()
    abstract val referenceApi: Property<ApiLocation>

    // Text file representing the current API surface to check.
    @get:Internal // already expressed by getTaskInputs()
    abstract val api: Property<ApiLocation>

    // Text file listing violations that should be ignored.
    @get:Internal // already expressed by getTaskInputs()
    abstract val baselines: Property<ApiBaselinesLocation>

    @PathSensitive(PathSensitivity.RELATIVE)
    @InputFiles
    fun getTaskInputs(): List<File> {
        val apiLocation = api.get()
        val referenceApiLocation = referenceApi.get()
        val baselineApiLocation = baselines.get()
        return listOf(
            apiLocation.publicApiFile,
            apiLocation.restrictedApiFile,
            apiLocation.removedApiFile,
            referenceApiLocation.publicApiFile,
            referenceApiLocation.restrictedApiFile,
            referenceApiLocation.removedApiFile,
            baselineApiLocation.publicApiFile,
            baselineApiLocation.restrictedApiFile
        )
    }

    @TaskAction
    fun exec() {
        check(bootClasspath.files.isNotEmpty()) { "Android boot classpath not set." }

        val apiLocation = api.get()
        val referenceApiLocation = referenceApi.get()
        val baselineApiLocation = baselines.get()

        checkApiFile(
            apiLocation.publicApiFile,
            apiLocation.removedApiFile,
            referenceApiLocation.publicApiFile,
            referenceApiLocation.removedApiFile,
            baselineApiLocation.publicApiFile
        )

        if (referenceApiLocation.restrictedApiFile.exists()) {
            checkApiFile(
                apiLocation.restrictedApiFile,
                null, // removed api
                referenceApiLocation.restrictedApiFile,
                null, // removed api
                baselineApiLocation.restrictedApiFile
            )
        }
    }

    // Confirms that <api>+<removedApi> is compatible with
    // <oldApi>+<oldRemovedApi> except for any baselines listed in <baselineFile>
    fun checkApiFile(
        api: File,
        removedApi: File?,
        oldApi: File,
        oldRemovedApi: File?,
        baselineFile: File
    ) {
        var args = listOf(
            "--classpath",
            (bootClasspath + dependencyClasspath.files).joinToString(File.pathSeparator),

            "--source-files",
            api.toString(),

            "--check-compatibility:api:released",
            oldApi.toString(),

            "--error-message:compatibility:released",
            CompatibilityCheckError,

            "--warnings-as-errors",
            "--format=v3"
        )
        if (removedApi != null && removedApi.exists()) {
            args = args + listOf("--source-files", removedApi.toString())
        }
        if (oldRemovedApi != null && oldRemovedApi.exists()) {
            args = args + listOf("--check-compatibility:removed:released", oldRemovedApi.toString())
        }
        if (baselineFile.exists()) {
            args = args + listOf("--baseline", baselineFile.toString())
        }
        runWithArgs(args)
    }
}

private const val CompatibilityCheckError = """
    ${TERMINAL_RED}Your change has API compatibility issues. Fix the code according to the messages above.$TERMINAL_RESET

    If you *intentionally* want to break compatibility, you can suppress it with
    ./gradlew ignoreApiChange && ./gradlew updateApi
"""