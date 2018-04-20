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
import androidx.build.checkapi.CheckApiTask
import androidx.build.checkapi.UpdateApiTask
import androidx.build.doclava.DoclavaTask
import androidx.build.doclava.DEFAULT_DOCLAVA_CONFIG
import androidx.build.doclava.CHECK_API_CONFIG_DEVELOP
import androidx.build.doclava.CHECK_API_CONFIG_RELEASE
import androidx.build.doclava.CHECK_API_CONFIG_PATCH
import androidx.build.doclava.ChecksConfig
import androidx.build.docs.ConcatenateFilesTask
import androidx.build.docs.GenerateDocsTask
import androidx.build.jdiff.JDiffTask
import com.android.build.gradle.AppExtension
import com.android.build.gradle.LibraryExtension
import com.android.build.gradle.api.BaseVariant
import com.android.build.gradle.api.LibraryVariant
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
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.collections.Collection
import kotlin.collections.List
import kotlin.collections.MutableMap
import kotlin.collections.emptyList
import kotlin.collections.filter
import kotlin.collections.find
import kotlin.collections.forEach
import kotlin.collections.listOf
import kotlin.collections.mapNotNull
import kotlin.collections.minus
import kotlin.collections.mutableMapOf
import kotlin.collections.plus
import kotlin.collections.set
import kotlin.collections.toSet

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
        rules = additionalRules + TIP_OF_TREE
        docsProject = root.findProject(":docs-fake")
        anchorTask = root.tasks.create("anchorDocsTask")
        val doclavaConfiguration = root.configurations.getByName("doclava")
        val generateSdkApiTask = createGenerateSdkApiTask(root, doclavaConfiguration)
        val now = LocalDateTime.now()
        // The diff output assumes that each library is of the same version, but our libraries may each be of different versions
        // So, we display the date as the new version
        val newVersion = now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
        rules.forEach {
            val task = createGenerateDocsTask(
                    project = root, generateSdkApiTask = generateSdkApiTask,
                    doclavaConfig = doclavaConfiguration,
                    supportRootFolder = supportRootFolder, dacOptions = dacOptions,
                    destDir = File(root.docsDir(), it.name),
                    taskName = "${it.name}DocsTask")
            docsTasks[it.name] = task
            anchorTask.dependsOn(createDistDocsTask(root, task, it.name))
        }

        root.tasks.create("generateDocs").dependsOn(docsTasks[TIP_OF_TREE.name])

        val docletClasspath = doclavaConfiguration.resolve()

        aggregateOldApiTxtsTask = root.tasks.create("aggregateOldApiTxts", ConcatenateFilesTask::class.java)
        aggregateOldApiTxtsTask.Output = File(root.docsDir(), "previous.txt")

        val oldApisTask = root.tasks.createWithConfig("oldApisXml", ApiXmlConversionTask::class.java) {
            classpath = root.files(docletClasspath)
            dependsOn(doclavaConfiguration)

            inputApiFile = aggregateOldApiTxtsTask.Output
            dependsOn(aggregateOldApiTxtsTask)

            outputApiXmlFile = File(root.docsDir(), "previous.xml")
        }

        aggregateNewApiTxtsTask = root.tasks.create("aggregateNewApiTxts", ConcatenateFilesTask::class.java)
        aggregateNewApiTxtsTask.Output = File(root.docsDir(), "$newVersion")

        val newApisTask = root.tasks.createWithConfig("newApisXml", ApiXmlConversionTask::class.java) {
            classpath = root.files(docletClasspath)

            inputApiFile = aggregateNewApiTxtsTask.Output
            dependsOn(aggregateNewApiTxtsTask)

            outputApiXmlFile = File(root.docsDir(), "$newVersion.xml")
        }

        val jdiffConfiguration = root.configurations.getByName("jdiff")
        generateDiffsTask = createGenerateDiffsTask(root,
                oldApisTask,
                newApisTask,
                jdiffConfiguration)

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
            throw GradleException("Failed to find prebuilts for $mavenId. " +
                    "A matching rule $originRule in docsRules(\"$originName\") " +
                    "in PublishDocsRules.kt requires it. You should either add a prebuilt, " +
                    "or add overriding \"ignore\" or \"tipOfTree\" rules", e)
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

    private fun registerPrebuilts(extension: SupportLibraryExtension) =
            docsProject?.afterEvaluate { docs ->
        val depHandler = docs.dependencies
        val root = docs.rootProject
        rules.forEach { rule ->
            val resolvedRule = rule.resolve(extension)
            val strategy = resolvedRule.strategy
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
        rules.filter { rule -> rule.resolve(extension).strategy == TipOfTree }
                .mapNotNull { rule -> docsTasks[rule.name] }
                .forEach(setup)
    }

    /**
     * Registers a Java project to be included in docs generation, local API file generation, and
     * local API diff generation tasks.
     */
    fun registerJavaProject(project: Project, extension: SupportLibraryExtension) {
        if (!hasApiTasks(project, extension)) {
            return
        }
        val compileJava = project.properties["compileJava"] as JavaCompile

        registerPrebuilts(extension)

        tipOfTreeTasks(extension) { task ->
            registerJavaProjectForDocsTask(task, compileJava)
        }

        if (!project.hasApiFolder()) {
            project.logger.info("Project ${project.name} doesn't have an api folder, " +
                    "ignoring API tasks.")
            return
        }
        val tasks = initializeApiChecksForProject(project, aggregateOldApiTxtsTask, aggregateNewApiTxtsTask)
        registerJavaProjectForDocsTask(tasks.generateApi, compileJava)
        registerJavaProjectForDocsTask(generateDiffsTask, compileJava)
        setupDocsTasks(project, tasks)
        anchorTask.dependsOn(tasks.checkApiTask)
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
        if (!hasApiTasks(project, extension)) {
            return
        }

        registerPrebuilts(extension)

        library.libraryVariants.all { variant ->
            if (variant.name == "release") {
                // include R.file generated for prebuilts
                rules.filter { it.resolve(extension).strategy is Prebuilts }.forEach { rule ->
                    docsTasks[rule.name]?.include { fileTreeElement ->
                        fileTreeElement.path.endsWith(variant.rFile())
                    }
                }

                tipOfTreeTasks(extension) { task ->
                    registerAndroidProjectForDocsTask(task, variant)
                }

                if (!variant.hasJavaSources()) {
                    return@all
                }
                if (!project.hasApiFolder()) {
                    project.logger.info("Project ${project.name} doesn't have " +
                            "an api folder, ignoring API tasks.")
                    return@all
                }
                val tasks = initializeApiChecksForProject(project, aggregateOldApiTxtsTask, aggregateNewApiTxtsTask)
                registerAndroidProjectForDocsTask(tasks.generateApi, variant)
                registerAndroidProjectForDocsTask(generateDiffsTask, variant)
                setupDocsTasks(project, tasks)
                anchorTask.dependsOn(tasks.checkApiTask)
            }
        }
    }

    private fun setupDocsTasks(project: Project, tasks: Tasks) {
        docsTasks.values.forEach { docs ->
            generateDiffsTask.dependsOn(docs)
            // Track API change history.
            docs.addSinceFilesFrom(project.projectDir)
            // Associate current API surface with the Maven artifact.
            val artifact = "${project.group}:${project.name}:${project.version}"
            docs.addArtifact(tasks.generateApi.apiFile!!.absolutePath, artifact)
            docs.dependsOn(tasks.generateApi)
        }
    }
}

@Suppress("DEPRECATION")
private fun LibraryVariant.hasJavaSources() = !javaCompile.source
        .filter { file -> file.name != "R.java" && file.name != "BuildConfig.java" }
        .isEmpty

fun Project.hasApiFolder() = File(projectDir, "api").exists()

private fun stripExtension(fileName: String) = fileName.substringBeforeLast('.')

private fun getLastReleasedApiFile(rootFolder: File, refVersion: Version?): File? {
    val apiDir = File(rootFolder, "api")
    return getLastReleasedApiFileFromDir(apiDir, refVersion)
}

/**
 * Returns the api file with highest version among those having version less than refVersion
 */
private fun getLastReleasedApiFileFromDir(apiDir: File, refVersion: Version?): File? {
    var lastFile: File? = null
    var lastVersion: Version? = null
    apiDir.listFiles().forEach { file ->
        val parsed = Version.parseOrNull(file)
        parsed?.let { version ->
            if ((lastFile == null || lastVersion!! < version)
                    && (refVersion == null || version < refVersion)) {
                lastFile = file
                lastVersion = version
            }
        }
    }

    return lastFile
}

private fun getApiFile(rootDir: File, refVersion: Version): File {
    return getApiFile(rootDir, refVersion, false)
}

/**
 * Returns the API file for the specified reference version.
 *
 * @param refVersion the reference API version, ex. 25.0.0-SNAPSHOT
 * @return the most recently released API file
 */
private fun getApiFile(rootDir: File, refVersion: Version, forceRelease: Boolean = false): File {
    val apiDir = File(rootDir, "api")

    if (refVersion.isFinalApi() || forceRelease) {
        // Release API file is always X.Y.0.txt.
        return File(apiDir, "${refVersion.major}.${refVersion.minor}.0.txt")
    }

    // Non-release API file is always current.txt.
    return File(apiDir, "current.txt")
}


// Creates a new task on the project for generating API files
private fun createGenerateApiTask(project: Project, docletpathParam: Collection<File>) =
        project.tasks.createWithConfig("generateApi", DoclavaTask::class.java) {
            setDocletpath(docletpathParam)
            destinationDir = project.docsDir()
            // Base classpath is Android SDK, sub-projects add their own.
            classpath = androidJarFile(project)
            apiFile = File(project.docsDir(), "release/${project.name}/current.txt")
            generateDocs = false

            coreJavadocOptions {
                addBooleanOption("stubsourceonly", true)
            }

            exclude("**/BuildConfig.java")
            exclude("**/R.java")
        }

// Creates a new task on the project for verifying the API
private fun createCheckApiTask(
    project: Project,
    taskName: String,
    docletpath: Collection<File>,
    config: ChecksConfig,
    oldApi: File?,
    newApi: File,
    whitelist: File? = null
) =
        project.tasks.createWithConfig(taskName, CheckApiTask::class.java) {
            doclavaClasspath = docletpath
            checksConfig = config
            newApiFile = newApi
            oldApiFile = oldApi
            whitelistErrorsFile = whitelist
            doFirst {
                logger.lifecycle("Verifying ${newApi.name} " +
                        "against ${oldApi?.name ?: "nothing"}...")
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
        fileTreeElement.name != "R.java" || fileTreeElement.path.endsWith(releaseVariant.rFile()) }
    @Suppress("DEPRECATION")
    task.source(releaseVariant.javaCompile.source)
    @Suppress("DEPRECATION")
    task.classpath += releaseVariant.getCompileClasspath(null) +
            task.project.files(releaseVariant.javaCompile.destinationDir)
}

/**
 * Constructs a new task to copy a generated API file to an appropriately-named "official" API file
 * suitable for source control. This task should be called prior to source control check-in whenever
 * the public API has been modified.
 * <p>
 * The output API file varies according to version:
 * <ul>
 * <li>Snapshot and pre-release versions (e.g. X.Y.Z-SNAPSHOT, X.Y.Z-alphaN) output to current.txt
 * <li>Release versions (e.g. X.Y.Z) output to X.Y.0.txt, throwing an exception if the API has been
 *     finalized and the file already exists
 * </ul>
 */
private fun createUpdateApiTask(project: Project, checkApiRelease: CheckApiTask) =
        project.tasks.createWithConfig("updateApi", UpdateApiTask::class.java) {
            group = JavaBasePlugin.VERIFICATION_GROUP
            description = "Updates the candidate API file to incorporate valid changes."
            newApiFile = checkApiRelease.newApiFile
            oldApiFile = getApiFile(project.projectDir, project.version())
            whitelistErrors = checkApiRelease.whitelistErrors
            whitelistErrorsFile = checkApiRelease.whitelistErrorsFile
            doFirst {
                val version = project.version()
                if (!version.isFinalApi() &&
                        getApiFile(project.projectDir, version, true).exists()) {
                    throw GradleException("Inconsistent version. Public API file already exists.")
                }
                // Replace the expected whitelist with the detected whitelist.
                whitelistErrors = checkApiRelease.detectedWhitelistErrors
            }
        }

/**
 * Returns the filepath of the previous API txt file (for computing diffs against)
 */
private fun getOldApiTxt(project: Project): File? {
    val toApi = project.processProperty("toApi")?.let {
        Version.parseOrNull(it)
    }
    val fromApi = project.processProperty("fromApi")
    val rootFolder = project.projectDir
    if (fromApi != null) {
        // Use an explicit API file.
        return File(rootFolder, "api/$fromApi.txt")
    } else {
        // Use the most recently released API file bounded by toApi.
        return getLastReleasedApiFile(rootFolder, toApi)
    }
}


data class FileProvider(val file: File, val task: Task?)

private fun getNewApiTxt(project: Project, generateApi: DoclavaTask): FileProvider {
    val toApi = project.processProperty("toApi")
    if (toApi != null) {
        // Use an explicit API file.
        return FileProvider(File(project.projectDir, "api/$toApi.txt"), null)
    } else {
        // Use the current API file (e.g. current.txt).
        return FileProvider(generateApi.apiFile!!, generateApi)
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

            exclude("**/BuildConfig.java", "**/R.java")
            dependsOn(oldApiTask, newApiTask, jdiffConfig)
            doLast {
                project.logger.lifecycle("generated diffs into $destinationDir")
            }
        }

// Generates a distribution artifact for online docs.
private fun createDistDocsTask(project: Project, generateDocs: DoclavaTask, ruleName: String = ""): Zip =
        project.tasks.createWithConfig("dist${ruleName}Docs", Zip::class.java) {
            dependsOn(generateDocs)
            group = JavaBasePlugin.DOCUMENTATION_GROUP
            description = "Generates distribution artifact for d.android.com-style documentation."
            from(generateDocs.destinationDir)
            baseName = "android-support-$ruleName-docs"
            version = project.buildNumber()
            destinationDir = project.distDir()
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
    taskName: String = "generateDocs"
): GenerateDocsTask =
        project.tasks.createWithConfig(taskName, GenerateDocsTask::class.java) {
            dependsOn(generateSdkApiTask, doclavaConfig)
            group = JavaBasePlugin.DOCUMENTATION_GROUP
            description = "Generates d.android.com-style documentation. To generate offline docs " +
                    "use \'-PofflineDocs=true\' parameter."

            setDocletpath(doclavaConfig.resolve())
            val offline = project.processProperty("offlineDocs") != null
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

                exclude("**/BuildConfig.java")
            }

            addArtifactsAndSince()
        }

private data class Tasks(
    val generateApi: DoclavaTask,
    val checkApiTask: CheckApiTask
)

/**
 * Sets up api tasks for the given project
 */
private fun initializeApiChecksForProject(project: Project, aggregateOldApiTxtsTask: ConcatenateFilesTask, aggregateNewApiTxtsTask:ConcatenateFilesTask): Tasks {
    if (!project.hasProperty("docsDir")) {
        project.extensions.add("docsDir", File(project.rootProject.docsDir(), project.name))
    }
    val version = project.version()
    val workingDir = project.projectDir

    val doclavaConfiguration = project.rootProject.configurations.getByName("doclava")
    val docletClasspath = doclavaConfiguration.resolve()
    val generateApi = createGenerateApiTask(project, docletClasspath)
    generateApi.dependsOn(doclavaConfiguration)

    // for verifying that the API surface has not broken since the last release
    val lastReleasedApiFile = getLastReleasedApiFile(workingDir, version)

    val whitelistFile = lastReleasedApiFile?.let { apiFile ->
        File(lastReleasedApiFile.parentFile, stripExtension(apiFile.name) + ".ignore")
    }
    val checkApiRelease = createCheckApiTask(project,
            "checkApiRelease",
            docletClasspath,
            CHECK_API_CONFIG_RELEASE,
            lastReleasedApiFile,
            generateApi.apiFile!!,
            whitelistFile)
    checkApiRelease.dependsOn(generateApi)

    // Allow a comma-delimited list of whitelisted errors.
    if (project.hasProperty("ignore")) {
        checkApiRelease.whitelistErrors = (project.properties["ignore"] as String)
                .split(',').toSet()
    }

    // Check whether the development API surface has changed.
    val verifyConfig = if (version.isPatch()) CHECK_API_CONFIG_PATCH else CHECK_API_CONFIG_DEVELOP
    val currentApiFile = getApiFile(workingDir, version)
    val checkApi = createCheckApiTask(project,
            "checkApi",
            docletClasspath,
            verifyConfig,
            currentApiFile,
            generateApi.apiFile!!,
            null)
    checkApi.dependsOn(generateApi, checkApiRelease)

    checkApi.group = JavaBasePlugin.VERIFICATION_GROUP
    checkApi.description = "Verify the API surface."

    val updateApiTask = createUpdateApiTask(project, checkApiRelease)
    updateApiTask.dependsOn(checkApiRelease)


    val oldApiTxt = getOldApiTxt(project)
    if (oldApiTxt != null) {
        aggregateOldApiTxtsTask.addInput(project.name, oldApiTxt)
    }
    val newApiTxtProvider = getNewApiTxt(project, generateApi)
    aggregateNewApiTxtsTask.inputs.file(newApiTxtProvider.file)
    aggregateNewApiTxtsTask.addInput(project.name, newApiTxtProvider.file)
    if (newApiTxtProvider.task != null) {
        aggregateNewApiTxtsTask.dependsOn(newApiTxtProvider.task)
    }

    return Tasks(generateApi, checkApi)
}

fun hasApiTasks(project: Project, extension: SupportLibraryExtension): Boolean {
    if (!extension.publish) {
        project.logger.info("Project ${project.name} is not published, ignoring API tasks.")
        return false
    }

    if (!extension.generateDocs) {
        project.logger.info("Project ${project.name} specified generateDocs = false, " +
                "ignoring API tasks.")
        return false
    }
    return true
}

private fun sdkApiFile(project: Project) = File(project.docsDir(), "release/sdk_current.txt")

private fun <T : Task> TaskContainer.createWithConfig(
    name: String,
    taskClass: Class<T>,
    config: T.() -> Unit
) =
        create(name, taskClass) { task -> task.config() }

private fun androidJarFile(project: Project): FileCollection =
        project.files(arrayOf(File(project.fullSdkPath(),
                "platforms/android-${SupportConfig.CURRENT_SDK_VERSION}/android.jar")))

private fun androidSrcJarFile(project: Project): File = File(project.fullSdkPath(),
        "platforms/android-${SupportConfig.CURRENT_SDK_VERSION}/android-stubs-src.jar")

private fun PublishDocsRules.resolve(extension: SupportLibraryExtension) =
        resolve(extension.mavenGroup!!, extension.project.name)

private fun Prebuilts.dependency(extension: SupportLibraryExtension) =
        "${extension.mavenGroup}:${extension.project.name}:$version"

private fun BaseVariant.rFile() = "${applicationId.replace('.', '/')}/R.java"

// Nasty part. Get rid of that eventually!
private fun Project.docsDir(): File = properties["docsDir"] as File

private fun Project.fullSdkPath(): File = rootProject.properties["fullSdkPath"] as File

private fun Project.version() = Version(project.version as String)

private fun Project.buildNumber() = properties["buildNumber"] as String

private fun Project.distDir(): File = rootProject.properties["distDir"] as File

private fun Project.processProperty(name: String) =
        if (hasProperty(name)) {
            properties[name] as String
        } else {
            null
        }
