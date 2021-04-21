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

package androidx.build.ftl

import androidx.build.getDistributionDirectory
import androidx.build.getSupportRootFolder
import com.android.build.gradle.api.ApkVariant
import com.android.build.gradle.api.ApkVariantOutput
import com.android.build.gradle.api.TestVariant
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.TaskProvider
import org.gradle.process.ExecOperations
import org.gradle.workers.WorkAction
import org.gradle.workers.WorkParameters
import org.gradle.workers.WorkerExecutor
import javax.inject.Inject

/**
 * Task to run instrumentation tests on FTL.
 *
 * This task is only enabled on playground projects and requires gcloud CLI to be available on
 * the device with the right permissions.
 *
 * Due to the limitations of FTL, this task only support application instrumentation tests for now.
 */
@Suppress("UnstableApiUsage") // for gradle property APIs
@CacheableTask
abstract class RunTestOnFTLTask @Inject constructor(
    private val workerExecutor: WorkerExecutor
) : DefaultTask() {
    /**
     * The test APK for the instrumentation test.
     */
    @get:[InputFile PathSensitive(PathSensitivity.NONE)]
    abstract val testApk: RegularFileProperty

    /**
     * The tested application APK.
     */
    @get:[InputFile PathSensitive(PathSensitivity.NONE)]
    abstract val testedApk: RegularFileProperty

    /**
     * Output file to write the results
     */
    @get:OutputDirectory
    abstract val testResults: DirectoryProperty

    @TaskAction
    fun executeTest() {
        workerExecutor.noIsolation().submit(
            RunFTLTestWorkAction::class.java
        ) {
            it.testApk.set(testApk)
            it.testedApk.set(testedApk)
            it.testResults.set(testResults)
            it.projectPath.set(project.relativeResultPath())
        }
    }

    interface RunFTLTestParams : WorkParameters {
        val projectPath: Property<String>
        val testApk: RegularFileProperty
        val testedApk: RegularFileProperty
        val testResults: DirectoryProperty
    }

    abstract class RunFTLTestWorkAction @Inject constructor(
        private val execOperations: ExecOperations
    ) : WorkAction<RunFTLTestParams> {
        override fun execute() {
            val localTestResultDir = parameters.testResults.asFile.get()
            localTestResultDir.apply {
                deleteRecursively()
                mkdirs()
            }
            val testApk = parameters.testApk.asFile.get()
            val testedApk = parameters.testedApk.asFile.get()
            val gcloud = GCloudCLIWrapper(execOperations)
            val params = GCloudCLIWrapper.RunTestParameters(
                testedApk = testedApk,
                testApk = testApk,
                projectPath = parameters.projectPath.get(),
                resultsLocalDir = localTestResultDir

            )
            val result = gcloud.runTest(params)
            val failed = result.filterNot {
                it.passed
            }
            if (failed.isNotEmpty()) {
                throw GradleException("These tests failed: $failed")
            }
        }
    }

    companion object {
        private const val TASK_SUFFIX = "OnFirebaseTestLab"

        /**
         * Creates an FTL test runner task and returns it.
         * Note that only application tests are supported hence this will return `null` for
         * library projects.
         */
        fun create(project: Project, testVariant: TestVariant): TaskProvider<RunTestOnFTLTask>? {
            // TODO add support for library project, which might require synthesizing another
            //  APK :facepalm:
            // see: // https://stackoverflow.com/questions/59827750/execute-instrumented-test-for-an-android-library-with-firebase-test-lab
            val testedVariant = testVariant.testedVariant as? ApkVariant
                ?: return null
            val taskName = testVariant.name + TASK_SUFFIX
            val testResultDir = project.layout.buildDirectory.dir(
                "ftl-results"
            )
            // create task to copy results into dist directory
            val copyToDistTask = project.tasks.register(
                "copyResultsOf${taskName}ToDist",
                Copy::class.java
            ) {
                it.description = "Copy test results from $taskName into DIST folder"
                it.group = "build"
                it.from(testResultDir)
                it.into(
                    project.getDistributionDirectory()
                        .resolve("ftl-results/${project.relativeResultPath()}/$taskName")
                )
            }
            return project.tasks.register(taskName, RunTestOnFTLTask::class.java) { task ->
                task.description = "Run ${testVariant.name} tests on Firebase Test Lab"
                task.group = "Verification"
                task.testResults.set(testResultDir)
                task.dependsOn(testVariant.packageApplicationProvider)
                task.dependsOn(testedVariant.packageApplicationProvider)

                task.testApk.set(
                    testVariant.outputs
                        .withType(ApkVariantOutput::class.java)
                        .firstOrNull()
                        ?.outputFile
                )
                task.testedApk.set(
                    testedVariant.outputs
                        .withType(ApkVariantOutput::class.java)
                        .firstOrNull()
                        ?.outputFile
                )
                task.finalizedBy(copyToDistTask)
            }
        }
    }
}

/**
 * Returns the relative path of the project wrt the support root. This path is used for both
 * local dist path and cloud bucket paths.
 */
private fun Project.relativeResultPath() = projectDir.relativeTo(
    project.getSupportRootFolder()
).path