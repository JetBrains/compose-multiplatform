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

import androidx.build.enforceKtlintVersion
import androidx.build.SupportConfig
import androidx.build.dackka.DackkaTask
import androidx.build.dependencies.KOTLIN_VERSION
import androidx.build.doclava.DacOptions
import androidx.build.doclava.DoclavaTask
import androidx.build.doclava.GENERATE_DOCS_CONFIG
import androidx.build.doclava.createGenerateSdkApiTask
import androidx.build.dokka.Dokka
import androidx.build.getAndroidJar
import androidx.build.getBuildId
import androidx.build.getCheckoutRoot
import androidx.build.getDistributionDirectory
import androidx.build.getKeystore
import androidx.build.getLibraryByName
import com.android.build.api.attributes.BuildTypeAttr
import com.android.build.gradle.LibraryExtension
import com.android.build.gradle.LibraryPlugin
import java.io.File
import java.io.FileNotFoundException
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
import org.gradle.api.model.ObjectFactory
import org.gradle.api.plugins.JavaBasePlugin
import org.gradle.api.tasks.InputFiles
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
import org.jetbrains.dokka.gradle.DokkaAndroidTask
import org.jetbrains.dokka.gradle.PackageOptions

/**
 * Plugin that allows to build documentation for a given set of prebuilt and tip of tree projects.
 */
abstract class AndroidXDocsImplPlugin : Plugin<Project> {
    lateinit var docsType: String
    lateinit var docsSourcesConfiguration: Configuration
    lateinit var samplesSourcesConfiguration: Configuration
    lateinit var dependencyClasspath: FileCollection

    @get:javax.inject.Inject
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
        val unzippedDocsSources = File(project.buildDir, "srcs")
        val unzipDocsTask = configureUnzipTask(
            project,
            "unzipDocsSources",
            unzippedDocsSources,
            docsSourcesConfiguration
        )

        val unzippedSourcesForDackka = File(project.buildDir, "unzippedSourcesForDackka")
        val unzipSourcesForDackkaTask = configureDackkaUnzipTask(
            project,
            unzippedSourcesForDackka,
            docsSourcesConfiguration
        )

        configureDackka(
            project,
            unzippedSourcesForDackka,
            unzipSourcesForDackkaTask,
            unzippedSamplesSources,
            unzipSamplesTask,
            dependencyClasspath,
            buildOnServer
        )
        configureDokka(
            project,
            unzippedDocsSources,
            unzipDocsTask,
            unzippedSamplesSources,
            unzipSamplesTask,
            dependencyClasspath,
            buildOnServer
        )
        configureDoclava(
            project,
            unzippedDocsSources,
            unzipDocsTask,
            dependencyClasspath,
            buildOnServer
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
        @Suppress("UnstableApiUsage")
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
                    jars.map {
                        localVar.zipTree(it).matching {
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
    private fun configureDackkaUnzipTask(
        project: Project,
        destinationDirectory: File,
        docsConfiguration: Configuration
    ): TaskProvider<Sync> {
        return project.tasks.register("unzipSourcesForDackka", Sync::class.java) { task ->
            val sources = docsConfiguration.incoming.artifactView { }.files

            // Store archiveOperations into a local variable to prevent access to the plugin
            // during the task execution, as that breaks configuration caching.
            val localVar = archiveOperations
            task.from(
                sources.elements.map { jars ->
                    jars.map {
                        localVar.zipTree(it)
                    }
                }
            )
            task.into(destinationDirectory)
            task.duplicatesStrategy = DuplicatesStrategy.INCLUDE
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
        unzippedDocsSources: File,
        unzipDocsTask: TaskProvider<Sync>,
        unzippedSamplesSources: File,
        unzipSamplesTask: TaskProvider<Sync>,
        dependencyClasspath: FileCollection,
        buildOnServer: TaskProvider<*>
    ) {
        val generatedDocsDir = project.file("${project.buildDir}/dackkaDocs")

        val dackkaConfiguration = project.configurations.create("dackka").apply {
            dependencies.add(project.dependencies.create(project.getLibraryByName("dackka")))
        }

        val dackkaTask = project.tasks.register("dackkaDocs", DackkaTask::class.java) { task ->
            task.apply {
                dependsOn(unzipDocsTask)
                dependsOn(unzipSamplesTask)

                description = "Generates reference documentation using a Google devsite Dokka" +
                    " plugin. Places docs in $generatedDocsDir"
                group = JavaBasePlugin.DOCUMENTATION_GROUP

                dackkaClasspath.from(project.files(dackkaConfiguration))
                destinationDir = generatedDocsDir
                frameworkSamplesDir = File(project.rootDir, "samples")
                samplesDir = unzippedSamplesSources
                sourcesDir = unzippedDocsSources
                docsProjectDir = File(project.rootDir, "docs-public")
                dependenciesClasspath = project.getAndroidJar() + dependencyClasspath
                excludedPackages = hiddenPackages.toSet()
                excludedPackagesForJava = hiddenPackagesJava
                excludedPackagesForKotlin = emptySet()
            }
        }

        val zipTask = project.tasks.register("zipDackkaDocs", Zip::class.java) { task ->
            task.apply {
                dependsOn(dackkaTask)
                from(generatedDocsDir)

                val baseName = "dackka-$docsType-docs"
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

    private fun configureDokka(
        project: Project,
        unzippedDocsSources: File,
        unzipDocsTask: TaskProvider<Sync>,
        unzippedSamplesSources: File,
        unzipSamplesTask: TaskProvider<Sync>,
        dependencyClasspath: FileCollection,
        buildOnServer: TaskProvider<*>
    ) {
        val dokkaTask = Dokka.createDokkaTask(
            project,
            hiddenPackages,
            "Kotlin",
            "dac",
            "/reference/kotlin"
        )
        dokkaTask.configure { task ->
            task.sourceDirs += unzippedDocsSources
            task.sourceDirs += unzippedSamplesSources
            task.dependsOn(unzipDocsTask)
            task.dependsOn(unzipSamplesTask)

            val androidJar = project.getAndroidJar()
            val dokkaClasspath = project.provider({
                project.files(androidJar).plus(dependencyClasspath)
            })
            // DokkaTask tries to resolve DokkaTask#classpath right away for jars that might not
            // be there yet. Delay the setting of this property to before we run the task.
            task.inputs.files(androidJar, dependencyClasspath)
            task.doFirst { dokkaTask ->
                dokkaTask as DokkaAndroidTask
                val packages =
                    unzippedSamplesSources.walkTopDown().filter { it.isFile }.mapNotNull { file ->
                        val lines = file.readLines()
                        lines.find { line ->
                            line.startsWith("package ")
                        }?.replace("package ", "")
                    }.distinct()

                packages.forEach { packageName ->
                    val opts = PackageOptions()
                    opts.prefix = packageName
                    opts.suppress = true
                    dokkaTask.perPackageOptions.add(opts)
                }
                dokkaTask.classpath = dokkaClasspath.get()
            }
        }
        val zipTask = project.tasks.register("zipDokkaDocs", Zip::class.java) {
            it.apply {
                it.dependsOn(dokkaTask)
                from(dokkaTask.map { it.outputDirectory }) { copySpec ->
                    copySpec.into("reference/kotlin")
                }
                val baseName = "dokka-$docsType-docs"
                val buildId = getBuildId()
                archiveBaseName.set(baseName)
                archiveVersion.set(buildId)
                destinationDirectory.set(project.getDistributionDirectory())
                group = JavaBasePlugin.DOCUMENTATION_GROUP
                val filePath = "${project.getDistributionDirectory().canonicalPath}/"
                val fileName = "$baseName-$buildId.zip"
                val destinationFile = filePath + fileName
                description = "Zips Kotlin documentation (generated via Dokka in the " +
                    "style of d.android.com) into $destinationFile"
            }
        }
        buildOnServer.configure { it.dependsOn(zipTask) }
    }

    private fun configureDoclava(
        project: Project,
        unzippedDocsSources: File,
        unzipDocsTask: TaskProvider<Sync>,
        dependencyClasspath: FileCollection,
        buildOnServer: TaskProvider<*>
    ) {
        // Hack to force tools.jar (required by com.sun.javadoc) to be available on the Doclava
        // run-time classpath. Note this breaks the ability to use JDK 9+ for compilation.
        val doclavaConfiguration = project.configurations.create("doclava")
        doclavaConfiguration.dependencies.add(project.dependencies.create(DOCLAVA_DEPENDENCY))
        doclavaConfiguration.dependencies.add(
            project.dependencies.create(
                project.files(System.getenv("JAVA_TOOLS_JAR"))
            )
        )

        val annotationConfiguration = project.configurations.create("annotation")
        annotationConfiguration.dependencies.add(
            project.dependencies.project(
                mapOf("path" to ":fakeannotations")
            )
        )

        val generatedSdk = File(project.buildDir, "generatedsdk")
        val generateSdkApiTask = createGenerateSdkApiTask(
            project, doclavaConfiguration, annotationConfiguration, generatedSdk
        )

        val destDir = File(project.buildDir, "javadoc")
        val offlineOverride = project.findProject("offlineDocs") as String?
        val offline = if (offlineOverride != null) { offlineOverride == "true" } else false
        val dacOptions = DacOptions("androidx", "ANDROIDX_DATA")

        val doclavaTask = project.tasks.register("doclavaDocs", DoclavaTask::class.java) {
            it.apply {
                dependsOn(unzipDocsTask)
                dependsOn(generateSdkApiTask)
                group = JavaBasePlugin.DOCUMENTATION_GROUP
                description = "Generates Java documentation in the style of d.android.com. To " +
                    "generate offline docs use \'-PofflineDocs=true\' parameter.  Places the " +
                    "documentation in $destDir"
                dependsOn(doclavaConfiguration)
                setDocletpath(doclavaConfiguration)
                destinationDir = destDir
                classpath = project.getAndroidJar() + dependencyClasspath
                checksConfig = GENERATE_DOCS_CONFIG
                extraArgumentsBuilder.apply {
                    addStringOption(
                        "templatedir",
                        "${project.getCheckoutRoot()}/external/doclava/res/assets/templates-sdk"
                    )
                    // Note, this is pointing to the root checkout directory.
                    addStringOption(
                        "samplesdir",
                        "${project.rootDir}/samples"
                    )
                    addStringOption(
                        "federate",
                        listOf("Android", "https://developer.android.com")
                    )
                    addStringOption(
                        "federationapi",
                        listOf(
                            "Android",
                            generateSdkApiTask.get().apiFile?.absolutePath.toString()
                        )
                    )
                    addStringOption("hdf", listOf("android.whichdoc", "online"))
                    addStringOption("hdf", listOf("android.hasSamples", "true"))
                    addStringOption("hdf", listOf("dac", "true"))

                    // Specific to reference docs.
                    if (!offline) {
                        addStringOption("toroot", "/")
                        addOption("devsite")
                        addOption("yamlV2")
                        addStringOption("dac_libraryroot", dacOptions.libraryroot)
                        addStringOption("dac_dataname", dacOptions.dataname)
                    }
                }
                it.source(project.fileTree(unzippedDocsSources))
            }
        }
        val zipTask = project.tasks.register("zipDoclavaDocs", Zip::class.java) {
            it.apply {
                it.dependsOn(doclavaTask)
                from(doclavaTask.map { it.destinationDir!! })
                val baseName = "doclava-$docsType-docs"
                val buildId = getBuildId()
                archiveBaseName.set(baseName)
                archiveVersion.set(buildId)
                destinationDirectory.set(project.getDistributionDirectory())
                group = JavaBasePlugin.DOCUMENTATION_GROUP
                val filePath = "${project.getDistributionDirectory().canonicalPath}/"
                val fileName = "$baseName-$buildId.zip"
                val destinationFile = filePath + fileName
                description = "Zips Java documentation (generated via Doclava in the " +
                    "style of d.android.com) into $destinationFile"
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
            File(distributionDirectory, "dackka-$docsType-docs-$buildId.zip"),
            File(distributionDirectory, "doclava-$docsType-docs-$buildId.zip"),
            File(distributionDirectory, "dokka-$docsType-docs-$buildId.zip")
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

private const val DOCLAVA_DEPENDENCY = "com.android:doclava:1.0.6"

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
    "androidx.*compose.*"
)
