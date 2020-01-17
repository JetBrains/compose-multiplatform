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

import androidx.build.getReleaseNotesDirectory
import androidx.build.gitclient.Commit
import com.google.gson.Gson
import org.gradle.api.DefaultTask
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import java.io.File
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Task for Generating Release notes in aggregate.  This task relies on the
 * [GenerateArtifactReleaseNotesTask] tasks for each project, each of which generate a
 * $group/${group}_${name}_release_notes.json file, which is the serialized form of
 * a [LibraryReleaseNotes] objects.  It iterates over all directories and then collects and
 * aggregates the release notes into groupId release note files and
 * androidx_aggregate_release_notes.txt
 */
abstract class GenerateAllReleaseNotesTask : DefaultTask() {

    init {
        group = "Documentation"
        description = "Creates release notes for all the libraries defined in the " +
                "artifactToCommitMap file"
    }

    @OutputFile
    val aggregateReleaseNotesFile = File(
        project.getReleaseNotesDirectory(),
        "androidx_aggregate_release_notes.txt"
    )

    /** ArtifactToCommitMap file, which contains all the release information */
    @get:Input
    abstract val artifactToCommitMapFile: Property<File>

    /** List of each artifact .json file for all artifactIds. */
    @get:Input
    abstract val artifactReleaseNoteFiles: ListProperty<File>

    /** List of each artifact output directory for all artifactIds */
    @get:Input
    abstract val artifactReleaseNoteOutputDirectories: SetProperty<File>

    /**
     * Check if an email address is a robot
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

    /** Merges CommitListB into CommitListA and removes duplicates.
     */
    private fun mergeCommitListBIntoCommitListA(
        commitListA: MutableList<Commit>,
        commitListB: List<Commit>
    ) {
        val commitListAShaSet: MutableSet<String> = mutableSetOf()
        commitListA.forEach { commit ->
            commitListAShaSet.add(commit.sha)
        }

        commitListB.forEach { commitB ->
            if (!commitListAShaSet.contains(commitB.sha)) {
                commitListA.add(commitB)
            }
        }
    }

    /** Writes the release notes for the groupId to the individual groupId release notes file
     */
    private fun writeGroupReleaseNotesToFile(
        groupReleaseNotesFile: File,
        releaseNotesString: String
    ) {
        // Make sure all files exist before attempting to write to them
        if (!project.getReleaseNotesDirectory().exists()) {
            if (!project.getReleaseNotesDirectory().mkdirs()) {
                throw RuntimeException("Failed to create " +
                        "output directory: ${project.getReleaseNotesDirectory()}")
            }
        }
        if (!groupReleaseNotesFile.exists()) {
            if (!groupReleaseNotesFile.createNewFile()) {
                throw RuntimeException("Failed to create " +
                        "output group Release Notes file: $groupReleaseNotesFile")
            }
        }
        groupReleaseNotesFile.writeText(releaseNotesString)
    }

    /** Writes all the release notes to the aggregate release notes file that
     * contains all release notes.
     */
    private fun writeAggregateReleaseNotesToFile(releaseNotesString: String) {
        // Make sure all files exist before attempting to write to them
        if (!project.getReleaseNotesDirectory().exists()) {
            if (!project.getReleaseNotesDirectory().mkdirs()) {
                throw RuntimeException("Failed to create " +
                        "output directory: ${project.getReleaseNotesDirectory()}")
            }
        }
        if (!aggregateReleaseNotesFile.exists()) {
            if (!aggregateReleaseNotesFile.createNewFile()) {
                throw RuntimeException("Failed to create " +
                        "output group aggregate Release Notes File: $aggregateReleaseNotesFile")
            }
        }

        // Always append to the aggregate Release Notes File
        aggregateReleaseNotesFile.writeText(releaseNotesString)
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

    /** Iterates over the LibraryReleaseNotes list and creates a map of project version to List of
     * LibraryReleaseNotes. Thus, each artifactId of the same version will be collected together
     * as list for that version. This is done so that release notes can be collected for all
     * artifactIds of the same version.
     *
     * This method assumes all artifactIds are in the same groupId.
     *
     * @param libraryReleaseNotesList The list artifactId release notes in the same groupId
     */
    private fun getVersionToReleaseNotesMap(
        libraryReleaseNotesList: MutableList<LibraryReleaseNotes>
    ): MutableMap<String, MutableList<LibraryReleaseNotes>> {
        val versionToArtifactRNMap: MutableMap<String,
                MutableList<LibraryReleaseNotes>> = mutableMapOf()
        libraryReleaseNotesList.forEach { artifactRN ->
            if (versionToArtifactRNMap.containsKey(artifactRN.version)) {
                versionToArtifactRNMap[artifactRN.version]?.add(artifactRN)
            } else {
                versionToArtifactRNMap[artifactRN.version] = mutableListOf(artifactRN)
            }
        }
        return versionToArtifactRNMap
    }

    /** Creates the groupId release notes using the list of artifactId LibraryReleaseNotes
     * Groups artifactIds of the same version.
     *
     * @param libraryReleaseNotesList The list of artifactId [LibraryReleaseNotes] objects which
     *          are read in form the artifactId release note .json files
     * @param releaseDate The release date of the entire release
     * @param includeAllCommits Set to true to include all commits regardless of whether or not they
     *          have the release notes tag
     */
    private fun createGroupIdReleaseNotes(
        libraryReleaseNotesList: MutableList<LibraryReleaseNotes>,
        releaseDate: LocalDate,
        includeAllCommits: Boolean
    ): String {
        val versionToArtifactReleaseNotesMap: MutableMap<String, MutableList<LibraryReleaseNotes>> =
            getVersionToReleaseNotesMap(libraryReleaseNotesList)

        // Create an output groupId file
        val groupReleaseNotesFile = File(
            project.getReleaseNotesDirectory(),
            "${libraryReleaseNotesList[0].groupId}/" +
                    "${libraryReleaseNotesList[0].groupId}_release_notes.txt"
        )

        val requiresSameVersion: Boolean = libraryReleaseNotesList[0].requiresSameVersion
        val groupReleaseNotesStringList: MutableList<String> = mutableListOf()

        // For each version, collect and write the release notes for all artifactIds of that version
        versionToArtifactReleaseNotesMap.forEach { (version, versionRNList) ->
            val versionArtifactIds: MutableList<String> = mutableListOf()
            val versionGroupCommitList: MutableList<Commit> = mutableListOf()
            var fromSHA: String = ""
            var untilSHA: String = ""
            var groupIdCommonDir: String = versionRNList[0].projectDir
            versionRNList.forEach { artifactReleaseNotes ->
                versionArtifactIds.add(artifactReleaseNotes.artifactIds[0])
                mergeCommitListBIntoCommitListA(
                    commitListA = versionGroupCommitList,
                    commitListB = artifactReleaseNotes.commitList
                )
                fromSHA = artifactReleaseNotes.fromSHA
                untilSHA = artifactReleaseNotes.untilSHA
                groupIdCommonDir =
                    artifactReleaseNotes.projectDir.commonPrefixWith(groupIdCommonDir)
            }

            val releaseNotes = LibraryReleaseNotes(
                "androidx.${project.name}",
                versionArtifactIds,
                version,
                releaseDate,
                fromSHA,
                untilSHA,
                groupIdCommonDir,
                versionGroupCommitList.filter { !isExcludedAuthorEmail(it.authorEmail) },
                requiresSameVersion,
                includeAllCommits
            )

            groupReleaseNotesStringList.add(releaseNotes.toString())
        }
        writeGroupReleaseNotesToFile(
            groupReleaseNotesFile,
            groupReleaseNotesStringList.joinToString("\n\n")
        )
        return groupReleaseNotesStringList.joinToString("\n\n")
    }

    private fun convertJsonFileToLibraryReleaseNotesList(releaseNoteJsonFile: File):
            LibraryReleaseNotesList {
        val releaseNoteJsonString: String = releaseNoteJsonFile.readText(Charsets.UTF_8)
        return Gson().fromJson(releaseNoteJsonString, LibraryReleaseNotesList::class.java)
    }

    @TaskAction
    fun createReleaseNotes() {
        // Reset the aggregate release notes file to be empty
        if (!aggregateReleaseNotesFile.exists()) {
            if (!aggregateReleaseNotesFile.createNewFile()) {
                throw RuntimeException("Failed to create " +
                        "output group aggregate Release Notes File: $aggregateReleaseNotesFile")
            }
        }
        aggregateReleaseNotesFile.writeText("")

        // Retrieve the includeAllCommits boolean and the formatted date from the
        // ArtifactToCommitMapFile
        val artifactToCommitMap: ArtifactToCommitMap = getReleaseNotesArtifactToCommitMap()
        val formatter = DateTimeFormatter.ofPattern("MM-d-yyyy")
        val releaseDate: LocalDate = if (artifactToCommitMap.releaseDate == "") {
            LocalDate.now()
        } else {
            LocalDate.parse(artifactToCommitMap.releaseDate, formatter)
        }

        /* Loop through each output directory and collect the release notes from each specified
         * file in that output directory.  For each group, generate both the groupId version
         * file and append to the aggregate release notes file.
         */
        val allReleaseNotesStringList: MutableList<String> = mutableListOf()
        val releaseNotesFiles: MutableList<File> = artifactReleaseNoteFiles.get()
        for (releaseNotesDir: File in artifactReleaseNoteOutputDirectories.get()) {
            var groupReleaseNotesList: MutableList<LibraryReleaseNotes> = mutableListOf()
            releaseNotesDir.listFiles()?.filter { file ->
                file.exists() && file.length() > 0 && releaseNotesFiles.contains(file)
            }?.forEach { releaseNoteJsonFile ->
                convertJsonFileToLibraryReleaseNotesList(
                    releaseNoteJsonFile
                ).list.forEach { releaseNote ->
                    groupReleaseNotesList.add(releaseNote)
                }
            }
            if (groupReleaseNotesList.size > 0) {
                val releaseGroupNotesString: String = createGroupIdReleaseNotes(
                    groupReleaseNotesList,
                    releaseDate,
                    artifactToCommitMap.includeAllCommits
                )
                allReleaseNotesStringList.add(releaseGroupNotesString)
            }
        }

        writeAggregateReleaseNotesToFile(allReleaseNotesStringList.joinToString("\n\n"))
    }
}