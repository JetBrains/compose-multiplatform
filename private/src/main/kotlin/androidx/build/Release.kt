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
import org.gradle.api.file.DuplicatesStrategy
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.tasks.bundling.Zip
import org.gradle.plugin.devel.GradlePluginDevelopmentExtension
import org.gradle.work.DisableCachingByDefault
import java.io.File
import java.util.Locale

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
 * Zip task that zips all artifacts from given candidates.
 */
@DisableCachingByDefault(because = "Zip tasks are not worth caching according to Gradle")
// See https://github.com/gradle/gradle/commit/7e5c5bc9b2c23d872e1c45c855f07ca223f6c270#diff-ce55b0f0cdcf2174eb47d333d348ff6fbd9dbe5cd8c3beeeaf633ea23b74ed9eR38
open class GMavenZipTask : Zip() {

    init {
        // multiple artifacts in the same group might have the same maven-metadata.xml
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    }

    /**
     * Set to true to include maven-metadata.xml
     */
    @get:Input
    var includeMetadata: Boolean = false

    /**
     * Repository containing artifacts to include
     */
    @get:Internal
    lateinit var androidxRepoOut: File

    fun addCandidate(artifact: Artifact) {
        val groupSubdir = artifact.mavenGroup.replace('.', '/')
        val projectSubdir = File("$groupSubdir/${artifact.projectName}")
        val includes = listOfNotNull(
            "${artifact.version}/**",
            if (includeMetadata)
                "maven-metadata.*"
            else
                null
        )
        // We specifically pass the subdirectory into 'from' so that changes in other artifacts
        // won't cause this task to become out of date
        from("$androidxRepoOut/$projectSubdir") { spec ->
            spec.into("m2repository/$projectSubdir")
            for (inclusion in includes) {
                include(inclusion)
            }
        }
    }
    /**
     * Config action that configures the task when necessary.
     */
    class ConfigAction(private val params: Params) : Action<GMavenZipTask> {
        data class Params(
            /**
             * Maven group for the task. "" if multiple groups or only one project
             */
            val mavenGroup: String,
            /**
             * Set to true to include maven-metadata.xml
             */
            var includeMetadata: Boolean,
            /**
             * The root of the repository where built libraries can be found
             */
            val androidxRepoOut: File,
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
                task.androidxRepoOut = androidxRepoOut
                task.destinationDirectory.set(distDir)
                task.includeMetadata = params.includeMetadata
                task.archiveBaseName.set(getZipName(fileNamePrefix, mavenGroup))
            }
        }
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

        val artifacts = extension.publishedArtifacts
        val publishTask = project.tasks.named("publish")
        zipTasks.forEach {
            it.configure { zipTask ->
                artifacts.forEach { artifact -> zipTask.addCandidate(artifact) }

                // Add additional artifacts needed for Gradle Plugins
                if (extension.type == LibraryType.GRADLE_PLUGIN) {
                    project.extensions.getByType(
                        GradlePluginDevelopmentExtension::class.java
                    ).plugins.forEach { plugin ->
                        zipTask.addCandidate(
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
        // Make base params or reuse if already created
        val params = configActionParams ?: GMavenZipTask.ConfigAction.Params(
            mavenGroup = "",
            includeMetadata = false,
            androidxRepoOut = project.getRepositoryDirectory(),
            distDir = distDir,
            fileNamePrefix = fileNamePrefix,
            buildNumber = getBuildId()
        ).also {
            configActionParams = it
        }
        distDir.mkdirs()

        // Copy base params and apply any specific differences
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
                        fileNamePrefix = GLOBAL_ZIP_PREFIX
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

val AndroidXExtension.publishedArtifacts: List<Artifact>
    get() {
        val groupString = mavenGroup?.group!!
        val versionString = project.version.toString()
        val artifacts = mutableListOf(
            Artifact(
                mavenGroup = groupString,
                projectName = project.name,
                version = versionString
            )
        )

        // Add platform-specific artifacts, if necessary.
        artifacts += publishPlatforms.map { suffix ->
            Artifact(
                mavenGroup = groupString,
                projectName = "${project.name}-$suffix",
                version = versionString
            )
        }

        return artifacts
    }

/**
 * Converts the maven group into a readable task name.
 */
private fun groupToTaskNameSuffix(group: String): String {
    return group
        .split('.')
        .joinToString("") {
            it.replaceFirstChar { char ->
                if (char.isLowerCase()) char.titlecase(Locale.getDefault()) else char.toString()
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
