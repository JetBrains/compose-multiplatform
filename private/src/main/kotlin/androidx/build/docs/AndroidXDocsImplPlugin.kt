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

package androidx.build.docs

import androidx.build.PROJECT_STRUCTURE_METADATA_FILENAME
import androidx.build.SupportConfig
import androidx.build.multiplatformUsage
import androidx.build.dackka.DackkaTask
import androidx.build.dackka.GenerateMetadataTask
import androidx.build.dependencies.KOTLIN_VERSION
import androidx.build.enforceKtlintVersion
import androidx.build.getAndroidJar
import androidx.build.getBuildId
import androidx.build.getDistributionDirectory
import androidx.build.getKeystore
import androidx.build.getLibraryByName
import com.android.build.api.attributes.BuildTypeAttr
import com.android.build.gradle.LibraryExtension
import com.android.build.gradle.LibraryPlugin
import com.google.gson.GsonBuilder
import java.io.File
import java.io.FileNotFoundException
import java.time.Duration
import java.time.LocalDateTime
import javax.inject.Inject
import org.gradle.api.DefaultTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.artifacts.ComponentMetadataContext
import org.gradle.api.artifacts.ComponentMetadataRule
import org.gradle.api.artifacts.Configuration
import org.gradle.api.attributes.Attribute
import org.gradle.api.attributes.Category
import org.gradle.api.attributes.DocsType
import org.gradle.api.attributes.LibraryElements
import org.gradle.api.attributes.Usage
import org.gradle.api.file.ArchiveOperations
import org.gradle.api.file.DuplicatesStrategy
import org.gradle.api.file.FileCollection
import org.gradle.api.file.FileSystemOperations
import org.gradle.api.file.RegularFile
import org.gradle.api.model.ObjectFactory
import org.gradle.api.plugins.JavaBasePlugin
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.Sync
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.tasks.bundling.Zip
import org.gradle.api.tasks.testing.Test
import org.gradle.kotlin.dsl.all
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.named
import org.gradle.kotlin.dsl.register
import org.gradle.work.DisableCachingByDefault

/**
 * Plugin that allows to build documentation for a given set of prebuilt and tip of tree projects.
 */
abstract class AndroidXDocsImplPlugin : Plugin<Project> {
    lateinit var docsType: String
    lateinit var docsSourcesConfiguration: Configuration
    lateinit var multiplatformDocsSourcesConfiguration: Configuration
    lateinit var samplesSourcesConfiguration: Configuration
    lateinit var dependencyClasspath: FileCollection

    @get:Inject
    abstract val archiveOperations: ArchiveOperations

    override fun apply(project: Project) {
        docsType = project.name.removePrefix("docs-")
        project.plugins.all { plugin ->
            when (plugin) {
                is LibraryPlugin -> {
                    val libraryExtension = project.extensions.getByType<LibraryExtension>()
                    libraryExtension.compileSdkVersion = SupportConfig.COMPILE_SDK_VERSION
                    libraryExtension.buildToolsVersion = SupportConfig.BUILD_TOOLS_VERSION

                    // Use a local debug keystore to avoid build server issues.
                    val debugSigningConfig = libraryExtension.signingConfigs.getByName("debug")
                    debugSigningConfig.storeFile = project.getKeystore()
                    libraryExtension.buildTypes.all { buildType ->
                        // Sign all the builds (including release) with debug key
                        buildType.signingConfig = debugSigningConfig
                    }
                }
            }
        }
        disableUnneededTasks(project)
        createConfigurations(project)
        val buildOnServer = project.tasks.register<DocsBuildOnServer>("buildOnServer") {
            buildId = getBuildId()
            docsType = this@AndroidXDocsImplPlugin.docsType
            distributionDirectory = project.getDistributionDirectory()
        }

        val unzippedSamplesSources = File(project.buildDir, "unzippedSampleSources")
        val unzipSamplesTask = configureUnzipTask(
            project,
            "unzipSampleSources",
            unzippedSamplesSources,
            samplesSourcesConfiguration
        )

        val unzippedJvmSourcesDirectory = File(project.buildDir, "unzippedJvmSources")
        val unzippedMultiplatformSourcesDirectory = File(
            project.buildDir,
            "unzippedMultiplatformSources"
        )
        val mergedProjectMetadata = File(
            project.buildDir,
            "project_metadata/$PROJECT_STRUCTURE_METADATA_FILENAME"
        )
        val unzipJvmSourcesTask = configureUnzipJvmSourcesTasks(
            project,
            unzippedJvmSourcesDirectory,
            docsSourcesConfiguration
        )
        val configureMultiplatformSourcesTask =
            configureMultiplatformInputsTasks(
                project,
                unzippedMultiplatformSourcesDirectory,
                multiplatformDocsSourcesConfiguration,
                mergedProjectMetadata
            )

        configureDackka(
            project,
            unzippedJvmSourcesDirectory,
            unzippedMultiplatformSourcesDirectory,
            unzipJvmSourcesTask,
            configureMultiplatformSourcesTask,
            unzippedSamplesSources,
            unzipSamplesTask,
            dependencyClasspath,
            buildOnServer,
            docsSourcesConfiguration,
            mergedProjectMetadata
        )
    }

    /**
     * Creates and configures a task that will build a list of all sources for projects in
     * [docsConfiguration] configuration, resolve them and put them to [destinationDirectory].
     */
    private fun configureUnzipTask(
        project: Project,
        taskName: String,
        destinationDirectory: File,
        docsConfiguration: Configuration
    ): TaskProvider<Sync> {
        return project.tasks.register(
            taskName,
            Sync::class.java
        ) { task ->
            val sources = docsConfiguration.incoming.artifactView { }.files
            // Store archiveOperations into a local variable to prevent access to the plugin
            // during the task execution, as that breaks configuration caching.
            val localVar = archiveOperations
            task.from(
                sources.elements.map { jars ->
                    jars.map { jar ->
                        localVar.zipTree(jar).matching {
                            // Filter out files that documentation tools cannot process.
                            it.exclude("**/*.MF")
                            it.exclude("**/*.aidl")
                            it.exclude("**/META-INF/**")
                            it.exclude("**/OWNERS")
                            it.exclude("**/package.html")
                            it.exclude("**/*.md")
                        }
                    }
                }
            )
            task.into(destinationDirectory)
            // TODO(123020809) remove this filter once it is no longer necessary to prevent Dokka
            //  from failing
            val regex = Regex("@attr ref ([^*]*)styleable#([^_*]*)_([^*]*)$")
            task.filter { line ->
                regex.replace(line, "{@link $1attr#$3}")
            }
        }
    }

    /**
     * Creates and configures a task that will build a list of select sources from jars and places
     * them in [destinationDirectory].
     *
     * This is a modified version of [configureUnzipTask], customized for Dackka usage.
     */
    private fun configureUnzipJvmSourcesTasks(
        project: Project,
        destinationDirectory: File,
        docsConfiguration: Configuration
    ): TaskProvider<Sync> {
        return project.tasks.register("unzipJvmSources", Sync::class.java) { task ->
            val sources = docsConfiguration.incoming.artifactView { }.files

            // Store archiveOperations into a local variable to prevent access to the plugin
            // during the task execution, as that breaks configuration caching.
            val localVar = archiveOperations
            task.into(destinationDirectory)
            task.from(
                sources.elements.map { jars ->
                    jars.map {
                        localVar.zipTree(it)
                    }
                }
            )
            task.duplicatesStrategy = DuplicatesStrategy.INCLUDE
        }
    }

    /**
     * Creates multiple tasks to unzip multiplatform sources and merge their metadata to be used
     * as input for Dackka. Returns a single umbrella task which depends on the others.
     */
    private fun configureMultiplatformInputsTasks(
        project: Project,
        unzippedMultiplatformSourcesDirectory: File,
        multiplatformDocsSourcesConfiguration: Configuration,
        mergedProjectMetadata: File
    ): TaskProvider<MergeMultiplatformMetadataTask> {
        val tempMultiplatformMetadataDirectory = File(
            project.buildDir,
            "tmp/multiplatformMetadataFiles"
        )
        // unzip the sources into source folder and metadata files into folders per project
        val unzipMultiplatformSources = project.tasks.register(
            "unzipMultiplatformSources",
            UnzipMultiplatformSourcesTask::class.java
        ) {
            it.inputJars.set(multiplatformDocsSourcesConfiguration.incoming.artifactView { }.files)
            it.metadataOutput = tempMultiplatformMetadataDirectory
            it.sourceOutput = unzippedMultiplatformSourcesDirectory
        }
        // merge all the metadata files from the individual project dirs
        return project.tasks.register(
            "mergeMultiplatformMetadata",
            MergeMultiplatformMetadataTask::class.java
        ) {
            it.dependsOn(unzipMultiplatformSources)
            it.mergedProjectMetadata = mergedProjectMetadata
            it.inputDirectory = tempMultiplatformMetadataDirectory
        }
    }

    /**
     *  The following configurations are created to build a list of projects that need to be
     * documented and should be used from build.gradle of docs projects for the following:
     * - docs(project(":foo:foo") or docs("androidx.foo:foo:1.0.0") for docs sources
     * - samples(project(":foo:foo-samples") or samples("androidx.foo:foo-samples:1.0.0") for
     *   samples sources
     * - stubs(project(":foo:foo-stubs")) - stubs needed for a documented library
     */
    private fun createConfigurations(project: Project) {
        project.configurations.all {
            project.enforceKtlintVersion(it)
        }
        project.dependencies.components.all<SourcesVariantRule>()
        val docsConfiguration = project.configurations.create("docs") {
            it.isCanBeResolved = false
            it.isCanBeConsumed = false
        }
        val multiplatformDocsConfiguration = project.configurations.create("kmpDocs") {
            it.isCanBeResolved = false
            it.isCanBeConsumed = false
        }
        val samplesConfiguration = project.configurations.create("samples") {
            it.isCanBeResolved = false
            it.isCanBeConsumed = false
        }
        val stubsConfiguration = project.configurations.create("stubs") {
            it.isCanBeResolved = false
            it.isCanBeConsumed = false
        }

        fun Configuration.setResolveSources() {
            isTransitive = false
            isCanBeConsumed = false
            attributes {
                it.attribute(
                    Usage.USAGE_ATTRIBUTE,
                    project.objects.named<Usage>(Usage.JAVA_RUNTIME)
                )
                it.attribute(
                    Category.CATEGORY_ATTRIBUTE,
                    project.objects.named<Category>(Category.DOCUMENTATION)
                )
                it.attribute(
                    DocsType.DOCS_TYPE_ATTRIBUTE,
                    project.objects.named<DocsType>(DocsType.SOURCES)
                )
                it.attribute(
                    LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE,
                    project.objects.named<LibraryElements>(LibraryElements.JAR)
                )
            }
        }
        docsSourcesConfiguration = project.configurations.create("docs-sources") {
            it.setResolveSources()
            it.extendsFrom(docsConfiguration)
        }
        multiplatformDocsSourcesConfiguration = project.configurations.create(
            "multiplatform-docs-sources"
        ) { configuration ->
            configuration.isTransitive = false
            configuration.isCanBeConsumed = false
            configuration.attributes {
                it.attribute(
                    Usage.USAGE_ATTRIBUTE,
                    project.multiplatformUsage
                )
                it.attribute(
                    Category.CATEGORY_ATTRIBUTE,
                    project.objects.named<Category>(Category.DOCUMENTATION)
                )
                it.attribute(
                    DocsType.DOCS_TYPE_ATTRIBUTE,
                    project.objects.named<DocsType>(DocsType.SOURCES)
                )
                it.attribute(
                    LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE,
                    project.objects.named<LibraryElements>(LibraryElements.JAR)
                )
            }
            configuration.extendsFrom(multiplatformDocsConfiguration)
        }
        samplesSourcesConfiguration = project.configurations.create("samples-sources") {
            it.setResolveSources()
            it.extendsFrom(samplesConfiguration)
        }

        fun Configuration.setResolveClasspathForUsage(usage: String) {
            isCanBeConsumed = false
            attributes {
                it.attribute(
                    Usage.USAGE_ATTRIBUTE,
                    project.objects.named<Usage>(usage)
                )
                it.attribute(
                    Category.CATEGORY_ATTRIBUTE,
                    project.objects.named<Category>(Category.LIBRARY)
                )
                it.attribute(
                    BuildTypeAttr.ATTRIBUTE,
                    project.objects.named<BuildTypeAttr>("release")
                )
            }
            extendsFrom(docsConfiguration, samplesConfiguration, stubsConfiguration)
        }

        // Build a compile & runtime classpaths for needed for documenting the libraries
        // from the configurations above.
        val docsCompileClasspath = project.configurations.create("docs-compile-classpath") {
            it.setResolveClasspathForUsage(Usage.JAVA_API)
        }
        val docsRuntimeClasspath = project.configurations.create("docs-runtime-classpath") {
            it.setResolveClasspathForUsage(Usage.JAVA_RUNTIME)
        }
        listOf(docsCompileClasspath, docsRuntimeClasspath).forEach { config ->
            config.resolutionStrategy {
                it.eachDependency { details ->
                    if (details.requested.group == "org.jetbrains.kotlin") {
                        details.useVersion(KOTLIN_VERSION)
                    }
                }
            }
        }
        dependencyClasspath = docsCompileClasspath.incoming.artifactView {
            it.attributes.attribute(
                Attribute.of("artifactType", String::class.java),
                "android-classes"
            )
        }.files + docsRuntimeClasspath.incoming.artifactView {
            it.attributes.attribute(
                Attribute.of("artifactType", String::class.java),
                "android-classes"
            )
        }.files
    }

    private fun configureDackka(
        project: Project,
        unzippedJvmSourcesDirectory: File,
        unzippedMultiplatformSourcesDirectory: File,
        unzipJvmSourcesTask: TaskProvider<Sync>,
        configureMultiplatformSourcesTask: TaskProvider<MergeMultiplatformMetadataTask>,
        unzippedSamplesSources: File,
        unzipSamplesTask: TaskProvider<Sync>,
        dependencyClasspath: FileCollection,
        buildOnServer: TaskProvider<*>,
        docsConfiguration: Configuration,
        mergedProjectMetadata: File
    ) {
        val generatedDocsDir = project.file("${project.buildDir}/docs")

        val dackkaConfiguration = project.configurations.create("dackka").apply {
            dependencies.add(project.dependencies.create(project.getLibraryByName("dackka")))
        }

        val generateMetadataTask = project.tasks.register(
            "generateMetadata",
            GenerateMetadataTask::class.java
        ) { task ->

            @Suppress("UnstableApiUsage") // getResolvedArtifacts() is marked @Incubating
            val artifacts = docsConfiguration.incoming.artifacts.resolvedArtifacts
            task.getArtifactIds().set(
                artifacts.map { result -> result.map { it.id } }
            )
            task.getArtifactFiles().set(
                artifacts.map { result -> result.map { it.file } }
            )
            task.destinationFile.set(getMetadataRegularFile(project))
        }

        val metricsDirectory = project.buildDir
        val metricsFile = File(metricsDirectory, "build-metrics.json")
        val projectName = project.name

        val dackkaTask = project.tasks.register("docs", DackkaTask::class.java) { task ->
            var taskStartTime: LocalDateTime? = null
            task.apply {
                dependsOn(unzipJvmSourcesTask)
                dependsOn(unzipSamplesTask)
                dependsOn(generateMetadataTask)
                dependsOn(configureMultiplatformSourcesTask)

                description = "Generates reference documentation using a Google devsite Dokka" +
                    " plugin. Places docs in $generatedDocsDir"
                group = JavaBasePlugin.DOCUMENTATION_GROUP

                dackkaClasspath.from(project.files(dackkaConfiguration))
                destinationDir = generatedDocsDir
                frameworkSamplesDir = File(project.rootDir, "samples")
                samplesDir = unzippedSamplesSources
                jvmSourcesDir = unzippedJvmSourcesDirectory
                multiplatformSourcesDir = unzippedMultiplatformSourcesDirectory
                docsProjectDir = File(project.rootDir, "docs-public")
                dependenciesClasspath = project.getAndroidJar() + dependencyClasspath
                excludedPackages = hiddenPackages.toSet()
                excludedPackagesForJava = hiddenPackagesJava
                excludedPackagesForKotlin = emptySet()
                libraryMetadataFile.set(getMetadataRegularFile(project))
                projectStructureMetadataFile = mergedProjectMetadata
                // See go/dackka-source-link for details on this link.
                baseSourceLink = "https://cs.android.com/search?" +
                    "q=file:%s+class:%s&ss=androidx/platform/frameworks/support"
                annotationsNotToDisplay = hiddenAnnotations
                annotationsNotToDisplayJava = hiddenAnnotationsJava
                annotationsNotToDisplayKotlin = hiddenAnnotationsKotlin
                task.doFirst {
                    taskStartTime = LocalDateTime.now()
                }
                task.doLast {
                    val taskEndTime = LocalDateTime.now()
                    val duration = Duration.between(taskStartTime, taskEndTime).toMillis()
                    metricsDirectory.mkdirs()
                    metricsFile.writeText(
                        "{ \"${projectName}_docs_execution_duration\": $duration }"
                    )
                }
            }
        }

        val zipTask = project.tasks.register("zipDocs", Zip::class.java) { task ->
            task.apply {
                dependsOn(dackkaTask)
                from(generatedDocsDir)

                val baseName = "docs-$docsType"
                val buildId = getBuildId()
                archiveBaseName.set(baseName)
                archiveVersion.set(buildId)
                destinationDirectory.set(project.getDistributionDirectory())
                group = JavaBasePlugin.DOCUMENTATION_GROUP

                val filePath = "${project.getDistributionDirectory().canonicalPath}/"
                val fileName = "$baseName-$buildId.zip"
                val destinationFile = filePath + fileName
                description = "Zips Java and Kotlin documentation (generated via Dackka in the" +
                    " style of d.android.com) into $destinationFile"
            }
        }
        buildOnServer.configure { it.dependsOn(zipTask) }
    }

    /**
     * Replace all tests etc with empty task, so we don't run anything
     * it is more effective then task.enabled = false, because we avoid executing deps as well
     */
    private fun disableUnneededTasks(project: Project) {
        var reentrance = false
        project.tasks.whenTaskAdded { task ->
            if (task is Test || task.name.startsWith("assemble") ||
                task.name == "lint" ||
                task.name == "lintDebug" ||
                task.name == "lintAnalyzeDebug" ||
                task.name == "transformDexArchiveWithExternalLibsDexMergerForPublicDebug" ||
                task.name == "transformResourcesWithMergeJavaResForPublicDebug" ||
                task.name == "checkPublicDebugDuplicateClasses"
            ) {
                if (!reentrance) {
                    reentrance = true
                    project.tasks.named(task.name) {
                        it.actions = emptyList()
                        it.dependsOn(emptyList<Task>())
                    }
                    reentrance = false
                }
            }
        }
    }
}

@DisableCachingByDefault(because = "Doesn't benefit from caching")
open class DocsBuildOnServer : DefaultTask() {
    @Internal
    lateinit var docsType: String
    @Internal
    lateinit var buildId: String
    @Internal
    lateinit var distributionDirectory: File

    @[InputFiles PathSensitive(PathSensitivity.RELATIVE)]
    fun getRequiredFiles(): List<File> {
        return listOf(
            File(distributionDirectory, "docs-$docsType-$buildId.zip"),
        )
    }

    @TaskAction
    fun checkAllBuildOutputs() {
        val missingFiles = mutableListOf<String>()
        getRequiredFiles().forEach { file ->
            if (!file.exists()) {
                missingFiles.add(file.path)
            }
        }

        if (missingFiles.isNotEmpty()) {
            val missingFileString = missingFiles.reduce { acc, s -> "$acc, $s" }
            throw FileNotFoundException("buildOnServer required output missing: $missingFileString")
        }
    }
}

/**
 * Adapter rule to handles prebuilt dependencies that do not use Gradle Metadata (only pom).
 * We create a new variant sources that we can later use in the same way we do for tip of tree
 * projects and prebuilts with Gradle Metadata.
 */
abstract class SourcesVariantRule : ComponentMetadataRule {
    @get:Inject
    abstract val objects: ObjectFactory
    override fun execute(context: ComponentMetadataContext) {
        context.details.maybeAddVariant("sources", "runtime") {
            it.attributes {
                it.attribute(Usage.USAGE_ATTRIBUTE, objects.named(Usage.JAVA_RUNTIME))
                it.attribute(Category.CATEGORY_ATTRIBUTE, objects.named(Category.DOCUMENTATION))
                it.attribute(DocsType.DOCS_TYPE_ATTRIBUTE, objects.named(DocsType.SOURCES))
            }
            it.withFiles {
                it.removeAllFiles()
                it.addFile("${context.details.id.name}-${context.details.id.version}-sources.jar")
            }
        }
    }
}

/**
 * Location of the library metadata JSON file that's used by Dackka, represented as a [RegularFile]
 */
private fun getMetadataRegularFile(project: Project): Provider<RegularFile> =
    project.layout.buildDirectory.file("AndroidXLibraryMetadata.json")

// List of packages to exclude from both Java and Kotlin refdoc generation
private val hiddenPackages = listOf(
    "androidx.camera.camera2.impl",
    "androidx.camera.camera2.internal.*",
    "androidx.camera.core.impl.*",
    "androidx.camera.core.internal.*",
    "androidx.core.internal",
    "androidx.preference.internal",
    "androidx.wear.internal.widget.drawer",
    "androidx.webkit.internal",
    "androidx.work.impl.*"
)

// Set of packages to exclude from Java refdoc generation
private val hiddenPackagesJava = setOf(
    "androidx.*compose.*",
    "androidx.*glance.*",
)

// List of annotations which should not be displayed in the docs
private val hiddenAnnotations: List<String> = listOf(
    // This information is compose runtime implementation details; not useful for most, those who
    // would want it should look at source
    "androidx.compose.runtime.Stable",
    "androidx.compose.runtime.Immutable",
    "androidx.compose.runtime.ReadOnlyComposable",
    // This opt-in requirement is non-propagating so developers don't need to know about it
    // https://kotlinlang.org/docs/opt-in-requirements.html#non-propagating-opt-in
    "androidx.annotation.OptIn",
    "kotlin.OptIn",
    // This annotation is used mostly in paging, and was removed at the request of the paging team
    "androidx.annotation.CheckResult",
    // This annotation is generated upstream. Dokka uses it for signature serialization. It doesn't
    // seem useful for developers
    "kotlin.ParameterName",
    // This annotations is not useful for developers but right now is @ShowAnnotation?
    "kotlin.js.JsName",
    // This annotation is intended to target the compiler and is general not useful for devs.
    "java.lang.Override"
)

// Annotations which should not be displayed in the Kotlin docs, in addition to hiddenAnnotations
private val hiddenAnnotationsKotlin: List<String> = listOf(
    "kotlin.ExtensionFunctionType"
)

// Annotations which should not be displayed in the Java docs, in addition to hiddenAnnotations
private val hiddenAnnotationsJava: List<String> = emptyList()

/**
 * Data class that matches JSON structure of kotlin source set metadata
 */
data class ProjectStructureMetadata(
    var sourceSets: List<SourceSetMetadata>
)

data class SourceSetMetadata(
    val name: String,
    val analysisPlatform: String,
    var dependencies: List<String>
)

@CacheableTask
abstract class UnzipMultiplatformSourcesTask() : DefaultTask() {

    @get:Input
    abstract val inputJars: ListProperty<File>

    @OutputDirectory
    lateinit var metadataOutput: File

    @OutputDirectory
    lateinit var sourceOutput: File

    @get:Inject
    abstract val fileSystemOperations: FileSystemOperations
    @get:Inject
    abstract val archiveOperations: ArchiveOperations

    @TaskAction
    fun execute() {
        val sources = inputJars.get().associate { it.name to archiveOperations.zipTree(it) }
        fileSystemOperations.sync {
            it.duplicatesStrategy = DuplicatesStrategy.FAIL
            it.from(sources.values)
            it.into(sourceOutput)
            it.exclude("META-INF/*")
        }
        sources.forEach { (name, fileTree) ->
            fileSystemOperations.sync {
                it.from(fileTree)
                it.into(metadataOutput.resolve(name))
                it.include("META-INF/*")
            }
        }
    }
}

/**
 * Merges multiplatform metadata files created by [CreateMultiplatformMetadata]
 */
@CacheableTask
abstract class MergeMultiplatformMetadataTask() : DefaultTask() {

    @get:InputFiles @get:PathSensitive(PathSensitivity.RELATIVE)
    lateinit var inputDirectory: File
    @OutputFile
    lateinit var mergedProjectMetadata: File
    @TaskAction
    fun execute() {
        val mergedMetadata = ProjectStructureMetadata(sourceSets = listOf())
        inputDirectory.walkTopDown().filter { file ->
            file.name == PROJECT_STRUCTURE_METADATA_FILENAME
        }.forEach { metaFile ->
            val gson = GsonBuilder().create()
            val metadata = gson.fromJson(
                metaFile.readText(),
                ProjectStructureMetadata::class.java
            )
            mergedMetadata.merge(metadata)
        }
        val gson = GsonBuilder().setPrettyPrinting().create()
        val json = gson.toJson(mergedMetadata)
        mergedProjectMetadata.apply {
            parentFile.mkdirs()
            createNewFile()
            writeText(json)
        }
    }

    private fun ProjectStructureMetadata.merge(metadata: ProjectStructureMetadata) {
        val originalSourceSets = this.sourceSets
        metadata.sourceSets.forEach { newSourceSet ->
            val existingSourceSet = originalSourceSets.find { it.name == newSourceSet.name }
            if (existingSourceSet != null) {
                existingSourceSet.dependencies =
                    (newSourceSet.dependencies + existingSourceSet.dependencies).toSet().toList()
            } else {
                sourceSets += listOf(newSourceSet)
            }
        }
    }
}
