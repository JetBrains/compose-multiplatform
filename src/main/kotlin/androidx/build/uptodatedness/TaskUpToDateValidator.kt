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

package androidx.build.uptodatedness

import androidx.build.VERIFY_UP_TO_DATE
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.execution.TaskExecutionGraph
import org.gradle.kotlin.dsl.extra
import java.io.File
import java.util.Date

/**
 * Validates that all tasks (except a temporary exception list) are considered up-to-date.
 * The expected usage of this is that the user will invoke a build with the
 * TaskUpToDateValidator disabled, and then reinvoke the same build with the TaskUpToDateValidator
 * enabled. If the second build actually runs any tasks, then some tasks don't have the correct
 * inputs/outputs declared and are running more often than necessary.
 */

const val DISALLOW_TASK_EXECUTION_FLAG_NAME = "disallowExecution"
const val RECORD_FLAG_NAME = VERIFY_UP_TO_DATE

// Temporary set of exempt tasks that are known to still be out-of-date after running once
// Entries in this set may be task names (like assembleRelease) or task paths
// (like :core:core:assembleRelease)
// Entries in this set do still get rerun because they might produce files that are needed by
// subsequent tasks
val ALLOW_RERUNNING_TASKS = setOf(
    "analyticsRecordingRelease",
    "buildOnServer",
    "checkExternalLicenses",
    "createArchive",
    "createDiffArchiveForAll",
    "createProjectZip",
    "externalNativeBuildDebug",
    "externalNativeBuildRelease",
    "generateDebugUnitTestConfig",
    "generateJsonModelDebug",
    "generateJsonModelRelease",
    "generateMetadataFileForAndroidDebugPublication",
    "generateMetadataFileForAndroidReleasePublication",
    "generateMetadataFileForDesktopPublication",
    "generateMetadataFileForJvmPublication",
    "generateMetadataFileForJvmlinux-x64Publication",
    "generateMetadataFileForJvmmacos-x64Publication",
    "generateMetadataFileForJvmmacos-arm64Publication",
    "generateMetadataFileForJvmwindows-x64Publication",
    "generateMetadataFileForJvmallPublication",
    "generateMetadataFileForMavenPublication",
    "generateMetadataFileForMetadataPublication",
    "generateMetadataFileForKotlinMultiplatformPublication",
    "generateMetadataFileForPluginMavenPublication",
    "generatePomFileForBenchmarkPluginMarkerMavenPublication",
    "generatePomFileForAndroidDebugPublication",
    "generatePomFileForAndroidReleasePublication",
    "generatePomFileForDesktopPublication",
    "generatePomFileForJvmlinux-x64Publication",
    "generatePomFileForJvmmacos-x64Publication",
    "generatePomFileForJvmmacos-arm64Publication",
    "generatePomFileForJvmwindows-x64Publication",
    "generatePomFileForJvmallPublication",
    "generatePomFileForJvmPublication",
    "generatePomFileForKotlinMultiplatformPublication",
    "generatePomFileForMavenPublication",
    "generatePomFileForPluginMavenPublication",
    "generatePomFileForMetadataPublication",
    "generatePomFileForSafeargsJavaPluginMarkerMavenPublication",
    "generatePomFileForSafeargsKotlinPluginMarkerMavenPublication",
    "partiallyDejetifyArchive",
    "publishBenchmarkPluginMarkerMavenPublicationToMavenRepository",
    "publishAndroidDebugPublicationToMavenRepository",
    "publishAndroidReleasePublicationToMavenRepository",
    "publishDesktopPublicationToMavenRepository",
    "publishJvmPublicationToMavenRepository",
    "publishJvmlinux-x64PublicationToMavenRepository",
    "publishJvmmacos-x64PublicationToMavenRepository",
    "publishJvmmacos-arm64PublicationToMavenRepository",
    "publishJvmwindows-x64PublicationToMavenRepository",
    "publishJvmallPublicationToMavenRepository",
    "publishKotlinMultiplatformPublicationToMavenRepository",
    "publishMavenPublicationToMavenRepository",
    "publishMetadataPublicationToMavenRepository",
    "publishPluginMavenPublicationToMavenRepository",
    "publishSafeargsJavaPluginMarkerMavenPublicationToMavenRepository",
    "publishSafeargsKotlinPluginMarkerMavenPublicationToMavenRepository",
    /**
     * relocateShadowJar is used to configure the ShadowJar hence it does not have any outputs.
     * https://github.com/johnrengelman/shadow/issues/561
     */
    "relocateShadowJar",
    "testDebugUnitTest",
    "stripArchiveForPartialDejetification",
    "verifyDependencyVersions",
    "zipConstrainedTestConfigsWithApks",
    "zipTestConfigsWithApks",
    "zipHtmlResultsOfTestDebugUnitTest",
    "zipXmlResultsOfTestDebugUnitTest",

    ":camera:integration-tests:camera-testapp-core:mergeLibDexDebug",
    ":camera:integration-tests:camera-testapp-core:packageDebug",
    ":camera:integration-tests:camera-testapp-extensions:mergeLibDexDebug",
    ":camera:integration-tests:camera-testapp-extensions:packageDebug",
    ":camera:integration-tests:camera-testapp-extensions:GenerateTestConfigurationdebugAndroidTest",
    ":camera:integration-tests:camera-testapp-uiwidgets:mergeLibDexDebug",
    ":camera:integration-tests:camera-testapp-uiwidgets:packageDebug",
    ":camera:integration-tests:camera-testapp-core:GenerateTestConfigurationdebug",
    ":camera:integration-tests:camera-testapp-core:GenerateTestConfigurationdebugAndroidTest",
    ":camera:integration-tests:camera-testapp-view:GenerateTestConfigurationdebug",
    ":camera:integration-tests:camera-testapp-view:GenerateTestConfigurationdebugAndroidTest",
    ":camera:integration-tests:camera-testapp-view:mergeLibDexDebug",
    ":camera:integration-tests:camera-testapp-view:packageDebug",
    ":benchmark:benchmark-macro:generateReleaseProtos",
    ":benchmark:benchmark-macro:generateDebugProtos",
    ":benchmark:benchmark-macro:compileReleaseKotlin",
    ":benchmark:benchmark-macro:compileDebugKotlin",
    ":benchmark:benchmark-macro:compileReleaseJavaWithJavac",
    ":benchmark:benchmark-macro:compileDebugJavaWithJavac",
    ":benchmark:benchmark-macro:extractReleaseAnnotations",
    ":benchmark:benchmark-macro:extractDebugAnnotations",
    ":benchmark:benchmark-macro:generateApi",
    ":benchmark:benchmark-macro:runErrorProne",
    ":benchmark:benchmark-macro:lintAnalyzeDebug",
    ":benchmark:benchmark-macro:lintDebug",
    "configureCMakeDebug",
    "buildCMakeDebug",
    "configureCMakeRelWithDebInfo",
    "buildCMakeRelWithDebInfo",
    ":appsearch:appsearch-local-storage:buildCMakeDebug[icing]",
    ":appsearch:appsearch-local-storage:buildCMakeRelWithDebInfo[icing]"
)

// Additional tasks that are expected to be temporarily out-of-date after running once
// Tasks in this set we don't even try to rerun, because they're known to be unnecessary
val DONT_TRY_RERUNNING_TASKS = setOf(
    ":buildSrc-tests:project-subsets:test",
    "listTaskOutputs",
    "validateProperties",
    "tasks",
    // More information about the fact that these dokka tasks rerun can be found at b/167569304
    "dokkaKotlinDocs",
    "zipDokkaDocs",
    "dackkaDocs",

    // Flakily not up-to-date, b/176120659
    "doclavaDocs",
)

class TaskUpToDateValidator {
    companion object {

        private val BUILD_START_TIME_KEY = "taskUpToDateValidatorSetupTime"

        private fun shouldRecord(project: Project): Boolean {
            return project.hasProperty(RECORD_FLAG_NAME)
        }

        private fun shouldValidate(project: Project): Boolean {
            return project.hasProperty(DISALLOW_TASK_EXECUTION_FLAG_NAME)
        }

        private fun isAllowedToRerunTask(task: Task): Boolean {
            return ALLOW_RERUNNING_TASKS.contains(task.name) ||
                ALLOW_RERUNNING_TASKS.contains(task.path)
        }

        private fun shouldTryRerunningTask(task: Task): Boolean {
            return !(
                DONT_TRY_RERUNNING_TASKS.contains(task.name) ||
                    DONT_TRY_RERUNNING_TASKS.contains(task.path)
                )
        }

        private fun recordBuildStartTime(rootProject: Project) {
            rootProject.extra.set(BUILD_START_TIME_KEY, Date())
        }

        private fun getBuildStartTime(project: Project): Date {
            return project.rootProject.extra.get(BUILD_START_TIME_KEY) as Date
        }

        fun setup(rootProject: Project) {
            recordBuildStartTime(rootProject)
            val taskGraph = rootProject.gradle.taskGraph
            if (shouldValidate(rootProject)) {
                taskGraph.beforeTask { task ->
                    if (!shouldTryRerunningTask(task)) {
                        task.enabled = false
                    }
                }
            }
            if (shouldRecord(rootProject) || shouldValidate(rootProject)) {
                taskGraph.afterTask { task ->
                    // In the second build, make sure that the task didn't rerun
                    if (shouldValidate(rootProject)) {
                        if (task.didWork) {
                            if (!isAllowedToRerunTask(task)) {
                                val message = "Ran two consecutive builds of the same tasks," +
                                    " and in the second build, observed $task to be not " +
                                    " UP-TO-DATE. This indicates that $task does not declare" +
                                    " inputs and/or outputs correctly.\n" +
                                    tryToExplainTaskExecution(task, taskGraph)
                                throw GradleException(message)
                            }
                        }
                    }
                    // In the first build, record the task's inputs so that if they change in
                    // the second build then we can compare.
                    // In the second build, also record the task's inputs because we recorded
                    // them in the first build, and we want the two builds to be as similar as
                    // possible
                    if (shouldTryRerunningTask(task) && !isAllowedToRerunTask(task)) {
                        recordTaskInputs(task)
                    }
                }
            }
        }

        fun recordTaskInputs(task: Task) {
            val text = task.inputs.files.files.joinToString("\n")
            val destFile = getTaskInputListPath(task)
            destFile.parentFile.mkdirs()
            destFile.writeText(text)
        }

        fun getTaskInputListPath(task: Task): File {
            return File(getTasksInputListPath(task.project), task.name)
        }

        fun getTasksInputListPath(project: Project): File {
            return File(project.buildDir, "TaskUpToDateValidator/inputs")
        }

        fun getPreviousTaskExecutionCompletionTimestamp(task: Task): Date {
            // we're already saving the inputs of the task into a file,
            // so we can check the timestamp of that file to know when the task last reran
            val inputsFile = getTaskInputListPath(task)
            return Date(inputsFile.lastModified())
        }

        fun checkForChangingSetOfInputs(task: Task): String {
            val previousInputsFile = getTaskInputListPath(task)
            val previousInputs = previousInputsFile.readLines()
            val currentInputs = task.inputs.files.files.map { f -> f.toString() }
            val addedInputs = currentInputs.minus(previousInputs)
            val removedInputs = previousInputs.minus(currentInputs)
            val addedMessage = if (addedInputs.size > 0) {
                "Added these " + addedInputs.size + " inputs: " +
                    addedInputs.joinToString("\n") + "\n"
            } else {
                ""
            }
            val removedMessage = if (removedInputs.size > 0) {
                "Removed these " + removedInputs.size + " inputs: " +
                    removedInputs.joinToString("\n") + "\n"
            } else {
                ""
            }
            return addedMessage + removedMessage
        }

        fun tryToExplainTaskExecution(task: Task, taskGraph: TaskExecutionGraph): String {
            val numOutputFiles = task.outputs.files.files.size
            val outputsMessage = if (numOutputFiles > 0) {
                task.path + " declares " + numOutputFiles + " output files. This seems fine.\n"
            } else {
                task.path + " declares " + numOutputFiles + " output files. This is probably " +
                    "an error.\n"
            }

            val inputFiles = task.inputs.files.files
            var lastModifiedFile: File? = null
            var lastModifiedWhen = Date(0)
            for (inputFile in inputFiles) {
                val modifiedWhen = Date(inputFile.lastModified())
                if (modifiedWhen.compareTo(lastModifiedWhen) > 0) {
                    lastModifiedFile = inputFile
                    lastModifiedWhen = modifiedWhen
                }
            }

            val inputSetModifiedMessage = checkForChangingSetOfInputs(task)
            val inputsMessage = if (inputSetModifiedMessage != "") {
                inputSetModifiedMessage
            } else {
                if (lastModifiedFile != null) {
                    task.path + " declares " + inputFiles.size + " input files. The " +
                        "last modified input file is\n" + lastModifiedFile + "\nmodified at " +
                        lastModifiedWhen + " (the previous execution of this task completed at " +
                        getPreviousTaskExecutionCompletionTimestamp(task) + " and this build " +
                        "started at about " + getBuildStartTime(task.project) + "). " +
                        tryToExplainFileModification(lastModifiedFile, taskGraph)
                } else {
                    task.path + " declares " + inputFiles.size + " input files.\n"
                }
            }

            val reproductionMessage = "\nTo reproduce this error you can try running " +
                "`./gradlew ${task.path} -P$RECORD_FLAG_NAME`\n"
            val readLogsMessage = "\nYou can check why Gradle executed ${task.path} by " +
                "passing the '--info' flag to Gradle and then searching stdout for output " +
                "generated immediately before the task began to execute.\n" +
                "Our best guess for the reason that ${task.path} executed is below.\n"
            return readLogsMessage + outputsMessage + inputsMessage + reproductionMessage
        }

        fun getTaskDeclaringFile(file: File, taskGraph: TaskExecutionGraph): Task? {
            for (task in taskGraph.allTasks) {
                if (task.outputs.files.files.contains(file)) {
                    return task
                }
            }
            return null
        }

        fun tryToExplainFileModification(file: File, taskGraph: TaskExecutionGraph): String {
            // Find the task declaring this file as an output,
            // or the task declaring one of its parent dirs as an output
            var createdByTask: Task? = null
            var declaredFile: File? = file
            while (createdByTask == null && declaredFile != null) {
                createdByTask = getTaskDeclaringFile(declaredFile, taskGraph)
                declaredFile = declaredFile.parentFile
            }
            if (createdByTask == null) {
                return "This file is not declared as the output of any task in this build."
            }
            if (isAllowedToRerunTask(createdByTask)) {
                return "This file is declared as an output of " + createdByTask +
                    ", which is a task that is not yet validated by the TaskUpToDateValidator"
            } else {
                return "This file is decared as an output of " + createdByTask +
                    ", which is a task that is validated by the TaskUpToDateValidator " +
                    "(and therefore must not have been out-of-date during this build)"
            }
        }
    }
}
