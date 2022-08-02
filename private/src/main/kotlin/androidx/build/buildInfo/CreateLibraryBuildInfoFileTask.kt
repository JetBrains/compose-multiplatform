/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.build.buildInfo

import androidx.build.AndroidXExtension
import androidx.build.LibraryGroup
import androidx.build.buildInfo.CreateLibraryBuildInfoFileTask.Companion.getFrameworksSupportCommitShaAtHead
import androidx.build.getBuildInfoDirectory
import androidx.build.getGroupZipPath
import androidx.build.getProjectZipPath
import androidx.build.getSupportRootFolder
import androidx.build.gitclient.Commit
import androidx.build.gitclient.GitClient
import androidx.build.gitclient.GitCommitRange
import androidx.build.jetpad.LibraryBuildInfoFile
import com.google.gson.GsonBuilder
import java.io.File
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.artifacts.Dependency
import org.gradle.api.artifacts.ProjectDependency
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.TaskProvider
import org.gradle.work.DisableCachingByDefault
import org.jetbrains.kotlin.gradle.plugin.getKotlinPluginVersion

/**
 * This task generates a library build information file containing the artifactId, groupId, and
 * version of public androidx dependencies and release checklist of the library for consumption
 * by the Jetpack Release Service (JetPad).
 *
 * Example:
 * If this task is configured
 * - for a project with group name "myGroup"
 * - on a variant with artifactId "myArtifact",
 * - and root project outDir is "out"
 * - and environment variable DIST_DIR is not set
 *
 * then the build info file will be written to
 * "out/dist/build-info/myGroup_myArtifact_build_info.txt"
 */
@DisableCachingByDefault(because = "uses git sha as input")
abstract class CreateLibraryBuildInfoFileTask : DefaultTask() {
    init {
        group = "Help"
        description = "Generates a file containing library build information serialized to json"
    }

    @get:OutputFile
    abstract val outputFile: Property<File>

    @get:Input
    abstract val artifactId: Property<String>

    @get:Input
    abstract val groupId: Property<String>

    @get:Input
    abstract val version: Property<String>

    @get:Optional
    @get:Input
    abstract val kotlinVersion: Property<String>

    @get:Input
    abstract val projectDir: Property<String>

    @get:Input
    abstract val commit: Property<String>

    @get:Input
    abstract val groupIdRequiresSameVersion: Property<Boolean>

    @get:Input
    abstract val groupZipPath: Property<String>

    @get:Input
    abstract val projectZipPath: Property<String>

    @get:Input
    abstract val dependencyList: ListProperty<LibraryBuildInfoFile.Dependency>

    /**
     * the local project directory without the full framework/support root directory path
     */
    @get:Input
    abstract val projectSpecificDirectory: Property<String>

    private fun writeJsonToFile(info: LibraryBuildInfoFile) {
        val resolvedOutputFile: File = outputFile.get()
        val outputDir = resolvedOutputFile.parentFile
        if (!outputDir.exists()) {
            if (!outputDir.mkdirs()) {
                throw RuntimeException(
                    "Failed to create " +
                        "output directory: $outputDir"
                )
            }
        }
        if (!resolvedOutputFile.exists()) {
            if (!resolvedOutputFile.createNewFile()) {
                throw RuntimeException(
                    "Failed to create output dependency dump file: $outputFile"
                )
            }
        }

        // Create json object from the artifact instance
        val gson = GsonBuilder().serializeNulls().setPrettyPrinting().create()
        val serializedInfo: String = gson.toJson(info)
        resolvedOutputFile.writeText(serializedInfo)
    }

    private fun resolveAndCollectDependencies(): LibraryBuildInfoFile {
        val libraryBuildInfoFile = LibraryBuildInfoFile()
        libraryBuildInfoFile.artifactId = artifactId.get()
        libraryBuildInfoFile.groupId = groupId.get()
        libraryBuildInfoFile.version = version.get()
        libraryBuildInfoFile.path = projectDir.get()
        libraryBuildInfoFile.sha = commit.get()
        libraryBuildInfoFile.groupIdRequiresSameVersion = groupIdRequiresSameVersion.get()
        libraryBuildInfoFile.groupZipPath = groupZipPath.get()
        libraryBuildInfoFile.projectZipPath = projectZipPath.get()
        libraryBuildInfoFile.kotlinVersion = kotlinVersion.orNull
        libraryBuildInfoFile.checks = ArrayList()
        libraryBuildInfoFile.dependencies = ArrayList(dependencyList.get())
        return libraryBuildInfoFile
    }

    /**
     * Task: createLibraryBuildInfoFile
     * Iterates through each configuration of the project and builds the set of all dependencies.
     * Then adds each dependency to the Artifact class as a project or prebuilt dependency.  Finally,
     * writes these dependencies to a json file as a json object.
     */
    @TaskAction
    fun createLibraryBuildInfoFile() {
        val resolvedArtifact = resolveAndCollectDependencies()
        writeJsonToFile(resolvedArtifact)
    }

    companion object {
        const val TASK_NAME = "createLibraryBuildInfoFiles"

        fun setup(
            project: Project,
            mavenGroup: LibraryGroup?,
            variant: VariantPublishPlan,
            shaProvider: Provider<String>
        ): TaskProvider<CreateLibraryBuildInfoFileTask> {
            return project.tasks.register(
                TASK_NAME + variant.taskSuffix,
                CreateLibraryBuildInfoFileTask::class.java
            ) { task ->
                val group = project.group.toString()
                val artifactId = variant.artifactId
                task.outputFile.set(
                    File(
                        project.getBuildInfoDirectory(),
                        "${group}_${artifactId}_build_info.txt"
                    )
                )
                task.artifactId.set(artifactId)
                task.groupId.set(group)
                task.version.set(project.version.toString())
                task.kotlinVersion.set(project.getKotlinPluginVersion())
                task.projectDir.set(
                    project.projectDir.absolutePath.removePrefix(
                        project.getSupportRootFolder().absolutePath
                    )
                )
                task.commit.set(shaProvider)
                task.groupIdRequiresSameVersion.set(mavenGroup?.requireSameVersion)
                task.groupZipPath.set(project.getGroupZipPath())
                task.projectZipPath.set(project.getProjectZipPath())

                // Note:
                // `project.projectDir.toString().removePrefix(project.rootDir.toString())`
                // does not work because the project rootDir is not guaranteed to be a
                // substring of the projectDir
                task.projectSpecificDirectory.set(
                    project.projectDir.absolutePath.removePrefix(
                        project.getSupportRootFolder().absolutePath
                    )
                )

                // lazily compute the task dependency list based on the variant dependencies.
                task.dependencyList.set(variant.dependencies.map { it.asBuildInfoDependencies() })
            }
        }

        fun List<Dependency>.asBuildInfoDependencies() =
            filter { it.group.isAndroidXDependency() }.map {
                LibraryBuildInfoFile.Dependency().apply {
                    this.artifactId = it.name.toString()
                    this.groupId = it.group.toString()
                    this.version = it.version.toString()
                    this.isTipOfTree = it is ProjectDependency
                }
            }.toHashSet().sortedWith(
                compareBy({ it.groupId }, { it.artifactId }, { it.version })
            )

        private fun String?.isAndroidXDependency() =
            this != null && startsWith("androidx.") && !startsWith("androidx.test")

        /* For androidx release notes, the most common use case is to track and publish the last sha
         * of the build that is released.  Thus, we use frameworks/support to get the sha
         */
        fun Project.getFrameworksSupportCommitShaAtHead(): String {
            val gitClient = GitClient.create(
                project.getSupportRootFolder(),
                logger,
                GitClient.getChangeInfoPath(project).get(),
                GitClient.getManifestPath(project).get()
            )
            val commitList: List<Commit> =
                gitClient
                    .getGitLog(
                        GitCommitRange(
                            fromExclusive = "",
                            untilInclusive = "HEAD",
                            n = 1
                        ),
                        keepMerges = true,
                        fullProjectDir = getSupportRootFolder()
                    )
            if (commitList.isEmpty()) {
                throw RuntimeException("Failed to find git commit for HEAD!")
            }
            return commitList.first().sha
        }
    }
}

// Task that creates a json file of a project's dependencies
fun Project.addCreateLibraryBuildInfoFileTask(extension: AndroidXExtension) {
    afterEvaluate {
        if (extension.shouldRelease()) {
            // Only generate build info files for published libraries.
            val task = CreateLibraryBuildInfoFileTask.setup(
                project,
                extension.mavenGroup,
                VariantPublishPlan(
                    artifactId = project.name.toString(),
                    dependencies = project.provider {
                        val config = project.configurations.findByName("releaseRuntimeElements")
                        config?.allDependencies.orEmpty().toList()
                    }),
                project.provider {
                    project.getFrameworksSupportCommitShaAtHead()
                }
            )

            rootProject.tasks.named(CreateLibraryBuildInfoFileTask.TASK_NAME).configure {
                it.dependsOn(task)
            }
            addTaskToAggregateBuildInfoFileTask(task)
        }
    }
}
