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

package androidx.build.dependencyTracker

import org.gradle.api.logging.Logger
import java.io.File
import java.util.concurrent.TimeUnit

internal interface GitClient {
    fun findChangedFilesSince(
        sha: String,
        top: String = "HEAD",
        includeUncommitted: Boolean = false
    ): List<String>
    fun findPreviousMergeCL(): String?

    /**
     * Abstraction for running execution commands for testability
     */
    interface CommandRunner {
        /**
         * Executes the given shell command and returns the stdout by lines.
         */
        fun execute(command: String): List<String>
    }
}
/**
 * A simple git client that uses system process commands to communicate with the git setup in the
 * given working directory.
 */
internal class GitClientImpl(
    /**
     * The root location for git
     */
    private val workingDir: File,
    private val logger: Logger? = null,
    private val commandRunner: GitClient.CommandRunner = RealCommandRunner(
            workingDir = workingDir,
            logger = logger
    )
) : GitClient {
    /**
     * Finds changed file paths since the given sha
     */
    override fun findChangedFilesSince(
        sha: String,
        top: String,
        includeUncommitted: Boolean
    ): List<String> {
        // use this if we don't want local changes
        return if (includeUncommitted) {
            "$CHANGED_FILES_CMD_PREFIX $sha"
        } else {
            "$CHANGED_FILES_CMD_PREFIX $top $sha"
        }.runCommand()
    }

    /**
     * checks the history to find the first merge CL.
     */
    override fun findPreviousMergeCL(): String? {
        return PREV_MERGE_CMD
                .runCommand()
                .firstOrNull()
                ?.split(" ")
                ?.firstOrNull()
    }

    private fun String.runCommand() = commandRunner.execute(this)

    private class RealCommandRunner(
        private val workingDir: File,
        private val logger: Logger?
    ) : GitClient.CommandRunner {
        override fun execute(command: String): List<String> {
            val parts = command.split("\\s".toRegex())
            logger?.info("running command $command")
            val proc = ProcessBuilder(*parts.toTypedArray())
                    .directory(workingDir)
                    .redirectOutput(ProcessBuilder.Redirect.PIPE)
                    .redirectError(ProcessBuilder.Redirect.PIPE)
                    .start()

            proc.waitFor(1, TimeUnit.MINUTES)
            val response = proc
                    .inputStream
                    .bufferedReader()
                    .readLines()
                    .filterNot {
                        it.isEmpty()
                    }
            logger?.info("Response: ${response.joinToString(System.lineSeparator())}")
            return response
        }
    }

    companion object {
        internal const val PREV_MERGE_CMD = "git log -1 --merges --oneline"
        internal const val CHANGED_FILES_CMD_PREFIX = "git diff --name-only"
    }
}
