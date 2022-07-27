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
import org.gradle.api.DefaultTask
import org.gradle.api.artifacts.component.ComponentArtifactIdentifier
import org.gradle.api.artifacts.component.ModuleComponentIdentifier
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.internal.component.external.model.DefaultModuleComponentIdentifier
import org.json.JSONArray

@CacheableTask
abstract class GenerateMetadataTask : DefaultTask() {

    /**
     * List of artifacts to convert to JSON
     */
    @Input
    abstract fun getArtifactIds(): ListProperty<ComponentArtifactIdentifier>

    /**
     * Location of the generated JSON file
     */
    @get:OutputFile
    abstract val destinationFile: RegularFileProperty

    @TaskAction
    fun generate() {
        val entries = arrayListOf<MetadataEntry>()
        getArtifactIds().get().forEach { id ->

            // Only process artifact if it can be cast to ModuleComponentIdentifier.
            //
            // In practice, metadata is generated only for docs-public and not docs-tip-of-tree
            // (where id.componentIdentifier is DefaultProjectComponentIdentifier).
            if (id.componentIdentifier !is DefaultModuleComponentIdentifier) return@forEach

            // Created https://github.com/gradle/gradle/issues/21415 to track surfacing
            // group / module / version in ComponentIdentifier
            val componentId = (id.componentIdentifier as ModuleComponentIdentifier)
            val entry = MetadataEntry(
                groupId = componentId.group,
                artifactId = componentId.module,
                releaseNotesUrl = generateReleaseNotesUrl(componentId.group),
                sourceDir = "TBD/SOURCE/DIR" // TODO: fetch from JAR file
            )
            entries.add(entry)
        }

        val jsonMapping = generateJsonMapping(entries)
        val json = JSONArray(jsonMapping)

        val outputFile = File(destinationFile.get().toString())
        outputFile.writeText(json.toString(2))
    }

    /**
     * Converts a list of [MetadataEntry] objects into a list of maps.
     */
    private fun generateJsonMapping(
        metadataEntries: List<MetadataEntry>
    ): List<Map<String, String>> {
        return metadataEntries.map { it.toMap() }
    }

    // TODO move to MetadataEntry + write test after MetadataEntry refactor
    private fun generateReleaseNotesUrl(group: String): String {
        // Example: androidx.arch.core => arch-core
        val library = group.removePrefix("androidx.").replace(".", "-")
        return "https://developer.android.com/jetpack/androidx/releases/$library"
    }
}
