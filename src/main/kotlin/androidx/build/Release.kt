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

import androidx.build.gmaven.GMavenVersionChecker
import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.tasks.Upload
import org.gradle.api.tasks.bundling.Zip
import java.io.File

/**
 * Simple description for an artifact that is released from this project.
 */
data class Artifact(
    val mavenGroup: String,
    val projectName: String,
    val version: String
)

/**
 * Zip task that zips all artifacts from given [candidates].
 *
 * Unless [includeReleased] is set to `true`, this task does not include any library artifact
 * that is already shipped to maven.google.com.
 */
open class GMavenZipTask : Zip() {
    /**
     * The version checker which is used to check if a library is already released.
     */
    lateinit var versionChecker: GMavenVersionChecker
    /**
     * If `true`, all libraries will be included in the zip. Otherwise, only those which are not
     * in maven.google.com are included.
     */
    var includeReleased = false
    /**
     * Set to true to include maven-metadata.xml
     */
    var includeMetadata: Boolean = false
    /**
     * List of artifacts that might be included in the generated zip.
     */
    val candidates = arrayListOf<Artifact>()

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
             * The out folder for uploadArchives.
             */
            val supportRepoOut: File,
            /**
             * The out folder where the zip will be created
             */
            val distDir: File,
            /**
             * The build number specified by the server
             */
            val buildNumber: String,
            /**
             * The version checker that queries maven.google.com
             */
            val gMavenVersionChecker: GMavenVersionChecker,
            /**
             * Set to true to include all libraries even if they are released
             */
            val includeReleased: Boolean
        )

        override fun execute(task: GMavenZipTask) {
            params.apply {
                val descSuffix = if (includeReleased) {
                    "."
                } else {
                    " without any libraries that are already on maven.google.com. " +
                            "If you need a full repo, use ${Release.FULL_ARCHIVE_TASK_NAME} task."
                }
                task.description =
                        """
                        Creates a maven repository that includes just the libraries compiled in
                        this project$descSuffix.
                        Group: ${if (mavenGroup != "") mavenGroup else "All"}
                        """.trimIndent()
                task.versionChecker = gMavenVersionChecker
                task.from(supportRepoOut)
                task.destinationDirectory.set(distDir)
                task.includeReleased = params.includeReleased
                task.includeMetadata = params.includeMetadata
                task.into("m2repository")
                val fileSuffix = if (mavenGroup == "") {
                    "all"
                } else {
                    mavenGroup
                            .split(".")
                            .joinToString("-")
                } + "-$buildNumber"
                if (includeReleased) {
                    task.archiveBaseName.set("top-of-tree-m2repository-$fileSuffix")
                } else {
                    task.archiveBaseName.set("gmaven-diff-$fileSuffix")
                }
                task.onlyIf {
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
            when {
                includeReleased -> listOfNotNull(
                        "$mavenGroupPath/${it.projectName}/${it.version}" + "/**",
                        if (includeMetadata)
                            "$mavenGroupPath/${it.projectName}" + "/maven-metadata.*"
                        else null)
                versionChecker.isReleased(it.mavenGroup, it.projectName, it.version) -> {
                    // query maven.google to check if it is released.
                    logger.info("looks like $it is released, skipping")
                    emptyList()
                }
                else -> {
                    logger.info("adding $it to partial maven zip because it cannot be found " +
                            "on maven.google.com")
                    listOfNotNull(
                            "$mavenGroupPath/${it.projectName}/${it.version}" + "/**",
                            if (includeMetadata)
                                "$mavenGroupPath/${it.projectName}" + "/maven-metadata.*"
                            else null)
                }
            }
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
    const val DIFF_TASK_PREFIX = "createDiffArchive"
    const val FULL_ARCHIVE_TASK_NAME = "createArchive"
    const val DEFAULT_PUBLISH_CONFIG = "release"
    // lazily created config action params so that we don't keep re-creating them
    private var configActionParams: GMavenZipTask.ConfigAction.Params? = null

    /**
     * Registers the project to be included in its group's zip file as well as the global zip files.
     */
    fun register(project: Project, extension: AndroidXExtension) {
        if (!extension.publish) {
            throw IllegalArgumentException(
                    "Cannot register ${project.path} into the release" +
                            " because publish is false!"
            )
        }
        val mavenGroup = extension.mavenGroup?.group ?: throw IllegalArgumentException(
                "Cannot register a project to release if it does not have a mavenGroup set up"
        )
        val version = extension.mavenVersion ?: throw IllegalArgumentException(
                "Cannot register a project to release if it does not have a mavenVersion set up"
        )
        val zipTasks = listOf(
                getGroupReleaseZipTask(project, mavenGroup),
                getGlobalReleaseZipTask(project),
                getGlobalFullZipTask(project))
        val artifact = Artifact(
                mavenGroup = mavenGroup,
                projectName = project.name,
                version = version.toString()
        )
        val uploadTask = project.tasks.named("uploadArchives", Upload::class.java)
        zipTasks.forEach {
            it.configure {
                it.candidates.add(artifact)
                it.dependsOn(uploadTask)
            }
        }
    }

    /**
     * Create config action parameters for the project and group. If group is `null`, parameters
     * are created for the global tasks.
     */
    private fun getParams(
        project: Project,
        group: String? = null
    ): GMavenZipTask.ConfigAction.Params {
        val projectDist = project.getDistributionDirectory()
        val params = configActionParams ?: GMavenZipTask.ConfigAction.Params(
                mavenGroup = "",
                includeMetadata = false,
                supportRepoOut = project.property("supportRepoOut") as File,
                gMavenVersionChecker =
                project.property("versionChecker") as GMavenVersionChecker,
                distDir = projectDist,
                includeReleased = false,
                buildNumber = getBuildId()
        ).also {
            configActionParams = it
        }

        return params.copy(
                mavenGroup = group ?: "",
                distDir = if (group == null) {
                    projectDist
                } else {
                    File(projectDist, "per-group-zips").also {
                        it.mkdirs()
                    }
                }
        )
    }

    /**
     * Creates and returns the task that generates the combined gmaven diff file for all projects.
     */
    private fun getGlobalReleaseZipTask(project: Project): TaskProvider<GMavenZipTask> {
        val taskName = "${DIFF_TASK_PREFIX}ForAll"
        return project.rootProject.maybeRegister(
            name = taskName,
            onConfigure = {
                GMavenZipTask.ConfigAction(getParams(project)).execute(it)
            },
            onRegister = {
            }
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
                    getParams(project).copy(
                        includeReleased = true,
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
        val taskName = "${DIFF_TASK_PREFIX}For${groupToTaskNameSuffix(group)}"
        return project.rootProject.maybeRegister(
            name = taskName,
            onConfigure = {
                GMavenZipTask.ConfigAction(getParams(project, group)).execute(it)
            },
            onRegister = {
            }
        )
    }
}

/**
 * Converts the maven group into a readable task name.
 */
private fun groupToTaskNameSuffix(group: String): String {
    return group
            .split('.')
            .joinToString("") {
                it.capitalize()
            }
}
