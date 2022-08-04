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

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import java.io.FileWriter
import java.util.zip.ZipFile
import org.gradle.api.DefaultTask
import org.gradle.api.artifacts.component.ComponentArtifactIdentifier
import org.gradle.api.artifacts.component.ModuleComponentIdentifier
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.internal.component.external.model.DefaultModuleComponentIdentifier
import org.gradle.internal.component.local.model.ComponentFileArtifactIdentifier

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

    /**
     * Location of the prebuilts root directory
     */
    @get:Input
    abstract val prebuiltsRoot: Property<String>

    @TaskAction
    fun generate() {
        val entries = arrayListOf<MetadataEntry>()
        val androidXBasePath = "${prebuiltsRoot.get()}/androidx/internal"

        getArtifactIds().get().forEach { id ->

            // Only process artifact if it can be cast to ModuleComponentIdentifier.
            //
            // In practice, metadata is generated only for docs-public and not docs-tip-of-tree
            // (where id.componentIdentifier is DefaultProjectComponentIdentifier).
            if (id.componentIdentifier !is DefaultModuleComponentIdentifier) return@forEach

            // Created https://github.com/gradle/gradle/issues/21415 to track surfacing
            // group / module / version in ComponentIdentifier
            val componentId = (id.componentIdentifier as ModuleComponentIdentifier)

            // Locate the .jar file associated with this artifact and fetch the list of files
            // contained in the .jar file
            val jarFilename = (id as ComponentFileArtifactIdentifier).fileName
            val componentIdPath = componentId.group.replace(".", "/")
            val jarLocation = "$androidXBasePath/$componentIdPath/${componentId.module}/" +
                "${componentId.version}/$jarFilename"
            val fileList = ZipFile(jarLocation).entries().toList().map { it.name }

            val entry = MetadataEntry(
                groupId = componentId.group,
                artifactId = componentId.module,
                releaseNotesUrl = generateReleaseNotesUrl(componentId.group),
                jarContents = fileList
            )
            entries.add(entry)
        }

        val gson = if (DEBUG) {
            GsonBuilder().setPrettyPrinting().create()
        } else {
            Gson()
        }
        val writer = FileWriter(destinationFile.get().toString())
        gson.toJson(entries, writer)
        writer.close()
    }

    private fun generateReleaseNotesUrl(groupId: String): String {
        val library = groupId.removePrefix("androidx.").replace(".", "-")
        return "https://developer.android.com/jetpack/androidx/releases/$library"
    }

    companion object {
        private const val DEBUG = false
    }
}
