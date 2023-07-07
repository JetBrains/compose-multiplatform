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

import androidx.build.dokka.kmpDocs.DokkaInputModels.MergeDocsInputs
import androidx.build.dokka.kmpDocs.DokkaInputModels.Module
import androidx.build.dokka.kmpDocs.DokkaInputModels.PluginsConfiguration
import androidx.build.dokka.kmpDocs.DokkaUtils.COMBINE_PLUGIN_LIBRARIES
import androidx.build.getSupportRootFolder
import androidx.build.gitclient.GitClient
import javax.inject.Inject
import org.gradle.api.DefaultTask
import org.gradle.api.NamedDomainObjectProvider
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Classpath
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.TaskProvider
import org.gradle.work.DisableCachingByDefault
import org.gradle.workers.WorkAction
import org.gradle.workers.WorkParameters
import org.gradle.workers.WorkerExecutor

/**
 * Merges outputs of [DokkaPartialDocsTask]s into 1 documentation using dokka and also replaces
 * source links with the HEAD sha.
 */
@DisableCachingByDefault(because = "Uses latest sha in the git repo as input")
internal abstract class DokkaCombinedDocsTask @Inject constructor(
    private val workerExecutor: WorkerExecutor,
) : DefaultTask() {
    /**
     * List of plugins that will be applied to Dokka.
     */
    @get:Classpath
    abstract val pluginsClasspath: Property<FileCollection>

    /**
     * Classpath for running dokka-cli.
     */
    @get:Classpath
    abstract val dokkaCliClasspath: Property<FileCollection>

    /**
     * Output artifacts of each Project's partial docs
     */
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val partialDocs: Property<FileCollection>

    /**
     * This file includes absolute URLs so it is not cache friendly.
     * Ignoring it is OK because all of its inputs are already covered in moduleDocs
     */
    @get:Internal
    abstract val docsJsonOutput: RegularFileProperty

    /**
     * The directory into which the combined docs will be written.
     */
    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty

    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val additionalDocumentation: Property<FileCollection>

    /**
     * The cs.android.com url with HEAD sha that will replace the placeholder.
     */
    @get:Input
    abstract val replacementUrl: Property<String>

    @TaskAction
    fun buildCombinedDocs() {
        // create output dir before calling dokka to make sure relative links work.
        val outputDir = outputDir.get().asFile.also {
            it.deleteRecursively()
            it.mkdirs()
        }
        val gson = DokkaUtils.createGson()
        // sub directory to contain library docs to avoid possible conflicts with the
        // generate folders from docs.
        val libsDir = outputDir.resolve("libs")
        val replaceUrlsWorkQueue = workerExecutor.noIsolation()
        val partialModules = partialDocs.get().files.map { partialDoc ->
            val metadataFile = partialDoc.resolve(
                DokkaInputModels.PartialDocsMetadata.FILE_NAME
            )
            val metadata = metadataFile.inputStream().reader(Charsets.UTF_8).use {
                gson.fromJson(it, DokkaInputModels.PartialDocsMetadata::class.java)
            }
            checkNotNull(metadata) {
                "Failed to find the metadata file in partial docs archive"
            }

            // move the rest of docs files into a separate directory
            val exportDir = libsDir.resolve(metadata.artifactKey).also {
                it.parentFile.mkdirs()
            }
            check(!exportDir.exists()) {
                "Duplicate module ids"
            }
            replaceUrlsWorkQueue.submit(
                CopyPartialDocsWithReplacedSourceLinks::class.java
            ) {
                it.inputDir.set(partialDoc)
                it.outputDir.set(exportDir)
                it.replacementUrl.set(replacementUrl)
            }
            metadata to exportDir
        }
        // wait until all replacement work is done.
        replaceUrlsWorkQueue.await()
        val input = MergeDocsInputs(
            moduleName = "Jetpack Multiplatform Preview Reference Documentation",
            outputDir = outputDir,
            pluginsClasspath = pluginsClasspath.get(),
            modules = partialModules.map { (module, docsDir) ->
                Module(
                    name = module.moduleName,
                    relativePathToOutputDirectory = docsDir.relativeTo(outputDir).path,
                    sourceOutputDirectory = docsDir,
                    includes = emptyList()
                )
            },
            pluginsConfiguration = listOf(
                PluginsConfiguration.ANDROIDX_COPYRIGHT
            ),
            includes = additionalDocumentation.get().files
        )
        docsJsonOutput.get().asFile.let {
            it.parentFile.mkdirs()
            it.writeText(gson.toJson(input))
        }

        runDokka(
            workerExecutor = workerExecutor,
            classpath = dokkaCliClasspath.get(),
            inputJson = docsJsonOutput.get().asFile
        )
    }

    companion object {
        private const val TASK_NAME = "generateCombinedKmpDocs"

        /**
         * Creates a [DokkaCombinedDocsTask] that will merge all sub-project docs into 1
         * directory.
         */
        fun register(
            project: Project,
            configuration: NamedDomainObjectProvider<Configuration>
        ): TaskProvider<DokkaCombinedDocsTask> {
            return project.tasks.register(
                TASK_NAME,
                DokkaCombinedDocsTask::class.java
            ) {
                it.outputDir.set(
                    project.layout.buildDirectory.dir("kmp-docs/output")
                )
                it.partialDocs.set(
                    configuration.map {
                        it.incoming.artifactView { }.files
                    }
                )
                it.docsJsonOutput.set(
                    project.layout.buildDirectory
                        .file("kmp-docs/workingDir/combined-kmp-docs-input.json")
                )
                it.pluginsClasspath.set(
                    DokkaUtils.createPluginsConfiguration(
                        project,
                        COMBINE_PLUGIN_LIBRARIES
                    )
                )
                it.dokkaCliClasspath.set(
                    DokkaUtils.createCliJarConfiguration(project)
                )
                it.additionalDocumentation.set(
                    project.files("homepage.md")
                )
                val gitClient = GitClient.create(
                    project.getSupportRootFolder(),
                    project.logger,
                    GitClient.getChangeInfoPath(project).get(),
                    GitClient.getManifestPath(project).get()
                )
                it.replacementUrl.set(
                    DokkaUtils.createCsAndroidUrl(
                        gitClient.getHeadSha(project.getSupportRootFolder())
                    )
                )
            }
        }
    }

    interface CopyPartialDocsWithReplacedSourceLinksInputs : WorkParameters {
        val inputDir: DirectoryProperty
        val outputDir: DirectoryProperty
        val replacementUrl: Property<String>
    }

    abstract class CopyPartialDocsWithReplacedSourceLinks :
        WorkAction<CopyPartialDocsWithReplacedSourceLinksInputs> {
        override fun execute() {
            val input = parameters.inputDir.get().asFile
            val output = parameters.outputDir.get().asFile
            output.deleteRecursively()
            output.mkdirs()
            val newUrl = parameters.replacementUrl.get()

            input.walkTopDown().forEach { inputFile ->
                // convert input file to its matching location in the output directory
                val outputFile = output.resolve(inputFile.relativeTo(input))
                when {
                    inputFile.isDirectory -> outputFile.mkdirs()
                    inputFile.extension == "html" -> outputFile.writeText(
                        inputFile.readText(Charsets.UTF_8).replace(
                            DokkaUtils.CS_ANDROID_PLACEHOLDER,
                            newUrl
                        )
                    )
                    inputFile.name == DokkaInputModels.PartialDocsMetadata.FILE_NAME -> {
                        // don't copy the metadata
                    }
                    else -> inputFile.copyTo(target = outputFile)
                }
            }
        }
    }
}