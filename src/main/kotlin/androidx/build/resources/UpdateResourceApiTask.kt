/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.build.resources

import androidx.build.checkapi.ApiLocation
import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputFiles
import org.gradle.api.tasks.TaskAction
import java.io.File
import java.nio.file.Files
import java.util.SortedSet

/**
 * Task for updating the public Android resource surface, e.g. `public.xml`.
 */
abstract class UpdateResourceApiTask : DefaultTask() {
    /**
     * Text file from which resource API signatures will be read.
     *
     * A file path must be specified but the file may not exist if the library has no resources.
     */
    @get:InputFiles
    abstract val inputApiFile: RegularFileProperty

    /** Text files to which resource signatures will be written. */
    @get:Internal // outputs are declared in getTaskOutputs()
    abstract val outputApiLocations: ListProperty<ApiLocation>

    @OutputFiles
    fun getTaskOutputs(): List<File> {
        return outputApiLocations.get().flatMap { outputApiLocation ->
            listOf(
                outputApiLocation.resourceFile
            )
        }
    }

    @TaskAction
    fun updateResourceApi() {
        var permitOverwriting = true
        for (outputApi in outputApiLocations.get()) {
            val version = outputApi.version()
            if (version != null && version.isFinalApi() &&
                outputApi.publicApiFile.exists() &&
                !project.hasProperty("force")) {
                permitOverwriting = false
            }
        }

        val inputApi = inputApiFile.get().asFile

        // Read the current API surface, if any, into memory.
        val inputApiSet = if (inputApi.exists()) {
            HashSet(inputApi.readLines())
        } else {
            emptySet<String>()
        }

        // Sort the resources for the sake of source control diffs.
        val inputApiSortedSet: SortedSet<String> = inputApiSet.toSortedSet()

        // Write current API surface to temporary file.
        val tempApi = Files.createTempFile("res-api", "txt").toFile()
        tempApi.deleteOnExit()
        tempApi.bufferedWriter().use { out ->
            inputApiSortedSet.forEach { api ->
                out.write(api)
                out.newLine()
            }
        }

        for (outputApi in outputApiLocations.get()) {
            androidx.build.metalava.copy(
                tempApi,
                outputApi.resourceFile,
                permitOverwriting,
                project.logger
            )
        }
    }
}
