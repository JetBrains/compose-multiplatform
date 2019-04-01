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

package androidx.build.checkapi

import androidx.build.AndroidXExtension
import androidx.build.Version
import androidx.build.androidJarFile
import androidx.build.doclava.CHECK_API_CONFIG_DEVELOP
import androidx.build.doclava.CHECK_API_CONFIG_PATCH
import androidx.build.doclava.CHECK_API_CONFIG_RELEASE
import androidx.build.doclava.ChecksConfig
import androidx.build.doclava.DoclavaTask
import androidx.build.docs.ConcatenateFilesTask
import androidx.build.docsDir
import androidx.build.jdiff.JDiffTask
import androidx.build.processProperty
import androidx.build.version
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.plugins.JavaBasePlugin
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.TaskProvider
import java.io.File

data class CheckApiTasks(
    val generateApi: TaskProvider<DoclavaTask>,
    val checkApi: TaskProvider<CheckApiTask>,
    val generateLocalDiffs: TaskProvider<JDiffTask>
)

enum class ApiType {
    CLASSAPI,
    RESOURCEAPI
}

/**
 * Sets up api tasks for the given project
 */
fun initializeApiChecksForProject(
    project: Project,
    aggregateOldApiTxtsTask: TaskProvider<ConcatenateFilesTask>,
    aggregateNewApiTxtsTask: TaskProvider<ConcatenateFilesTask>
): CheckApiTasks {
    if (!project.hasProperty("docsDir")) {
        project.extensions.add("docsDir", File(project.rootProject.docsDir(), project.name))
    }
    val version = project.version()

    val doclavaConfiguration = project.rootProject.configurations.getByName("doclava")
    val docletClasspath = doclavaConfiguration.resolve()
    val generateApi = createGenerateApiTask(project, docletClasspath)
    generateApi.configure {
        it.dependsOn(doclavaConfiguration)
    }

    // for verifying that the API surface has not broken since the last minor release
    val lastReleasedApiFile = project.getRequiredCompatibilityApiFile()

    val whitelistFile = lastReleasedApiFile?.let { apiFile ->
        File(lastReleasedApiFile.parentFile, stripExtension(apiFile.name) + ".ignore")
    }
    val checkApiRelease = createCheckApiTask(project,
            "checkApiRelease",
            docletClasspath,
            CHECK_API_CONFIG_RELEASE,
            lastReleasedApiFile,
            generateApi.map {
                it.apiFile!!
            },
            whitelistFile)
    checkApiRelease.configure {
        it.dependsOn(generateApi)
    }

    // Allow a comma-delimited list of whitelisted errors.
    if (project.hasProperty("ignore")) {
        checkApiRelease.configure {
            it.whitelistErrors = (project.properties["ignore"] as String)
                    .split(',').toSet()
        }
    }

    // Check whether the development API surface has changed.
    val verifyConfig = if (version.isPatch()) CHECK_API_CONFIG_PATCH else CHECK_API_CONFIG_DEVELOP
    val currentApiFile = project.getCurrentApiFile()
    val checkApi = createCheckApiTask(project,
            "checkApi",
            docletClasspath,
            verifyConfig,
            currentApiFile,
            generateApi.map {
                it.apiFile!!
            },
            null)
    checkApi.configure {
        it.dependsOn(generateApi, checkApiRelease)
        it.group = JavaBasePlugin.VERIFICATION_GROUP
        it.description = "Verify the API surface."
    }

    val updateApiTask = createUpdateApiTask(project, checkApiRelease)
    updateApiTask.configure {
        it.dependsOn(checkApiRelease)
    }

    val oldApiTxt = getOldApiTxtForDocDiffs(project)
    if (oldApiTxt != null) {
        aggregateOldApiTxtsTask.configure {
            it.addInput(project.name, oldApiTxt)
        }
    }
    val newApiTxtProvider = getNewApiTxt(project, generateApi)
    aggregateNewApiTxtsTask.configure {
        it.inputs.file(newApiTxtProvider.file)
        it.addInput(project.name, newApiTxtProvider.file.get())
    }

    val newApiTask = createNewApiXmlTask(project, generateApi, doclavaConfiguration)
    val oldApiTask = createOldApiXml(project, doclavaConfiguration)

    val jdiffConfiguration = project.rootProject.configurations.getByName("jdiff")

    val generatelocalDiffsTask = createGenerateLocalDiffsTask(project,
            oldApiTask,
            newApiTask,
            jdiffConfiguration)

    return CheckApiTasks(generateApi, checkApi, generatelocalDiffsTask)
}

private fun createGenerateLocalDiffsTask(
    project: Project,
    oldApiTask: TaskProvider<ApiXmlConversionTask>,
    newApiTask: TaskProvider<ApiXmlConversionTask>,
    jdiffConfig: Configuration
): TaskProvider<JDiffTask> =
        project.tasks.register("generateLocalDiffs", JDiffTask::class.java) {
            it.apply {
                // Base classpath is Android SDK, sub-projects add their own.
                classpath = androidJarFile(project)

                // JDiff properties.
                oldApiXmlFile = oldApiTask.get().outputApiXmlFile
                newApiXmlFile = newApiTask.get().outputApiXmlFile

                val newApi = project.processProperty("toApi") ?: project.version
                val docsDir = project.rootProject.docsDir()

                newJavadocPrefix = "../../../../../reference/"
                destinationDir = File(docsDir,
                        "online/sdk/support_api_diff/${project.name}/$newApi")
                // Javadoc properties.
                docletpath = jdiffConfig.resolve()
                title = "AndroidX&nbsp;Library&nbsp;API&nbsp;Differences&nbsp;Report"

                exclude("**/BuildConfig.java", "**/R.java")
                dependsOn(oldApiTask, newApiTask, jdiffConfig)
                doFirst {
                    println("Generating diffs from api version " +
                            "${stripExtension(oldApiTask.get().outputApiXmlFile.name)} " +
                            "to api version $newApi")
                }
            }
        }

/**
 * Converts the <code>fromApi</code>.txt file (or the most recently released
 * X.Y.Z.txt if not explicitly defined using -PfromAPi=<file>) to XML format
 * for use by JDiff.
 */
private fun createOldApiXml(project: Project, doclavaConfig: Configuration) =
        project.tasks.register("oldApiXml", ApiXmlConversionTask::class.java) {
            it.apply {
                val fromApi = project.processProperty("fromApi")
                classpath = project.files(doclavaConfig.resolve())
                val rootFolder = project.projectDir
                inputApiFile = if (fromApi != null) {
                    // Use an explicit API file.
                    File(rootFolder, "api/$fromApi.txt")
                } else {
                    // Use the most recently released API file.
                    getLastReleasedApiFile(rootFolder, Version(project.processProperty("toApi")
                        ?: project.version.toString()), false, false)
                }

                outputApiXmlFile = File(project.docsDir(),
                    "release/${stripExtension(inputApiFile?.name ?: "creation")}.xml")

                dependsOn(doclavaConfig)
            }
        }

/**
 * Converts the <code>toApi</code>.txt file (or current.txt if not explicitly
 * defined using -PtoApi=<file>) to XML format for use by JDiff.
 */
private fun createNewApiXmlTask(
    project: Project,
    generateApi: TaskProvider<DoclavaTask>,
    doclavaConfig: Configuration
) =
        project.tasks.register("newApiXml", ApiXmlConversionTask::class.java) {
            it.apply {
                classpath = project.files(doclavaConfig.resolve())
                val toApi = project.processProperty("toApi")

                if (toApi != null && toApi != project.version) {
                    // Use an explicit API file.
                    inputApiFile = File(project.projectDir, "api/$toApi.txt")
                } else {
                    // Use the current API file (e.g. current.txt).
                    inputApiFile = generateApi.get().apiFile!!
                    dependsOn(generateApi, doclavaConfig)
                }
                // Use either the toApi version, otherwise the most recent version.
                outputApiXmlFile = File(project.docsDir(),
                    "release/${toApi ?: project.version}.xml")
            }
        }

fun Project.hasApiFolder() = File(projectDir, "api").exists()

fun hasApiTasks(project: Project, extension: AndroidXExtension): Boolean {
    if (extension.toolingProject) {
        project.logger.info("Project ${project.name} is tooling project ignoring API tasks.")
        return false
    }

    if (project.hasApiFolder()) {
        return true
    }

    if (!extension.publish) {
        project.logger.info("Project ${project.name} is not published, ignoring API tasks." +
                "If you still want to trackApi, simply create \"api\" folder in your project path")
        return false
    }

    if (extension.publish && project.version().isFinalApi()) {
        throw GradleException("Project ${project.name} must track API before stabilizing API\n." +
                "To do that create \"api\" in your project directory and " +
                "run \"./gradlew updateApi\" command")
    }
    return false
}

// Creates a new task on the project for generating API files
private fun createGenerateApiTask(project: Project, docletpathParam: Collection<File>) =
        project.tasks.register("generateApi", DoclavaTask::class.java) {
            it.apply {
                // Base classpath is Android SDK, sub-projects add their own.
                classpath = androidJarFile(project)
                apiFile = File(project.docsDir(), "release/${project.name}/current.txt")
                setDocletpath(docletpathParam)
                destinationDir = project.docsDir()
                generateDocs = false

                coreJavadocOptions {
                    addBooleanOption("stubsourceonly", true)
                }

                exclude("**/R.java")
            }
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
private fun createUpdateApiTask(project: Project, checkApiRelease: TaskProvider<CheckApiTask>) =
        project.tasks.register("updateApi", UpdateApiTask::class.java) {
            it.apply {
                group = JavaBasePlugin.VERIFICATION_GROUP
                description = "Updates the candidate API file to incorporate valid changes."
                newApiFile = checkApiRelease.get().newApiFile
                oldApiFile = project.getCurrentApiFile()
                whitelistErrors = checkApiRelease.get().whitelistErrors
                whitelistErrorsFile = checkApiRelease.get().whitelistErrorsFile
                doFirst {
                    // Replace the expected whitelist with the detected whitelist.
                    whitelistErrors = checkApiRelease.get().detectedWhitelistErrors
                }
            }
        }

/**
 * Returns the API file whose contents match the project's source code.
 * This is the API file that the updateApi task will write to.
 * Note that in many cases the filename will be current.txt but not always (such as for release versions).
 *
 * @param project the project to query
 * @return the current api file for that project
 */
fun Project.getCurrentApiFile() = getApiFile(project.projectDir, project.version())

/**
 * Same as getCurrentApiFile but also contains a restricted API file too
 */
fun Project.getCurrentApiLocation() = ApiLocation.fromPublicApiFile(project.getCurrentApiFile())

/**
 * Returns the API file containing the public API that this library promises to support
 * This is API file that checkApiRelease validates against
 * @return the API file
 */
fun Project.getRequiredCompatibilityApiFile() =
        getLastReleasedApiFile(project.projectDir, project.version(), true, true)

/*
 * Same as getRequiredCompatibilityApiFile but also contains a restricted API file
 */
fun Project.getRequiredCompatibilityApiLocation(): ApiLocation? {
    val publicFile = project.getRequiredCompatibilityApiFile()
    if (publicFile == null) {
        return null
    }
    return ApiLocation.fromPublicApiFile(publicFile)
}

/**
 * Returns the API file for the API of the specified version.
 *
 * @param version the API version, ex. 25.0.0-SNAPSHOT
 * @return the API file of this version
 */
private fun getApiFile(rootDir: File, version: Version): File {
    if (version.patch != 0 && (version.isAlpha() || version.isBeta())) {
        val suggestedVersion = Version("${version.major}.${version.minor}.${version.patch}-rc01")
        throw GradleException("Illegal version $version . It is not allowed to have a nonzero " +
                "patch number and be alpha or beta at the same time.\n" +
                "Did you mean $suggestedVersion?")
    }

    var extra = ""
    if (version.patch == 0 && version.extra != null) {
        extra = version.extra
    }
    val apiDir = File(rootDir, "api")
    return File(apiDir, "${version.major}.${version.minor}.0$extra.txt")
}

/**
 * Returns the filepath of the previous API txt file
 */
private fun getOldApiTxtForDocDiffs(project: Project): File? {
    val toApi = project.processProperty("toApi")?.let {
        Version.parseOrNull(it)
    }
    val fromApi = project.processProperty("fromApi")
    val rootFolder = project.projectDir
    return if (fromApi != null) {
        // Use an explicit API file.
        File(rootFolder, "api/$fromApi.txt")
    } else {
        // Use the most recently released API file bounded by toApi.
        getLastReleasedApiFile(rootFolder, toApi, false, false)
    }
}

private fun getLastReleasedApiFile(
    rootFolder: File,
    refVersion: Version?,
    requireFinalApi: Boolean,
    requireSameMajorRevision: Boolean
): File? {
    val apiDir = File(rootFolder, "api")
    return getLastReleasedApiFileFromDir(apiDir, refVersion, requireFinalApi,
            requireSameMajorRevision, ApiType.CLASSAPI)
}

/**
 * Returns the api file with highest version among those having version less than
 * maxVersionExclusive or null.
 * Ignores alpha versions if requireFinalApi is true.
 * If requireSameMajorRevision is true then only considers releases having the same major revision.
 * If apiType is RESOURCEAPI, it will return the resource api file and if it is CLASSAPI, it will
 * return the regular api file.
 */
fun getLastReleasedApiFileFromDir(
    apiDir: File,
    maxVersionExclusive: Version?,
    requireFinalApi: Boolean,
    requireSameMajorRevision: Boolean,
    apiType: ApiType
): File? {
    if (requireSameMajorRevision && maxVersionExclusive == null) {
        throw GradleException("Version is not specified for the current project, " +
                "please specify a mavenVersion in your gradle build file")
    }
    var lastFile: File? = null
    var lastVersion: Version? = null
    var apiFiles = apiDir.listFiles().toList()
    apiFiles = apiFiles.filter { (apiType == ApiType.RESOURCEAPI && it.name.startsWith("res")) ||
            (apiType == ApiType.CLASSAPI && !it.name.startsWith("res")) }
    apiFiles.forEach { file ->
        val parsed = Version.parseOrNull(file)
        parsed?.let { version ->
            if ((lastFile == null || lastVersion!! < version) &&
                    (maxVersionExclusive == null || version < maxVersionExclusive) &&
                    (!requireFinalApi || version.isFinalApi()) &&
                    (!requireSameMajorRevision || version.major == maxVersionExclusive?.major)) {
                lastFile = file
                lastVersion = version
            }
        }
    }
    return lastFile
}

// Creates a new task on the project for verifying the API
private fun createCheckApiTask(
    project: Project,
    taskName: String,
    docletpath: Collection<File>,
    config: ChecksConfig,
    oldApi: File?,
    newApi: Provider<File>,
    whitelist: File? = null
) =
        project.tasks.register(taskName, CheckApiTask::class.java) {
            it.apply {
                doclavaClasspath = docletpath
                checksConfig = config
                newApiFile = newApi.get()
                oldApiFile = oldApi
                whitelistErrorsFile = whitelist
                doFirst {
                    logger.lifecycle("Verifying ${newApi.get().name} " +
                            "against ${oldApi?.name ?: "nothing"}...")
                }
            }
        }

private fun getNewApiTxt(project: Project, generateApi: TaskProvider<DoclavaTask>): FileProvider {
    val toApi = project.processProperty("toApi")
    return if (toApi != null) {
        // Use an explicit API file.
        FileProvider(project.provider {
            File(project.projectDir, "api/$toApi.txt")
        }, null)
    } else {
        // Use the current API file (e.g. current.txt).
        FileProvider(generateApi.map {
            it.apiFile!!
        }, generateApi)
    }
}

private data class FileProvider(val file: Provider<File>, val task: TaskProvider<*>?)

private fun stripExtension(fileName: String) = fileName.substringBeforeLast('.')
