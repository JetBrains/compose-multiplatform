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

import java.io.File
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault

/**
 * Finds the outputs of every task and saves this mapping into a file
 */
@DisableCachingByDefault(because = "Uses too many inputs to be feasible to cache, but runs quickly")
abstract class ListTaskOutputsTask : DefaultTask() {
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

    fun removePrefix(prefix: String) {
        removePrefixes.add("$prefix/")
    }

    // Given a map from output file to Task, formats into a String
    private fun formatTasks(tasksByOutput: Map<File, Task>): String {
        val messages: MutableList<String> = mutableListOf()
        for ((output, task) in tasksByOutput) {
            var filePath = output.path
            for (prefix in removePrefixes) {
                filePath = filePath.removePrefix(prefix)
            }

            messages.add(
                formatInColumns(
                    listOf(filePath, " - " + task.path + " (" + task::class.qualifiedName + ")")
                )
            )
        }
        messages.sort()
        return messages.joinToString("\n")
    }

    // Given a list of columns, indents and joins them to be easy to read
    private fun formatInColumns(columns: List<String>): String {
        val components = mutableListOf<String>()
        var textLength = 0
        for (column in columns) {
            val roundedTextLength = if (textLength == 0) {
                textLength
            } else {
                ((textLength / 32) + 1) * 32
            }
            val extraSpaces = " ".repeat(roundedTextLength - textLength)
            components.add(extraSpaces)
            textLength = roundedTextLength
            components.add(column)
            textLength += column.length
        }
        return components.joinToString("")
    }

    @TaskAction
    fun exec() {
        val tasksByOutput = project.rootProject.findAllTasksByOutput()
        val text = formatTasks(tasksByOutput)

        val outputFile = outputFile.get()
        outputFile.writeText(text)
        logger.lifecycle("Wrote ${outputFile.path}")
    }
}

// TODO(149103692): remove all elements of this set
val taskNamesKnownToDuplicateOutputs = setOf(
    "jarRelease",
    "jarDebug",
    "kotlinSourcesJar",
    "releaseSourcesJar",
    "sourceJarRelease",
    "sourceJar",
    // MPP plugin has issues with modules using withJava() clause, see b/158747039.
    "processTestResources",
    "jvmTestProcessResources",
    "desktopTestProcessResources",
    "processResources",
    "jvmProcessResources",
    "desktopProcessResources",
    // https://github.com/square/wire/issues/1947
    "generateDebugProtos",
    "generateReleaseProtos",
    // Release APKs
    "copyReleaseApk",
    // b/223733695
    "pixel2api31DebugAndroidTest",
    "pixel2api31ReleaseAndroidTest",
    "pixel2api31WithExpandProjectionDebugAndroidTest",
    "pixel2api31WithNullAwareTypeConverterDebugAndroidTest",
    "pixel2api31WithoutExpandProjectionDebugAndroidTest",
    "pixel2api31WithKaptDebugAndroidTest",
    "pixel2api31WithKspDebugAndroidTest",
    "pixel2api31TargetSdk29DebugAndroidTest",
    "pixel2api31TargetSdk30DebugAndroidTest",
    "pixel2api31TargetSdkLatestDebugAndroidTest",
    "pixel2api30DebugAndroidTest",
    "pixel2api30ReleaseAndroidTest",
    "pixel2api30WithExpandProjectionDebugAndroidTest",
    "pixel2api30WithNullAwareTypeConverterDebugAndroidTest",
    "pixel2api30WithoutExpandProjectionDebugAndroidTest",
    "pixel2api30WithKaptDebugAndroidTest",
    "pixel2api30WithKspDebugAndroidTest",
    "pixel2api30TargetSdk29DebugAndroidTest",
    "pixel2api30TargetSdk30DebugAndroidTest",
    "pixel2api30TargetSdkLatestDebugAndroidTest",
    "pixel2api29DebugAndroidTest",
    "pixel2api29ReleaseAndroidTest",
    "pixel2api29WithExpandProjectionDebugAndroidTest",
    "pixel2api29WithNullAwareTypeConverterDebugAndroidTest",
    "pixel2api29WithoutExpandProjectionDebugAndroidTest",
    "pixel2api29WithKaptDebugAndroidTest",
    "pixel2api29WithKspDebugAndroidTest",
    "pixel2api29TargetSdk29DebugAndroidTest",
    "pixel2api29TargetSdk30DebugAndroidTest",
    "pixel2api29TargetSdkLatestDebugAndroidTest",
)

val taskTypesKnownToDuplicateOutputs = setOf(
    // b/224564238
    "com.android.build.gradle.internal.lint.AndroidLintTask_Decorated"
)

fun shouldValidateTaskOutput(task: Task): Boolean {
    if (!task.enabled) {
        return false
    }
    return !taskNamesKnownToDuplicateOutputs.contains(task.name) &&
        !taskTypesKnownToDuplicateOutputs.contains(task::class.qualifiedName)
}

// For this project and all subprojects, collects all tasks and creates a map keyed by their output files
fun Project.findAllTasksByOutput(): Map<File, Task> {
    // find list of all tasks
    val allTasks = mutableListOf<Task>()
    project.allprojects { otherProject ->
        otherProject.tasks.all { task ->
            allTasks.add(task)
        }
    }

    // group tasks by their outputs
    val tasksByOutput: MutableMap<File, Task> = hashMapOf()
    for (otherTask in allTasks) {
        for (otherTaskOutput in otherTask.outputs.files.files) {
            val existingTask = tasksByOutput[otherTaskOutput]
            if (existingTask != null) {
                if (shouldValidateTaskOutput(existingTask) && shouldValidateTaskOutput(otherTask)) {
                    throw GradleException(
                        "Output file " + otherTaskOutput + " was declared as an output of " +
                            "multiple tasks: " + otherTask + " and " + existingTask
                    )
                }
            }
            tasksByOutput[otherTaskOutput] = otherTask
        }
    }
    return tasksByOutput
}
