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

import com.jakewharton.dex.DexParser.Companion.toDexParser
import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.TaskAction
import org.json.simple.JSONObject
import java.io.File
import java.lang.IllegalStateException

private const val AAR_FILE_EXTENSION = ".aar"
private const val BYTECODE_SIZE = "bytecode_size"
private const val METHOD_COUNT = "method_count"
private const val METRICS_DIRECTORY = "librarymetrics"
private const val METRICS_FILE_SUFFIX = "_librarymetrics.json"
private const val JAR_FILE_EXTENSION = ".jar"

abstract class ReportLibraryMetricsTask : DefaultTask() {

    init {
        group = "LibraryMetrics"
        description = "Task for reporting build time library metrics. Currently gathers .aar sizes."
    }

    /**
     * The variants we are interested in gathering metrics for.
     */
    @get:InputFiles
    abstract val jarFiles: ConfigurableFileCollection

    @TaskAction
    fun reportLibraryMetrics() {
        val distDir = project.rootProject.getDistributionDirectory()
        val outputDir = File(distDir.canonicalPath + '/' + METRICS_DIRECTORY)
        outputDir.mkdirs()
        val outputFile = File(
            outputDir,
            "${project.group}_${project.name}$METRICS_FILE_SUFFIX"
        )
        val json = JSONObject()

        val jarFiles = getJarFiles()
        val bytecodeSize = getBytecodeSize(jarFiles)
        if (bytecodeSize > 0L) {
            json[BYTECODE_SIZE] = bytecodeSize
        }

        val methodCount = getMethodCount(jarFiles)
        if (methodCount > 0) {
            json[METHOD_COUNT] = methodCount
        }

        outputFile.writeText(json.toJSONString())
    }

    private fun getJarFiles(): List<File> {
        return jarFiles.files.filter { file ->
                file.name.endsWith(JAR_FILE_EXTENSION)
        }
    }

    private fun getBytecodeSize(jarFiles: List<File>): Long {
        return jarFiles.map { it.length() }.sum()
    }

    private fun getMethodCount(jarFiles: List<File>): Int {
        return when {
            jarFiles.isEmpty() -> 0
            jarFiles.all { it.isFile } -> jarFiles.toDexParser().listMethods().size
            else ->
                throw IllegalStateException("One or more of the items in $jarFiles is not a file.")
        }
    }
}
