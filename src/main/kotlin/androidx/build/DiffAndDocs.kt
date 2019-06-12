/*
 * Copyright 2018 The Android Open Source Project
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

package androidx.build

import androidx.build.Strategy.Prebuilts
import androidx.build.Strategy.TipOfTree
import androidx.build.doclava.ChecksConfig
import androidx.build.doclava.DEFAULT_DOCLAVA_CONFIG
import androidx.build.doclava.DoclavaTask
import androidx.build.docs.GenerateDocsTask
import androidx.build.gradle.isRoot
import com.android.build.gradle.AppExtension
import com.android.build.gradle.LibraryExtension
import com.android.build.gradle.api.BaseVariant
import com.android.build.gradle.api.SourceKind
import com.google.common.base.Preconditions
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.ResolveException
import org.gradle.api.file.FileCollection
import org.gradle.api.file.FileTree
import org.gradle.api.plugins.ExtraPropertiesExtension
import org.gradle.api.plugins.JavaBasePlugin
import org.gradle.api.tasks.TaskContainer
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.tasks.bundling.Zip
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.api.tasks.javadoc.Javadoc
import org.gradle.api.tasks.util.PatternSet
import java.io.File
import java.net.URLClassLoader
import javax.tools.ToolProvider
import kotlin.collections.set

private const val DOCLAVA_DEPENDENCY = "com.android:doclava:1.0.6"

data class DacOptions(val libraryroot: String, val dataname: String)

/**
 * Object used to manage configuration of documentation generation tasks.
 *
 * @property root the top-level AndroidX project.
 * @property supportRootFolder the directory in which the top-level AndroidX project lives.
 * @property dacOptions additional options for generating output compatible with d.android.com.
 * @property additionalRules optional list of rule sets used to generate documentation.
 * @constructor Creates a DiffAndDocs object and immediately creates related documentation tasks.
 */
class DiffAndDocs private constructor(
    root: Project,
    supportRootFolder: File,
    dacOptions: DacOptions,
    additionalRules: List<PublishDocsRules> = emptyList()
) {
    private val anchorTask: TaskProvider<Task>

    /**
     * Placeholder project used to generate top-level documentation.
     */
    private val docsProject: Project?

    /**
     * List of documentation rule sets.
     */
    private val rules: List<PublishDocsRules>

    /**
     * Map of documentation rule sets (by human-readable label) to documentation generation tasks.
     */
    private val docsTasks: MutableMap<String, TaskProvider<GenerateDocsTask>> = mutableMapOf()

    init {
        // Hack to force tools.jar (required by com.sun.javadoc) to be available on the Doclava
        // run-time classpath. Note this breaks the ability to use JDK 9+ for compilation.
        val doclavaConfiguration = root.configurations.create("doclava")
        doclavaConfiguration.dependencies.add(root.dependencies.create(DOCLAVA_DEPENDENCY))
        doclavaConfiguration.dependencies.add(root.dependencies.create(root.files(
                (ToolProvider.getSystemToolClassLoader() as URLClassLoader).urLs)))

        // Pulls in the :fakeannotations project, which provides modified annotations required to
        // generate SDK API stubs in Doclava from Metalava-generated platform SDK stubs.
        val annotationConfiguration = root.configurations.create("annotation")
        annotationConfiguration.dependencies.add(root.dependencies.project(
            mapOf("path" to ":fakeannotations")))

        rules = additionalRules + TIP_OF_TREE
        docsProject = root.findProject(":docs-fake")
        anchorTask = root.tasks.register("anchorDocsTask")
        val generateSdkApiTask = createGenerateSdkApiTask(root, doclavaConfiguration,
            annotationConfiguration)
        val offlineOverride = root.processProperty("offlineDocs")

        // Associate each documentation generation rule set with a GenerateDocsTask.
        rules.forEach { rule ->
            val offline = if (offlineOverride != null) {
                offlineOverride == "true"
            } else {
                rule.offline
            }

            val generateDocsTask = createGenerateDocsTask(
                project = root, generateSdkApiTask = generateSdkApiTask,
                doclavaConfig = doclavaConfiguration,
                supportRootFolder = supportRootFolder, dacOptions = dacOptions,
                destDir = File(root.docsDir(), rule.name),
                taskName = "${rule.name}DocsTask",
                offline = offline)
            docsTasks[rule.name] = generateDocsTask
            val createDistDocsTask = createDistDocsTask(root, generateDocsTask, rule.name)
            anchorTask.configure { task ->
                task.dependsOn(createDistDocsTask)
            }
        }

        root.tasks.register("generateDocs") { task ->
            task.group = JavaBasePlugin.DOCUMENTATION_GROUP
            task.description = "Generates documentation (both Java and Kotlin) from tip-of-tree " +
                "sources, in the style of those used in d.android.com."
            task.dependsOn(docsTasks[TIP_OF_TREE.name])
        }
    }

    companion object {
        private const val EXT_NAME = "DIFF_AND_DOCS_EXT"
        /**
         * Returns the instance of DiffAndDocs from the Root project
         */
        fun get(project: Project): DiffAndDocs {
            return project.rootProject.extensions.findByName(EXT_NAME) as? DiffAndDocs
                ?: throw IllegalStateException("must call configureDiffAndDocs first")
        }

        /**
         * Initializes documentation generation.
         *
         * This should happen only once (and on the root project).
         *
         * @property root the top-level AndroidX project.
         * @property supportRootFolder the directory in which the top-level AndroidX project lives.
         * @property dacOptions additional options for generating output compatible with
         *           d.android.com.
         * @property additionalRules optional list of rule sets used to generate documentation.
         * @return the anchor task.
         */
        fun configureDiffAndDocs(
            root: Project,
            supportRootFolder: File,
            dacOptions: DacOptions,
            additionalRules: List<PublishDocsRules> = emptyList()
        ): TaskProvider<Task> {
            Preconditions.checkArgument(root.isRoot, "Must pass the root project")
            Preconditions.checkState(root.extensions.findByName(EXT_NAME) == null,
                "Cannot initialize DiffAndDocs twice")
            val instance = DiffAndDocs(
                root = root,
                supportRootFolder = supportRootFolder,
                dacOptions = dacOptions,
                additionalRules = additionalRules
            )
            root.extensions.add(EXT_NAME, instance)
            instance.setupDocsProject()
            return instance.anchorTask
        }
    }

    /**
     * Builds a file tree containing source files for the specified [mavenId]. As a side-effect, the
     * resulting file tree is also added to a configuration on the [root] project.
     *
     * This method is intended to be called as the result of a resolved DocsRule, and takes the
     * [originName] as the name of the containing rule set and [originRule] as the name of the rule.
     *
     * @param root the project to which the sources of the resolved artifact should be added.
     * @param mavenId the Maven coordinate of the artifact whose source files should be returned.
     * @param originName the name of the documentation rule set in which [originRule] was specified.
     * @param originRule the documentation rule that depends on the source of [mavenId].
     * @return a file tree containing the source files to be documented.
     */
    private fun prebuiltSources(
        root: Project,
        mavenId: String,
        originName: String,
        originRule: DocsRule
    ): FileTree {
        val configName = "docs-temp_${mavenId.replace(":", "-")}"
        val configuration = root.configurations.create(configName)
        root.dependencies.add(configName, mavenId)

        val artifacts = try {
            configuration.resolvedConfiguration.resolvedArtifacts
        } catch (e: ResolveException) {
            root.logger.error("Failed to find prebuilts for $mavenId. " +
                    "A matching rule $originRule in docsRules(\"$originName\") " +
                    "in PublishDocsRules.kt requires it. You should either add a prebuilt, " +
                    "or add overriding \"ignore\" or \"tipOfTree\" rules")
            throw e
        }

        val artifact = artifacts.find { it.moduleVersion.id.toString() == mavenId }
                ?: throw GradleException()

        val folder = artifact.file.parentFile
        val tree = root.zipTree(File(folder, "${artifact.file.nameWithoutExtension}-sources.jar"))
                .matching {
                    it.exclude("**/*.MF")
                    it.exclude("**/*.aidl")
                    it.exclude("**/*.html")
                    it.exclude("**/*.kt")
                    it.exclude("**/META-INF/**")
                    it.exclude("**/OWNERS")
                    it.exclude("**/NOTICE.txt")
                }
        root.configurations.remove(configuration)
        return tree
    }

    private fun setupDocsProject() {
        docsProject?.afterEvaluate { docs ->
            val appExtension = docs.extensions.findByType(AppExtension::class.java)
                    ?: throw GradleException("Android app plugin is missing on docsProject")

            rules.forEach { rule ->
                appExtension.productFlavors.register(rule.name).configure {
                    it.dimension = "library-group"
                }
            }

            appExtension.applicationVariants.all { appVariant ->
                val taskProvider = docsTasks[appVariant.flavorName]
                if (appVariant.buildType.name == "release" && taskProvider != null) {
                    registerAndroidProjectForDocsTask(taskProvider, appVariant)

                    // Exclude the R.java file from documentation.
                    taskProvider.configure {
                        it.exclude { fileTreeElement ->
                            fileTreeElement.path.endsWith(appVariant.rFile())
                        }
                    }
                }
            }
        }

        // Before evaluation, make the docs placeholder project depend on every other project.
        docsProject?.let { docsProject ->
            docsProject.beforeEvaluate {
                docsProject.rootProject.subprojects.asSequence()
                    .filter { docsProject != it }
                    .forEach { docsProject.evaluationDependsOn(it.path) }
            }
        }
    }

    /**
     * Registers prebuilt sources for the library represented by the specified [extension].
     *
     * Note that this method is not synchronous. It sets up an after-evaluate block that resolves
     * the documentation rule for the library and sets up the necessary prebuilt dependencies.
     *
     * @param extension the library for which prebuilts should be registered.
     */
    fun registerPrebuilts(extension: AndroidXExtension) {
        docsProject?.afterEvaluate { docs ->
            val depHandler = docs.dependencies
            val root = docs.rootProject
            rules.forEach { rule ->
                val resolvedRule = rule.resolve(extension)
                val strategy = resolvedRule?.strategy
                if (strategy is Prebuilts) {
                    // Add the library's prebuilt JAR to the documentation generation project's
                    // implementation dependencies as a Maven spec. Note there is no requirement to
                    // use the Maven spec here -- this could also be a direct reference to the AAR.
                    val dependency = strategy.dependency(extension)
                    depHandler.add("${rule.name}Implementation", dependency)

                    // Optionally add the library's stub JAR dependencies (ex. sidecar JARs) to the
                    // documentation generation project's compilation classpath. This ensures the
                    // stub JARs will be available on the documentation generators's run-time
                    // classpath.
                    strategy.stubs?.forEach { path ->
                        depHandler.add("${rule.name}CompileOnly", root.files(path))
                    }

                    // Add the library's prebuilt source JAR to the GenerateDocsTask associated
                    // with this rule.
                    docsTasks[rule.name]!!.configure {
                        it.source(prebuiltSources(root, dependency, rule.name, resolvedRule))
                    }
                }
            }
        }
    }

    /**
     * Applies the [setup] lambda to all docs rules where the strategy for [extension] resolves to
     * TipOfTree.
     */
    private fun tipOfTreeTasks(
        extension: AndroidXExtension,
        setup: (TaskProvider<out DoclavaTask>) -> Unit
    ) {
        rules.filter { rule -> rule.resolve(extension)?.strategy == TipOfTree }
                .mapNotNull { rule -> docsTasks[rule.name] }
                .forEach(setup)
    }

    /**
     * Registers a Java project to be included in docs generation, local API file generation, and
     * local API diff generation tasks.
     */
    fun registerJavaProject(project: Project, extension: AndroidXExtension) {
        val compileJava = project.tasks.named("compileJava", JavaCompile::class.java)

        registerPrebuilts(extension)

        tipOfTreeTasks(extension) { task ->
            registerJavaProjectForDocsTask(task, compileJava)
        }
    }

    /**
     * Registers an Android project to be included in global docs generation, local API file
     * generation, and local API diff generation tasks.
     */
    fun registerAndroidProject(
        library: LibraryExtension,
        extension: AndroidXExtension
    ) {

        registerPrebuilts(extension)
        library.libraryVariants.all { variant ->
            if (variant.name == Release.DEFAULT_PUBLISH_CONFIG) {
                // include R.file generated for prebuilts
                rules.filter { it.resolve(extension)?.strategy is Prebuilts }.forEach { rule ->
                    docsTasks[rule.name]?.configure {
                        it.include { fileTreeElement ->
                            fileTreeElement.path.endsWith(variant.rFile())
                        }
                    }
                }

                tipOfTreeTasks(extension) { task ->
                    registerAndroidProjectForDocsTask(task, variant)
                }
            }
        }
    }
}

/**
 * Registers a Java project on the given Javadocs task.
 * <p>
 * <ul>
 * <li>Sets up a dependency to ensure the project is compiled prior to running the task
 * <li>Adds the project's source files to the Javadoc task's source files
 * <li>Adds the project's compilation classpath (e.g. dependencies) to the task classpath to ensure
 *     that references in the source files may be resolved
 * <li>Adds the project's output artifacts to the task classpath to ensure that source references to
 *     generated code may be resolved
 * </ul>
 */
private fun registerJavaProjectForDocsTask(
    docsTaskProvider: TaskProvider<out Javadoc>,
    javaCompileTaskProvider: TaskProvider<JavaCompile>
) {
    docsTaskProvider.configure { docsTask ->
        docsTask.dependsOn(javaCompileTaskProvider)
        var javaCompileTask = javaCompileTaskProvider.get()
        docsTask.source(javaCompileTask.source)
        val project = docsTask.project
        docsTask.classpath += project.files(javaCompileTask.classpath) +
                project.files(javaCompileTask.destinationDir)
    }
}

/**
 * Registers an Android project on the given Javadocs task.
 * <p>
 * @see #registerJavaProjectForDocsTask
 */
private fun registerAndroidProjectForDocsTask(
    task: TaskProvider<out Javadoc>,
    releaseVariant: BaseVariant
) {
    // This code makes a number of unsafe assumptions about Android Gradle Plugin,
    // and there's a good chance that this will break in the near future.
    val javaCompileProvider = releaseVariant.javaCompileProvider
    task.configure {
        it.dependsOn(javaCompileProvider)
        it.include { fileTreeElement ->
            fileTreeElement.name != "R.java" ||
                    fileTreeElement.path.endsWith(releaseVariant.rFile())
        }
        releaseVariant.getSourceFolders(SourceKind.JAVA).forEach { sourceSet ->
            it.source(sourceSet)
        }
        it.classpath += releaseVariant.getCompileClasspath(null) +
                it.project.files(javaCompileProvider.get().destinationDir)
    }
}

/**
 * Registers a task for bundling online documentation as a ZIP file.
 *
 * @param project the project from which source files and JARs will be used to generate docs.
 * @param generateDocs a Doclava task configured to generate online documentation.
 * @param ruleName the human-readable label to use for the task and ZIP file.
 */
private fun createDistDocsTask(
    project: Project,
    generateDocs: TaskProvider<out DoclavaTask>,
    ruleName: String = ""
): TaskProvider<Zip> = project.tasks.register("dist${ruleName}Docs", Zip::class.java) {
    it.apply {
        dependsOn(generateDocs)
        from(generateDocs.map {
            it.destinationDir
        })
        val baseName = "androidx-$ruleName-docs"
        val buildId = getBuildId()
        archiveBaseName.set(baseName)
        archiveVersion.set(buildId)
        destinationDirectory.set(project.getDistributionDirectory())
        group = JavaBasePlugin.DOCUMENTATION_GROUP
        val filePath = "${project.getDistributionDirectory().canonicalPath}/"
        val fileName = "$baseName-$buildId.zip"
        val destinationFile = filePath + fileName
        description = "Zips $ruleName Java documentation (generated via Doclava in the " +
            "style of d.android.com) into $destinationFile"
        doLast {
            logger.lifecycle("'Wrote API reference to $destinationFile")
        }
    }
}

/**
 * Creates a task to generate an API file from the platform SDK's source and stub JARs.
 * <p>
 * This is useful for federating docs against the platform SDK when no API XML file is available.
 */
private fun createGenerateSdkApiTask(
    project: Project,
    doclavaConfig: Configuration,
    annotationConfig: Configuration
): DoclavaTask =
        project.tasks.createWithConfig("generateSdkApi", DoclavaTask::class.java) {
            dependsOn(doclavaConfig)
            dependsOn(annotationConfig)
            description = "Generates API files for the current SDK."
            setDocletpath(doclavaConfig.resolve())
            destinationDir = project.docsDir()
            // Strip the androidx.annotation classes injected by Metalava. They are not accessible.
            classpath = androidJarFile(project)
                .filter { it.path.contains("androidx/annotation") }
                .plus(project.files(annotationConfig.resolve()))
            source(project.zipTree(androidSrcJarFile(project))
                .matching(PatternSet().include("**/*.java")))
            exclude("**/overview.html") // TODO https://issuetracker.google.com/issues/116699307
            apiFile = sdkApiFile(project)
            generateDocs = false
            coreJavadocOptions {
                addStringOption("stubpackages", "android.*")
            }
        }

/**
 * List of Doclava checks that should be ignored when generating documentation.
 */
private val GENERATEDOCS_HIDDEN = listOf(105, 106, 107, 111, 112, 113, 115, 116, 121)

/**
 * Doclava checks configuration for use in generating documentation.
 */
private val GENERATE_DOCS_CONFIG = ChecksConfig(
        warnings = emptyList(),
        hidden = GENERATEDOCS_HIDDEN + DEFAULT_DOCLAVA_CONFIG.hidden,
        errors = ((101..122) - GENERATEDOCS_HIDDEN)
)

/**
 * Registers a documentation generation task for the specified project.
 *
 * Note that unlike many other methods, the [project] passed into this method is *not* the root
 * project but rather the project for which documentation should be generated.
 *
 * @param project the project from which source files and JARs will be used to generate docs.
 * @param generateSdkApiTask the task that provides the Android SDK's API txt file.
 * @param doclavaConfig command-line options to pass to the Doclava javadoc tool.
 * @param supportRootFolder the directory in which the top-level AndroidX project lives.
 * @param dacOptions additional options for generating output compatible with d.android.com.
 * @param destDir the directory into which generated documentation should be output.
 * @param taskName the name to give the resulting task.
 * @param offline true if generating documentation for local use, false otherwise.
 */
private fun createGenerateDocsTask(
    project: Project,
    generateSdkApiTask: DoclavaTask,
    doclavaConfig: Configuration,
    supportRootFolder: File,
    dacOptions: DacOptions,
    destDir: File,
    taskName: String = "generateDocs",
    offline: Boolean
): TaskProvider<GenerateDocsTask> =
        project.tasks.register(taskName, GenerateDocsTask::class.java) {
            it.apply {
                exclude("**/R.java")
                dependsOn(generateSdkApiTask, doclavaConfig)
                group = JavaBasePlugin.DOCUMENTATION_GROUP
                description = "Generates Java documentation in the style of d.android.com. To " +
                        "generate offline docs use \'-PofflineDocs=true\' parameter.  Places the " +
                        "documentation in $destDir"

                setDocletpath(doclavaConfig.resolve())
                destinationDir = File(destDir, if (offline) "offline" else "online")
                classpath = androidJarFile(project)
                checksConfig = GENERATE_DOCS_CONFIG
                addSinceFilesFrom(supportRootFolder)

                coreJavadocOptions {
                    addStringOption("templatedir",
                        "$supportRootFolder/../../external/doclava/res/assets/templates-sdk")
                    addStringOption("samplesdir", "$supportRootFolder/samples")
                    addMultilineMultiValueOption("federate").value = listOf(
                        listOf("Android", "https://developer.android.com")
                    )
                    addMultilineMultiValueOption("federationapi").value = listOf(
                        listOf("Android", generateSdkApiTask.apiFile?.absolutePath)
                    )
                    addMultilineMultiValueOption("hdf").value = listOf(
                        listOf("android.whichdoc", "online"),
                        listOf("android.hasSamples", "true"),
                        listOf("dac", "true")
                    )

                    // Specific to reference docs.
                    if (!offline) {
                        addStringOption("toroot", "/")
                        addBooleanOption("devsite", true)
                        addBooleanOption("yamlV2", true)
                        addStringOption("dac_libraryroot", dacOptions.libraryroot)
                        addStringOption("dac_dataname", dacOptions.dataname)
                    }
                }

                addArtifactsAndSince()
            }
        }

/**
 * @return the project's Android SDK API txt as a File.
 */
private fun sdkApiFile(project: Project) = File(project.docsDir(), "release/sdk_current.txt")

/**
 * @return the [taskClass] constructed and configured using the provided [config].
 */
fun <T : Task> TaskContainer.createWithConfig(
    name: String,
    taskClass: Class<T>,
    config: T.() -> Unit
) =
        create(name, taskClass) { task -> task.config() }

/**
 * @return the project's Android SDK stub JAR as a File.
 */
fun androidJarFile(project: Project): FileCollection =
        project.files(arrayOf(File(project.sdkPath(),
                "platforms/${SupportConfig.COMPILE_SDK_VERSION}/android.jar")))

/**
 * @return the project's Android SDK stub source JAR as a File.
 */
private fun androidSrcJarFile(project: Project): File = File(project.sdkPath(),
        "platforms/${SupportConfig.COMPILE_SDK_VERSION}/android-stubs-src.jar")

/**
 * @return the R.java file for the variant, which may not exist.
 */
private fun BaseVariant.rFile() = "${applicationId.replace('.', '/')}/R.java"

/**
 * @return the directory in which to place documentation output.
 */
fun Project.docsDir(): File {
    val actualRootProject = if (project.isRoot) project else project.rootProject
    return File(actualRootProject.buildDir, "javadoc")
}

/**
 * @return the root project's SDK path as a File.
 */
private fun Project.sdkPath(): File {
    val supportRoot = (project.rootProject.property("ext") as ExtraPropertiesExtension)
        .get("supportRootFolder") as File
    return getSdkPath(supportRoot)
}

/**
 * Extension for accessing Strings in Project.properties by [name].
 */
fun Project.processProperty(name: String) =
        if (hasProperty(name)) {
            properties[name] as String
        } else {
            null
        }
