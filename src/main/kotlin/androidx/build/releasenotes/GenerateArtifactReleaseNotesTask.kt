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

import androidx.build.AndroidXExtension
import androidx.build.getReleaseNotesDirectory
import androidx.build.gitclient.Commit
import androidx.build.gitclient.GitClientImpl
import androidx.build.gitclient.GitCommitRange
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import org.gradle.api.DefaultTask
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import java.io.File
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Task for Generating Release notes for this specific project, based on a <from> SHA (inclusive)
 * and an <end> SHA (exclusive).
 * It outputs release notes in the format of developer.android.com.  See [LibraryReleaseNotes] for
 * more info about the formatting.
 */
abstract class GenerateArtifactReleaseNotesTask : DefaultTask() {

    init {
        group = "Documentation"
        description = "Task for creating release notes for a specific library module (artifactId)" +
                "as defined in the artifactToCommitMap file."
    }

    @Input
    var fromSHA: String = ""
    @Input
    var untilSHA: String = ""
    @Input
    var date: LocalDate = LocalDate.now()
    @Input
    var keepMerges: Boolean = false
    @Input
    var includeAllCommits: Boolean = false

    /** ArtifactToCommitMap file */
    @get:Input
    abstract val artifactToCommitMapFile: Property<File>

    @OutputFile
    val outputFile: Property<File> = project.objects.property(File::class.java)

    @OutputFile
    val outputJsonFile: Property<File> = project.objects.property(File::class.java)

    @OutputDirectory
    val outputDirectory: Property<File> = project.objects.property(File::class.java)

    /**
     * @return the local project directory without the full framework/support root directory path
     * Example input: /Users/<username>/androidx-master-dev/frameworks/support/core/core
     * Example output: /core/core
     */
    private fun getProjectRelPath(): String {
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

    /** Returns whether or not the groupId of the project requires the same version for all
     * artifactIds.  See CheckSameVersionLibraryGroupsTask.kt
     */
    private fun requiresSameVersion(): Boolean {
        val library = project.extensions.findByType(AndroidXExtension::class.java)
        return library?.mavenGroup?.requireSameVersion ?: false
    }

    /** Writes the release notes for the artifactId into the artifact-specific release notes files.
     * Outputs a regular .txt release notes file and a .json file with the serialized
     * LibraryReleaseNotes class for consumption by the [GenerateAllReleaseNotesTask] task
     */
    private fun writeReleaseNotesToFile(artifactReleaseNotesList: LibraryReleaseNotesList) {
        if (!project.getReleaseNotesDirectory().exists()) {
            if (!project.getReleaseNotesDirectory().mkdirs()) {
                throw RuntimeException("Failed to create " +
                        "output directory: ${project.getReleaseNotesDirectory()}")
            }
        }
        val resolvedOutputDirectory: File = outputDirectory.get()
        if (!resolvedOutputDirectory.exists()) {
            if (!resolvedOutputDirectory.mkdirs()) {
                throw RuntimeException("Failed to create " +
                        "output directory: $resolvedOutputDirectory")
            }
        }
        val resolvedOutputFile: File = outputFile.get()
        if (!resolvedOutputFile.exists()) {
            if (!resolvedOutputFile.createNewFile()) {
                throw RuntimeException("Failed to create " +
                        "output dependency dump file: $outputFile")
            }
        }
        val resolvedOutputJsonFile: File = outputJsonFile.get()
        if (!resolvedOutputJsonFile.exists()) {
            if (!resolvedOutputJsonFile.createNewFile()) {
                throw RuntimeException("Failed to create " +
                        "output dependency dump file: $resolvedOutputJsonFile")
            }
        }
        val releaseNotesStringList: MutableList<String> = mutableListOf()
        artifactReleaseNotesList.list.forEach { releaseNote ->
            releaseNotesStringList.add(releaseNote.toString())
        }

        // Create json object from the LibraryReleaseNotes instance
        val gson: Gson = GsonBuilder().setPrettyPrinting().create()
        val serializedInfo: String = gson.toJson(artifactReleaseNotesList)

        resolvedOutputFile.writeText(releaseNotesStringList.joinToString("\n\n"))
        resolvedOutputJsonFile.writeText(serializedInfo)
    }

    private fun getReleaseNotesArtifactToCommitMapFile(): File {
        val artifactToCommitMapFile: File = artifactToCommitMapFile.get()
        if (!artifactToCommitMapFile.exists()) {
            throw RuntimeException("You must include a artifactToCommitMap File that maps " +
                    "groupId:artifactId to a commit range.  You can pass this to this task " +
                    "using the argument -PartifactToCommitMap=<YourFile>")
        }
        return artifactToCommitMapFile
    }

    private fun getReleaseNotesArtifactToCommitMap(): ArtifactToCommitMap {
        val artifactToCommitMapFile: File = getReleaseNotesArtifactToCommitMapFile()
        val artifactToCommitMapJsonString: String = artifactToCommitMapFile.readText(Charsets.UTF_8)
        return Gson().fromJson(artifactToCommitMapJsonString, ArtifactToCommitMap::class.java)
    }

    private fun getArtifactReleaseNotesInfoList(
        artifactToCommitMap: ArtifactToCommitMap
    ): MutableList<ArtifactReleaseNotesInfo?> {
        val artifactReleaseNotesInfoList: MutableList<ArtifactReleaseNotesInfo?> = mutableListOf()
        artifactToCommitMap.modules.forEach { module ->
            if (module.groupId == project.group.toString() &&
                module.artifactId == project.name.toString()
            ) {
                artifactReleaseNotesInfoList.add(module)
            }
        }
        return artifactReleaseNotesInfoList
    }

    private fun createVersionSpecificReleaseNotes(
        artifactReleaseNotesInfo: ArtifactReleaseNotesInfo
    ): LibraryReleaseNotes? {
        // If there are is no fromCommit specified for this artifact, then simply return because
        // we don't know how far back to query the commit log
        if (artifactReleaseNotesInfo.fromCommit == "NULL") {
            logger.warn("WARNING: Project ${project.group}:${project.name} has no SHA specified" +
                    "for the 'fromSHA', so release cannot be generated for it.")
            return null
        } else {
            fromSHA = artifactReleaseNotesInfo.fromCommit
        }
        untilSHA = if (
            artifactReleaseNotesInfo.untilCommit == "" ||
            artifactReleaseNotesInfo.untilCommit == "NULL"
        ) {
            "HEAD"
        } else {
            artifactReleaseNotesInfo.untilCommit
        }

        val commitList: List<Commit> = GitClientImpl(project.rootDir).getGitLog(
            GitCommitRange(
                fromExclusive = fromSHA,
                untilInclusive = untilSHA
            ),
            keepMerges,
            project.projectDir
        )

        if (commitList.isEmpty()) {
            logger.warn("WARNING: Found no commits for ${project.group}:${project.name} from " +
                    "start SHA $fromSHA to end SHA $untilSHA.  To double check, you can run " +
                    "`git log --no-merges $fromSHA..$untilSHA ${project.projectDir}`")
        }

        return LibraryReleaseNotes(
            project.group.toString(),
            mutableListOf(project.name.toString()),
            artifactReleaseNotesInfo.version,
            date,
            fromSHA,
            untilSHA,
            getProjectRelPath(),
            commitList.filter { !isExcludedAuthorEmail(it.authorEmail) },
            artifactReleaseNotesInfo.requiresSameVersion,
            includeAllCommits
        )
    }

    /**
     * Given the [GenerateArtifactReleaseNotesTask.fromSHA],
     * [GenerateArtifactReleaseNotesTask.untilSHA], and [GenerateArtifactReleaseNotesTask.date],
     * creates release notes for this specific project, starting at
     * [GenerateArtifactReleaseNotesTask.fromSHA] and ending at
     * [GenerateArtifactReleaseNotesTask.untilSHA].
     */
    @TaskAction
    fun createReleaseNotes() {
        val artifactToCommitMap: ArtifactToCommitMap = getReleaseNotesArtifactToCommitMap()

        val artifactReleaseNotesInfoList: MutableList<ArtifactReleaseNotesInfo?> =
            getArtifactReleaseNotesInfoList(artifactToCommitMap)
        // If there are no release notes requested for this artifact, then simply return
        if (artifactReleaseNotesInfoList.size == 0) return

        val formatter = DateTimeFormatter.ofPattern("MM-d-yyyy")
        date = if (artifactToCommitMap.releaseDate == "") {
            LocalDate.now()
        } else {
            print("artifactToCommitMap.releaseDate: ${artifactToCommitMap.releaseDate}\n")
            LocalDate.parse(artifactToCommitMap.releaseDate, formatter)
        }
        /* includeAllCommits: For use during the migration to using release note fields,
         * or in the case where a team forgot to include a release notes field in their commit
         * messages
         */
        includeAllCommits = artifactToCommitMap.includeAllCommits

        val artifactReleaseNotesList: LibraryReleaseNotesList = LibraryReleaseNotesList()
        artifactReleaseNotesInfoList.forEach { artifactVersionReleaseNotesInfo ->
            if (artifactVersionReleaseNotesInfo != null) {
                val artifactVersionReleaseNotes: LibraryReleaseNotes? =
                    createVersionSpecificReleaseNotes(artifactVersionReleaseNotesInfo)
                if (artifactVersionReleaseNotes != null) {
                    artifactReleaseNotesList.list.add(artifactVersionReleaseNotes)
                }
            }
        }
        writeReleaseNotesToFile(artifactReleaseNotesList)
    }
}