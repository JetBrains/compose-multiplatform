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

import androidx.build.checkapi.ApiLocation
import androidx.build.checkapi.ApiViolationExclusions
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputFiles
import org.gradle.api.tasks.TaskAction
import java.io.File

// Validate an API signature text file against a set of source files.
open class CheckApiCompatibilityTask : MetalavaTask() {
    // Text file from which the API signatures will be obtained.
    var referenceApi: ApiLocation? = null
    // Text file listing violations that should be ignored
    var exclusions: ApiViolationExclusions? = null

    // Whether to confirm that no restricted APIs were removed since the previous release
    var checkRestrictedAPIs = false

    @InputFiles
    fun getTaskInputs(): List<File> {
        if (checkRestrictedAPIs) {
            return referenceApi!!.files() + exclusions!!.files()
        }
        return listOf(referenceApi!!.publicApiFile, exclusions!!.publicApiFile)
    }

    // Declaring outputs prevents Gradle from rerunning this task if the inputs haven't changed
    @OutputFiles
    fun getTaskOutputs(): List<File> {
        return listOf(referenceApi!!.publicApiFile)
    }

    @TaskAction
    fun exec() {
        val referenceApi = checkNotNull(referenceApi) { "referenceApi not set." }
        val exclusions = checkNotNull(exclusions) { "exclusions not set." }

        check(bootClasspath.isNotEmpty()) { "Android boot classpath not set." }

        checkApiFile(referenceApi.publicApiFile, exclusions.publicApiFile, false)
        if (checkRestrictedAPIs) {
            checkApiFile(referenceApi.restrictedApiFile, exclusions.restrictedApiFile, true)
        }
    }

    // Confirms that the public API of this library (or the restricted API, if <checkRestrictedAPIs> is set
    // is compatible with <apiFile> except for any exclusions listed in <exclusionsFile>
    fun checkApiFile(apiFile: File, exclusionsFile: File, checkRestrictedAPIs: Boolean) {
        var args = listOf("--classpath",
                (bootClasspath + dependencyClasspath!!.files).joinToString(File.pathSeparator),

                "--source-path",
                sourcePaths.filter { it.exists() }.joinToString(File.pathSeparator),

                "--check-compatibility:api:released",
                apiFile.toString(),

                "--warnings-as-errors",
                "--format=v3"
        )
        if (exclusionsFile.exists()) {
            args = args + listOf("--baseline", exclusionsFile.toString())
        }
        if (checkRestrictedAPIs) {
            args = args + listOf("--show-annotation", "androidx.annotation.RestrictTo")
        }
        runWithArgs(args)
    }
}
