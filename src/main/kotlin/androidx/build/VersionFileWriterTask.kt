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
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import java.io.File
import java.io.PrintWriter

/**
 * Task that allows to write a version to a given output file.
 */
open class VersionFileWriterTask : DefaultTask() {
    @get:Input
    lateinit var version: String
    @get:OutputFile
    lateinit var outputFile: File

    /**
     * The main method for actually writing out the file.
     */
    @TaskAction
    fun run() {
        val writer = PrintWriter(outputFile)
        writer.println(version)
        writer.close()
    }

    companion object {
        val RESOURCE_DIRECTORY = "generatedResources"
        val VERSION_FILE_PATH = RESOURCE_DIRECTORY + "/META-INF/%s_%s.version"

        /**
         * Sets up Android Library project to have a task that generates a version file.
         * It must be called after [LibraryExtension] has been resolved.
         *
         * @param project an Android Library project.
         */
        fun setUpAndroidLibrary(project: Project, library: LibraryExtension) {
            val group = project.properties["group"] as String
            val artifactId = project.properties["name"] as String
            val version = project.properties["version"] as String

            // Add a java resource file to the library jar for version tracking purposes.
            val artifactName = File(
                    project.buildDir,
                    String.format(VERSION_FILE_PATH, group, artifactId))

            val writeVersionFile = project.tasks.create("writeVersionFile",
                    VersionFileWriterTask::class.java)
            writeVersionFile.version = version
            writeVersionFile.outputFile = artifactName

            library.libraryVariants.all {
                it.processJavaResources.dependsOn(writeVersionFile)
            }

            val resources = library.sourceSets.getByName("main").resources
            resources.srcDir(File(project.buildDir, RESOURCE_DIRECTORY))
            if (!resources.includes.isEmpty()) {
                val includes = resources.includes
                includes.add("META-INF/*.version")
                resources.setIncludes(includes)
            }
        }
    }
}
