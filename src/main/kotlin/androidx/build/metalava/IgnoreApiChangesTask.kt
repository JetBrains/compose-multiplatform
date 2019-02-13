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

package androidx.build.metalava

import androidx.build.checkapi.ApiLocation
import androidx.build.checkapi.ApiViolationExclusions
import com.google.common.io.Files
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputFiles
import org.gradle.api.tasks.TaskAction
import java.io.File

// Updates an API tracking exceptions file (api/*.*.*.ignore) to match the current set of violations
open class IgnoreApiChangesTask : MetalavaTask() {
    // The API that the library is supposed to be compatible with
    var referenceApi: ApiLocation? = null

    // The exclusions files (api/*.*.*.ignore) to update
    var exclusions: ApiViolationExclusions? = null

    // Whether to update the file having restricted APIs too
    var processRestrictedAPIs = false

    // Path of temporary api-changes exemptions file
    var intermediateExclusionsFile: File? = null

    @InputFiles
    fun getTaskInputs(): List<File> {
        if (processRestrictedAPIs) {
            return referenceApi!!.files()
        }
        return listOf(referenceApi!!.publicApiFile)
    }

    // Declaring outputs prevents Gradle from rerunning this task if the inputs haven't changed
    @OutputFiles
    fun getTaskOutputs(): List<File>? {
        if (processRestrictedAPIs) {
            return exclusions!!.files()
        }
        return listOf(exclusions!!.publicApiFile)
    }

    @TaskAction
    fun exec() {
        val referenceApi = checkNotNull(referenceApi) { "referenceApi not set." }
        val exclusions = checkNotNull(exclusions) { "exclusions not set." }

        check(bootClasspath.isNotEmpty()) { "Android boot classpath not set." }

        updateExclusions(referenceApi.publicApiFile, exclusions.publicApiFile, false)
        if (processRestrictedAPIs && referenceApi.restrictedApiFile.exists()) {
            updateExclusions(referenceApi.restrictedApiFile, exclusions.restrictedApiFile, true)
        }
    }

    // Updates the contents of exclusionsFile to specify an exception for every API present in apiFile but not
    // present in the current source path
    fun updateExclusions(apiFile: File, exclusionsFile: File, processRestrictedAPIs: Boolean) {
        val intermediateExclusionsFile = checkNotNull(intermediateExclusionsFile) { "intermediateExclusionsFile not set" }
        intermediateExclusionsFile.parentFile.mkdirs()
        intermediateExclusionsFile.createNewFile()

        var args = listOf("--classpath",
            (bootClasspath + dependencyClasspath!!.files).joinToString(File.pathSeparator),

            "--source-path",
            sourcePaths.filter { it.exists() }.joinToString(File.pathSeparator),

            "--check-compatibility:api:released",
            apiFile.toString(),

            "--update-baseline",
            intermediateExclusionsFile.toString(),
            "--baseline", intermediateExclusionsFile.toString(),
            "--pass-baseline-updates",

            "--format=v3",
            "--omit-common-packages=yes"
        )
        if (processRestrictedAPIs) {
            args = args + listOf("--show-annotation", "androidx.annotation.RestrictTo")
        }
        runWithArgs(args)

        if (intermediateExclusionsFile.length() > 0) {
            Files.copy(intermediateExclusionsFile, exclusionsFile)
        } else {
            if (exclusionsFile.exists()) {
                exclusionsFile.delete()
            }
        }
    }
}
