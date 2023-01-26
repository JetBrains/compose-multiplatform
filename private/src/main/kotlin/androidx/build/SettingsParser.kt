/*
 * Copyright 2022 The Android Open Source Project
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

import java.io.File

/**
 * Helper class to parse the settings.gradle file from the main build and extract a list of
 * projects.
 *
 * This is used by Playground projects too, so if it is changed please run `cd room && ./gradlew tasks`
 */
object SettingsParser {
    /**
     * Match lines that start with includeProject, followed by a require argument for project gradle
     * path and an optional argument for project file path.
     */
    /* ktlint-disable max-line-length */
    private val includeProjectPattern = Regex(
        """^[\n\r\s]*includeProject\("(?<name>[a-z0-9-:]*)"(,[\n\r\s]*"(?<path>[a-z0-9-/]+))?.*\).*$""",
        setOf(RegexOption.MULTILINE, RegexOption.IGNORE_CASE)
    ).toPattern()

    fun findProjects(
        settingsFile: File,
    ): List<IncludedProject> {
        return findProjects(
            fileContents = settingsFile.readText(Charsets.UTF_8)
        )
    }

    fun findProjects(
        fileContents: String,
    ): List<IncludedProject> {
        val matcher = includeProjectPattern.matcher(fileContents)
        val includedProjects = mutableListOf<IncludedProject>()
        while (matcher.find()) {
            // check if is an include project line, if so, extract project gradle path and
            // file system path and call the filter
            val projectGradlePath =
                matcher.group("name") ?: error("Project gradle path should not be null")
            val projectFilePath =
                matcher.group("path") ?: createFilePathFromGradlePath(projectGradlePath)
            includedProjects.add(IncludedProject(projectGradlePath, projectFilePath))
        }
        return includedProjects
    }

    /**
     * Converts a gradle path (e.g. :a:b:c) to a file path (a/b/c)
     */
    private fun createFilePathFromGradlePath(gradlePath: String): String {
        return gradlePath.trimStart(':').replace(':', '/')
    }

    /**
     * Represents an included project from the main settings.gradle file.
     */
    data class IncludedProject(
        /**
         * Gradle path of the project (using : as separator)
         */
        val gradlePath: String,
        /**
         * File path for the project, relative to support root folder.
         */
        val filePath: String
    )
}
