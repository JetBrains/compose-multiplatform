/*
 * Copyright (C) 2019 The Android Open Source Project
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

package androidx.build.releasenotes

import androidx.build.gitclient.Commit
import androidx.build.gitclient.GitClientImpl
import androidx.build.getReleaseNotesDirectory
import androidx.build.gitclient.GitCommitRange
import org.gradle.api.DefaultTask
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import java.io.File
import java.time.LocalDate

/**
 * Task for Generating Release notes for this specific project, based on a start SHA and an end SHA.
 * It outputs release notes in the format of developer.android.com.  See [LibraryReleaseNotes] for
 * more info about the formatting.
 */
open class GenerateReleaseNotesTask : DefaultTask() {

    init {
        group = "Documentation"
        description = "Task for creating release notes for a specific library"
    }

    @Input
    lateinit var startSHA: String
    @Input
    lateinit var endSHA: String
    @Input
    lateinit var date: LocalDate
    @Input
    var keepMerges: Boolean = false
    @Input
    var includeAllCommits: Boolean = false

    @OutputFile
    val outputFile: Property<File> = project.objects.property(File::class.java)

    /**
     * @return the local project directory without the full framework/support root directory path
     * Example input: /Users/<username>/androidx-master-dev/frameworks/support/core/core
     * Example output: /core/core
     */
    private fun getProjectSpecificDirectory(): String {
        return project.projectDir.toString().removePrefix(project.rootDir.toString())
    }

    /**
     * Check if an email address is a robot
     *
     * @param authorEmail email to check
     */
    fun isExcludedAuthorEmail(authorEmail: String): Boolean {
        val excludedAuthorEmails = listOf(
            "treehugger-gerrit@google.com",
            "android-build-merger@google.com",
            "noreply-gerritcodereview@google.com"
        )
        return excludedAuthorEmails.contains(authorEmail)
    }

    private fun writeReleaseNotesToFile(releaseNotes: LibraryReleaseNotes) {
        if (!project.getReleaseNotesDirectory().exists()) {
            if (!project.getReleaseNotesDirectory().mkdirs()) {
                throw RuntimeException("Failed to create " +
                        "output directory: ${project.getReleaseNotesDirectory()}")
            }
        }
        var resolvedOutputFile: File = outputFile.get()
        if (!resolvedOutputFile.exists()) {
            if (!resolvedOutputFile.createNewFile()) {
                throw RuntimeException("Failed to create " +
                        "output dependency dump file: $outputFile")
            }
        }
        resolvedOutputFile.writeText(releaseNotes.toString())
    }

    /**
     * Given the [GenerateReleaseNotesTask.startSHA], [GenerateReleaseNotesTask.endSHA], and
     * [GenerateReleaseNotesTask.date], creates release notes for this specific project, starting
     * at [GenerateReleaseNotesTask.startSHA] and ending at [GenerateReleaseNotesTask.endSHA].
     */
    @TaskAction
    fun createReleaseNotes() {
        if (startSHA.isEmpty()) {
            throw RuntimeException("The generate release notes task need a start SHA from" +
                    "which to start generating release notes.  You can pass it a start SHA by" +
                    "adding the argument -PstartCommit=<yourSHA>")
        }

        val commitList: List<Commit> = GitClientImpl(project.rootDir).getGitLog(
            GitCommitRange(
                fromExclusive = startSHA,
                untilInclusive = endSHA
            ),
            keepMerges,
            project.projectDir
        )

        val releaseNotes = LibraryReleaseNotes(
            project.group.toString(),
            mutableListOf(project.name.toString()),
            project.version.toString(),
            date,
            startSHA,
            endSHA,
            getProjectSpecificDirectory(),
            includeAllCommits
        )

        if (commitList.isEmpty()) {
            logger.warn("WARNING: Found no commits for ${project.group}:${project.name} from " +
                    "start SHA $startSHA to end SHA $endSHA.  To double check, you can run " +
                    "`git log --no-merges $startSHA..$endSHA ${project.projectDir}`")
        }

        commitList.forEach { commit ->
            if (!isExcludedAuthorEmail(commit.authorEmail)) {
                releaseNotes.addCommit(commit)
            }
        }

        writeReleaseNotesToFile(releaseNotes)
    }
}