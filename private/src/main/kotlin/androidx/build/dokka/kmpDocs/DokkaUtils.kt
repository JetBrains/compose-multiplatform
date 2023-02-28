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

package androidx.build.dokka.kmpDocs

import androidx.build.getLibraryByName
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonElement
import com.google.gson.JsonPrimitive
import com.google.gson.JsonSerializationContext
import com.google.gson.JsonSerializer
import java.io.File
import java.lang.reflect.Type
import org.gradle.api.NamedDomainObjectProvider
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.file.FileCollection

internal object DokkaUtils {
    /**
     * List of Dokka plugins that needs to be added to produce partial docs for a project.
     */
    private val REQUIRED_PLUGIN_LIBRARIES = listOf(
        "dokkaBase",
        "kotlinXHtml",
        "dokkaAnalysis",
        "dokkaAnalysisIntellij",
        "dokkaAnalysisCompiler",
        "freemarker",
        "dokkaAndroidDocumentation",
    )

    /**
     * List of Dokka plugins that needs to be added to produce combined docs for a set of partial
     * docs.
     */
    val COMBINE_PLUGIN_LIBRARIES = listOf(
        "dokkaAllModules",
        "dokkaTemplating",
    )

    /**
     * The CLI executable of Dokka.
     */
    private val CLI_JAR_COORDINATES = listOf(
        "dokkaCli"
    )

    private const val CS_ANDROID_SRC_ROOT =
        "https://cs.android.com/androidx/platform/frameworks/support/+/"

    /**
     * Placeholder URL that use used by the first step of partial docs generation.
     * This placeholder is later replaced by the HEAD sha of the repository.
     * It needs to look like a URL because Dokka parses it as URL.
     */
    internal const val CS_ANDROID_PLACEHOLDER = "https://__DOKKA_REPLACE_WITH_SRC_LINK__.com/"

    fun createCsAndroidUrl(sha: String) = "$CS_ANDROID_SRC_ROOT$sha:"

    /**
     * Creates a configuration for the [project] from the given set of dokka plugin libraries.
     * These libraries are defined in the Gradle Version catalog.
     * @see COMBINE_PLUGIN_LIBRARIES
     * @see REQUIRED_PLUGIN_LIBRARIES
     */
    fun createPluginsConfiguration(
        project: Project,
        additionalPluginLibraries: List<String> = emptyList()
    ): NamedDomainObjectProvider<Configuration> {
        return project.configurations.register("dokkaCliPlugins") { config ->
            (REQUIRED_PLUGIN_LIBRARIES + additionalPluginLibraries).forEach {
                config.dependencies.add(
                    project.dependencies.create(
                        // find the coordinates from the version catalog
                        project.getLibraryByName(it)
                    )
                )
            }
        }
    }

    /**
     * Creates a configuration to resolve dokka cli jar and its dependencies.
     */
    internal fun createCliJarConfiguration(
        project: Project
    ): NamedDomainObjectProvider<Configuration> {
        return project.configurations.register("dokkaCliJar") { config ->
            CLI_JAR_COORDINATES.forEach {
                config.dependencies.add(
                    project.dependencies.create(
                        // find the coordinates from the version catalog
                        project.getLibraryByName(it)
                    )
                )
            }
        }
    }

    /**
     * Creates a GSON instance that can be used to serialize Dokka CLI json models.
     */
    fun createGson(): Gson = GsonBuilder().setPrettyPrinting()
        .registerTypeAdapter(
            File::class.java, CanonicalFileSerializer()
        ).registerTypeAdapter(
            FileCollection::class.java,
            FileCollectionSerializer()
        )
        .create()

    /**
     * Serializer for Gradle's [FileCollection]
     */
    private class FileCollectionSerializer : JsonSerializer<FileCollection> {
        override fun serialize(
            src: FileCollection,
            typeOfSrc: Type,
            context: JsonSerializationContext
        ): JsonElement {
            return context.serialize(src.files)
        }
    }

    /**
     * Serializer for [File] instances in the Dokka CLI model.
     *
     * Dokka doesn't work well with relative paths hence we use a canonical paths while setting up
     * its parameters.
     */
    private class CanonicalFileSerializer : JsonSerializer<File> {
        override fun serialize(
            src: File,
            typeOfSrc: Type,
            context: JsonSerializationContext
        ): JsonElement {
            return JsonPrimitive(src.canonicalPath)
        }
    }
}