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
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault
import java.io.File
import java.io.PrintWriter

/**
 * Task that allows to write a version to a given output file.
 */
@DisableCachingByDefault(because = "Doesn't benefit from caching")
abstract class VersionFileWriterTask : DefaultTask() {
    @get:Input
    abstract val version: Property<String>
    @get:Input
    abstract val relativePath: Property<String>
    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty

    /**
     * The main method for actually writing out the file.
     */
    @TaskAction
    fun run() {
        val outputFile = File(outputDir.get().asFile, relativePath.get())
        outputFile.parentFile.mkdirs()
        val writer = PrintWriter(outputFile)
        writer.println(version)
        writer.close()
    }
}

/**
 * Sets up Android Library project to have a task that generates a version file.
 * It must be called after [LibraryExtension] has been resolved.
 *
 * @receiver an Android Library project.
 */
fun Project.configureVersionFileWriter(
    library: LibraryExtension,
    androidXExtension: AndroidXExtension
) {
    val writeVersionFile = tasks.register(
        "writeVersionFile",
        VersionFileWriterTask::class.java
    )

    afterEvaluate {
        writeVersionFile.configure {
            val group = findProperty("group") as String
            val artifactId = findProperty("name") as String
            val version = if (androidXExtension.shouldPublish()) {
                version().toString()
            } else {
                "0.0.0"
            }

            it.version.set(version)
            it.relativePath.set(
                String.format("META-INF/%s_%s.version", group, artifactId)
            )
            it.outputDir.set(File(buildDir, "generatedVersionFile"))

            // We only add version file if is a library that is publishing.
            it.enabled = androidXExtension.shouldPublish()
        }
        val resources = library.sourceSets.getByName("main").resources
        resources.srcDir(writeVersionFile.map { it.outputDir })
        val includes = resources.includes
        if (includes.isNotEmpty()) {
            includes.add("META-INF/*.version")
            resources.setIncludes(includes)
        }
    }

    library.libraryVariants.all { variant ->
        variant.processJavaResourcesProvider.configure {
            it.dependsOn(writeVersionFile)
        }
    }
}
