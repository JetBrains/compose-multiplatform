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
import org.gradle.api.DefaultTask
import org.gradle.api.file.FileCollection
import org.gradle.api.logging.Logger
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputFiles
import org.gradle.api.tasks.TaskAction
import java.io.File

/**
 * Updates API signature text files.
 * In practice, the values they will be updated to will match the APIs defined by the source code.
 */
open class UpdateApiTask : DefaultTask() {
    /** Text file from which API signatures will be read. */
    var inputApiLocation: ApiLocation? = null

    /** Text files to which API signatures will be written. */
    var outputApiLocations: List<ApiLocation> = listOf()

    @InputFiles
    fun getTaskInputs(): List<File>? {
        return inputApiLocation?.files()
    }

    @OutputFiles
    fun getTaskOutputs(): List<File> {
        return outputApiLocations.flatMap { it.files() }
    }

    @TaskAction
    fun exec() {
        val inputPublicApi = checkNotNull(inputApiLocation?.publicApiFile) { "inputPublicApi not set" }
        val inputRestrictedApi = checkNotNull(inputApiLocation?.restrictedApiFile) { "inputRestrictedApi not set" }
        for (outputApi in outputApiLocations) {
            copy(inputPublicApi, outputApi.publicApiFile, project.logger)

            // TODO(jeffrygaston) enable this once validation is fully ready (b/87457009)
            // copy(inputRestrictedApi, outputApi.restrictedApiFile, project.logger)
        }
    }

    fun copy(source: File, dest: File, logger: Logger) {
        Files.copy(source, dest)
        logger.lifecycle("Copied ${source} to ${dest}")
    }
}
