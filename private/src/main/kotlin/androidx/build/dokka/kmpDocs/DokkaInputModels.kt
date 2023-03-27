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

@file:Suppress("unused") // used by gson

package androidx.build.dokka.kmpDocs

import com.google.gson.annotations.SerializedName
import java.io.File
import org.gradle.api.file.FileCollection
import org.gradle.api.tasks.Classpath
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Nested
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity

// These are models used to invoke dokka from the command line.
// Most of these models are identical to
// https://github.com/Kotlin/dokka/blob/master/core/src/main/kotlin/configuration.kt
// with the caveat that they have Gradle task input annotations when necessary.

internal object DokkaInputModels {

    class MergeDocsInputs(
        val outputDir: File,
        val moduleName: String,
        val pluginsClasspath: FileCollection,
        val modules: List<Module>,
        val pluginsConfiguration: List<PluginsConfiguration>,
        val includes: Set<File>
    )

    class Module(
        val name: String,
        val relativePathToOutputDirectory: String,
        val sourceOutputDirectory: File,
        val includes: List<File> = emptyList()
    )

    class PartialDocsInput(
        @get:Input
        val moduleName: String,
        @get:OutputDirectory
        val outputDir: File,
        // order of plugins is important here
        @get:InputFiles
        @PathSensitive(PathSensitivity.RELATIVE)
        val pluginsClasspath: FileCollection,
        @get:Nested
        val globalLinks: List<GlobalDocsLink>,
        @get:Input
        val delayTemplateSubstitution: Boolean = true,
        @get:Nested
        val sourceSets: List<SourceSet>,
        @get:Nested
        val pluginsConfiguration: List<PluginsConfiguration>
    )

    class SourceSet(
        @get:Input
        val displayName: String,
        @get:Nested
        @SerializedName("sourceSetID")
        val id: SourceSetId,
        @Classpath
        val classpath: FileCollection,
        @get:InputFiles
        @PathSensitive(PathSensitivity.RELATIVE)
        val sourceRoots: FileCollection,
        @get:InputFiles
        @PathSensitive(PathSensitivity.RELATIVE)
        val samples: FileCollection,
        @get:InputFiles
        @PathSensitive(PathSensitivity.RELATIVE)
        val includes: FileCollection,
        @get:Input
        val analysisPlatform: String,
        @get:Input
        val documentedVisibilities: List<String> = listOf(
            "PUBLIC",
            "PROTECTED"
        ),
        @get:Input
        val noStdlibLink: Boolean,
        @get:Input
        val noJdkLink: Boolean,
        @get:Input
        val noAndroidSdkLink: Boolean,
        @Nested
        val dependentSourceSets: List<SourceSetId>,
        @Nested
        val externalDocumentationLinks: List<GlobalDocsLink>,
        @Nested
        val sourceLinks: List<SrcLink>
    )

    class SourceSetId(
        @get:Input
        val sourceSetName: String,
        @get:Input
        val scopeId: String,
    )

    class SrcLink(
        @get:InputDirectory
        @PathSensitive(PathSensitivity.RELATIVE)
        val localDirectory: File,
        @get:Input
        val remoteUrl: String,
        @get:Input
        val remoteLineSuffix: String = ";l="
    )

    class GlobalDocsLink(
        @get:Input
        val url: String,
        @get:Input
        val packageListUrl: String?
    )

    class PluginsConfiguration(
        @get:Input
        val fqPluginName: String,
        @get:Input
        val serializationFormat: String,
        @get:Input
        val values: String
    ) {
        companion object {
            val ANDROIDX_COPYRIGHT = PluginsConfiguration(
                fqPluginName = "org.jetbrains.dokka.base.DokkaBase",
                serializationFormat = "JSON",
                values =
                "{\"footerMessage\":\"Copyright (C) 2022 The Android Open Source Project\"}"
            )
        }
    }

    /**
     * Metadata exported from partial docs that can be used during the merge.
     */
    class PartialDocsMetadata(
        /**
         * Display name for the module
         */
        val moduleName: String,
        /**
         * Unique key for the module that distinguishes it from other modules.
         */
        val artifactKey: String
    ) {
        companion object {
            /**
             * File name used when this is serialized into a gradle artifact.
             */
            const val FILE_NAME = "androidXPartialDocsMetadata.json"
        }
    }
}