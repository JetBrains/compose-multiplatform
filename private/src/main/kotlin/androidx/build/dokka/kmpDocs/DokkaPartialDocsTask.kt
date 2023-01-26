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

import androidx.build.dokka.kmpDocs.DokkaInputModels.GlobalDocsLink
import androidx.build.dokka.kmpDocs.DokkaInputModels.PartialDocsInput
import androidx.build.dokka.kmpDocs.DokkaInputModels.PartialDocsMetadata.Companion.FILE_NAME
import androidx.build.dokka.kmpDocs.DokkaInputModels.PluginsConfiguration
import androidx.build.dokka.kmpDocs.DokkaInputModels.SourceSet
import androidx.build.dokka.kmpDocs.DokkaInputModels.SourceSetId
import androidx.build.dokka.kmpDocs.DokkaInputModels.SrcLink
import androidx.build.getSupportRootFolder
import java.util.IdentityHashMap
import javax.inject.Inject
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Classpath
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Nested
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.TaskProvider
import org.gradle.workers.WorkerExecutor
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinProjectExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinSingleTargetExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.plugin.KotlinTarget

/**
 * Creates partial docs for a Kotlin project with placeholder source links that will be replaced
 * with HEAD sha in the [DokkaCombinedDocsTask] task.
 * @see DokkaCombinedDocsTask
 * @see androidx.build.docs.AndroidXKmpDocsImplPlugin
 */
@CacheableTask
internal abstract class DokkaPartialDocsTask @Inject constructor(
    private val workerExecutor: WorkerExecutor
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
     * This file includes absolute URLs so it is not cache friendly.
     * Ignoring it is OK because all of its inputs are already covered in [dokkaCliInput]
     */
    @get:Internal
    abstract val docsJsonOutput: RegularFileProperty

    /**
     * The directory into which the partial docs will be written.
     */
    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty

    @get:Input
    abstract val moduleName: Property<String>

    @get:Input
    abstract val uniqueArtifactKey: Property<String>

    @get:Nested
    abstract val sourceSets: ListProperty<SourceSet>

    /**
     * The parameters we pass into dokka, also defining the inputs of this task.
     */
    @get:Nested
    val dokkaCliInput by lazy {
        PartialDocsInput(
            moduleName = moduleName.get(),
            outputDir = outputDir.asFile.get(),
            pluginsClasspath = pluginsClasspath.get(),
            globalLinks = buildExternalDocLinks(project),
            sourceSets = sourceSets.get(),
            pluginsConfiguration = listOf(
                PluginsConfiguration.ANDROIDX_COPYRIGHT
            )
        )
    }

    @TaskAction
    fun buildDocs() {
        val outputDir = outputDir.get().asFile.also {
            it.deleteRecursively()
            it.mkdirs()
        }
        val gson = DokkaUtils.createGson()
        docsJsonOutput.get().asFile.let {
            it.parentFile.mkdirs()
            it.writeText(gson.toJson(dokkaCliInput))
        }
        runDokka(
            workerExecutor = workerExecutor,
            classpath = dokkaCliClasspath.get(),
            inputJson = docsJsonOutput.get().asFile
        )

        outputDir.resolve(FILE_NAME).writeText(
            gson.toJson(
                DokkaInputModels.PartialDocsMetadata(
                    moduleName = dokkaCliInput.moduleName,
                    artifactKey = uniqueArtifactKey.get()
                )
            )
        )
    }

    companion object {
        private const val TASK_NAME = "generateKmpDocs"

        private fun buildExternalDocLinks(project: Project): List<GlobalDocsLink> {
            val docsUrl = project.getSupportRootFolder().resolve("docs-public/package-lists")
            fun getPackageList(name: String) =
                docsUrl.resolve("$name/package-list").also {
                    check(it.exists()) {
                        "invalid package list file: $it"
                    }
                }.toURI().toString()
            return listOf(
                GlobalDocsLink(
                    url = "https://guava.dev/releases/31.1-jre/api/docs/",
                    packageListUrl = getPackageList("guava")
                ),
                GlobalDocsLink(
                    url = "https://square.github.io/okio/3.x/okio",
                    packageListUrl = getPackageList("okio")
                ),
                GlobalDocsLink(
                    url = "https://developer.android.com/reference/kotlin",
                    packageListUrl = getPackageList("android")
                ),
                GlobalDocsLink(
                    url = "https://kotlin.github.io/kotlinx.coroutines/kotlinx-coroutines-core",
                    packageListUrl = getPackageList("coroutinesCore")
                ),
                GlobalDocsLink(
                    url = "https://developer.android.com/reference/kotlin/",
                    packageListUrl = getPackageList("androidx-without-kmp")
                )
            )
        }

        /**
         * Creates a partial docs task for the given project.
         */
        fun register(
            project: Project,
            kotlinProjectExtension: KotlinProjectExtension
        ): TaskProvider<DokkaPartialDocsTask> {
            // these configurations are created eagerly due to
            // https://github.com/gradle/gradle/issues/22236
            val cliJarConfiguration = DokkaUtils.createCliJarConfiguration(
                project
            )
            val pluginsClasspath = DokkaUtils.createPluginsConfiguration(
                project
            )
            return project.tasks.register(
                TASK_NAME,
                DokkaPartialDocsTask::class.java
            ) { docsTask ->
                docsTask.sourceSets.set(
                    project.provider {
                        buildSourceSets(project, kotlinProjectExtension)
                    }
                )
                docsTask.moduleName.set(
                    project.name
                )
                docsTask.uniqueArtifactKey.set(
                    project.provider {
                        project.group.toString() + "/" + project.name
                    }
                )
                docsTask.dokkaCliClasspath.set(
                    cliJarConfiguration
                )
                docsTask.pluginsClasspath.set(
                    pluginsClasspath
                )
                docsTask.outputDir.set(
                    project.layout.buildDirectory.dir("kmp-docs/output")
                )
                docsTask.docsJsonOutput.set(
                    project.layout.buildDirectory.file("kmp-docs/workingDir/kmp-docs-input.json")
                )
            }
        }

        private fun buildSourceSets(
            project: Project,
            kotlinExtension: KotlinProjectExtension
        ): List<SourceSet> {
            val targets = when (kotlinExtension) {
                is KotlinSingleTargetExtension<*> -> listOf(kotlinExtension.target)
                is KotlinMultiplatformExtension -> kotlinExtension.targets
                else -> error("unsupported kotlin extension")
            }

            // Find source sets that are used in compilations that we are interested in.
            // Also associate them with the classpath of the compilation to be able to resolve
            // dependencies.
            val sourceSets = IdentityHashMap<KotlinSourceSet, DokkaSourceSetInput>()
            targets.forEach { target ->
                val platform = target.docsPlatform()
                val compilation =
                    target.compilations.findByName(KotlinCompilation.MAIN_COMPILATION_NAME)
                    // Android projects use "debug"
                        ?: target.compilations.findByName("debug")
                compilation?.allKotlinSourceSets?.forEach { kotlinSourceSet ->
                    val existing = sourceSets.getOrPut(kotlinSourceSet) {
                        DokkaSourceSetInput(
                            platform = platform,
                            classpath = project.files()
                        )
                    }
                    existing.platform += platform

                    val additionalClasspath =
                        if (compilation.target.platformType == KotlinPlatformType.androidJvm) {
                            // This is a workaround for https://youtrack.jetbrains.com/issue/KT-33893
                            @Suppress("DEPRECATION") // for compatibility
                            (compilation.compileKotlinTask as
                                org.jetbrains.kotlin.gradle.tasks.KotlinCompile).libraries
                        } else {
                            compilation.compileDependencyFiles
                        }
                    existing.classpath.from(
                        additionalClasspath
                    )
                }
            }

            return sourceSets.map { (sourceSet, docsPlatform) ->
                val sourceDirectories = sourceSet.kotlin.sourceDirectories.filter {
                    it.exists()
                }
                SourceSet(
                    displayName = sourceSet.displayName(),
                    id = sourceSet.dokkaId(project),
                    classpath = docsPlatform.classpath,
                    sourceRoots = sourceDirectories,
                    analysisPlatform = docsPlatform.platform.jsonName,
                    noStdlibLink = false,
                    noJdkLink = !docsPlatform.platform.androidOrJvm(),
                    noAndroidSdkLink = docsPlatform.platform != DokkaAnalysisPlatform.ANDROID,
                    dependentSourceSets = sourceSet.dependsOn.map {
                        it.dokkaId(project)
                    }.sortedBy { it.sourceSetName },
                    externalDocumentationLinks = buildExternalDocLinks(project),
                    sourceLinks = sourceDirectories.map {
                        SrcLink(
                            localDirectory = it,
                            remoteUrl = DokkaUtils.CS_ANDROID_PLACEHOLDER +
                                it.relativeTo(project.getSupportRootFolder()).path
                        )
                    }.sortedBy {
                        it.localDirectory
                    },
                    samples = project.files(),
                    includes = project.files()
                )
            }.sortedBy { it.displayName }
        }
    }
}

enum class DokkaAnalysisPlatform(val jsonName: String) {
    JVM("jvm"),
    ANDROID("jvm"), // intentionally same as JVM as dokka only support jvm
    JS("js"),
    NATIVE("native"),
    COMMON("common");

    fun androidOrJvm() = this == JVM || this == ANDROID
}

private class DokkaSourceSetInput(
    var platform: DokkaAnalysisPlatform,
    val classpath: ConfigurableFileCollection
)

private fun KotlinSourceSet.dokkaId(project: Project) = SourceSetId(
    sourceSetName = name,
    scopeId = project.path
)

private fun KotlinSourceSet.displayName() = if (name.endsWith("Main")) {
    name.substringBeforeLast("Main")
} else {
    name
}

fun KotlinTarget.docsPlatform() = when (platformType) {
    KotlinPlatformType.common -> DokkaAnalysisPlatform.COMMON
    KotlinPlatformType.jvm -> DokkaAnalysisPlatform.JVM
    KotlinPlatformType.js -> DokkaAnalysisPlatform.JS
    KotlinPlatformType.wasm -> DokkaAnalysisPlatform.JS
    KotlinPlatformType.androidJvm -> DokkaAnalysisPlatform.ANDROID
    KotlinPlatformType.native -> DokkaAnalysisPlatform.NATIVE
}

private operator fun DokkaAnalysisPlatform?.plus(
    other: DokkaAnalysisPlatform
): DokkaAnalysisPlatform {
    if (this == null) {
        return other
    }
    if (this == other) {
        return this
    }
    if (this.androidOrJvm() && other.androidOrJvm()) {
        return DokkaAnalysisPlatform.JVM
    }
    return DokkaAnalysisPlatform.COMMON
}