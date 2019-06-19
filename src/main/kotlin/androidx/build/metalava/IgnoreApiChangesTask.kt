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
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputFiles
import org.gradle.api.tasks.TaskAction
import java.io.File

// Updates an API tracking exceptions file (api/*.*.*.ignore) to match the current set of violations
abstract class IgnoreApiChangesTask : MetalavaTask() {
    // The API that the library is supposed to be compatible with
    @get:Input
    abstract val referenceApi: Property<ApiLocation>

    // The exclusions files (api/*.*.*.ignore) to update
    @get:Input
    abstract val exclusions: Property<ApiViolationExclusions>

    // Whether to update the file having restricted APIs too
    @get:Input
    var processRestrictedAPIs = false

    // Path of temporary api-changes exemptions file
    abstract val intermediateExclusionsFile: Property<File>

    @InputFiles
    fun getTaskInputs(): List<File> {
        if (processRestrictedAPIs) {
            return referenceApi.get().files()
        }
        return listOf(referenceApi.get().publicApiFile)
    }

    // Declaring outputs prevents Gradle from rerunning this task if the inputs haven't changed
    @OutputFiles
    fun getTaskOutputs(): List<File>? {
        if (processRestrictedAPIs) {
            return exclusions.get().files()
        }
        return listOf(exclusions.get().publicApiFile)
    }

    @TaskAction
    fun exec() {
        check(bootClasspath.isNotEmpty()) { "Android boot classpath not set." }

        updateExclusions(referenceApi.get().publicApiFile, exclusions.get().publicApiFile, false)
        if (processRestrictedAPIs && referenceApi.get().restrictedApiFile.exists()) {
            updateExclusions(referenceApi.get().restrictedApiFile,
                exclusions.get().restrictedApiFile,
                true)
        }
    }

    // Updates the contents of exclusionsFile to specify an exception for every API present in apiFile but not
    // present in the current source path
    fun updateExclusions(apiFile: File, exclusionsFile: File, processRestrictedAPIs: Boolean) {
        val intermediateExclusions = intermediateExclusionsFile.get()
        intermediateExclusions.parentFile.mkdirs()
        intermediateExclusions.createNewFile()

        var args = listOf("--classpath",
            (bootClasspath + dependencyClasspath!!.files).joinToString(File.pathSeparator),

            "--source-path",
            sourcePaths.filter { it.exists() }.joinToString(File.pathSeparator),

            "--check-compatibility:api:released",
            apiFile.toString(),

            "--update-baseline",
            intermediateExclusions.toString(),
            "--baseline", intermediateExclusions.toString(),
            "--pass-baseline-updates",

            "--format=v3",
            "--omit-common-packages=yes"
        )
        if (processRestrictedAPIs) {
            args = args + listOf("--show-annotation", "androidx.annotation.RestrictTo")
        }
        runWithArgs(args)

        var moreThanHeader = false
        intermediateExclusions.forEachLine {
            if (!it.startsWith("// Baseline format: ")) {
                moreThanHeader = true
                return@forEachLine
            }
        }
        if (moreThanHeader) {
            Files.copy(intermediateExclusions, exclusionsFile)
        } else {
            if (exclusionsFile.exists()) {
                exclusionsFile.delete()
            }
        }
    }
}
