/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.build.dackka

import java.io.File
import java.io.Serializable
import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.json.JSONArray

@CacheableTask
abstract class GenerateMetadataTask : DefaultTask() {

    /**
     * List of [MetadataEntry] objects to convert to JSON
     */
    @get:Input
    abstract val metadataEntries: ListProperty<MetadataEntry>

    /**
     * Location of the generated JSON file
     */
    @get:OutputFile
    abstract val destinationFile: RegularFileProperty

    @TaskAction
    fun generate() {
        val jsonMapping = generateJsonMapping(metadataEntries)
        val json = JSONArray(jsonMapping)

        val outputFile = File(destinationFile.get().toString())
        outputFile.writeText(json.toString(2))
    }

    /**
     * Converts a list of [MetadataEntry] objects into a list of maps.
     */
    @OptIn(ExperimentalStdlibApi::class)
    private fun generateJsonMapping(
        metadata: ListProperty<MetadataEntry>
    ): List<Map<String, String>> {
        return buildList {
            metadata.get().forEach { entry ->
                val map = mapOf(
                    "groupId" to entry.groupId,
                    "artifactId" to entry.artifactId,
                    "releaseNotesUrl" to entry.releaseNotesUrl,
                    "sourceDir" to entry.sourceDir
                )
                add(map)
            }
        }
    }
}

/**
 * Helper data class to store the metadata information for each library/path.
 */
data class MetadataEntry(
    val groupId: String,
    val artifactId: String,
    val releaseNotesUrl: String,
    val sourceDir: String,
) : Serializable