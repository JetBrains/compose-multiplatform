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

import androidx.build.ftl.GCloudCLIWrapper.RunTestParameters.Companion.TEST_OUTPUT_FILE_NAME
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.google.gson.reflect.TypeToken
import org.gradle.api.GradleException
import org.gradle.process.ExecOperations
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.Locale
import java.util.UUID

/**
 * Wrapper around GCloud CLI.
 *
 * https://cloud.google.com/sdk/gcloud
 *
 * Note that this wrapper requires gcloud to be available on the host machine.
 *
 * documentation for FTL:
 * https://cloud.google.com/sdk/gcloud/reference/firebase/test/android/run
 */
@Suppress("UnstableApiUsage") // ExecOperations
internal class GCloudCLIWrapper(
    private val execOperations: ExecOperations
) {
    private val gson = Gson()

    /**
     * Path to the gcloud executable, derived from `which gcloud` call.
     */
    private val gcloud: String by lazy {
        findExecutable("gcloud")
    }

    /**
     * Path to the gsutil executable, derived from `which gsutil` call.
     */
    private val gsutil: String by lazy {
        findExecutable("gsutil")
    }

    private inline fun <reified T> executeGcloud(
        vararg params: String
    ): T {
        val output = ByteArrayOutputStream()
        val errorOutput = ByteArrayOutputStream()
        val execResult = execOperations.exec {
            it.executable = gcloud
            it.args = params.toList() + "--format=json"
            it.standardOutput = output
            it.errorOutput = errorOutput
            it.isIgnoreExitValue = true
        }
        if (execResult.exitValue != 0) {
            System.err.println("GCloud command failed: ${errorOutput.toString(Charsets.UTF_8)}")
        }
        // still try to parse the because when it fails (e.g. test failure), it returns a non-0
        // exit code but still prints the output. We are interested in the output.
        val commandOutput = output.toString(Charsets.UTF_8)
        return gson.parse(commandOutput)
    }

    private fun execGsUtil(
        vararg params: String
    ): String {
        val output = ByteArrayOutputStream()
        execOperations.exec {
            it.executable = gsutil
            it.args = params.toList()
            it.standardOutput = output
        }
        return output.toString(Charsets.UTF_8)
    }

    /**
     * https://cloud.google.com/sdk/gcloud/reference/firebase/test/android/run
     */
    fun runTest(
        params: RunTestParameters
    ): List<TestResult> {
        val testResults = executeGcloud<List<TestResult>>(
            "firebase", "test", "android", "run",
            "--type", "instrumentation",
            "--test", params.testApk.canonicalPath,
            "--app", params.testedApk.canonicalPath,
            "--num-flaky-test-attempts", "2",
            "--results-bucket=${params.bucketName}",
            "--results-dir=${params.resultsBucketDir}",
            "--results-history-name=${params.projectPath}"
        )
        // copy the test results from the bucket to the build directory
        execGsUtil(
            "cp", "-r", params.cloudBucketPath() + "/*", params.resultsLocalDir.canonicalPath
        )
        // finally, write the command response into the directory as well
        val testResultOutput = params.resultsLocalDir.resolve(TEST_OUTPUT_FILE_NAME)
        testResultOutput.bufferedWriter(Charsets.UTF_8).use {
            gson.toJson(
                testResults,
                it
            )
        }
        return testResults
    }

    /**
     * find the given executable's path in the PATH via `which` command.
     */
    private fun findExecutable(name: String): String {
        val output = ByteArrayOutputStream()
        val result = execOperations.exec {
            it.commandLine("which", name)
            it.standardOutput = output
            it.isIgnoreExitValue = true
        }
        if (result.exitValue != 0) {
            throw GradleException(
                """
                Unable to find $name CLI executable.
                `which $name` returned exit code ${result.exitValue}.
                Make sure gcloud CLI is installed, authenticated and is part of your PATH.
                See https://cloud.google.com/sdk/gcloud for installation instructions.
                """.trimIndent()
            )
        }
        return output.toString(Charsets.UTF_8).trim()
    }

    /**
     * Data structure format for gcloud FTL command
     */
    internal data class TestResult(
        @SerializedName("axis_value")
        val axisValue: String,
        val outcome: String,
        @SerializedName("test_details")
        val testDetails: String
    ) {
        val passed
            get() = outcome.toLowerCase(Locale.US) in SUCCESS_OUTCOMES

        companion object {
            private val SUCCESS_OUTCOMES = listOf("passed", "flaky")
        }
    }

    /**
     * Parameters for invoking a test on the Firebase Test Lab
     */
    internal data class RunTestParameters(
        /**
         * The project path for which we are executing the tests for.
         */
        val projectPath: String,
        /**
         * The tested APK file
         */
        val testedApk: File,
        /**
         * The test APK file which includes the instrumentation tests
         */
        val testApk: File,
        /**
         * The name of the GS bucket to save the results
         */
        val bucketName: String = DEFAULT_BUCKET_NAME,
        /**
         * The GS Bucket directory where the results will be saved
         */
        val resultsBucketDir: String = buildRelativeResultDirPath(projectPath),
        /**
         * The local directory where we will download the test results
         */
        val resultsLocalDir: File,
    ) {

        /**
         * Returns the path to the Google Cloud bucket where the test run results are saved
         */
        fun cloudBucketPath(): String {
            return "gs://$bucketName/$resultsBucketDir"
        }

        companion object {
            const val DEFAULT_BUCKET_NAME = "androidx-ftl-test-results"

            /**
             * The file into which the result of the gcloud command will be written.
             */
            const val TEST_OUTPUT_FILE_NAME = "testResults.json"

            /**
             * Generates a relative path for test results.
             *
             * If run on Github Actions CI, this method will use the environment variables to
             * create a unique path for the action.
             * If run locally, this will create a random UUID for the directory.
             */
            private fun buildRelativeResultDirPath(
                projectPath: String
            ): String {
                // github action env variables:
                // https://docs.github.com/en/actions/reference/environment-variables
                val inGithubActions = System.getenv().containsKey("GITHUB_ACTIONS")
                val pathValues = if (inGithubActions) {
                    val workflowName = requireEnvValue("GITHUB_WORKFLOW")
                    val runNumber = requireEnvValue("GITHUB_RUN_NUMBER")
                    val runId = requireEnvValue("GITHUB_RUN_ID")
                    val ref = System.getenv("GITHUB_REF")
                    listOfNotNull(
                        "github",
                        projectPath,
                        ref,
                        workflowName,
                        runNumber,
                        runId,
                    )
                } else {
                    listOf(
                        "local",
                        projectPath,
                        UUID.randomUUID().toString()
                    )
                }
                return pathValues.joinToString("/")
            }

            private fun requireEnvValue(name: String): String {
                return System.getenv(name) ?: throw GradleException(
                    "Cannot find required environment variable: $name"
                )
            }
        }
    }
}

private inline fun <reified T> Gson.parse(
    input: String
): T {
    val typeToken = object : TypeToken<T>() {}.type
    return this.fromJson(input, typeToken)
}