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

package androidx.build

import com.google.common.io.Files
import java.io.BufferedWriter
import java.io.File
import java.io.Writer
import kotlin.reflect.full.memberProperties
import kotlin.text.Charsets.UTF_8
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction

/**
 * Task that parses the contents of a given library group file (usually [LibraryGroups]) and writes
 * the groups that are atomic to a text file. The file is then used by Lint.
 */
@CacheableTask
abstract class ExportAtomicLibraryGroupsToTextTask : DefaultTask() {

    @get:[InputFile PathSensitive(PathSensitivity.NONE)]
    lateinit var libraryGroupFile: File

    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty

    @TaskAction
    fun exec() {
        // This must match the definition in BanInappropriateExperimentalUsage.kt
        val filename = "atomic-library-groups.txt"

        val textOutputFile = outputDir.file(filename).get().asFile
        val writer: Writer = BufferedWriter(Files.newWriter(textOutputFile, UTF_8))

        LibraryGroups::class.memberProperties.forEach { member ->
            try {
                val libraryGroup = member.get(LibraryGroups) as LibraryGroup
                val groupName = libraryGroup.group

                if (libraryGroup.requireSameVersion) {
                    writer.write("$groupName\n")
                }
            } catch (ignore: ClassCastException) {
                // Object isn't a LibraryGroup, skip it
            }
        }
        writer.close()
    }
}
