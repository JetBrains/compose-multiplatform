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
import org.gradle.api.GradleException
import org.gradle.api.tasks.OutputFiles
import org.gradle.api.tasks.TaskAction
import java.io.File

/** Generate an API signature text file from a set of source files. */
open class GenerateApiTask : MetalavaTask() {
    /** Text file to which API signatures will be written. */
    var apiLocation: ApiLocation? = null

    var generateRestrictedAPIs = false

    @OutputFiles
    fun getTaskOutputs(): List<File>? {
        if (generateRestrictedAPIs) {
            return apiLocation?.files()
        }
        return listOf(apiLocation!!.publicApiFile)
    }

    @TaskAction
    fun exec() {
        val dependencyClasspath = checkNotNull(
                dependencyClasspath) { "Dependency classpath not set." }
        val publicApiFile = checkNotNull(apiLocation?.publicApiFile) { "Current public API file not set." }
        val restrictedApiFile = checkNotNull(apiLocation?.restrictedApiFile) { "Current restricted API file not set." }
        check(bootClasspath.isNotEmpty()) { "Android boot classpath not set." }
        check(sourcePaths.isNotEmpty()) { "Source paths not set." }

        // generate public API txt
        runWithArgs(
            "--classpath",
            (bootClasspath + dependencyClasspath.files).joinToString(File.pathSeparator),

            "--source-path",
            sourcePaths.filter { it.exists() }.joinToString(File.pathSeparator),

            "--api",
            publicApiFile.toString(),

            "--format=v3",
            "--output-kotlin-nulls=yes"
        )

        if (generateRestrictedAPIs) {
            // generate restricted API txt
            val metalavaRestrictedOutputFile = File(restrictedApiFile.path + ".tmp")
            runWithArgs(
                "--classpath",
                (bootClasspath + dependencyClasspath.files).joinToString(File.pathSeparator),

                "--source-path",
                sourcePaths.filter { it.exists() }.joinToString(File.pathSeparator),

                "--api",
                metalavaRestrictedOutputFile.toString(),

                "--show-annotation",
                "androidx.annotation.RestrictTo",

                "--format=v3",
                "--output-kotlin-nulls=yes"
            )

            removeRestrictToLibraryLines(metalavaRestrictedOutputFile, restrictedApiFile)
        }
    }

    // until b/119617147 is done, remove lines containing "@RestrictTo(androidx.annotation.RestrictTo.Scope.LIBRARY)"
    fun removeRestrictToLibraryLines(inputFile: File, outputFile: File) {
        val outputBuilder = StringBuilder()
        val lines = inputFile.readLines()
        var skipScopeUntil: String? = null
        for (line in lines) {
            val skip = line.contains("@RestrictTo(androidx.annotation.RestrictTo.Scope.LIBRARY)")
            if (skip && line.endsWith("{")) {
                skipScopeUntil = line.commonPrefixWith("    ") + "}"
            }
            if (!skip && skipScopeUntil == null) {
                outputBuilder.append(line)
                outputBuilder.append("\n")
            }
            if (line == skipScopeUntil) {
                skipScopeUntil = null
            }
        }
        if (skipScopeUntil != null) {
            throw GradleException("Skipping until `$skipScopeUntil`, but found EOF")
        }
        outputFile.writeText(outputBuilder.toString())
    }
}
