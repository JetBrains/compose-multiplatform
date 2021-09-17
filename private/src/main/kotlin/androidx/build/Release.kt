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

import com.android.build.gradle.LibraryExtension
import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Nested
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.tasks.bundling.Zip
import org.gradle.plugin.devel.GradlePluginDevelopmentExtension
import java.io.File
import java.util.Locale
import java.util.TreeSet

/**
 * Simple description for an artifact that is released from this project.
 */
data class Artifact(
    @get:Input
    val mavenGroup: String,
    @get:Input
    val projectName: String,
    @get:Input
    val version: String
) {
    override fun toString() = "$mavenGroup:$projectName:$version"
}

/**
 * Zip task that zips all artifacts from given [candidates].
 */
open class GMavenZipTask : Zip() {
    /**
     * Set to true to include maven-metadata.xml
     */
    @get:Input
    var includeMetadata: Boolean = false
    /**
     * List of artifacts that might be included in the generated zip.
     */
    @get:Nested
    val candidates = TreeSet<Artifact>(compareBy { it.toString() })

    /**
     * Config action that configures the task when necessary.
     */
    class ConfigAction(private val params: Params) : Action<GMavenZipTask> {
        data class Params(
            /**
             * Maven group for the task. "" if for all projects
             */
            val mavenGroup: String,
            /**
             * Set to true to include maven-metadata.xml
             */
            var includeMetadata: Boolean,
            /**
             * The out folder for publishing libraries.
             */
            val supportRepoOut: File,
            /**
             * The out folder where the zip will be created
             */
            val distDir: File,
            /**
             * Prefix of file name to create
             */
            val fileNamePrefix: String,
            /**
             * The build number specified by the server
             */
            val buildNumber: String
        )

        override fun execute(task: GMavenZipTask) {
            params.apply {
                task.description =
                    """
                    Creates a maven repository that includes just the libraries compiled in
                    this project.
                    Group: ${if (mavenGroup != "") mavenGroup else "All"}
                    """.trimIndent()
                task.from(supportRepoOut)
                task.destinationDirectory.set(distDir)
                task.includeMetadata = params.includeMetadata
                task.into("m2repository")
                task.archiveBaseName.set(getZipName(fileNamePrefix, mavenGroup))
                task.onlyIf {
                    // always run top diff zip as it is required by build on server task
                    task.setupIncludes()
                }
            }
        }
    }

    /**
     * Decides which files should be included in the zip. Should be invoked right before task
     * runs as an `onlyIf` block. Returns `false` if there is nothing to zip.
     */
    private fun setupIncludes(): Boolean {
        // have 1 default include so that by default, nothing is included
        val includes = candidates.flatMap {
            val mavenGroupPath = it.mavenGroup.replace('.', '/')
            listOfNotNull(
                "$mavenGroupPath/${it.projectName}/${it.version}" + "/**",
                if (includeMetadata)
                    "$mavenGroupPath/${it.projectName}" + "/maven-metadata.*"
                else
                    null
            )
        }
        includes.forEach {
            include(it)
        }
        return includes.isNotEmpty()
    }
}

/**
 * Handles creating various release tasks that create zips for the maven upload and local use.
 */
object Release {
    @Suppress("MemberVisibilityCanBePrivate")
    const val PROJECT_ARCHIVE_ZIP_TASK_NAME = "createProjectZip"
    const val DIFF_TASK_PREFIX = "createDiffArchive"
    const val FULL_ARCHIVE_TASK_NAME = "createArchive"
    const val DEFAULT_PUBLISH_CONFIG = "release"
    const val GROUP_ZIPS_FOLDER = "per-group-zips"
    const val PROJECT_ZIPS_FOLDER = "per-project-zips"
    const val GROUP_ZIP_PREFIX = "gmaven"
    const val GLOBAL_ZIP_PREFIX = "top-of-tree-m2repository"

    // lazily created config action params so that we don't keep re-creating them
    private var configActionParams: GMavenZipTask.ConfigAction.Params? = null

    /**
     * Registers the project to be included in its group's zip file as well as the global zip files.
     */
    fun register(project: Project, extension: AndroidXExtension) {
        if (extension.publish == Publish.NONE) {
            project.logger.info(
                "project ${project.name} isn't part of release," +
                    " because its \"publish\" property is explicitly set to Publish.NONE"
            )
            return
        }
        if (extension.publish == Publish.UNSET) {
            project.logger.info(
                "project ${project.name} isn't part of release, because" +
                    " it does not set the \"publish\" property or the \"type\" property"
            )
            return
        }
        if (extension.publish == Publish.SNAPSHOT_ONLY && !isSnapshotBuild()) {
            project.logger.info(
                "project ${project.name} isn't part of release, because its" +
                    " \"publish\" property is SNAPSHOT_ONLY, but it is not a snapshot build"
            )
            return
        }

        val mavenGroup = extension.mavenGroup?.group ?: throw IllegalArgumentException(
            "Cannot register a project to release if it does not have a mavenGroup set up"
        )
        if (!extension.isVersionSet()) {
            throw IllegalArgumentException(
                "Cannot register a project to release if it does not have a mavenVersion set up"
            )
        }
        val version = project.version

        val zipTasks = listOf(
            getProjectZipTask(project),
            getGroupReleaseZipTask(project, mavenGroup),
            getGlobalFullZipTask(project)
        )
        val artifact = Artifact(
            mavenGroup = mavenGroup,
            projectName = project.name,
            version = version.toString()
        )
        val publishTask = project.tasks.named("publish")
        zipTasks.forEach {
            it.configure { zipTask ->
                zipTask.candidates.add(artifact)

                // Add additional artifacts needed for Gradle Plugins
                if (extension.type == LibraryType.GRADLE_PLUGIN) {
                    project.extensions.getByType(
                        GradlePluginDevelopmentExtension::class.java
                    ).plugins.forEach { plugin ->
                        zipTask.candidates.add(
                            Artifact(
                                mavenGroup = plugin.id,
                                projectName = "${plugin.id}.gradle.plugin",
                                version = version.toString()
                            )
                        )
                    }
                }

                zipTask.dependsOn(publishTask)
            }
        }
    }

    /**
     * Create config action parameters for the project and group. If group is `null`, parameters
     * are created for the global tasks.
     */
    private fun getParams(
        project: Project,
        distDir: File,
        fileNamePrefix: String = "",
        group: String? = null
    ): GMavenZipTask.ConfigAction.Params {
        val params = configActionParams ?: GMavenZipTask.ConfigAction.Params(
            mavenGroup = "",
            includeMetadata = false,
            supportRepoOut = project.getRepositoryDirectory(),
            distDir = distDir,
            fileNamePrefix = fileNamePrefix,
            buildNumber = getBuildId()
        ).also {
            configActionParams = it
        }
        distDir.mkdirs()

        return params.copy(
            mavenGroup = group ?: "",
            distDir = distDir,
            fileNamePrefix = fileNamePrefix
        )
    }

    /**
     * Creates and returns the task that includes all projects regardless of their release status.
     */
    fun getGlobalFullZipTask(project: Project): TaskProvider<GMavenZipTask> {
        return project.rootProject.maybeRegister(
            name = FULL_ARCHIVE_TASK_NAME,
            onConfigure = {
                GMavenZipTask.ConfigAction(
                    getParams(
                        project,
                        project.getDistributionDirectory(),
                        Release.GLOBAL_ZIP_PREFIX
                    ).copy(
                        includeMetadata = true
                    )
                ).execute(it)
            },
            onRegister = {
            }
        )
    }

    /**
     * Creates and returns the zip task that includes artifacts only in the given maven group.
     */
    private fun getGroupReleaseZipTask(
        project: Project,
        group: String
    ): TaskProvider<GMavenZipTask> {
        val taskProvider: TaskProvider<GMavenZipTask> = project.rootProject.maybeRegister(
            name = "${DIFF_TASK_PREFIX}For${groupToTaskNameSuffix(group)}",
            onConfigure = {
                GMavenZipTask.ConfigAction(
                    getParams(
                        project = project,
                        distDir = File(project.getDistributionDirectory(), GROUP_ZIPS_FOLDER),
                        fileNamePrefix = GROUP_ZIP_PREFIX,
                        group = group
                    )
                ).execute(it)
            },
            onRegister = {
            }
        )
        project.addToBuildOnServer(taskProvider)
        return taskProvider
    }

    private fun getProjectZipTask(
        project: Project
    ): TaskProvider<GMavenZipTask> {
        val taskProvider = project.tasks.register(
            PROJECT_ARCHIVE_ZIP_TASK_NAME,
            GMavenZipTask::class.java
        ) {
            GMavenZipTask.ConfigAction(
                getParams(
                    project = project,
                    distDir = File(project.getDistributionDirectory(), PROJECT_ZIPS_FOLDER),
                    fileNamePrefix = project.projectZipPrefix()
                ).copy(
                    includeMetadata = true
                )
            ).execute(it)
        }
        project.addToBuildOnServer(taskProvider)
        return taskProvider
    }
}

/**
 * Let you configure a library variant associated with [Release.DEFAULT_PUBLISH_CONFIG]
 */
@Suppress("DEPRECATION") // LibraryVariant
fun LibraryExtension.defaultPublishVariant(
    config: (com.android.build.gradle.api.LibraryVariant) -> Unit
) {
    libraryVariants.all { variant ->
        if (variant.name == Release.DEFAULT_PUBLISH_CONFIG) {
            config(variant)
        }
    }
}

/**
 * Converts the maven group into a readable task name.
 */
private fun groupToTaskNameSuffix(group: String): String {
    return group
        .split('.')
        .joinToString("") {
            it.replaceFirstChar {
                if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
            }
        }
}
/**
 * Converts the project name into a readable task name
 */
private fun projectToNameSuffix(project: Project): String {
    return project.path
        .split(":", "-")
        .joinToString("") {
            it.replaceFirstChar {
                if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
            }
        }
}

private fun Project.projectZipPrefix(): String {
    return "${project.group}-${project.name}"
}

private fun getZipName(fileNamePrefix: String, mavenGroup: String): String {
    val fileSuffix = if (mavenGroup == "") {
        "all"
    } else {
        mavenGroup
            .split(".")
            .joinToString("-")
    } + "-${getBuildId()}"
    return "$fileNamePrefix-$fileSuffix"
}

fun Project.getProjectZipPath(): String {
    return Release.PROJECT_ZIPS_FOLDER + "/" +
        // We pass in a "" because that mimics not passing the group to getParams() inside
        // the getProjectZipTask function
        getZipName(projectZipPrefix(), "") + "-${project.version}.zip"
}

fun Project.getGroupZipPath(): String {
    return Release.GROUP_ZIPS_FOLDER + "/" +
        getZipName(Release.GROUP_ZIP_PREFIX, project.group.toString()) + ".zip"
}

fun Project.getGlobalZipFile(): File {
    return File(
        project.getDistributionDirectory(),
        getZipName(Release.GLOBAL_ZIP_PREFIX, "") + ".zip"
    )
}
