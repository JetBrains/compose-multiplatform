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
import org.gradle.api.attributes.Attribute
import org.gradle.api.file.FileCollection
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputFiles
import org.gradle.api.tasks.TaskAction
import java.io.File

/** Validate an API signature text file against a set of source files. */
open class CheckApiCompatibilityTask : MetalavaTask() {
    /**
     * Text file from which the API signatures will be obtained.
     */
    var apiLocation: ApiLocation? = null

    @InputFiles
    fun getTaskInputs(): List<File>? {
        return apiLocation?.files()
    }

    /**
     * Declaring outputs prevents Gradle from rerunning this task if the inputs haven't changed
     */
    @OutputFiles
    fun getTaskOutputs(): List<File>? {
        return getTaskInputs()
    }

    @TaskAction
    fun exec() {
        val publicApiFile = checkNotNull(apiLocation?.publicApiFile) { "publicApiFile not set." }
        val restrictedApiFile = checkNotNull(apiLocation?.restrictedApiFile) { "restrictedApiFile not set." }

        check(bootClasspath.isNotEmpty()) { "Android boot classpath not set." }

        checkApiFile(publicApiFile, false)
        // checkApiFile(restrictedApiFile, true) // TODO(jeffrygaston) enable this once validation is fully ready (b/87457009)
    }


    fun checkApiFile(apiFile: File, checkRestrictedApis: Boolean) {
        var args = listOf("--classpath",
                (bootClasspath + dependencyClasspath!!.files).joinToString(File.pathSeparator),

                "--source-path",
                sourcePaths.filter { it.exists() }.joinToString(File.pathSeparator),

                "--check-compatibility:api:released",
                apiFile.toString(),

                "--compatible-output=no",
                "--omit-common-packages=yes",
                "--input-kotlin-nulls=yes"
        )
        if (checkRestrictedApis) {
            args = args + listOf("--show-annotation", "androidx.annotation.RestrictTo")
        }
        runWithArgs(args)
    }
}
