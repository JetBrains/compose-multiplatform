/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.build.gitclient

import org.gradle.api.logging.Logger
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * A simple git client that uses system process commands to communicate with the git setup in the
 * given working directory.
 */
class GitRunnerGitClient(
    /**
     * The root location for git
     */
    private val workingDir: File,
    private val logger: Logger?,
    private val commandRunner: GitClient.CommandRunner = RealCommandRunner(
        workingDir = workingDir,
        logger = logger
    )
) : GitClient {

    private val gitRoot: File = findGitDirInParentFilepath(workingDir) ?: workingDir

    /**
     * Finds changed file paths since the given sha
     */
    override fun findChangedFilesSince(
        sha: String,
        top: String,
        includeUncommitted: Boolean
    ): List<String> {
        // use this if we don't want local changes
        return commandRunner.executeAndParse(
            if (includeUncommitted) {
                "$CHANGED_FILES_CMD_PREFIX HEAD..$sha"
            } else {
                "$CHANGED_FILES_CMD_PREFIX $top $sha"
            }
        )
    }

    /**
     * checks the history to find the first merge CL.
     */
    override fun findPreviousSubmittedChange(): String? {
        return commandRunner.executeAndParse(PREVIOUS_SUBMITTED_CMD)
            .firstOrNull()
            ?.split(" ")
            ?.firstOrNull()
    }

    private fun findGitDirInParentFilepath(filepath: File): File? {
        var curDirectory: File = filepath
        while (curDirectory.path != "/") {
            if (File("$curDirectory/.git").exists()) {
                return curDirectory
            }
            curDirectory = curDirectory.parentFile
        }
        return null
    }

    private fun parseCommitLogString(
        commitLogString: String,
        commitStartDelimiter: String,
        commitSHADelimiter: String,
        subjectDelimiter: String,
        authorEmailDelimiter: String,
        localProjectDir: String
    ): List<Commit> {
        // Split commits string out into individual commits (note: this removes the deliminter)
        val gitLogStringList: List<String>? = commitLogString.split(commitStartDelimiter)
        var commitLog: MutableList<Commit> = mutableListOf()
        gitLogStringList?.filter { gitCommit ->
            gitCommit.trim() != ""
        }?.forEach { gitCommit ->
            commitLog.add(
                Commit(
                    gitCommit,
                    localProjectDir,
                    commitSHADelimiter = commitSHADelimiter,
                    subjectDelimiter = subjectDelimiter,
                    authorEmailDelimiter = authorEmailDelimiter
                )
            )
        }
        return commitLog.toList()
    }

    /**
     * Converts a diff log command into a [List<Commit>]
     *
     * @param gitCommitRange the [GitCommitRange] that defines the parameters of the git log command
     * @param keepMerges boolean for whether or not to add merges to the return [List<Commit>].
     * @param fullProjectDir a [File] object that represents the full project directory.
     */
    override fun getGitLog(
        gitCommitRange: GitCommitRange,
        keepMerges: Boolean,
        fullProjectDir: File
    ): List<Commit> {
        val commitStartDelimiter: String = "_CommitStart"
        val commitSHADelimiter: String = "_CommitSHA:"
        val subjectDelimiter: String = "_Subject:"
        val authorEmailDelimiter: String = "_Author:"
        val dateDelimiter: String = "_Date:"
        val bodyDelimiter: String = "_Body:"
        val localProjectDir: String = fullProjectDir.relativeTo(gitRoot).toString()
        val relativeProjectDir: String = fullProjectDir.relativeTo(workingDir).toString()

        var gitLogOptions: String =
            "--pretty=format:$commitStartDelimiter%n" +
                "$commitSHADelimiter%H%n" +
                "$authorEmailDelimiter%ae%n" +
                "$dateDelimiter%ad%n" +
                "$subjectDelimiter%s%n" +
                "$bodyDelimiter%b" +
                if (!keepMerges) {
                    " --no-merges"
                } else {
                    ""
                }
        var gitLogCmd: String
        if (gitCommitRange.fromExclusive != "") {
            gitLogCmd = "$GIT_LOG_CMD_PREFIX $gitLogOptions " +
                "${gitCommitRange.fromExclusive}..${gitCommitRange.untilInclusive}" +
                " -- ./$relativeProjectDir"
        } else {
            gitLogCmd = "$GIT_LOG_CMD_PREFIX $gitLogOptions ${gitCommitRange.untilInclusive} -n " +
                "${gitCommitRange.n} -- ./$relativeProjectDir"
        }
        val gitLogString: String = commandRunner.execute(gitLogCmd)
        val commits = parseCommitLogString(
            gitLogString,
            commitStartDelimiter,
            commitSHADelimiter,
            subjectDelimiter,
            authorEmailDelimiter,
            localProjectDir
        )
        if (commits.isEmpty()) {
            // Probably an error; log this
            logger?.warn(
                "No git commits found! Ran this command: '" +
                    gitLogCmd + "' and received this output: '" + gitLogString + "'"
            )
        }
        return commits
    }

    private class RealCommandRunner(
        private val workingDir: File,
        private val logger: Logger?
    ) : GitClient.CommandRunner {
        override fun execute(command: String): String {
            val parts = command.split("\\s".toRegex())
            logger?.info("running command $command in $workingDir")
            val proc = ProcessBuilder(*parts.toTypedArray())
                .directory(workingDir)
                .redirectOutput(ProcessBuilder.Redirect.PIPE)
                .redirectError(ProcessBuilder.Redirect.PIPE)
                .start()

            // Read output, waiting for process to finish, as needed
            val stdout = proc
                .inputStream
                .bufferedReader()
                .readText()
            val stderr = proc
                .errorStream
                .bufferedReader()
                .readText()
            val message = stdout + stderr
            // wait potentially a little bit longer in case Git was waiting for us to
            // read its response before it exited
            proc.waitFor(10, TimeUnit.SECONDS)
            logger?.info("Response: $message")
            check(proc.exitValue() == 0) {
                "Nonzero exit value running git command. Response: $message"
            }
            return stdout
        }
        override fun executeAndParse(command: String): List<String> {
            val response = execute(command)
                .split(System.lineSeparator())
                .filterNot {
                    it.isEmpty()
                }
            return response
        }
    }

    companion object {
        const val PREVIOUS_SUBMITTED_CMD =
            "git log -1 --merges --oneline"
        const val CHANGED_FILES_CMD_PREFIX = "git diff --name-only"
        const val GIT_LOG_CMD_PREFIX = "git log --name-only"
    }
}
