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
import androidx.build.checkapi.ApiXmlConversionTask
import androidx.build.checkapi.CheckApiTasks
import androidx.build.doclava.ChecksConfig
import androidx.build.doclava.DEFAULT_DOCLAVA_CONFIG
import androidx.build.doclava.DoclavaTask
import androidx.build.docs.ConcatenateFilesTask
import androidx.build.docs.GenerateDocsTask
import androidx.build.gradle.isRoot
import androidx.build.jdiff.JDiffTask
import com.android.build.gradle.AppExtension
import com.android.build.gradle.LibraryExtension
import com.android.build.gradle.api.BaseVariant
import com.android.build.gradle.api.SourceKind;
import com.google.common.base.Preconditions
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.ResolveException
import org.gradle.api.file.FileCollection
import org.gradle.api.file.FileTree
import org.gradle.api.plugins.JavaBasePlugin
import org.gradle.api.tasks.TaskContainer
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.tasks.bundling.Zip
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.api.tasks.javadoc.Javadoc
import org.gradle.api.tasks.util.PatternSet
import java.io.File
import java.lang.IllegalStateException
import java.net.URLClassLoader
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.tools.ToolProvider
import kotlin.collections.set

private const val DOCLAVA_DEPENDENCY = "com.android:doclava:1.0.6"

private const val JDIFF_DEPENDENCY = "com.android:jdiff:1.1.0"
private const val XML_PARSER_APIS_DEPENDENCY = "xerces:xmlParserAPIs:2.6.2"
private const val XERCES_IMPL_DEPENDENCY = "xerces:xercesImpl:2.6.2"

data class DacOptions(val libraryroot: String, val dataname: String)

class DiffAndDocs private constructor(
    root: Project,
    supportRootFolder: File,
    dacOptions: DacOptions,
    additionalRules: List<PublishDocsRules> = emptyList()
) {
    private val anchorTask: TaskProvider<Task>
    private var docsProject: Project? = null

    private val rules: List<PublishDocsRules>
    private val docsTasks: MutableMap<String, TaskProvider<GenerateDocsTask>> = mutableMapOf()
    private val aggregateOldApiTxtsTask: TaskProvider<ConcatenateFilesTask>
    private val aggregateNewApiTxtsTask: TaskProvider<ConcatenateFilesTask>
    private val generateDiffsTask: TaskProvider<JDiffTask>

    init {
        val doclavaConfiguration = root.configurations.create("doclava")
        doclavaConfiguration.dependencies.add(root.dependencies.create(DOCLAVA_DEPENDENCY))

        // tools.jar required for com.sun.javadoc
        // TODO this breaks the ability to use JDK 9+ for compilation.
        doclavaConfiguration.dependencies.add(root.dependencies.create(root.files(
                (ToolProvider.getSystemToolClassLoader() as URLClassLoader).urLs)))

        rules = additionalRules + TIP_OF_TREE
        docsProject = root.findProject(":docs-fake")
        anchorTask = root.tasks.register("anchorDocsTask")
        val generateSdkApiTask = createGenerateSdkApiTask(root, doclavaConfiguration)
        val now = LocalDateTime.now()
        // The diff output assumes that each library is of the same version,
        // but our libraries may each be of different versions
        // So, we display the date as the new version
        val newVersion = now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
        val offlineOverride = root.processProperty("offlineDocs")

        rules.forEach {
            val offline = if (offlineOverride != null) {
                offlineOverride == "true"
            } else {
                it.offline
            }

            val task = createGenerateDocsTask(
                project = root, generateSdkApiTask = generateSdkApiTask,
                doclavaConfig = doclavaConfiguration,
                supportRootFolder = supportRootFolder, dacOptions = dacOptions,
                destDir = File(root.docsDir(), it.name),
                taskName = "${it.name}DocsTask",
                offline = offline)
            docsTasks[it.name] = task
            val createDistDocsTask = createDistDocsTask(root, task, it.name)
            anchorTask.configure {
                it.dependsOn(createDistDocsTask)
            }
        }

        root.tasks.register("generateDocs") { task ->
            task.group = JavaBasePlugin.DOCUMENTATION_GROUP
            task.description = "Generates documentation (both Java and Kotlin) from tip-of-tree " +
                "sources, in the style of those used in d.android.com."
            task.dependsOn(docsTasks[TIP_OF_TREE.name])
        }

        val docletClasspath = doclavaConfiguration.resolve()
        val oldOutputTxt = File(root.docsDir(), "previous.txt")
        aggregateOldApiTxtsTask = root.tasks.register("aggregateOldApiTxts",
            ConcatenateFilesTask::class.java) {
            it.Output = oldOutputTxt
        }

        val oldApisTask = root.tasks.register("oldApisXml",
            ApiXmlConversionTask::class.java) {
            it.classpath = root.files(docletClasspath)
            it.dependsOn(doclavaConfiguration)

            it.inputApiFile = oldOutputTxt
            it.dependsOn(aggregateOldApiTxtsTask)

            it.outputApiXmlFile = File(root.docsDir(), "previous.xml")
        }

        val newApiTxt = File(root.docsDir(), newVersion)
        aggregateNewApiTxtsTask = root.tasks.register("aggregateNewApiTxts",
            ConcatenateFilesTask::class.java) {
            it.Output = newApiTxt
        }

        val newApisTask = root.tasks.register("newApisXml",
            ApiXmlConversionTask::class.java) {
            it.classpath = root.files(docletClasspath)

            it.inputApiFile = newApiTxt
            it.dependsOn(aggregateNewApiTxtsTask)

            it.outputApiXmlFile = File(root.docsDir(), "$newVersion.xml")
        }

        val jdiffConfiguration = root.configurations.create("jdiff")
        jdiffConfiguration.dependencies.add(root.dependencies.create(JDIFF_DEPENDENCY))
        jdiffConfiguration.dependencies.add(root.dependencies.create(XML_PARSER_APIS_DEPENDENCY))
        jdiffConfiguration.dependencies.add(root.dependencies.create(XERCES_IMPL_DEPENDENCY))

        generateDiffsTask = createGenerateDiffsTask(root,
            oldApisTask,
            newApisTask,
            jdiffConfiguration)

        generateDiffsTask.configure { diffTask ->
            docsTasks.values.forEach { docs ->
                diffTask.dependsOn(docs)
            }
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
         * Initialization that should happen only once (and on the root project).
         * Returns the anchor task
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
                appExtension.productFlavors.create(rule.name) {
                    it.dimension = "library-group"
                }
            }
            appExtension.applicationVariants.all { v ->
                val taskProvider = docsTasks[v.flavorName]
                if (v.buildType.name == "release" && taskProvider != null) {
                    registerAndroidProjectForDocsTask(taskProvider, v)
                    taskProvider.configure {
                        it.exclude { fileTreeElement ->
                            fileTreeElement.path.endsWith(v.rFile())
                        }
                    }
                }
            }
        }

        docsProject?.let { docsProject ->
            docsProject.beforeEvaluate {
                docsProject.rootProject.subprojects.asSequence()
                    .filter { docsProject != it }
                    .forEach { docsProject.evaluationDependsOn(it.path) }
            }
        }
    }

    fun registerPrebuilts(extension: AndroidXExtension) =
            docsProject?.afterEvaluate { docs ->
        val depHandler = docs.dependencies
        val root = docs.rootProject
        rules.forEach { rule ->
            val resolvedRule = rule.resolve(extension)
            val strategy = resolvedRule?.strategy
            if (strategy is Prebuilts) {
                val dependency = strategy.dependency(extension)
                depHandler.add("${rule.name}Implementation", dependency)
                strategy.stubs?.forEach { path ->
                    depHandler.add("${rule.name}CompileOnly", root.files(path))
                }
                docsTasks[rule.name]!!.configure {
                    it.source(prebuiltSources(root, dependency, rule.name, resolvedRule))
                }
            }
        }
    }

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

        registerJavaProjectForDocsTask(generateDiffsTask, compileJava)
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

    private fun setupApiVersioningInDocsTasks(
        extension: AndroidXExtension,
        checkApiTasks: CheckApiTasks
    ) {
        rules.forEach { rules ->
            val project = extension.project
            val strategy = rules.resolve(extension)?.strategy
            val version = if (strategy is Prebuilts) {
                strategy.version
            } else {
                extension.project.version()
            }
            docsTasks[rules.name]!!.configure { docs ->
                // Track API change history.
                docs.addSinceFilesFrom(project.projectDir)
                // Associate current API surface with the Maven artifact.
                val artifact = "${project.group}:${project.name}:$version"
                docs.addArtifact(checkApiTasks.generateApi.get().apiFile!!.absolutePath, artifact)
                docs.dependsOn(checkApiTasks.generateApi)
            }
        }
    }

    private fun addCheckApiTasksToGraph(tasks: CheckApiTasks) {
        docsTasks.values.forEach { docs ->
            docs.configure {
                it.dependsOn(tasks.generateApi)
            }
        }
        anchorTask.configure {
            it.dependsOn(tasks.checkApi)
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
 * Generates API diffs.
 * <p>
 * By default, diffs are generated for the delta between current.txt and the
 * next most recent X.Y.Z.txt API file. Behavior may be changed by specifying
 * one or both of -PtoApi and -PfromApi.
 * <p>
 * If both fromApi and toApi are specified, diffs will be generated for
 * fromApi -> toApi. For example, 25.0.0 -> 26.0.0 diffs could be generated by
 * using:
 * <br><code>
 *   ./gradlew generateDiffs -PfromApi=25.0.0 -PtoApi=26.0.0
 * </code>
 * <p>
 * If only toApi is specified, it MUST be specified as X.Y.Z and diffs will be
 * generated for (release before toApi) -> toApi. For example, 24.2.0 -> 25.0.0
 * diffs could be generated by using:
 * <br><code>
 *   ./gradlew generateDiffs -PtoApi=25.0.0
 * </code>
 * <p>
 * If only fromApi is specified, diffs will be generated for fromApi -> current.
 * For example, lastApiReview -> current diffs could be generated by using:
 * <br><code>
 *   ./gradlew generateDiffs -PfromApi=lastApiReview
 * </code>
 * <p>
 */
private fun createGenerateDiffsTask(
    project: Project,
    oldApiTask: TaskProvider<ApiXmlConversionTask>,
    newApiTask: TaskProvider<ApiXmlConversionTask>,
    jdiffConfig: Configuration
): TaskProvider<JDiffTask> =
        project.tasks.register("generateDiffs", JDiffTask::class.java) {
            it.apply {
                // Base classpath is Android SDK, sub-projects add their own.
                classpath = androidJarFile(project)

                // JDiff properties.
                oldApiXmlFile = oldApiTask.get().outputApiXmlFile
                newApiXmlFile = newApiTask.get().outputApiXmlFile

                val newApi = newApiXmlFile.name.substringBeforeLast('.')
                val docsDir = File(project.rootProject.docsDir(), "public")

                newJavadocPrefix = "../../../../../reference/"
                destinationDir = File(docsDir,
                        "online/sdk/support_api_diff/${project.name}/$newApi")

                // Javadoc properties.
                docletpath = jdiffConfig.resolve()
                title = "Support&nbsp;Library&nbsp;API&nbsp;Differences&nbsp;Report"

                exclude("**/R.java")
                dependsOn(oldApiTask, newApiTask, jdiffConfig)
                doLast {
                    project.logger.lifecycle("generated diffs into $destinationDir")
                }
            }
        }

// Generates a distribution artifact for online docs.
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
        archiveBaseName.set("android-support-$ruleName-docs")
        archiveVersion.set(getBuildId())
        destinationDirectory.set(project.getDistributionDirectory())
        group = JavaBasePlugin.DOCUMENTATION_GROUP
        description = "Zips $ruleName Java documentation (generated via Doclava in the " +
            "style of d.android.com) into $archivePath"
        doLast {
            logger.lifecycle("'Wrote API reference to $archivePath")
        }
    }
}

/**
 * Creates a task to generate an API file from the platform SDK's source and stub JARs.
 * <p>
 * This is useful for federating docs against the platform SDK when no API XML file is available.
 */
private fun createGenerateSdkApiTask(project: Project, doclavaConfig: Configuration): DoclavaTask =
        project.tasks.createWithConfig("generateSdkApi", DoclavaTask::class.java) {
            dependsOn(doclavaConfig)
            description = "Generates API files for the current SDK."
            setDocletpath(doclavaConfig.resolve())
            destinationDir = project.docsDir()
            classpath = androidJarFile(project)
            source(project.zipTree(androidSrcJarFile(project))
                .matching(PatternSet().include("**/*.java")))
            exclude("**/overview.html") // TODO https://issuetracker.google.com/issues/116699307
            apiFile = sdkApiFile(project)
            generateDocs = false
            coreJavadocOptions {
                addStringOption("stubpackages", "android.*")
            }
        }

private val GENERATEDOCS_HIDDEN = listOf(105, 106, 107, 111, 112, 113, 115, 116, 121)
private val GENERATE_DOCS_CONFIG = ChecksConfig(
        warnings = emptyList(),
        hidden = GENERATEDOCS_HIDDEN + DEFAULT_DOCLAVA_CONFIG.hidden,
        errors = ((101..122) - GENERATEDOCS_HIDDEN)
)

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
                        "documentation in ${destDir}"

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

private fun createGenerateLocalApiDiffsArchiveTask(
    project: Project,
    diffTask: TaskProvider<JDiffTask>
): TaskProvider<Zip> = project.tasks.register("generateLocalApiDiffsArchive", Zip::class.java) {
    val docsDir = project.rootProject.docsDir()
    it.from(diffTask.map {
        it.destinationDir
    })
    it.destinationDirectory.set(File(docsDir, "online/sdk/support_api_diff/${project.name}"))
    it.to("${project.version}.zip")
}

private fun sdkApiFile(project: Project) = File(project.docsDir(), "release/sdk_current.txt")

fun <T : Task> TaskContainer.createWithConfig(
    name: String,
    taskClass: Class<T>,
    config: T.() -> Unit
) =
        create(name, taskClass) { task -> task.config() }

fun androidJarFile(project: Project): FileCollection =
        project.files(arrayOf(File(project.sdkPath(),
                "platforms/${SupportConfig.COMPILE_SDK_VERSION}/android.jar")))

private fun androidSrcJarFile(project: Project): File = File(project.sdkPath(),
        "platforms/${SupportConfig.COMPILE_SDK_VERSION}/android-stubs-src.jar")

private fun BaseVariant.rFile() = "${applicationId.replace('.', '/')}/R.java"

// Nasty part. Get rid of that eventually!
fun Project.docsDir(): File = properties["docsDir"] as File

private fun Project.sdkPath(): File = getSdkPath(rootProject.projectDir)

fun Project.processProperty(name: String) =
        if (hasProperty(name)) {
            properties[name] as String
        } else {
            null
        }
