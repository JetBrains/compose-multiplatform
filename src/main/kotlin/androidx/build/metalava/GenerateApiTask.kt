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
import com.android.build.gradle.BaseExtension
import com.android.build.gradle.api.BaseVariant
import com.google.common.io.Files
import org.gradle.api.attributes.Attribute
import org.gradle.api.file.FileCollection
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputFiles
import org.gradle.api.tasks.TaskAction
import java.io.File

/** Generate an API signature text file from a set of source files. */
open class GenerateApiTask : MetalavaTask() {
    /** Text file to which API signatures will be written. */
    var apiLocation: ApiLocation? = null

    @OutputFiles
    fun getTaskOutputs(): List<File>? {
        return apiLocation?.files()
    }

    @TaskAction
    fun exec() {
        val dependencyClasspath = checkNotNull(
                dependencyClasspath) { "Dependency classpath not set." }
        val publicApiFile = checkNotNull(apiLocation?.publicApiFile) { "Current public API file not set." }
        val restrictedApiFile = checkNotNull(apiLocation?.restrictedApiFile) { "Current restricted API file not set." }
        check(bootClasspath.isNotEmpty()) { "Android boot classpath not set." }
        check(sourcePaths.isNotEmpty()) { "Source paths not set." }

        runWithArgs(
            "--classpath",
            (bootClasspath + dependencyClasspath.files).joinToString(File.pathSeparator),

            "--source-path",
            sourcePaths.filter { it.exists() }.joinToString(File.pathSeparator),

            "--api",
            publicApiFile.toString(),

            "--compatible-output=no",
            "--omit-common-packages=yes",
            "--output-kotlin-nulls=yes"
        )

        runWithArgs(
            "--classpath",
            (bootClasspath + dependencyClasspath.files).joinToString(File.pathSeparator),

            "--source-path",
            sourcePaths.filter { it.exists() }.joinToString(File.pathSeparator),

            "--api",
            restrictedApiFile.toString(),

            "--show-annotation",
            "androidx.annotation.RestrictTo",

            "--compatible-output=no",
            "--omit-common-packages=yes",
            "--output-kotlin-nulls=yes"
        )
    }
}
