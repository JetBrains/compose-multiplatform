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
import androidx.build.checkapi.ApiViolationBaselines
import androidx.build.java.JavaCompileInputs
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputFiles
import org.gradle.api.tasks.TaskAction
import java.io.File

/** Generate an API signature text file from a set of source files. */
abstract class GenerateApiTask : MetalavaTask() {
    /** Text file to which API signatures will be written. */
    @get:Input
    abstract val apiLocation: Property<ApiLocation>

    @get:Input
    abstract val baselines: Property<ApiViolationBaselines>

    @get:Input
    var generateRestrictedAPIs = false

    @Optional
    @InputFile
    fun getApiLintBaseline(): File? {
        val baseline = baselines.get().apiLintFile
        return if (baseline.exists()) baseline else null
    }

    @OutputFiles
    fun getTaskOutputs(): List<File>? {
        if (generateRestrictedAPIs) {
            return apiLocation.get().files()
        }
        return listOf(apiLocation.get().publicApiFile)
    }

    @TaskAction
    fun exec() {
        val dependencyClasspath = checkNotNull(
                dependencyClasspath) { "Dependency classpath not set." }
        check(bootClasspath.isNotEmpty()) { "Android boot classpath not set." }
        check(sourcePaths.isNotEmpty()) { "Source paths not set." }

        val inputs = JavaCompileInputs.fromSourcesAndDeps(sourcePaths, dependencyClasspath,
            project)
        project.generateApi(inputs, apiLocation.get(), apiLocation.get().publicApiFile.parentFile,
            ApiLintMode.CheckBaseline(baselines.get().apiLintFile), generateRestrictedAPIs)
    }
}
