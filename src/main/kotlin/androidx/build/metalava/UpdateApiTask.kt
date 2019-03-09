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
import com.google.common.io.Files
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
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

    /** Whether to update restricted API files too */
    var updateRestrictedAPIs = false

    @InputFiles
    fun getTaskInputs(): List<File>? {
        return inputApiLocation?.files()
    }

    @OutputFiles
    fun getTaskOutputs(): List<File> {
        if (updateRestrictedAPIs) {
            return outputApiLocations.flatMap { it.files() }
        }
        return outputApiLocations.map { it.publicApiFile }
    }

    @TaskAction
    fun exec() {
        val inputPublicApi = checkNotNull(inputApiLocation?.publicApiFile) { "inputPublicApi not set" }
        val inputRestrictedApi = checkNotNull(inputApiLocation?.restrictedApiFile) { "inputRestrictedApi not set" }
        var permitOverwriting = true
        for (outputApi in outputApiLocations) {
            val version = outputApi.version()
            if (version != null && version.isFinalApi() && outputApi.publicApiFile.exists() && !project.hasProperty("force")) {
                permitOverwriting = false
            }
        }
        for (outputApi in outputApiLocations) {
            copy(inputPublicApi, outputApi.publicApiFile, permitOverwriting, project.logger)
            if (updateRestrictedAPIs) {
                copy(inputRestrictedApi, outputApi.restrictedApiFile, permitOverwriting, project.logger)
            }
        }
    }

    fun copy(source: File, dest: File, permitOverwriting: Boolean, logger: Logger) {
        val overwriting = (dest.exists() && source.readText() != dest.readText())
        val changing = overwriting || !dest.exists()
        if (changing) {
            if (overwriting && !permitOverwriting) {
                val message = "Modifying the API definition for a previously released artifact " +
                        "having a final API version (version not ending in '-alpha') is not " +
                        "allowed.\n\n" +
                        "Previously declared definition is $dest\n" +
                        "Current generated   definition is $source\n\n" +
                        "Did you mean to increment the library version first?\n\n" +
                        "If you have reason to overwrite the API files for the previous release " +
                        "anyway, you can run `./gradlew updateApi -Pforce` to ignore this message"
                throw GradleException(message)
            }
            Files.copy(source, dest)
            logger.lifecycle("Copied $source to $dest")
        }
    }
}
