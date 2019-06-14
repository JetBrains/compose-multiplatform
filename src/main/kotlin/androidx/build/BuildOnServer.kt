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

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.TaskAction
import java.io.File
import java.io.FileNotFoundException

/**
 * Task for building all of Androidx libraries and documentation
 *
 * AndroidXPlugin configuration adds dependencies to BuildOnServer for all of the tasks that
 * produce artifacts that we want to build on server builds
 * When BuildOnServer executes, it double-checks that all expected artifacts were built
 */
open class BuildOnServer : DefaultTask() {

    init {
        group = "Build"
        description = "Builds all of the Androidx libraries and documentation"
    }

    @InputFiles
    fun getRequiredFiles(): List<File> {
        val distributionDirectory = project.getDistributionDirectory()
        val buildId = getBuildId()

        val filesNames = mutableListOf(
        // TODO: re-add after merge to compose merge to master
        // "androidx-public-docs-$buildId.zip",
        // "dokkaPublicDocs-$buildId.zip",
        "androidx-tipOfTree-docs-$buildId.zip",
        "dokkaTipOfTreeDocs-$buildId.zip",
        "androidx_aggregate_build_info.txt",
        "gmaven-diff-all-$buildId.zip",
        "top-of-tree-m2repository-all-$buildId.zip")

        if (project.findProject(":jetifier-standalone") != null) {
            filesNames.add("jetifier-standalone.zip")
            filesNames.add("top-of-tree-m2repository-partially-dejetified-$buildId.zip")
        }

        return filesNames.map { fileName -> File(distributionDirectory, fileName) }
    }

    @TaskAction
    fun checkAllBuildOutputs() {

        val missingFiles = mutableListOf<String>()
        getRequiredFiles().forEach { file ->
            if (!file.exists()) {
                missingFiles.add(file.name)
            }
        }

        if (missingFiles.isNotEmpty()) {
            val missingFileString = missingFiles.reduce { acc, s -> "$acc, $s" }
            throw FileNotFoundException("buildOnServer required output missing: $missingFileString")
        }
    }
}
