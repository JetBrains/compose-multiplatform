/*
 * Copyright 2019 The Android Open Source Project
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

import androidx.build.jetpad.LibraryBuildInfoFile
import com.google.gson.Gson
import org.gradle.api.DefaultTask
import org.gradle.api.provider.ListProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import java.io.File
import java.util.ArrayList

/**
 * Task for a json file of all dependencies for each artifactId
 */
abstract class CreateAggregateLibraryBuildInfoFileTask : DefaultTask() {
    init {
        group = "Help"
        description = "Generates a file containing library build information serialized to json"
    }

    /** List of each build_info.txt file for each project. */
    @get:Input
    abstract val libraryBuildInfoFiles: ListProperty<File>

    @OutputFile
    val outputFile = File(
        project.getDistributionDirectory(),
        getAndroidxAggregateBuildInfoFilename()
    )

    private fun getAndroidxAggregateBuildInfoFilename(): String {
        return "androidx_aggregate_build_info.txt"
    }

    private data class AllLibraryBuildInfoFiles(
        val artifacts: ArrayList<LibraryBuildInfoFile>
    )

    /**
     * Reads in file and checks that json is valid
     */
    private fun jsonFileIsValid(jsonFile: File, artifactList: MutableList<String>): Boolean {
        if (!jsonFile.exists()) {
            return false
        }
        val gson = Gson()
        val jsonString: String = jsonFile.readText(Charsets.UTF_8)
        val aggregateBuildInfoFile = gson.fromJson(jsonString, AllLibraryBuildInfoFiles::class.java)
        aggregateBuildInfoFile.artifacts.forEach { artifact ->
            if (!artifactList.contains("${artifact.groupId}_${artifact.artifactId}")) {
                println("Failed to find ${artifact.artifactId} in artifact list!")
                return false
            }
        }
        return true
    }

    /**
     * Create the output file to contain the final complete AndroidX project build info graph
     * file.  Iterate through the list of project-specific build info files, and collects
     * all dependencies as a JSON string. Finally, write this complete dependency graph to a text
     * file as a json list of every project's build information
     */
    @TaskAction
    fun createAndroidxAggregateBuildInfoFile() {
        // Loop through each file in the list of libraryBuildInfoFiles and collect all build info
        // data from each of these $groupId-$artifactId-_build_info.txt files
        var output = StringBuilder()
        output.append("{ \"artifacts\": [\n")
        var artifactList = mutableListOf<String>()
        for (infoFile in libraryBuildInfoFiles.get()) {
            if ((infoFile.isFile and (infoFile.name != outputFile.name))
                and (infoFile.name.contains("_build_info.txt"))
            ) {
                var fileText: String = infoFile.readText(Charsets.UTF_8)
                output.append("$fileText,")
                artifactList.add(infoFile.name.replace("_build_info.txt", ""))
            }
        }
        // Remove final ',' from list (so a null object doesn't get added to the end of the list)
        output.setLength(output.length - 1)
        output.append("]}")
        outputFile.writeText(output.toString(), Charsets.UTF_8)
        if (!jsonFileIsValid(outputFile, artifactList)) {
            throw RuntimeException("JSON written to $outputFile was invalid.")
        }
    }
}
