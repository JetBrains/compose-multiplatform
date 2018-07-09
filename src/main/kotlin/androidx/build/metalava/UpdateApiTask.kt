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

package androidx.build.metalava

import com.android.build.gradle.BaseExtension
import com.android.build.gradle.api.BaseVariant
import org.gradle.api.attributes.Attribute
import org.gradle.api.file.FileCollection
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import java.io.File

/** Generate an API signature text file from a set of source files. */
open class UpdateApiTask : MetalavaTask() {
    /** Text file to which API signatures will be written. */
    @get:OutputFile
    var currentTxtFile: File? = null

    /** Android's boot classpath. Obtained from [BaseExtension.getBootClasspath]. */
    @get:InputFiles
    var bootClasspath: Collection<File> = emptyList()

    /** Dependencies of [sourcePaths]. */
    @get:InputFiles
    var dependencyClasspath: FileCollection? = null

    /** Source files for which API signatures will be generated. */
    @get:InputFiles
    var sourcePaths: Collection<File> = emptyList()

    /** Convenience method for setting [dependencyClasspath] and [sourcePaths] from a variant. */
    fun setVariant(variant: BaseVariant) {
        sourcePaths = variant.sourceSets.flatMap { it.javaDirectories }
        dependencyClasspath = variant.compileConfiguration.incoming.artifactView { config ->
            config.attributes { container ->
                container.attribute(Attribute.of("artifactType", String::class.java), "jar")
            }
        }.artifacts.artifactFiles
    }

    @TaskAction
    fun exec() {
        val dependencyClasspath = checkNotNull(
                dependencyClasspath) { "Dependency classpath not set." }
        val currentTxtFile = checkNotNull(currentTxtFile) { "current.txt not set." }
        check(bootClasspath.isNotEmpty()) { "Android boot classpath not set." }
        check(sourcePaths.isNotEmpty()) { "Source paths not set." }

        runWithArgs(
            "--classpath",
            (bootClasspath + dependencyClasspath.files).joinToString(File.pathSeparator),

            "--source-path",
            sourcePaths.filter { it.exists() }.joinToString(File.pathSeparator),

            "--api",
            currentTxtFile.toString(),

            "--no-banner",
            "--compatible-output=no",
            "--omit-common-packages=yes",
            "--output-kotlin-nulls=yes"
        )
    }
}
