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
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.work.DisableCachingByDefault

import java.io.File

@DisableCachingByDefault(because = "Doesn't benefit from cache")
open class SingleFileCopy : DefaultTask() {
    @InputFile @PathSensitive(PathSensitivity.ABSOLUTE)
    lateinit var sourceFile: File

    @OutputFile
    lateinit var destinationFile: File

    @TaskAction
    fun copyFile() {
        @Suppress("UnstableApiUsage")
        Files.copy(sourceFile, destinationFile)
    }
}
