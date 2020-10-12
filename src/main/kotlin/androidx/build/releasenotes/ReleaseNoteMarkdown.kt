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
import androidx.build.gitclient.CommitType
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Classes for generating androidx release note specific markdown
 */

/**
 * Markdown class for a Library Header in the format:
 * ### Version <version> {:#<version>}
 */
class LibraryHeader(groupId: String, version: String) : MarkdownHeader() {
    init {
        markdownType = HeaderType.H3
        text = "$groupId Version $version {:#$version}"
    }
}

/**
 * Generates the markdown list of commits with sections defined by enum [CommitType], in the format:
 *
 * **New Features**
 *
 * - <[Commit.summary]> <[getChangeIdAOSPLink]> <[getBuganizerLink] 1> <[getBuganizerLink] 2>...
 *
 * **API Changes**
 *
 * - <[Commit.summary]> <[getChangeIdAOSPLink]> <[getBuganizerLink] 1> <[getBuganizerLink] 2>...
 *
 * **Bug Fixes**
 *
 * - <[Commit.summary]> <[getChangeIdAOSPLink]> <[getBuganizerLink] 1> <[getBuganizerLink] 2>...
 *
 * **External Contribution**
 *
 * - <[Commit.summary]> <[getChangeIdAOSPLink]> <[getBuganizerLink] 1> <[getBuganizerLink] 2>...
 *
 */
class CommitMarkdownList(
    private var includeAllCommits: Boolean = false
) {
    private var commits: MutableList<Commit> = mutableListOf()

    fun add(commit: Commit) {
        commits.add(commit)
    }

    fun getListItemStr(): String {
        return "- "
    }

    private fun makeReleaseNotesSection(sectionCommitType: CommitType): String {
        var sectionHeader: MarkdownBoldText = MarkdownBoldText(
            CommitType.getTitle(sectionCommitType)
        )
        var markdownStringSection: String = ""
        commits.filter { commit ->
            commit.type == sectionCommitType
        }.forEach { commit ->
            // While we are choosing to ignore Release Note field
            val commitString: String = getListItemStr() + if (commit.releaseNote.isNotEmpty())
                commit.getReleaseNoteString() else commit.toString()
            if (includeAllCommits || commit.releaseNote.isNotEmpty()) {
                markdownStringSection = markdownStringSection + commitString
                if (markdownStringSection.last() != '\n') {
                    markdownStringSection += '\n'
                }
            }
            /* If we are not ignoring Release Note fields (meaning we are respecting it) and
             * the commit does not contain a Release Note field, then don't include the commit
             * in the release notes.
             */
        }
        markdownStringSection = if (markdownStringSection.isEmpty()) {
            "\n${MarkdownComment(sectionHeader.toString())}\n\n$markdownStringSection"
        } else {
            "\n$sectionHeader\n\n$markdownStringSection"
        }
        return markdownStringSection
    }

    @Override
    override fun toString(): String {
        var markdownString: String = ""
        CommitType.values().forEach { commitType ->
            markdownString += makeReleaseNotesSection(commitType)
        }
        return markdownString
    }

    fun print() {
        println(toString())
    }
}

/**
 * @param startSHA the SHA at which to start the diff log (exclusive)
 * @param endSHA the last SHA to include in the diff log (inclusive)
 * @param projectDir the local directory of the project, in relation to frameworks/support
 *
 * @return A [MarkdownLink] to the public Gitiles diff log
 */
fun getGitilesDiffLogLink(startSHA: String, endSHA: String, projectDir: String): MarkdownLink {
    val baseGitilesUrl: String =
        "https://android.googlesource.com/platform/frameworks/support/+log/"
    /* The root project directory is already existent in the url path, so the directory here
     * should be relative to frameworks/support/.
     */
    if (projectDir.contains("frameworks/support")) {
        throw RuntimeException(
            "Gitiles directory should only contain the directory structure" +
                "within frameworks/support/*, but received incorrect directory: $projectDir"
        )
    }
    // Remove extra preceeding directory slashes, if they exist
    var verifiedProjectDir = projectDir
    while (verifiedProjectDir.first() == '/') {
        verifiedProjectDir = verifiedProjectDir.removePrefix("/")
    }
    var gitilesLink: MarkdownLink = MarkdownLink()
    gitilesLink.linkText = "here"
    gitilesLink.linkUrl = "$baseGitilesUrl$startSHA..$endSHA/$verifiedProjectDir"
    return gitilesLink
}

/**
 * @param changeId The Gerrit Change-Id to link to
 * @return A [MarkdownLink] to AOSP Gerrit
 */
fun getChangeIdAOSPLink(changeId: String): MarkdownLink {
    val baseAOSPUrl: String = "https://android-review.googlesource.com/#/q/"
    var aospLink: MarkdownLink = MarkdownLink()
    aospLink.linkText = changeId.take(6)
    aospLink.linkUrl = "$baseAOSPUrl$changeId"
    return aospLink
}

/**
 * @param bugId the Id of the buganizer issue
 * @return A [MarkdownLink] to the public buganizer issue tracker
 *
 * Note: This method does not check if the bug is public
 */
fun getBuganizerLink(bugId: Int): MarkdownLink {
    val baseBuganizerUrl: String = "https://issuetracker.google.com/issues/"
    var buganizerLink: MarkdownLink = MarkdownLink()
    buganizerLink.linkText = "b/$bugId"
    buganizerLink.linkUrl = "$baseBuganizerUrl$bugId"
    return buganizerLink
}

/**
 * Data class to contain an array of LibraryReleaseNotes when serializing collections of release
 * notes
 */
data class LibraryReleaseNotesList(
    val list: MutableList<LibraryReleaseNotes> = mutableListOf()
)

/**
 * Structured release notes class, that connects all parts of the release notes.  Create release
 * notes in the format:
 * <pre>
 * <[LibraryHeader]>
 * <Date>
 *
 * `androidx.<groupId>:<artifactId>:<version>` is released.  The commits included in this version
 * can be found <[MarkdownLink]>.
 *
 *  <[CommitMarkdownList]>
 * </pre>
 *
 * @property groupId Library GroupId.
 * @property artifactIds List of ArtifactIds included in these release notes.
 * @property version Version of the library, assuming all artifactIds have the same version.
 * @property releaseDate Date the release will go live.  Defaults to the current date.
 * @property fromSHA The oldest SHA to include in the release notes.
 * @property untilSHA The newest SHA to be included in the release notes.
 * @property projectDir The filepath relative to the parent directory of the .git directory.
 * @property commitList The initial list of Commits to include in these release notes.  Defaults to an
 *           empty list.  Users can always add more commits with [LibraryReleaseNotes.addCommit]
 * @property requiresSameVersion True if the groupId of this module requires the same version for
 *           all artifactIds in the groupId.  When true, uses the GroupId for the release notes
 *           header.  When false, uses the list of artifactIds for the header.
 * @param includeAllCommits Set to true to include all commits, both with and without a
 *          release note field in the commit message.  Defaults to false, which means only commits
 *          with a release note field are included in the release notes.
 */
class LibraryReleaseNotes(
    val groupId: String,
    val artifactIds: MutableList<String>,
    val version: String,
    val releaseDate: LocalDate,
    val fromSHA: String,
    val untilSHA: String,
    val projectDir: String,
    val commitList: List<Commit> = listOf(),
    val requiresSameVersion: Boolean,
    includeAllCommits: Boolean = false
) {
    private var diffLogLink: MarkdownLink
    private var header: LibraryHeader
    private var commits: MutableList<Commit> = mutableListOf()
    private var commitMarkdownList: CommitMarkdownList = CommitMarkdownList(includeAllCommits)
    private var summary: String = ""
    private var bugsFixed: MutableList<Int> = mutableListOf()

    init {
        if (version == "" || groupId == "") {
            throw RuntimeException(
                "Tried to create Library Release Notes Header without setting" +
                    "the groupId or version!"
            )
        }
        if (fromSHA == "" || untilSHA == "") {
            throw RuntimeException("Tried to create Library Release Notes with an empty SHA!")
        }
        header = if (requiresSameVersion) {
            LibraryHeader(groupId, version)
        } else {
            LibraryHeader(artifactIds.joinToString(), version)
        }
        diffLogLink = getGitilesDiffLogLink(fromSHA, untilSHA, projectDir)
        if (commitList.isNotEmpty()) {
            commitList.forEach { commit ->
                addCommit(commit)
            }
        }
    }

    fun getFormattedDate(): String {
        val formatter = DateTimeFormatter.ofPattern("MMMM d, yyyy")
        return formatter.format(releaseDate)
    }

    fun getFormattedReleaseSummary(): String {
        val numberArtifacts = artifactIds.size
        for (i: Int in 0..(numberArtifacts - 1)) {
            var currentArtifactId: String = artifactIds[i]
            when (numberArtifacts) {
                1 -> {
                    summary = "`$groupId:$currentArtifactId:$version` is released.  "
                }
                2 -> {
                    if (i == 0) {
                        summary = "`$groupId:$currentArtifactId:$version` and "
                    }
                    if (i == 1) {
                        summary += "`$groupId:$currentArtifactId:$version` are released. "
                    }
                }
                else -> {
                    if (i < numberArtifacts - 1) {
                        summary += "`$groupId:$currentArtifactId:$version`, "
                    } else {
                        summary += "and `$groupId:$currentArtifactId:$version` are released. "
                    }
                }
            }
        }

        summary += "The commits included in this version can be found $diffLogLink.\n"
        return summary
    }

    fun addCommit(newCommit: Commit) {
        newCommit.bugs.forEach { bug ->
            bugsFixed.add(bug)
        }
        commits.add(newCommit)
        commitMarkdownList.add(newCommit)
    }

    override fun toString(): String {
        return "$header\n" +
            "${getFormattedDate()}\n\n" +
            getFormattedReleaseSummary() +
            "$commitMarkdownList"
    }
}
