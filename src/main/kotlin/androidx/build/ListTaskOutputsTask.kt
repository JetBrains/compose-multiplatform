/*
 * Copyright 2019 The Android Open Source Project
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

import org.gradle.api.DefaultTask
import org.gradle.api.Task
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import java.io.File

/**
 * Finds the outputs of every task and saves this mapping into a file
 */
abstract class ListTaskOutputsTask() : DefaultTask() {
    @OutputFile
    val outputFile: Property<File> = project.objects.property(File::class.java)
    @Input
    val removePrefixes: MutableList<String> = mutableListOf()
    @Input
    val tasks: MutableList<Task> = mutableListOf()

    init {
        group = "Help"
        outputs.upToDateWhen { false }
    }

    fun setOutput(f: File) {
        outputFile.set(f)
        description = "Finds the outputs of every task and saves the resulting mapping into $f"
    }

    fun addTask(task: Task) {
        tasks.add(task)
    }

    fun removePrefix(prefix: String) {
        removePrefixes.add(prefix + "/")
    }

    fun findTasksByOutput(): Map<File, Task> {
        val tasksByOutput: MutableMap<File, Task> = hashMapOf()
        for (otherTask in tasks) {
            for (otherTaskOutput in otherTask.outputs.files.files) {
                tasksByOutput.put(otherTaskOutput, otherTask)
            }
        }
        return tasksByOutput
    }

    fun format(tasksByOutput: Map<File, Task>): String {
        val messages: MutableList<String> = mutableListOf()
        for ((output, task) in tasksByOutput) {
            var filePath = output.path
            for (prefix in removePrefixes) {
                filePath = filePath.removePrefix(prefix)
            }

            val keyLength = filePath.length.toInt()
            val roundedKeyLength = ((keyLength / 32) + 1) * 32
            val extraSpaces = " ".repeat(roundedKeyLength - keyLength)
            messages.add("$filePath $extraSpaces - ${task.path}")
        }
        messages.sort()
        val text = messages.joinToString("\n")
        return text
    }

    @TaskAction
    fun exec() {
        val tasksByOutput = findTasksByOutput()
        val text = format(tasksByOutput)
        val outputFile = outputFile.get()
        outputFile.writeText(text)
        logger.lifecycle("Wrote ${outputFile.path}")
    }
}
