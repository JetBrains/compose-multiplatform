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

import androidx.build.SupportLibraryExtension
import androidx.build.Version
import androidx.build.androidJarFile
import androidx.build.createWithConfig
import androidx.build.doclava.CHECK_API_CONFIG_DEVELOP
import androidx.build.doclava.CHECK_API_CONFIG_PATCH
import androidx.build.doclava.CHECK_API_CONFIG_RELEASE
import androidx.build.doclava.ChecksConfig
import androidx.build.doclava.DoclavaTask
import androidx.build.docs.ConcatenateFilesTask
import androidx.build.docsDir
import androidx.build.processProperty
import androidx.build.version
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.plugins.JavaBasePlugin
import java.io.File

data class CheckApiTasks(
    val generateApi: DoclavaTask,
    val checkApi: CheckApiTask
)

/**
 * Sets up api tasks for the given project
 */
fun initializeApiChecksForProject(
    project: Project,
    aggregateOldApiTxtsTask: ConcatenateFilesTask,
    aggregateNewApiTxtsTask: ConcatenateFilesTask
): CheckApiTasks {
    if (!project.hasProperty("docsDir")) {
        project.extensions.add("docsDir", File(project.rootProject.docsDir(), project.name))
    }
    val version = project.version()
    val workingDir = project.projectDir

    val doclavaConfiguration = project.rootProject.configurations.getByName("doclava")
    val docletClasspath = doclavaConfiguration.resolve()
    val generateApi = createGenerateApiTask(project, docletClasspath)
    generateApi.dependsOn(doclavaConfiguration)

    // for verifying that the API surface has not broken since the last minor release
    val lastReleasedApiFile = getLastReleasedApiFile(workingDir, version, true, true)

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

    val oldApiTxt = getOldApiTxtForDocDiffs(project)
    if (oldApiTxt != null) {
        aggregateOldApiTxtsTask.addInput(project.name, oldApiTxt)
    }
    val newApiTxtProvider = getNewApiTxt(project, generateApi)
    aggregateNewApiTxtsTask.inputs.file(newApiTxtProvider.file)
    aggregateNewApiTxtsTask.addInput(project.name, newApiTxtProvider.file)
    if (newApiTxtProvider.task != null) {
        aggregateNewApiTxtsTask.dependsOn(newApiTxtProvider.task)
    }

    return CheckApiTasks(generateApi, checkApi)
}

fun Project.hasApiFolder() = File(projectDir, "api").exists()

fun hasApiTasks(project: Project, extension: SupportLibraryExtension): Boolean {
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

            exclude("**/R.java")
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
        requireSameMajorRevision)
}

/**
 * Returns the api file with highest version among those having version less than
 * maxVersionExclusive or null.
 * Ignores alpha versions if requireFinalApi is true.
 * If requireSameMajorRevision is true then only considers releases having the same major revision.
 */
private fun getLastReleasedApiFileFromDir(
    apiDir: File,
    maxVersionExclusive: Version?,
    requireFinalApi: Boolean,
    requireSameMajorRevision: Boolean
): File? {
    if (requireSameMajorRevision && maxVersionExclusive == null) {
        throw GradleException("Version is not specified for the current project, " +
                "please specify a mavenVersion in your gradle build file")
    }
    var lastFile: File? = null
    var lastVersion: Version? = null
    apiDir.listFiles().forEach { file ->
        val parsed = Version.parseOrNull(file)
        parsed?.let { version ->
            if ((lastFile == null || lastVersion!! < version) &&
                (maxVersionExclusive == null || version < maxVersionExclusive) &&
                if (requireFinalApi) version.isFinalApi() else true &&
                if (requireSameMajorRevision) version.major == maxVersionExclusive?.major
                else true) {
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

private fun getNewApiTxt(project: Project, generateApi: DoclavaTask): FileProvider {
    val toApi = project.processProperty("toApi")
    return if (toApi != null) {
        // Use an explicit API file.
        FileProvider(File(project.projectDir, "api/$toApi.txt"), null)
    } else {
        // Use the current API file (e.g. current.txt).
        FileProvider(generateApi.apiFile!!, generateApi)
    }
}

private data class FileProvider(val file: File, val task: Task?)

private fun stripExtension(fileName: String) = fileName.substringBeforeLast('.')
