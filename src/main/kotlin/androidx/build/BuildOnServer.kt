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
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.TaskProvider
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

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
            "androidx_aggregate_build_info.txt",
            "top-of-tree-m2repository-all-$buildId.zip"
        )

        if (project.isDocumentationEnabled()) {
            filesNames.add("dackka-tip-of-tree-docs-$buildId.zip")
            filesNames.add("dackka-public-docs-$buildId.zip")
            filesNames.add("doclava-tip-of-tree-docs-$buildId.zip")
            filesNames.add("doclava-public-docs-$buildId.zip")
            filesNames.add("dokka-tip-of-tree-docs-$buildId.zip")
            filesNames.add("dokka-public-docs-$buildId.zip")
        }

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
                missingFiles.add(file.path)
            }
        }

        if (missingFiles.isNotEmpty()) {
            val missingFileString = missingFiles.reduce { acc, s -> "$acc, $s" }
            throw FileNotFoundException("buildOnServer required output missing: $missingFileString")
        }

        verifyVersionFilesPresent()
    }

    private fun verifyVersionFilesPresent() {
        project.getRepositoryDirectory().walk().forEach { file ->
            if (file.extension == "aar") {
                val inputStream = FileInputStream(file)
                val aarFileInputStream = ZipInputStream(inputStream)
                var entry: ZipEntry? = aarFileInputStream.nextEntry
                while (entry != null) {
                    if (entry.name == "classes.jar") {
                        var foundVersionFile = false
                        val classesJarInputStream = ZipInputStream(aarFileInputStream)
                        var jarEntry = classesJarInputStream.nextEntry
                        while (jarEntry != null) {
                            if (jarEntry.name.startsWith("META-INF/androidx.") &&
                                jarEntry.name.endsWith(".version")
                            ) {
                                foundVersionFile = true
                                break
                            }
                            jarEntry = classesJarInputStream.nextEntry
                        }
                        if (!foundVersionFile) {
                            throw Exception(
                                "Missing META-INF/ version file in ${file.absolutePath}"
                            )
                        }
                        break
                    }
                    entry = aarFileInputStream.nextEntry
                }
            }
        }
    }
}

/**
 * Configures the root project's buildOnServer task to run the specified task.
 */
fun <T : Task> Project.addToBuildOnServer(taskProvider: TaskProvider<T>) {
    rootProject.tasks.named(AndroidXPlugin.BUILD_ON_SERVER_TASK).configure {
        it.dependsOn(taskProvider)
    }
}
