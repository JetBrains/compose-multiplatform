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

package androidx.build.libabigail

import androidx.build.OperatingSystem
import androidx.build.getOperatingSystem
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputFiles
import org.gradle.api.tasks.TaskAction
import org.gradle.process.ExecOperations
import org.gradle.workers.WorkAction
import org.gradle.workers.WorkParameters
import org.gradle.workers.WorkerExecutor
import java.io.File
import javax.inject.Inject
import org.gradle.api.tasks.InputFile

private const val ARCH_PREFIX = "android."
internal val architectures = listOf("arm64-v8a", "armeabi-v7a", "x86", "x86_64")

/**
 * Task which generates native APIs files for each library built by the 'buildCmakeDebug' task using
 * `abidw` and stores them in the /native-api in the project build directory.
 */
abstract class GenerateNativeApiTask : DefaultTask() {

    @get:Inject
    abstract val workerExecutor: WorkerExecutor

    @get:InputDirectory
    abstract val prefabDirectory: Property<File>

    @get:InputFile
    abstract val symbolFile: Property<File>

    @get:Internal
    abstract val projectRootDir: Property<File>

    @get:Internal
    abstract val apiLocation: Property<File>

    @get:Internal
    abstract val artifactNames: ListProperty<String>

    @OutputFiles
    fun getTaskOutputs(): List<File> {
        return getLocationsForArtifacts(
            apiLocation.get(),
            artifactNames.get()
        )
    }

    @TaskAction
    fun exec() {
        if (getOperatingSystem() != OperatingSystem.LINUX) {
            logger.warn(
                "Native API checking is currently not supported on non-linux devices"
            )
            return
        }
        val destinationDir = apiLocation.get()
        if (!destinationDir.exists()) {
            destinationDir.mkdirs()
        } else {
            destinationDir.deleteRecursively()
            destinationDir.mkdirs()
        }
        val prefabDir = prefabDirectory.get()
        val workQueue = workerExecutor.processIsolation()
        artifactNames.get().forEach { moduleName ->
            val module = prefabDir.resolve("modules/$moduleName/libs")
            module.listFiles().forEach { archDir ->
                val artifacts = archDir.listFiles().filter {
                    // skip abi.json
                    it.extension == "a" || it.extension == "so"
                }
                val nameCounts = artifacts.groupingBy { it.nameWithoutExtension }.eachCount()
                nameCounts.forEach { (name, count) ->
                    if (count > 1) {
                        throw GradleException(
                            "Found multiple artifacts in $archDir with name '$name'"
                        )
                    }
                }
                artifacts.forEach { artifact ->
                    val arch = archDir.name.removePrefix(ARCH_PREFIX)
                    val outputFilePath = getLocationForArtifact(
                        destinationDir,
                        arch,
                        artifact.nameWithoutExtension
                    )
                    outputFilePath.parentFile.mkdirs()
                    workQueue.submit(AbiDwWorkAction::class.java) { parameters ->
                        parameters.rootDir = projectRootDir.get().toString()
                        parameters.symbolList = symbolFile.get().toString()
                        parameters.pathToLib = artifact.canonicalPath
                        parameters.outputFilePath = outputFilePath.toString()
                    }
                }
            }
        }
    }
}

interface AbiDwParameters : WorkParameters {
    var rootDir: String
    var symbolList: String
    var pathToLib: String
    var outputFilePath: String
}

abstract class AbiDwWorkAction @Inject constructor(private val execOperations: ExecOperations) :
    WorkAction<AbiDwParameters> {
    override fun execute() {
        val tempFile = File.createTempFile("abi", null)
        execOperations.exec {
            it.executable = LibabigailPaths.Linux.abidwPath(parameters.rootDir)
            it.args = listOf(
                "--drop-private-types",
                "--no-show-locs",
                // Do not actually pass the symbol list to `abidw`. As long as the version script
                // is being used to build the library `abidw` will only document the visible symbols
                // and there are currently some unresolved issues with certain symbols being
                // incorrectly omitted from the output of abidw.
                // "-w",
                // parameters.symbolList,
                "--out-file",
                tempFile.toString(),
                parameters.pathToLib
            )
        }
        execOperations.exec {
            it.executable = LibabigailPaths.Linux.abitidyPath(parameters.rootDir)
            it.args = listOf(
                "--input",
                tempFile.toString(),
                "--output",
                parameters.outputFilePath,
                "--abort-on-untyped-symbols"
            )
        }
    }
}

internal fun getLocationsForArtifacts(baseDir: File, artifactNames: List<String>): List<File> {
    return artifactNames.flatMap { artifactName ->
        architectures.map { arch ->
            getLocationForArtifact(baseDir, arch, artifactName)
        }
    }
}

/**
 * Takes an [archName] and [artifactName] and returns the location within the build folder where
 * that artifacts xml representation should be stored.
 */
private fun getLocationForArtifact(baseDir: File, archName: String, artifactName: String): File =
    baseDir.resolve("$archName/$artifactName.xml")
