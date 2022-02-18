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

package androidx.build.libabigail.symbolfiles

import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.api.provider.Property

/**
 * Task to generate a symbol file from a single .map.txt file in the project. Requires exactly one
 * .map.txt file to exist.
 */
abstract class GenerateSymbolFileTask : DefaultTask() {
    @get:Internal
    abstract val buildDir: Property<File>

    @get:Internal
    abstract val projectDir: Property<File>

    @InputFile
    fun getInputFile(): File {
        return runCatching {
            projectDir.get().walk().single {
                it.isFile && it.name.endsWith(".map.txt")
            }
        }.getOrNull() ?: throw GradleException(
            "A single version script with extension .map.txt is required in $projectDir."
        )
    }

    @OutputFile
    fun getOutputFile(): File {
        return buildDir.get().resolve("symbol_files/abi_symbol_list.txt")
    }

    @TaskAction
    fun exec() {
        SymbolListGenerator(
            input = FileInputStream(getInputFile()),
            output = FileOutputStream(getOutputFile())
        ).generate()
    }
}