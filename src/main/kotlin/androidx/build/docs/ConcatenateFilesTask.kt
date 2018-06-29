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

package androidx.build.docs

import org.gradle.api.Action
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.OutputFile
import java.io.File
import java.util.SortedMap

open class ConcatenateFilesTask : DefaultTask() {
    private var keyedInputs: MutableMap<String, File> = mutableMapOf()

    @get:OutputFile
    lateinit var Output: File

    // Adds the given input file
    // The order that files are concatenated in is based on sorting the corresponding keys
    fun addInput(key: String, inputFile: File) {
        if (this.keyedInputs.containsKey(key)) {
            throw IllegalArgumentException("Key $key already exists")
        }
        this.inputs.file(inputFile)
        this.keyedInputs[key] = inputFile
    }

    @TaskAction
    fun aggregate() {
        val destFile = this.Output

        // sort the input files to make sure this task always concatenates them in the same order
        val sortedInputs = this.keyedInputs.toSortedMap()

        val inputFiles = sortedInputs.values
        if (inputFiles.contains(destFile)) {
            throw IllegalArgumentException("Output file $destFile is also an input file")
        }

        val text = inputFiles.joinToString(separator = "") { file -> file.readText() }
        this.project.logger.info("Joining ${inputFiles.count()} files, and storing the result in ${destFile.path}")
        destFile.writeText(text)
    }
}
