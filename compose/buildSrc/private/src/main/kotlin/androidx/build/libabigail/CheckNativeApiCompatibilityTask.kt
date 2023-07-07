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
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputFiles
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.process.ExecOperations
import org.gradle.workers.WorkAction
import org.gradle.workers.WorkParameters
import org.gradle.workers.WorkerExecutionException
import org.gradle.workers.WorkerExecutor
import java.io.ByteArrayOutputStream
import java.io.File
import javax.inject.Inject

/**
 * Task which depends on [GenerateNativeApiTask] and compares the current native API from the build
 * directory to that stored under /native-api using abidiff. Throws an [AbiDiffException] if the API
 * has incompatible changes.
 */
@CacheableTask
abstract class CheckNativeApiCompatibilityTask : DefaultTask() {

    @get:Inject
    abstract val workerExecutor: WorkerExecutor

    @get:Internal
    abstract val artifactNames: ListProperty<String>

    @get:Internal
    abstract val builtApiLocation: Property<File>

    @get:Internal
    abstract val currentApiLocation: Property<File>

    @get:Input
    abstract val strict: Property<Boolean>

    @[InputFiles PathSensitive(PathSensitivity.RELATIVE)]
    fun getTaskInputs(): List<File> {
        return getLocationsForArtifacts(
            builtApiLocation.get(),
            artifactNames.get()
        )
    }

    @OutputFiles
    fun getTaskOutputs(): List<File> {
        return getLocationsForArtifacts(
            currentApiLocation.get(),
            artifactNames.get()
        )
    }

    @TaskAction
    fun exec() {
        if (getOperatingSystem() != OperatingSystem.LINUX) {
            project.logger.warn(
                "Native API checking is currently not supported on non-linux devices"
            )
            return
        }
        val builtApiFiles = builtApiLocation.get().walk().toList()
        val currentApiFiles = currentApiLocation.get().walk().toList()

        // Unless this is the first time we've generated these files, a difference in the number of
        // API files indicates that a library has been added / removed and the API has changed.
        if (currentApiFiles.isNotEmpty() && builtApiFiles.size != currentApiFiles.size) {
            throw AbiDiffException("Number of built artifacts has changed, expected " +
                "${currentApiFiles.size} but was ${builtApiFiles.size}")
        }
        val workQueue = workerExecutor.processIsolation()
        builtApiLocation.get().listFiles().forEach { archDir ->
            archDir.listFiles().forEach { apiFile ->
                workQueue.submit(AbiDiffWorkAction::class.java) { parameters ->
                    // the current API file of the same name as the one in the built location
                    parameters.pathToPreviousLib = currentApiLocation.get()
                        .resolve(archDir.name)
                        .resolve(apiFile.name)
                        .toString()
                    // the newly built API file we want to check
                    parameters.pathToCurrentLib = apiFile.toString()
                    // necessary to locate `abidiff`
                    parameters.rootDir = project.rootDir.toString()
                }
            }
        }
        workQueue.await()
        logger.info("Native API check succeeded")
    }
}

class AbiDiffException(message: String) : WorkerExecutionException(message)

interface AbiDiffParameters : WorkParameters {
    var rootDir: String
    var pathToPreviousLib: String
    var pathToCurrentLib: String
}

/**
 * The exit value from `abidiff` is an 8-bit field, the specific bits have meaning.The exit codes
 * we are about are:
 *
 * 0000 (0) -> success
 * 0001 (1) -> tool error
 * 0010 (2) -> user error (bad flags etc)
 * 0100 (4) -> ABI changed
 * 1100 (12) -> ABI changed + incompatible changes
 *
 * Remaining bits unused for now, so we should indeed error if we encounter them until we know
 * their meaning.
 * https://sourceware.org/libabigail/manual/abidiff.html#return-values
 */
enum class AbiDiffExitCode(val value: Int) {
    SUCCESS(0),
    TOOL_ERROR(1),
    USER_ERROR(2),
    ABI_CHANGE(4),
    ABI_INCOMPATIBLE_CHANGE(12),
    UNKNOWN(-1);
    companion object {
        fun fromInt(value: Int): AbiDiffExitCode = values().find { it.value == value } ?: UNKNOWN
    }
}

abstract class AbiDiffWorkAction @Inject constructor(
    private val execOperations: ExecOperations
) : WorkAction<AbiDiffParameters> {
    override fun execute() {
        val outputStream = ByteArrayOutputStream()
        val result = execOperations.exec {
            it.executable = LibabigailPaths.Linux.abidiffPath(parameters.rootDir)
            it.args = listOf(
                parameters.pathToPreviousLib,
                parameters.pathToCurrentLib
            )
            it.standardOutput = outputStream
            it.isIgnoreExitValue = true
        }
        outputStream.close()
        val exitValue = result.exitValue
        val output = outputStream.toString()
        when (AbiDiffExitCode.fromInt(exitValue)) {
            AbiDiffExitCode.ABI_INCOMPATIBLE_CHANGE -> {
                throw AbiDiffException("Incompatible API changes found! Please make sure these " +
                    "are intentional and if so update the API file by " +
                    "running 'ignoreBreakingChangesAndUpdateNativeApi'\n\n$output")
            }
            AbiDiffExitCode.TOOL_ERROR,
            AbiDiffExitCode.USER_ERROR,
            AbiDiffExitCode.UNKNOWN -> {
                throw AbiDiffException("Encountered an error while executing 'abidiff', " +
                    "this is likely a bug.\n\n$output")
            }
            AbiDiffExitCode.ABI_CHANGE, // non breaking changes are okay
            AbiDiffExitCode.SUCCESS -> Unit
        }
    }
}
