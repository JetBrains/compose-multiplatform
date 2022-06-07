/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.build.playground

import androidx.build.dependencyTracker.AffectedModuleDetectorImpl
import androidx.build.dependencyTracker.DependencyTracker
import androidx.build.dependencyTracker.ProjectGraph
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.options.Option
import org.gradle.work.DisableCachingByDefault
import java.io.File

/**
 * A task to print the list of affected modules based on given parameters.
 *
 * The list of changed files can be passed via [changedFiles] property and the list of module
 * paths will be written to the given [outputFilePath].
 *
 * This task is specialized for Playground projects where any change in .github or
 * playground-common will be considered as an `INFRA` change and will be listed in the outputs.
 */
@DisableCachingByDefault(because = "Fast to run, and declaring all inputs is difficult")
abstract class FindAffectedModulesTask : DefaultTask() {
    @get:Input
    @set:Option(
        option = "changedFilePath",
        description = "Changed file in the build (including removed files). Can be passed " +
            "multiple times, e.g.: --changedFilePath=a.kt --changedFilePath=b.kt " +
            "File paths must be relative to the root directory of the main checkout"
    )
    abstract var changedFiles: List<String>

    @get:Input
    @set:Option(
        option = "outputFilePath",
        description = """
            The output file path which will contain the list of project paths (line separated) that
            are affected by the given list of changed files. It might also include "$INFRA_CHANGE"
            if the change affects any of the common playground build files outside the project.
        """
    )
    abstract var outputFilePath: String

    @get:Input
    abstract var projectGraph: ProjectGraph

    @get:Input
    abstract var dependencyTracker: DependencyTracker

    @get:OutputFile
    val outputFile by lazy {
        File(outputFilePath)
    }

    init {
        group = "Tooling"
        description = """
            Outputs the list of projects in the playground project that are affected by the
            given list of files.
            ./gradlew findAffectedModules --changedFilePath=file1 --changedFilePath=file2 \
                      --outputFilePath=`pwd`/changes.txt
        """.trimIndent()
    }

    @TaskAction
    fun checkAffectedModules() {
        val hasChangedGithubInfraFiles = changedFiles.any {
            it.contains(".github") ||
                it.contains("playground-common") ||
                it.contains("buildSrc")
        }
        val detector = AffectedModuleDetectorImpl(
            projectGraph = projectGraph,
            dependencyTracker = dependencyTracker,
            logger = logger,
            cobuiltTestPaths = setOf<Set<String>>(),
            changedFilesProvider = {
                changedFiles
            }
        )
        val changedProjectPaths = detector.affectedProjects.map {
            it
        } + if (hasChangedGithubInfraFiles) {
            listOf(INFRA_CHANGE)
        } else {
            emptyList()
        }
        check(outputFile.parentFile?.exists() == true) {
            "invalid output file argument: $outputFile. Make sure to pass an absolute path"
        }
        val changedProjects = changedProjectPaths.joinToString(System.lineSeparator())
        outputFile.writeText(changedProjects, charset = Charsets.UTF_8)
        logger.info("putting result $changedProjects into ${outputFile.absolutePath}")
    }

    companion object {
        /**
         * Denotes that the changes affect common playground build files / configuration.
         */
        const val INFRA_CHANGE = "INFRA"
    }
}
