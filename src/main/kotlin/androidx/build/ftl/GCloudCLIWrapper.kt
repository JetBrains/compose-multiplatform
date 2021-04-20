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

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.google.gson.reflect.TypeToken
import org.gradle.api.GradleException
import org.gradle.process.ExecOperations
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.Locale

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
    private val executable: String by lazy {
        val output = ByteArrayOutputStream()
        val result = execOperations.exec {
            it.commandLine("which", "gcloud")
            it.standardOutput = output
            it.isIgnoreExitValue = true
        }
        if (result.exitValue != 0) {
            throw GradleException(
                """
                Unable to find gcloud CLI executable.
                `which gcloud` returned exit code ${result.exitValue}.
                Make sure gcloud CLI is installed, authenticated and is part of your PATH.
                See https://cloud.google.com/sdk/gcloud for installation instructions.
                """.trimIndent()
            )
        }
        output.toString(Charsets.UTF_8).trim()
    }

    private inline fun <reified T> executeGcloud(
        vararg params: String
    ): T {
        val output = ByteArrayOutputStream()
        execOperations.exec {
            it.executable = executable
            it.args = params.toList() + "--format=json"
            it.standardOutput = output
        }
        val commandOutput = output.toString(Charsets.UTF_8)
        return gson.parse(commandOutput)
    }

    /**
     * https://cloud.google.com/sdk/gcloud/reference/firebase/test/android/run
     */
    fun runTest(
        testedApk: File,
        testApk: File
    ): List<TestResult> {
        return executeGcloud(
            "firebase", "test", "android", "run",
            "--type", "instrumentation",
            "--test", testApk.canonicalPath,
            "--app", testedApk.canonicalPath,
            "--num-flaky-test-attempts", "3",
        )
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
}

private inline fun <reified T> Gson.parse(
    input: String
): T {
    val typeToken = object : TypeToken<T>() {}.type
    return this.fromJson(input, typeToken)
}