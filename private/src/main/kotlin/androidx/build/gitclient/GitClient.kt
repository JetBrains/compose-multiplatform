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

package androidx.build.gitclient

import androidx.build.releasenotes.getBuganizerLink
import androidx.build.releasenotes.getChangeIdAOSPLink
import java.io.File
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.gradle.api.logging.Logger

interface GitClient {
    fun findChangedFilesSince(
        sha: String,
        top: String = "HEAD",
        includeUncommitted: Boolean = false
    ): List<String>
    fun findPreviousSubmittedChange(): String?

    fun getGitLog(
        gitCommitRange: GitCommitRange,
        keepMerges: Boolean,
        fullProjectDir: File
    ): List<Commit>

    /**
     * Abstraction for running execution commands for testability
     */
    interface CommandRunner {
        /**
         * Executes the given shell command and returns the stdout as a string.
         */
        fun execute(command: String): String
        /**
         * Executes the given shell command and returns the stdout by lines.
         */
        fun executeAndParse(command: String): List<String>
    }

    companion object {
        fun getChangeInfoPath(project: Project): Provider<String> {
            return project.providers.environmentVariable("CHANGE_INFO").orElse("")
        }
        fun getManifestPath(project: Project): Provider<String> {
            return project.providers.environmentVariable("MANIFEST").orElse("")
        }
        fun create(
            rootProjectDir: File,
            logger: Logger,
            changeInfoPath: String,
            manifestPath: String
        ): GitClient {
            if (changeInfoPath != "") {
                if (manifestPath == "") {
                    throw GradleException("Setting CHANGE_INFO requires also setting MANIFEST")
                }
                val changeInfoFile = File(changeInfoPath)
                val manifestFile = File(manifestPath)
                if (!changeInfoFile.exists()) {
                    throw GradleException("changeinfo file $changeInfoFile does not exist")
                }
                if (!manifestFile.exists()) {
                    throw GradleException("manifest $manifestFile does not exist")
                }
                val changeInfoText = changeInfoFile.readText()
                val manifestText = manifestFile.readText()
                logger.info("Using ChangeInfoGitClient with change info path $changeInfoPath, " +
                    "manifest $manifestPath")
                return ChangeInfoGitClient(changeInfoText, manifestText)
            }
            logger.info("UsingGitRunnerGitClient")
            return GitRunnerGitClient(rootProjectDir, logger)
        }
    }
}

enum class CommitType {
    NEW_FEATURE, API_CHANGE, BUG_FIX, EXTERNAL_CONTRIBUTION;
    companion object {
        fun getTitle(commitType: CommitType): String {
            return when (commitType) {
                NEW_FEATURE -> "New Features"
                API_CHANGE -> "API Changes"
                BUG_FIX -> "Bug Fixes"
                EXTERNAL_CONTRIBUTION -> "External Contribution"
            }
        }
    }
}

/**
 * Defines the parameters for a git log command
 *
 * @property fromExclusive the oldest SHA at which the git log starts. Set to an empty string to use
 * [n]
 * @property untilInclusive the latest SHA included in the git log.  Defaults to HEAD
 * @property n a count of how many commits to go back to.  Only used when [fromExclusive] is an
 * empty string
 */
data class GitCommitRange(
    val fromExclusive: String = "",
    val untilInclusive: String = "HEAD",
    val n: Int = 0
)

/**
 * Class implementation of a git commit.  It uses the input delimiters to parse the commit
 *
 * @property formattedCommitText a string representation of a git commit
 * @property projectDir the project directory for which to parse file paths from a commit
 * @property commitSHADelimiter the term to use to search for the commit SHA
 * @property subjectDelimiter the term to use to search for the subject (aka commit summary)
 * @property changeIdDelimiter the term to use to search for the change-id in the body of the commit
 *           message
 * @property authorEmailDelimiter the term to use to search for the author email
 */
data class Commit(
    val formattedCommitText: String,
    val projectDir: String,
    private val commitSHADelimiter: String = "_CommitSHA:",
    private val subjectDelimiter: String = "_Subject:",
    private val authorEmailDelimiter: String = "_Author:"
) {
    private val changeIdDelimiter: String = "Change-Id:"
    var bugs: MutableList<Int> = mutableListOf()
    var files: MutableList<String> = mutableListOf()
    var sha: String = ""
    var authorEmail: String = ""
    var changeId: String = ""
    var summary: String = ""
    var type: CommitType = CommitType.BUG_FIX
    var releaseNote: String = ""
    private val releaseNoteDelimiters: List<String> = listOf(
        "Relnote:"
    )

    init {
        val listedCommit: List<String> = formattedCommitText.split('\n')
        listedCommit.filter { line -> line.trim() != "" }.forEach { line ->
            processCommitLine(line)
        }
    }

    private fun processCommitLine(line: String) {
        if (commitSHADelimiter in line) {
            getSHAFromGitLine(line)
            return
        }
        if (subjectDelimiter in line) {
            getSummary(line)
            return
        }
        if (changeIdDelimiter in line) {
            getChangeIdFromGitLine(line)
            return
        }
        if (authorEmailDelimiter in line) {
            getAuthorEmailFromGitLine(line)
            return
        }
        if ("Bug:" in line ||
            "b/" in line ||
            "bug:" in line ||
            "Fixes:" in line ||
            "fixes b/" in line
        ) {
            getBugsFromGitLine(line)
            return
        }
        releaseNoteDelimiters.forEach { delimiter ->
            if (delimiter in line) {
                getReleaseNotesFromGitLine(line, formattedCommitText)
                return
            }
        }
        if (projectDir.trim('/') in line) {
            getFileFromGitLine(line)
            return
        }
    }

    private fun isExternalAuthorEmail(authorEmail: String): Boolean {
        return !(authorEmail.contains("@google.com"))
    }

    /**
     * Parses SHAs from git commit line, with the format:
     * [Commit.commitSHADelimiter] <commitSHA>
     */
    private fun getSHAFromGitLine(line: String) {
        sha = line.substringAfter(commitSHADelimiter).trim()
    }

    /**
     * Parses subject from git commit line, with the format:
     * [Commit.subjectDelimiter]<commit subject>
     */
    private fun getSummary(line: String) {
        summary = line.substringAfter(subjectDelimiter).trim()
    }

    /**
     * Parses commit Change-Id lines, with the format:
     * `commit.changeIdDelimiter` <changeId>
     */
    private fun getChangeIdFromGitLine(line: String) {
        changeId = line.substringAfter(changeIdDelimiter).trim()
    }

    /**
     * Parses commit author lines, with the format:
     * [Commit.authorEmailDelimiter]email@google.com
     */
    private fun getAuthorEmailFromGitLine(line: String) {
        authorEmail = line.substringAfter(authorEmailDelimiter).trim()
        if (isExternalAuthorEmail(authorEmail)) {
            type = CommitType.EXTERNAL_CONTRIBUTION
        }
    }

    /**
     * Parses filepath to get changed files from commit, with the format:
     * {project_directory}/{filepath}
     */
    private fun getFileFromGitLine(filepath: String) {
        files.add(filepath.trim())
        if (filepath.contains("current.txt") && type != CommitType.EXTERNAL_CONTRIBUTION) {
            type = CommitType.API_CHANGE
        }
    }

    /**
     *  Parses bugs from a git commit message line
     */
    private fun getBugsFromGitLine(line: String) {
        var formattedLine = line.replace("b/", " ")
        formattedLine = formattedLine.replace(":", " ")
        formattedLine = formattedLine.replace(",", " ")
        var words: List<String> = formattedLine.split(' ')
        words.forEach { word ->
            var possibleBug: Int? = word.toIntOrNull()
            if (possibleBug != null && possibleBug > 1000) {
                bugs.add(possibleBug)
            }
        }
    }

    /**
     * Reads in the release notes field from the git commit message line
     *
     * They can have a couple valid formats:
     *
     * `Release notes: This is a one-line release note`
     * `Release Notes: "This is a multi-line release note.  This accounts for the use case where
     *                  the commit cannot be explained in one line"
     * `release notes: "This is a one-line release note.  The quotes can be used this way too"`
     */
    private fun getReleaseNotesFromGitLine(line: String, formattedCommitText: String) {
        /* Account for the use of quotes in a release note line
         * No quotes in the Release Note line means it's a one-line release note
         * If there are quotes, assume it's a multi-line release note
         */
        var quoteCountInRelNoteLine: Int = 0
        line.forEach { character ->
            if (character == '"') { quoteCountInRelNoteLine++ }
        }
        if (quoteCountInRelNoteLine == 0) {
            getOneLineReleaseNotesFromGitLine(line)
        } else {
            releaseNoteDelimiters.forEach { delimiter ->
                if (delimiter in line) {
                    // Find the starting quote of the release notes quote block
                    var releaseNoteStartIndex = formattedCommitText.lastIndexOf(delimiter)
                    + delimiter.length
                    releaseNoteStartIndex = formattedCommitText.indexOf('"', releaseNoteStartIndex)
                    // Move to the character after the first quote
                    if (formattedCommitText[releaseNoteStartIndex] == '"') {
                        releaseNoteStartIndex++
                    }
                    // Find the ending quote of the release notes quote block
                    var releaseNoteEndIndex = releaseNoteStartIndex + 1
                    releaseNoteEndIndex = formattedCommitText.indexOf('"', releaseNoteEndIndex)
                    // If there is no closing quote, just use the first line
                    if (releaseNoteEndIndex < 0) {
                        getOneLineReleaseNotesFromGitLine(line)
                        return
                    }
                    releaseNote = formattedCommitText.substring(
                        startIndex = releaseNoteStartIndex,
                        endIndex = releaseNoteEndIndex
                    ).trim()
                }
            }
        }
    }

    private fun getOneLineReleaseNotesFromGitLine(line: String) {
        releaseNoteDelimiters.forEach { delimiter ->
            if (delimiter in line) {
                releaseNote = line.substringAfter(delimiter).trim(' ', '"')
                return
            }
        }
    }

    fun getReleaseNoteString(): String {
        var releaseNoteString: String = releaseNote
        releaseNoteString += " ${getChangeIdAOSPLink(changeId)}"
        bugs.forEach { bug ->
            releaseNoteString += " ${getBuganizerLink(bug)}"
        }
        return releaseNoteString
    }

    override fun toString(): String {
        var commitString: String = summary
        commitString += " ${getChangeIdAOSPLink(changeId)}"
        bugs.forEach { bug ->
            commitString += " ${getBuganizerLink(bug)}"
        }
        return commitString
    }
}
