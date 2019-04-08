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

import org.gradle.api.DefaultTask
import org.gradle.api.artifacts.ProjectDependency
import org.gradle.api.artifacts.ExternalModuleDependency
import org.gradle.api.tasks.TaskAction
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import java.io.File
import java.lang.StringBuilder

/**
 * Task to create a json file containing the set of all dependencies for each
 * Gradle Configuration of this project
 */
open class ListProjectDependencyVersionsTask : DefaultTask() {

    init {
        group = "Help"
        description = "Creates a json file of the project-specific dependency information in " +
            getProjectDependencyGraphFileName().replace(project.name.toString(), "<project_name>")
    }

    // Output Dependency Graph File
    var outputDepFile = File(getProjectDependencyGraphFileName())

    data class ArtifactDependency(
        val artifactId: String,
        val groupId: String,
        val version: String,
        val isProjectDependency: Boolean
    )
    data class ResolvedArtifact(
        val artifactId: String,
        val groupId: String,
        val version: String
    ) {
        val prebuiltDependencies: MutableList<ArtifactDependency> = mutableListOf()
        val projectDependency: MutableList<ArtifactDependency> = mutableListOf()
    }

    private fun getProjectDependencyGraphDirName(): String {
        return project.buildDir.toString() + "/dependencyGraph"
    }

    private fun getProjectDependencyGraphFileName(): String {
        return "${getProjectDependencyGraphDirName()}/${project.name}-dependency-graph.json"
    }

    private fun writeJsonToFile(artifact: ResolvedArtifact) {
        // Create the dependencyGraph folder and file in build directory of project
        var outBuildDir = File(getProjectDependencyGraphDirName())
        if (!outBuildDir.exists()) {
            if (!outBuildDir.mkdirs()) {
                throw RuntimeException("Failed to create " +
                        "output directory: $outBuildDir")
            }
        }
        if (!outputDepFile.exists()) {
            if (!outputDepFile.createNewFile()) {
                throw RuntimeException("Failed to create " +
                        "output dependency dump file: $outputDepFile")
            }
        }

        // Create json object from the artifact instance
        val gson = GsonBuilder().setPrettyPrinting().create()
        val jsonArtifact: String = gson.toJson(artifact)
        outputDepFile.writeText(jsonArtifact)
    }

    private fun resolveAndCollectDependencies(): ResolvedArtifact {
        val artifact = ResolvedArtifact(project.name.toString(),
            project.group.toString(),
            project.version.toString())
        val dependencySet: MutableSet<String> = mutableSetOf()
        project.configurations.all { configuration ->
            configuration.allDependencies.forEach { dep ->
                // Only consider androidx dependencies
                if (dep.group != null &&
                    dep.group.toString().startsWith("androidx.") &&
                    !dep.group.toString().startsWith("androidx.test")) {
                    val depString: String = "${dep.group}:${dep.name}:${dep.version}"
                    if (!(dependencySet.contains(depString))) {
                        if (dep is ProjectDependency) {
                            artifact.projectDependency.add(
                                ArtifactDependency(
                                    dep.name.toString(),
                                    dep.group.toString(),
                                    dep.version.toString(),
                                    true)
                            )
                            dependencySet.add(depString)
                        } else if (dep is ExternalModuleDependency) {
                            artifact.prebuiltDependencies.add(
                                ArtifactDependency(
                                    dep.name.toString(),
                                    dep.group.toString(),
                                    dep.version.toString(),
                                    false)
                            )
                            dependencySet.add(depString)
                        }
                    }
                }
            }
        }
        return artifact
    }

    /**
     * Task: dumpDependencies
     * Iterates through each configuration of the project and builds the set of all dependencies.
     * Then adds each dependency to the Artifact class as a project or prebuilt dependency.  Finally,
     * writes these dependencies to a json file as a json object.
     */
    @TaskAction
    fun dumpDependencies() {
        val resolvedArtifact = resolveAndCollectDependencies()
        writeJsonToFile(resolvedArtifact)
    }
}

/**
 * Task for a json file of all dependencies for each artifactId
 */
open class DependencyGraphFileTask : DefaultTask() {
    init {
        group = "Help"
        description = "Creates a json file of the AndroidX dependency graph in " +
                getDependencyGraphFileName()
    }

    // List of each project Dependency File where all dependencies were dumped
    var projectDepDumpFiles: MutableList<File> = mutableListOf()

    private fun getDependencyGraphDirName(): String {
        return project.buildDir.parentFile.toString() + "/dependencyGraph"
    }

    private fun getDependencyGraphFileName(): String {
        return "${getDependencyGraphDirName()}/AndroidXDependencyGraph.json"
    }

    private data class DependencyGraph(
        val artifacts: MutableList<ListProjectDependencyVersionsTask.ResolvedArtifact>
    )

    /**
     * Reads in file and checks that json is valid
     */
    private fun jsonFileIsValid(jsonFile: File, artifactList: MutableList<String>): Boolean {
        if (!jsonFile.exists()) {
            return(false)
        }
        val gson = Gson()
        val jsonString: String = jsonFile.readText(Charsets.UTF_8)
        val depGraph = gson.fromJson(jsonString, DependencyGraph::class.java)
        depGraph.artifacts.forEach { artifact ->
            if (!(artifactList.contains(artifact.artifactId))) {
                println("Failed to find ${artifact.artifactId} in artifact list!")
                return false
            }
        }
        return true
    }

    /**
     * Task: createDependencyGraphFile
     * Create the output file to contain the final complete AndroidX project dependency graph JSON
     * file.  Then iterate through the list of input project-specific dependency files, and collect
     * all dependencies as a JSON string. Finally, write this complete dependency graph to a JSON
     * file.
     */
    @TaskAction
    fun createDependencyGraphFile() {
        // Create dependencyGraph folder and complete dependency graphy output file in
        // the support/build directory
        val depGraphDir = File(getDependencyGraphDirName())
        if (!depGraphDir.exists()) {
            if (!depGraphDir.mkdirs()) {
                throw RuntimeException("Failed to find " +
                        "create dependency Graph directory: $depGraphDir")
            }
        }
        val depGraphFile = File(getDependencyGraphFileName())
        if (!depGraphFile.exists()) {
            if (!depGraphFile.createNewFile()) {
                throw RuntimeException("Failed to create " +
                        "output dependency graph file: $depGraphFile")
            }
        }

        // Loop through each file in the list of projectDepGraphFiles and collect all dependency
        // graph data from each of these $project-dependency-graph.json files
        var output = StringBuilder()
        output.append("{ \"artifacts\": [\n")
        var artifactList = mutableListOf<String>()
        projectDepDumpFiles.filter {
                ((it.isFile and (it.name != depGraphFile.name))
                and (it.name.contains("-dependency-graph.json")))
            }.forEach { file ->
                var fileText: String = file.readText(Charsets.UTF_8)
                output.append("$fileText,")
                artifactList.add(file.name.replace("-dependency-graph.json", ""))
                // Delete '-dependency-graph.json' file
                file.delete()
                // Delete parent 'dependencyGraph' directory.  It should never be the case that
                // dependencyGraph isn't the parent file, but this check has been added in caution
                if (file.parentFile.name == "dependencyGraph") {
                    file.parentFile.delete()
                }
        }
        // Remove final ',' from list (so a null object doesn't get added to list
        output.setLength(output.length - 1)
        output.append("]}")
        depGraphFile.writeText(output.toString(), Charsets.UTF_8)
        if (!jsonFileIsValid(depGraphFile, artifactList)) {
            throw RuntimeException("JSON written to $depGraphFile was invalid.")
        }
    }
}
