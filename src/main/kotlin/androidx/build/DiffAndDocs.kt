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
import androidx.build.checkapi.hasApiTasks
import androidx.build.checkapi.initializeApiChecksForProject
import androidx.build.doclava.ChecksConfig
import androidx.build.doclava.DEFAULT_DOCLAVA_CONFIG
import androidx.build.doclava.DoclavaTask
import androidx.build.docs.ConcatenateFilesTask
import androidx.build.docs.GenerateDocsTask
import androidx.build.jdiff.JDiffTask
import com.android.build.gradle.AppExtension
import com.android.build.gradle.LibraryExtension
import com.android.build.gradle.api.BaseVariant
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.ResolveException
import org.gradle.api.file.FileCollection
import org.gradle.api.file.FileTree
import org.gradle.api.plugins.JavaBasePlugin
import org.gradle.api.tasks.TaskContainer
import org.gradle.api.tasks.bundling.Zip
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.api.tasks.javadoc.Javadoc
import java.io.File
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

object DiffAndDocs {
    private lateinit var anchorTask: Task
    private var docsProject: Project? = null

    private lateinit var rules: List<PublishDocsRules>
    private val docsTasks: MutableMap<String, GenerateDocsTask> = mutableMapOf()
    private lateinit var aggregateOldApiTxtsTask: ConcatenateFilesTask
    private lateinit var aggregateNewApiTxtsTask: ConcatenateFilesTask
    private lateinit var generateDiffsTask: JDiffTask

    /**
     * Initialization that should happen only once (and on the root project)
     */
    @JvmStatic
    fun configureDiffAndDocs(
        root: Project,
        supportRootFolder: File,
        dacOptions: DacOptions,
        additionalRules: List<PublishDocsRules> = emptyList()
    ): Task {
        val doclavaConfiguration = root.configurations.create("doclava")
        doclavaConfiguration.dependencies.add(root.dependencies.create(DOCLAVA_DEPENDENCY))

        // tools.jar required for com.sun.javadoc
        // TODO this breaks the ability to use JDK 9+ for compilation.
        doclavaConfiguration.dependencies.add(root.dependencies.create(root.files(
                (ToolProvider.getSystemToolClassLoader() as URLClassLoader).urLs)))

        rules = additionalRules + TIP_OF_TREE
        docsProject = root.findProject(":docs-fake")
        anchorTask = root.tasks.create("anchorDocsTask")
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
            anchorTask.dependsOn(createDistDocsTask(root, task, it.name))
        }

        root.tasks.create("generateDocs").dependsOn(docsTasks[TIP_OF_TREE.name])

        val docletClasspath = doclavaConfiguration.resolve()

        aggregateOldApiTxtsTask = root.tasks.create("aggregateOldApiTxts",
                ConcatenateFilesTask::class.java)
        aggregateOldApiTxtsTask.Output = File(root.docsDir(), "previous.txt")

        val oldApisTask = root.tasks.createWithConfig("oldApisXml",
                ApiXmlConversionTask::class.java) {
            classpath = root.files(docletClasspath)
            dependsOn(doclavaConfiguration)

            inputApiFile = aggregateOldApiTxtsTask.Output
            dependsOn(aggregateOldApiTxtsTask)

            outputApiXmlFile = File(root.docsDir(), "previous.xml")
        }

        aggregateNewApiTxtsTask = root.tasks.create("aggregateNewApiTxts",
                ConcatenateFilesTask::class.java)
        aggregateNewApiTxtsTask.Output = File(root.docsDir(), newVersion)

        val newApisTask = root.tasks.createWithConfig("newApisXml",
                ApiXmlConversionTask::class.java) {
            classpath = root.files(docletClasspath)

            inputApiFile = aggregateNewApiTxtsTask.Output
            dependsOn(aggregateNewApiTxtsTask)

            outputApiXmlFile = File(root.docsDir(), "$newVersion.xml")
        }

        val jdiffConfiguration = root.configurations.create("jdiff")
        jdiffConfiguration.dependencies.add(root.dependencies.create(JDIFF_DEPENDENCY))
        jdiffConfiguration.dependencies.add(root.dependencies.create(XML_PARSER_APIS_DEPENDENCY))
        jdiffConfiguration.dependencies.add(root.dependencies.create(XERCES_IMPL_DEPENDENCY))

        generateDiffsTask = createGenerateDiffsTask(root,
                oldApisTask,
                newApisTask,
                jdiffConfiguration)

        docsTasks.values.forEach { docs -> generateDiffsTask.dependsOn(docs) }
        setupDocsProject()

        return anchorTask
    }

    private fun prebuiltSources(
        root: Project,
        mavenId: String,
        originName: String,
        originRule: DocsRule
    ): FileTree {
        val configName = "docs-temp_$mavenId"
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
                val task = docsTasks[v.flavorName]
                if (v.buildType.name == "release" && task != null) {
                    registerAndroidProjectForDocsTask(task, v)
                    task.exclude { fileTreeElement ->
                        fileTreeElement.path.endsWith(v.rFile())
                    }
                }
            }
        }

        docsProject?.rootProject?.subprojects
                ?.filter { docsProject != it }
                ?.forEach { docsProject?.evaluationDependsOn(it.path) }
    }

    fun registerPrebuilts(extension: SupportLibraryExtension) =
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
                docsTasks[rule.name]!!.source(prebuiltSources(root, dependency,
                        rule.name, resolvedRule))
            }
        }
    }

    private fun tipOfTreeTasks(extension: SupportLibraryExtension, setup: (DoclavaTask) -> Unit) {
        rules.filter { rule -> rule.resolve(extension)?.strategy == TipOfTree }
                .mapNotNull { rule -> docsTasks[rule.name] }
                .forEach(setup)
    }

    /**
     * Registers a Java project to be included in docs generation, local API file generation, and
     * local API diff generation tasks.
     */
    fun registerJavaProject(project: Project, extension: SupportLibraryExtension) {
        val compileJava = project.properties["compileJava"] as JavaCompile

        registerPrebuilts(extension)

        tipOfTreeTasks(extension) { task ->
            registerJavaProjectForDocsTask(task, compileJava)
        }

        registerJavaProjectForDocsTask(generateDiffsTask, compileJava)
        if (!hasApiTasks(project, extension)) {
            return
        }

        val tasks = initializeApiChecksForProject(project,
                aggregateOldApiTxtsTask, aggregateNewApiTxtsTask)
        registerJavaProjectForDocsTask(tasks.generateApi, compileJava)
        setupApiVersioningInDocsTasks(extension, tasks)
        addCheckApiTasksToGraph(tasks)
        registerJavaProjectForDocsTask(tasks.generateLocalDiffs, compileJava)
        val generateApiDiffsArchiveTask = createGenerateLocalApiDiffsArchiveTask(project,
                tasks.generateLocalDiffs)
        generateApiDiffsArchiveTask.dependsOn(tasks.generateLocalDiffs)
    }

    /**
     * Registers an Android project to be included in global docs generation, local API file
     * generation, and local API diff generation tasks.
     */
    fun registerAndroidProject(
        project: Project,
        library: LibraryExtension,
        extension: SupportLibraryExtension
    ) {

        registerPrebuilts(extension)
        library.libraryVariants.all { variant ->
            if (variant.name == Release.DEFAULT_PUBLISH_CONFIG) {
                // include R.file generated for prebuilts
                rules.filter { it.resolve(extension)?.strategy is Prebuilts }.forEach { rule ->
                    docsTasks[rule.name]?.include { fileTreeElement ->
                        fileTreeElement.path.endsWith(variant.rFile())
                    }
                }

                tipOfTreeTasks(extension) { task ->
                    registerAndroidProjectForDocsTask(task, variant)
                }

                if (!hasApiTasks(project, extension)) {
                    return@all
                }
                val tasks = initializeApiChecksForProject(project, aggregateOldApiTxtsTask,
                        aggregateNewApiTxtsTask)
                registerAndroidProjectForDocsTask(tasks.generateApi, variant)
                setupApiVersioningInDocsTasks(extension, tasks)
                addCheckApiTasksToGraph(tasks)
                registerAndroidProjectForDocsTask(tasks.generateLocalDiffs, variant)
                val generateApiDiffsArchiveTask = createGenerateLocalApiDiffsArchiveTask(project,
                        tasks.generateLocalDiffs)
                generateApiDiffsArchiveTask.dependsOn(tasks.generateLocalDiffs)
            }
        }
    }

    private fun setupApiVersioningInDocsTasks(
        extension: SupportLibraryExtension,
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
            val docs = docsTasks[rules.name]!!
            // Track API change history.
            docs.addSinceFilesFrom(project.projectDir)
            // Associate current API surface with the Maven artifact.
            val artifact = "${project.group}:${project.name}:$version"
            docs.addArtifact(checkApiTasks.generateApi.apiFile!!.absolutePath, artifact)
            docs.dependsOn(checkApiTasks.generateApi)
        }
    }

    private fun addCheckApiTasksToGraph(tasks: CheckApiTasks) {
        docsTasks.values.forEach { docs -> docs.dependsOn(tasks.generateApi) }
        anchorTask.dependsOn(tasks.checkApi)
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
private fun registerJavaProjectForDocsTask(task: Javadoc, javaCompileTask: JavaCompile) {
    task.dependsOn(javaCompileTask)
    task.source(javaCompileTask.source)
    val project = task.project
    task.classpath += project.files(javaCompileTask.classpath) +
            project.files(javaCompileTask.destinationDir)
}

/**
 * Registers an Android project on the given Javadocs task.
 * <p>
 * @see #registerJavaProjectForDocsTask
 */
private fun registerAndroidProjectForDocsTask(task: Javadoc, releaseVariant: BaseVariant) {
    // This code makes a number of unsafe assumptions about Android Gradle Plugin,
    // and there's a good chance that this will break in the near future.
    @Suppress("DEPRECATION")
    task.dependsOn(releaseVariant.javaCompile)
    task.include { fileTreeElement ->
        fileTreeElement.name != "R.java" || fileTreeElement.path.endsWith(releaseVariant.rFile())
    }
    @Suppress("DEPRECATION")
    task.source(releaseVariant.javaCompile.source)
    @Suppress("DEPRECATION")
    task.classpath += releaseVariant.getCompileClasspath(null) +
            task.project.files(releaseVariant.javaCompile.destinationDir)
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
    oldApiTask: ApiXmlConversionTask,
    newApiTask: ApiXmlConversionTask,
    jdiffConfig: Configuration
): JDiffTask =
        project.tasks.createWithConfig("generateDiffs", JDiffTask::class.java) {
            // Base classpath is Android SDK, sub-projects add their own.
            classpath = androidJarFile(project)

            // JDiff properties.
            oldApiXmlFile = oldApiTask.outputApiXmlFile
            newApiXmlFile = newApiTask.outputApiXmlFile

            val newApi = newApiXmlFile.name.substringBeforeLast('.')
            val docsDir = File(project.rootProject.docsDir(), "public")

            newJavadocPrefix = "../../../../../reference/"
            destinationDir = File(docsDir, "online/sdk/support_api_diff/${project.name}/$newApi")

            // Javadoc properties.
            docletpath = jdiffConfig.resolve()
            title = "Support&nbsp;Library&nbsp;API&nbsp;Differences&nbsp;Report"

            exclude("**/R.java")
            dependsOn(oldApiTask, newApiTask, jdiffConfig)
            doLast {
                project.logger.lifecycle("generated diffs into $destinationDir")
            }
        }

// Generates a distribution artifact for online docs.
private fun createDistDocsTask(
    project: Project,
    generateDocs: DoclavaTask,
    ruleName: String = ""
): Zip = project.tasks.createWithConfig("dist${ruleName}Docs", Zip::class.java) {
    dependsOn(generateDocs)
    group = JavaBasePlugin.DOCUMENTATION_GROUP
    description = "Generates distribution artifact for d.android.com-style documentation."
    from(generateDocs.destinationDir)
    baseName = "android-support-$ruleName-docs"
    version = getBuildId()
    destinationDir = project.getDistributionDirectory()
    doLast {
        logger.lifecycle("'Wrote API reference to $archivePath")
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
            source(project.zipTree(androidSrcJarFile(project)))
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
): GenerateDocsTask =
        project.tasks.createWithConfig(taskName, GenerateDocsTask::class.java) {
            dependsOn(generateSdkApiTask, doclavaConfig)
            group = JavaBasePlugin.DOCUMENTATION_GROUP
            description = "Generates d.android.com-style documentation. To generate offline docs " +
                    "use \'-PofflineDocs=true\' parameter."

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

private fun createGenerateLocalApiDiffsArchiveTask(
    project: Project,
    diffTask: JDiffTask
): Zip = project.tasks.createWithConfig("generateLocalApiDiffsArchive", Zip::class.java) {
    val docsDir = project.rootProject.docsDir()
    from(diffTask.destinationDir)
    destinationDir = File(docsDir, "online/sdk/support_api_diff/${project.name}")
    to("${project.version}.zip")
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
                "platforms/android-${SupportConfig.CURRENT_SDK_VERSION}/android.jar")))

private fun androidSrcJarFile(project: Project): File = File(project.sdkPath(),
        "platforms/android-${SupportConfig.CURRENT_SDK_VERSION}/android-stubs-src.jar")

private fun PublishDocsRules.resolve(extension: SupportLibraryExtension): DocsRule? {
    val mavenGroup = extension.mavenGroup
    return if (mavenGroup == null) null else resolve(mavenGroup, extension.project.name)
}

private fun Prebuilts.dependency(extension: SupportLibraryExtension) =
        "${extension.mavenGroup}:${extension.project.name}:$version"

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
