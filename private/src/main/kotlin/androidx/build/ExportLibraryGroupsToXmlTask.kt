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
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import java.io.BufferedWriter
import java.io.File
import java.io.Writer
import kotlin.reflect.full.memberProperties
import kotlin.text.Charsets.UTF_8

/**
 * Task that parses the contents of a given library group file (usually [LibraryGroups]) and writes
 * them to an XML file. The XML file is then used by Lint.
 */
@CacheableTask
abstract class ExportLibraryGroupsToXmlTask : DefaultTask() {

    @get:[InputFile PathSensitive(PathSensitivity.NONE)]
    lateinit var libraryGroupFile: File

    @get:OutputFile
    lateinit var xmlOutputFile: File

    @TaskAction
    fun exec() {
        val writer: Writer = BufferedWriter(Files.newWriter(xmlOutputFile, UTF_8))

        // Write XML header and outermost opening tag
        writer.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
        writer.write("<libraryGroups>\n")

        LibraryGroups::class.memberProperties.forEach { member ->
            try {
                val libraryGroup = member.get(LibraryGroups) as LibraryGroup
                val groupName = libraryGroup.group
                val isAtomic = (libraryGroup.forcedVersion != null)

                // Write data for this LibraryGroup
                writer.run {
                    write("    <libraryGroup>\n")
                    write("        <group>$groupName</group>\n")
                    write("        <isAtomic>$isAtomic</isAtomic>\n")
                    write("    </libraryGroup>\n")
                }
            } catch (ignore: ClassCastException) {
                // Object isn't a LibraryGroup, skip it
            }
        }

        // Write outermost closing tag and close writer
        writer.write("<libraryGroups>\n")
        writer.close()
    }
}
