/*
 * Copyright 2017 The Android Open Source Project
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

package android.support

import android.support.checkapi.ApiXmlConversionTask
import android.support.checkapi.CheckApiTask
import android.support.checkapi.UpdateApiTask
import android.support.doclava.DoclavaTask
import android.support.docs.GenerateDocsTask
import android.support.jdiff.JDiffTask
import com.android.build.gradle.LibraryExtension
import com.android.build.gradle.api.LibraryVariant
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.artifacts.Configuration
import org.gradle.api.file.FileCollection
import org.gradle.api.plugins.JavaBasePlugin
import org.gradle.api.tasks.TaskContainer
import org.gradle.api.tasks.bundling.Zip
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.api.tasks.javadoc.Javadoc
import java.io.File

data class DacOptions(val libraryroot: String, val dataname: String)

object DiffAndDocs {
    @JvmStatic
    fun configureDiffAndDocs(
            root: Project,
            supportRootFolder: File,
            dacOptions: DacOptions) = configure(root, supportRootFolder, dacOptions)
}

private data class CheckApiConfig(
        val onFailMessage: String,
        val errors: List<Int>,
        val warnings: List<Int>,
        val hidden: List<Int>)

private const val MSG_HIDE_API =
        "If you are adding APIs that should be excluded from the public API surface,\n" +
                "consider using package or private visibility. If the API must have public\n" +
                "visibility, you may exclude it from public API by using the @hide javadoc\n" +
                "annotation paired with the @RestrictTo(LIBRARY_GROUP) code annotation."

@Suppress("DEPRECATION")
private fun hasJavaSources(variant: LibraryVariant) = !variant.javaCompile.source
        .filter { file -> file.name != "R.java" && file.name != "BuildConfig.java" }
        .isEmpty

private val CHECK_API_CONFIG_RELEASE = CheckApiConfig(
        onFailMessage =
        "Compatibility with previously released public APIs has been broken. Please\n" +
                "verify your change with Support API Council and provide error output,\n" +
                "including the error messages and associated SHAs.\n" +
                "\n" +
                "If you are removing APIs, they must be deprecated first before being removed\n" +
                "in a subsequent release.\n" +
                "\n" + MSG_HIDE_API,
        errors = (7..18).toList(),
        warnings = emptyList(),
        hidden = (2..6) + (19..30)
)

// Check that the API we're building hasn't changed from the development
// version. These types of changes require an explicit API file update.
private val CHECK_API_CONFIG_DEVELOP = CheckApiConfig(
        onFailMessage =
        "Public API definition has changed. Please run ./gradlew updateApi to confirm\n" +
                "these changes are intentional by updating the public API definition.\n" +
                "\n" + MSG_HIDE_API,
        errors = (2..30) - listOf(22),
        warnings = emptyList(),
        hidden = listOf(22)
)

// This is a patch or finalized release. Check that the API we're building
// hasn't changed from the current.
private val CHECK_API_CONFIG_PATCH = CHECK_API_CONFIG_DEVELOP.copy(
        onFailMessage = "Public API definition may not change in finalized or patch releases.\n" +
                "\n" + MSG_HIDE_API)

private fun hasApiFolder(project: Project) = File(project.projectDir, "api").exists()

private fun stripExtension(fileName: String) = fileName.substringBeforeLast('.')

private fun getLastReleasedApiFile(rootFolder: File, refVersion: Version): File? {
    val apiDir = File(rootFolder, "api")

    var lastFile: File? = null
    var lastVersion: Version? = null
    apiDir.listFiles().forEach { file ->
        Version.from(file)?.let { version ->
            if ((lastFile == null || lastVersion!! < version) && version < refVersion) {
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

    if (!refVersion.isSnapshot || forceRelease) {
        // Release API file is always X.Y.0.txt.
        return File(apiDir, "${refVersion.major}.${refVersion.minor}.0.txt")
    }

    // Non-release API file is always current.txt.
    return File(apiDir, "current.txt")
}

private fun createVerifyUpdateApiAllowedTask(project: Project) =
        project.tasks.createWithConfig("verifyUpdateApiAllowed") {
            // This could be moved to doFirst inside updateApi, but using it as a
            // dependency with no inputs forces it to run even when updateApi is a
            // no-op.
            doLast {
                val rootFolder = project.projectDir
                val version = Version(project.version as String)

                if (version.isPatch) {
                    throw GradleException("Public APIs may not be modified in patch releases.")
                } else if (version.isSnapshot && getApiFile(rootFolder,
                        version,
                        true).exists()) {
                    throw GradleException("Inconsistent version. Public API file already exists.")
                } else if (!version.isSnapshot && getApiFile(rootFolder, version).exists()
                        && !project.hasProperty("force")) {
                    throw GradleException("Public APIs may not be modified in finalized releases.")
                }
            }
        }

// Generates API files
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

private fun createCheckApiTask(
        project: Project,
        taskName: String,
        docletpath: Collection<File>,
        checkApiConfig: CheckApiConfig,
        oldApi: File?,
        newApi: File,
        whitelist: File? = null) =
        project.tasks.createWithConfig(taskName, CheckApiTask::class.java) {
            doclavaClasspath = docletpath
            onFailMessage = checkApiConfig.onFailMessage
            checkApiErrors = checkApiConfig.errors
            checkApiWarnings = checkApiConfig.warnings
            checkApiHidden = checkApiConfig.hidden
            newApiFile = newApi
            oldApiFile = oldApi
            whitelistErrorsFile = whitelist
            doFirst {
                logger.lifecycle("Verifying ${newApi.name} " +
                        "against ${oldApi?.name ?: "nothing"}...")
            }
        }

// configuration file for setting up api diffs and api docs
private fun registerJavaProjectForDocsTask(task: Javadoc, javaCompileTask: JavaCompile) {
    task.dependsOn(javaCompileTask)
    task.source(javaCompileTask.source)
    val project = task.project
    task.classpath += project.files(javaCompileTask.classpath) +
            project.files(javaCompileTask.destinationDir)
}

// configuration file for setting up api diffs and api docs
private fun registerAndroidProjectForDocsTask(task: Javadoc, releaseVariant: LibraryVariant) {
    @Suppress("DEPRECATION")
    task.dependsOn(releaseVariant.javaCompile)
    val packageDir = releaseVariant.applicationId.replace('.', '/')
    @Suppress("DEPRECATION")
    val sources = releaseVariant.javaCompile.source.filter { file ->
        file.name != "R.java" || file.parent.endsWith(packageDir)
    }
    task.source(sources)
    @Suppress("DEPRECATION")
    task.classpath += releaseVariant.getCompileClasspath(null) +
            task.project.files(releaseVariant.javaCompile.destinationDir)
}

private fun createUpdateApiTask(project: Project, checkApiRelease: CheckApiTask) =
        project.tasks.createWithConfig("updateApi", UpdateApiTask::class.java) {
            group = JavaBasePlugin.VERIFICATION_GROUP
            description = "Updates the candidate API file to incorporate valid changes."
            newApiFile = checkApiRelease.newApiFile
            oldApiFile = getApiFile(project.projectDir, project.version())
            whitelistErrors = checkApiRelease.whitelistErrors
            whitelistErrorsFile = checkApiRelease.whitelistErrorsFile

            doFirst {
                // Replace the expected whitelist with the detected whitelist.
                whitelistErrors = checkApiRelease.detectedWhitelistErrors
            }
        }

/**
 * Converts the <code>fromApi</code>.txt file (or the most recently released
 * X.Y.Z.txt if not explicitly defined using -PfromAPi=<file>) to XML format
 * for use by JDiff.
 */
private fun createOldApiXml(project: Project, doclavaConfig: Configuration) =
        project.tasks.createWithConfig("oldApiXml", ApiXmlConversionTask::class.java) {
            val toApi = project.processProperty("toApi")?.let(Version::from)
            val fromApi = project.processProperty("fromApi")
            classpath = project.files(doclavaConfig.resolve())
            val rootFolder = project.projectDir
            if (fromApi != null) {
                // Use an explicit API file.
                inputApiFile = File(rootFolder, "api/$fromApi.txt")
            } else if (toApi != null) {
                // If toApi matches released API (X.Y.Z) format, use the most recently
                // released API file prior to toApi.
                inputApiFile = getLastReleasedApiFile(rootFolder, toApi)
            } else {
                // Use the most recently released API file.
                inputApiFile = getApiFile(rootFolder, project.version())
            }

            outputApiXmlFile = File(project.docsDir(),
                    "release/${stripExtension(inputApiFile.name)}.xml")

            dependsOn(doclavaConfig)
        }

/**
 * Converts the <code>toApi</code>.txt file (or current.txt if not explicitly
 * defined using -PtoApi=<file>) to XML format for use by JDiff.
 */
private fun createNewApiXmlTask(
        project: Project,
        generateApi: DoclavaTask,
        doclavaConfig: Configuration) =
        project.tasks.createWithConfig("newApiXml", ApiXmlConversionTask::class.java) {
            classpath = project.files(doclavaConfig.resolve())
            val toApi = project.processProperty("toApi")

            if (toApi != null) {
                // Use an explicit API file.
                inputApiFile = File(project.projectDir, "api/$toApi.txt")
            } else {
                // Use the current API file (e.g. current.txt).
                inputApiFile = generateApi.apiFile
                dependsOn(generateApi, doclavaConfig)
            }

            outputApiXmlFile = File(project.docsDir(),
                    "release/${stripExtension(inputApiFile.name)}.xml")
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
        jdiffConfig: Configuration) =
        project.tasks.createWithConfig("generateDiffs", JDiffTask::class.java) {
            // Base classpath is Android SDK, sub-projects add their own.
            classpath = androidJarFile(project)

            // JDiff properties.
            oldApiXmlFile = oldApiTask.outputApiXmlFile
            newApiXmlFile = newApiTask.outputApiXmlFile

            val newApi = newApiXmlFile.name.substringBeforeLast('.')
            val docsDir = project.rootProject.docsDir()

            newJavadocPrefix = "../../../../../reference/"
            destinationDir = File(docsDir, "online/sdk/support_api_diff/${project.name}/$newApi")

            // Javadoc properties.
            docletpath = jdiffConfig.resolve()
            title = "Support&nbsp;Library&nbsp;API&nbsp;Differences&nbsp;Report"

            exclude("**/BuildConfig.java", "**/R.java")
            dependsOn(oldApiTask, newApiTask, jdiffConfig)
        }

// Generates a distribution artifact for online docs.
private fun createDistDocsTask(project: Project, generateDocs: DoclavaTask) =
        project.tasks.createWithConfig("distDocs", Zip::class.java) {
            dependsOn(generateDocs)
            group = JavaBasePlugin.DOCUMENTATION_GROUP
            description = "Generates distribution artifact for d.android.com-style documentation."
            from(generateDocs.destinationDir)
            baseName = "android-support-docs"
            version = project.buildNumber()

            doLast {
                logger.lifecycle("'Wrote API reference to $archivePath")
            }
        }

// Set up platform API files for federation.
private fun createGenerateSdkApiTask(project: Project, doclavaConfig: Configuration): Task =
        project.tasks.createWithConfig("generateSdkApi", DoclavaTask::class.java) {
            dependsOn(doclavaConfig)
            description = "Generates API files for the current SDK."
            setDocletpath(doclavaConfig.resolve())
            destinationDir = project.docsDir()
            classpath = androidJarFile(project)
            source(project.zipTree(androidSrcJarFile(project)))
            apiFile = sdkApiFile(project)
            removedApiFile = removedSdkApiFile(project)
            generateDocs = false
            coreJavadocOptions {
                addStringOption("stubpackages", "android.*")
            }
        }

private fun createGenerateDocsTask(
        project: Project,
        generateSdkApiTask: Task,
        doclavaConfig: Configuration,
        supportRootFolder: File,
        dacOptions: DacOptions) =
        project.tasks.createWithConfig("generateDocs", GenerateDocsTask::class.java) {
            dependsOn(generateSdkApiTask, doclavaConfig)
            group = JavaBasePlugin.DOCUMENTATION_GROUP
            description = "Generates d.android.com-style documentation. To generate offline docs " +
                    "use \'-PofflineDocs=true\' parameter."

            setDocletpath(doclavaConfig.resolve())
            val offline = project.processProperty("offlineDocs") != null
            destinationDir = File(project.docsDir(), if (offline) "offline" else "online")
            classpath = androidJarFile(project)
            val hidden = listOf<Int>(105, 106, 107, 111, 112, 113, 115, 116, 121)
            doclavaErrors = ((101..122) - hidden).toSet()
            doclavaWarnings = emptySet()
            doclavaHidden += hidden

            addSinceFilesFrom(supportRootFolder)

            coreJavadocOptions {
                addStringOption("templatedir",
                        "$supportRootFolder/../../external/doclava/res/assets/templates-sdk")
                addStringOption("samplesdir", "$supportRootFolder/samples")
                addMultilineMultiValueOption("federate").value = listOf(
                        listOf("Android", "https://developer.android.com")
                )
                addMultilineMultiValueOption("federationapi").value = listOf(
                        listOf("Android", sdkApiFile(project).absolutePath)
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
                    addStringOption("dac_libraryroot", dacOptions.libraryroot)
                    addStringOption("dac_dataname", dacOptions.dataname)
                }

                exclude("**/BuildConfig.java")
            }

            addArtifactsAndSince()
        }

private data class Tasks(
        val generateApi: DoclavaTask,
        val generateDiffs: JDiffTask,
        val checkApiTask: CheckApiTask)

private fun initializeApiChecksForProject(project: Project, generateDocs: GenerateDocsTask): Tasks {
    if (!project.hasProperty("docsDir")) {
        project.extensions.add("docsDir", File(project.rootProject.docsDir(), project.name))
    }
    val artifact = "${project.group}:${project.name}:${project.version}"
    val version = project.version()
    val workingDir = project.projectDir

    val doclavaConfiguration = project.rootProject.configurations.getByName("doclava")
    val docletClasspath = doclavaConfiguration.resolve()
    val generateApi = createGenerateApiTask(project, docletClasspath)
    generateApi.dependsOn(doclavaConfiguration)
    val verifyUpdateTask = createVerifyUpdateApiAllowedTask(project)

    // Make sure the API surface has not broken since the last release.
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
    val verifyConfig = if (version.isPatch) CHECK_API_CONFIG_PATCH else CHECK_API_CONFIG_DEVELOP
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
    updateApiTask.dependsOn(checkApiRelease, verifyUpdateTask)
    val newApiTask = createNewApiXmlTask(project, generateApi, doclavaConfiguration)
    val oldApiTask = createOldApiXml(project, doclavaConfiguration)

    val jdiffConfiguration = project.rootProject.configurations.getByName("jdiff")
    val generateDiffTask = createGenerateDiffsTask(project,
            oldApiTask,
            newApiTask,
            jdiffConfiguration)
    generateDiffTask.dependsOn(generateDocs)

    // Track API change history.
    generateDocs.addSinceFilesFrom(project.projectDir)

    // Associate current API surface with the Maven artifact.
    generateDocs.addArtifact(generateApi.apiFile!!.absolutePath, artifact)
    generateDocs.dependsOn(generateApi)
    return Tasks(generateApi, generateDiffTask, checkApi)
}

private fun configure(root: Project, supportRootFolder: File, dacOptions: DacOptions): Task {
    val allChecks = root.tasks.create("AnchorCheckApis")
    val doclavaConfiguration = root.configurations.getByName("doclava")
    val generateSdkApiTask = createGenerateSdkApiTask(root, doclavaConfiguration)

    val generateDocsTask = createGenerateDocsTask(root, generateSdkApiTask,
            doclavaConfiguration, supportRootFolder, dacOptions)
    createDistDocsTask(root, generateDocsTask)

    root.subprojects { subProject ->
        subProject.afterEvaluate { project ->
            val extension = if (project.hasProperty("supportLibrary")) {
                project.properties["supportLibrary"] as SupportLibraryExtension
            } else {
                null
            }
            if (extension == null || !extension.publish) {
                project.logger.info("Project ${project.name} is not published, ignoring API tasks.")
                return@afterEvaluate
            }

            if (!extension.generateDocs) {
                project.logger.info("Project ${project.name} specified generateDocs = false, " +
                        "ignoring API tasks.")
                return@afterEvaluate
            }

            val library = project.extensions.findByType(LibraryExtension::class.java)
            if (library != null) {
                library.libraryVariants.all { variant ->
                    if (variant.name == "release") {
                        registerAndroidProjectForDocsTask(generateDocsTask, variant)
                        if (!hasJavaSources(variant)) {
                            return@all
                        }
                        if (!hasApiFolder(project)) {
                            project.logger.info("Project ${project.name} doesn't have " +
                                    "an api folder, ignoring API tasks.")
                            return@all
                        }
                        val tasks = initializeApiChecksForProject(project, generateDocsTask)
                        registerAndroidProjectForDocsTask(tasks.generateApi, variant)
                        registerAndroidProjectForDocsTask(tasks.generateDiffs, variant)
                        allChecks.dependsOn(tasks.checkApiTask)
                    }
                }
            } else if (project.hasProperty("compileJava")) {
                val compileJava = project.properties["compileJava"] as JavaCompile
                registerJavaProjectForDocsTask(generateDocsTask, compileJava)
                if (!hasApiFolder(project)) {
                    project.logger.info("Project ${project.name} doesn't have an api folder, " +
                            "ignoring API tasks.")
                    return@afterEvaluate
                }
                project.afterEvaluate { proj ->
                    val tasks = initializeApiChecksForProject(proj, generateDocsTask)
                    registerJavaProjectForDocsTask(tasks.generateApi, compileJava)
                    registerJavaProjectForDocsTask(tasks.generateDiffs, compileJava)
                    allChecks.dependsOn(tasks.checkApiTask)
                }
            }
        }
    }
    return allChecks
}

private fun sdkApiFile(project: Project) = File(project.docsDir(), "release/sdk_current.txt")
private fun removedSdkApiFile(project: Project) = File(project.docsDir(), "release/sdk_removed.txt")

private fun TaskContainer.createWithConfig(name: String, config: Task.() -> Unit) =
        create(name) { task -> task.config() }

private fun <T : Task> TaskContainer.createWithConfig(
        name: String, taskClass: Class<T>,
        config: T.() -> Unit) =
        create(name, taskClass) { task -> task.config() }

private fun androidJarFile(project: Project): FileCollection =
        project.files(arrayOf(File(project.fullSdkPath(),
                "platforms/android-${SupportConfig.CURRENT_SDK_VERSION}/android.jar")))

private fun androidSrcJarFile(project: Project): File = File(project.fullSdkPath(),
        "platforms/android-${SupportConfig.CURRENT_SDK_VERSION}/android-stubs-src.jar")

// Nasty part. Get rid of that eventually!
private fun Project.docsDir(): File = properties["docsDir"] as File

private fun Project.fullSdkPath(): File = rootProject.properties["fullSdkPath"] as File

private fun Project.version() = Version(project.version as String)

private fun Project.buildNumber() = properties["buildNumber"] as String

private fun Project.processProperty(name: String) =
        if (hasProperty(name)) {
            properties[name] as String
        } else {
            null
        }
