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

import androidx.build.uptodatedness.cacheEvenIfNoOutputs
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskAction
import java.io.File

/**
 * Task that validates a Jetpack library's Gradle project structure, including the fully-qualified
 * Gradle project name and project directory.
 */
abstract class ValidateProjectStructureTask : DefaultTask() {

    init {
        group = "Verification"
        description = "Task for verifying Jetpack library Gradle project structure"

        cacheEvenIfNoOutputs()
    }

    @get:Input
    @get:Optional
    abstract val libraryGroup: Property<LibraryGroup>

    @TaskAction
    fun validateProjectStructure() {
        val groupId = libraryGroup.orNull?.group ?: return
        val shortGroupId = if (groupId.startsWith(GROUP_PREFIX)) {
            groupId.substring(GROUP_PREFIX.length)
        } else {
            groupId
        }

        // Fully-qualified Gradle project name should match the Maven coordinate.
        val expectedName = ":${shortGroupId.replace(".",":")}:${project.name}"
        val actualName = project.path
        if (expectedName != actualName) {
            throw GradleException("Expected $expectedName as project name, found $actualName")
        }

        // Project directory should match the Maven coordinate.
        val expectedDir = shortGroupId.replace(".", File.separator) +
            "${File.separator}${project.name}"
        val actualDir = project.projectDir.toRelativeString(project.rootDir)
        if (expectedDir != actualDir) {
            throw GradleException("Expected $expectedDir as project directory, found $actualDir")
        }
    }
}

private const val GROUP_PREFIX = "androidx."
