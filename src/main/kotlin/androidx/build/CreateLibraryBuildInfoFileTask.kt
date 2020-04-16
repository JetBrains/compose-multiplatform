/*
 * Copyright (C) 2019 The Android Open Source Project
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

import androidx.build.gitclient.Commit
import androidx.build.gitclient.GitClientImpl
import androidx.build.gitclient.GitCommitRange
import androidx.build.gmaven.GMavenVersionChecker
import androidx.build.jetpad.LibraryBuildInfoFile
import org.gradle.api.DefaultTask
import org.gradle.api.artifacts.ProjectDependency
import org.gradle.api.artifacts.ExternalModuleDependency
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.api.provider.Property
import com.google.gson.GsonBuilder
import java.io.File
import java.util.ArrayList

/**
 * This task generates a library build information file containing the artifactId, groupId, and
 * version of public androidx dependencies and release checklist of the library for consumption
 * by the Jetpack Release Service (JetPad).
 */
open class CreateLibraryBuildInfoFileTask : DefaultTask() {
    init {
        group = "Help"
        description = "Generates a file containing library build information serialized to json"
    }

    @OutputFile
    val outputFile: Property<File> = project.objects.property(File::class.java)

    /**
     * Returns the local project directory without the full framework/support root directory path
     * <p>
     * Note: `project.projectDir.toString().removePrefix(project.rootDir.toString())` does not work
     * because the project rootDir is not guaranteed to be a substring of the projectDir
     */
    private fun getProjectSpecificDirectory(): String {
        return project.projectDir.absolutePath.removePrefix(
            project.getSupportRootFolder().absolutePath)
    }

    /**
     * Returns whether or not the groupId of the project requires the same version for all
     * artifactIds.
     */
    private fun requiresSameVersion(): Boolean {
        val library = project.extensions.findByType(AndroidXExtension::class.java)
        return library?.mavenGroup?.requireSameVersion ?: false
    }

    /* For androidx release notes, the most common use case is to track and publish the last sha
     * of the build that is released.  Thus, we use frameworks/support to get the sha
     */
    private fun getFrameworksSupportCommitShaAtHead(): String {
        val commitList: List<Commit> = GitClientImpl(project.getSupportRootFolder(), logger)
            .getGitLog(
                GitCommitRange(
                    fromExclusive = "",
                    untilInclusive = "HEAD",
                    n = 1
                ),
                keepMerges = true,
                fullProjectDir = project.getSupportRootFolder()
            )
        if (commitList.isEmpty()) {
            throw RuntimeException("Failed to find git commit for HEAD!")
        }
        return commitList.first().sha
    }

    private fun writeJsonToFile(info: LibraryBuildInfoFile) {
        if (!project.getBuildInfoDirectory().exists()) {
            if (!project.getBuildInfoDirectory().mkdirs()) {
                throw RuntimeException("Failed to create " +
                        "output directory: ${project.getBuildInfoDirectory()}")
            }
        }
        val resolvedOutputFile: File = outputFile.get()
        if (!resolvedOutputFile.exists()) {
            if (!resolvedOutputFile.createNewFile()) {
                throw RuntimeException("Failed to create " +
                        "output dependency dump file: $outputFile")
            }
        }

        // Create json object from the artifact instance
        val gson = GsonBuilder().setPrettyPrinting().create()
        val serializedInfo: String = gson.toJson(info)
        resolvedOutputFile.writeText(serializedInfo)
    }

    private fun resolveAndCollectDependencies(): LibraryBuildInfoFile {
        val libraryBuildInfoFile = LibraryBuildInfoFile()
        libraryBuildInfoFile.artifactId = project.name.toString()
        libraryBuildInfoFile.groupId = project.group.toString()
        libraryBuildInfoFile.version = project.version.toString()
        libraryBuildInfoFile.path = getProjectSpecificDirectory()
        libraryBuildInfoFile.sha = getFrameworksSupportCommitShaAtHead()
        libraryBuildInfoFile.groupIdRequiresSameVersion = requiresSameVersion()
        val libraryDependencies = ArrayList<LibraryBuildInfoFile.Dependency>()
        val checks = ArrayList<LibraryBuildInfoFile.Check>()
        libraryBuildInfoFile.checks = checks
        val publishedProjects = project.getProjectsMap()
        val versionChecker = project.property("versionChecker") as GMavenVersionChecker
        project.configurations.filter {
            /* Ignore test configuration dependencies */
            !it.name.contains("test", ignoreCase = true) &&
            /* Ignore compile configuration dependencies */
            !it.name.contains("compileClasspath", ignoreCase = true) &&
            !it.name.contains("compileOnly", ignoreCase = true)
        }.forEach { configuration ->
            configuration.allDependencies.forEach { dep ->
                // Only consider androidx dependencies
                if (dep.group != null &&
                    dep.group.toString().startsWith("androidx.") &&
                    !dep.group.toString().startsWith("androidx.test")
                ) {
                    if ((dep is ProjectDependency && publishedProjects
                            .containsKey("${dep.group}:${dep.name}")) ||
                        dep is ExternalModuleDependency
                    ) {
                        val androidXPublishedDependency = LibraryBuildInfoFile().Dependency()
                        androidXPublishedDependency.artifactId = dep.name.toString()
                        androidXPublishedDependency.groupId = dep.group.toString()
                        androidXPublishedDependency.version = dep.version.toString()
                        androidXPublishedDependency.isTipOfTree = dep is ProjectDependency
                        // Check GMaven to confirm that the pinned dependency is not an unreleased
                        // version
                        if (!androidXPublishedDependency.isTipOfTree &&
                            !versionChecker.isReleased(
                                    androidXPublishedDependency.groupId,
                                    androidXPublishedDependency.artifactId,
                                    androidXPublishedDependency.version)) {
                                androidXPublishedDependency.isTipOfTree = true
                        }
                        addDependencyToListIfNotAlreadyAdded(
                            libraryDependencies,
                            androidXPublishedDependency
                        )
                    }
                }
            }
        }
        libraryBuildInfoFile.dependencies = libraryDependencies
        return libraryBuildInfoFile
    }

    private fun addDependencyToListIfNotAlreadyAdded(
        dependencyList: ArrayList<LibraryBuildInfoFile.Dependency>,
        dependency: LibraryBuildInfoFile.Dependency
    ) {
        for (existingDependency in dependencyList) {
            if (existingDependency.groupId == dependency.groupId &&
                existingDependency.artifactId == dependency.artifactId &&
                existingDependency.version == dependency.version &&
                existingDependency.isTipOfTree == dependency.isTipOfTree) {
                return
            }
        }
        dependencyList.add(dependency)
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
}
