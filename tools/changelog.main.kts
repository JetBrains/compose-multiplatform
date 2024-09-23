/**
 * Script for creating a changelog. Call
 * ```
 * kotlin changelog.main.kts v1.6.0-rc02 release/1.6.0
 * ```
 * where v1.6.0-rc02 - the first commit
 * where release/1.6.0 - the last commit
 */

/**
 * Run from command line:
 * 1. Download https://github.com/JetBrains/kotlin/releases/tag/v1.9.22 and add `bin` to PATH
 * 2. Call `kotlin <fileName>`
 *
 * Run from IntelliJ:
 * 1. Right click on the script
 * 2. More Run/Debug
 * 3. Modify Run Configuration...
 * 4. Clear all "Before launch" tasks (you can edit the system-wide template as well)
 * 5. OK
 */

@file:Repository("https://repo1.maven.org/maven2/")
@file:DependsOn("com.google.code.gson:gson:2.10.1")

import com.google.gson.Gson
import java.io.File
import java.io.IOException
import java.net.URL
import java.util.concurrent.TimeUnit
import kotlin.math.max

val firstCommit = args.getOrNull(0) ?: error("Please call this way: kotlin changelog.main.kts <firstCommit> <lastCommit>")
val lastCommit = args.getOrNull(1) ?: error("Please call this way: kotlin changelog.main.kts <firstCommit> <lastCommit>")
val token = args.getOrNull(2)

val sectionOrder = listOf(
    "Highlights",
    "Known issues",
    "Breaking changes",
    "Features",
    "Fixes",
    null
)

val subsectionOrder = listOf(
    "Multiple Platforms",
    "iOS",
    "Desktop",
    "Web",
    "Resources",
    "Gradle Plugin",
    "Lifecycle",
    "Navigation",
    null
)

if (token == null) {
    println("To increase the rate limit, specify token (https://github.com/settings/tokens): kotlin changelog.main.kts <firstCommit> <lastCommit> TOKEN")
}

val entries = entriesForRepo("JetBrains/compose-multiplatform-core") +
        entriesForRepo("JetBrains/compose-multiplatform")

println("\n# CHANGELOG")

println(
    buildString {
        append("_Changes since ${commitToVersion(firstCommit)}_\n")
        append("\n")

        entries
            .sortedBy { it.sectionOrder() }
            .groupBy { it.sectionName() }
            .forEach { (section, sectionEntries) ->
                appendLine("## $section")

                sectionEntries
                    .sortedBy { it.subsectionOrder() }
                    .groupBy { it.subsectionName() }
                    .forEach { (subsection, subsectionEntries) ->
                        appendLine("### $subsection")
                        subsectionEntries.forEach {
                            appendLine(it.format())
                        }
                        appendLine()
                    }
            }
    }
)

/**
 * Transforms v1.6.0-beta01 to 1.6.0-beta01
 */
fun commitToVersion(commit: String) =
    if (commit.startsWith("v") && commit.contains(".")) {
        commit.removePrefix("v")
    } else {
        commit
    }

/**
 * Formats:
 * - A new approach to implementation of `platformLayers`. Now extra layers (such as Dialogs and Popups) drawing is merged into a single screen size canvas.
 *
 * to:
 * - [A new approach to implementation of `platformLayers`](link). Now extra layers (such as Dialogs and Popups) drawing is merged into a single screen size canvas.
 */
fun ChangelogEntry.format(): String {
    return if (link != null) {
        val linkStartIndex = maxOf(
            message.indexOfFirst { !it.isWhitespace() && it != '-' }.ifNegative { 0 },
            message.endIndexOf("_(prerelease fix)_ ").ifNegative { 0 },
            message.endIndexOf("(prerelease fix) ").ifNegative { 0 },
        )
        val linkLastIndex = message.indexOfAny(listOf(". ", " (")).ifNegative { message.length }

        val beforeLink = message.substring(0, linkStartIndex)
        val inLink = message.substring(linkStartIndex, linkLastIndex).removeLinks()
        val afterLink = message.substring(linkLastIndex, message.length)

        "$beforeLink[$inLink]($link)$afterLink"
    } else {
        message
    }
}

fun Int.ifNegative(value: () -> Int): Int = if (this < 0) value() else this

fun String.endIndexOf(value: String): Int = indexOf(value).let {
    if (it >= 0)  {
        it + value.length
    } else {
        it
    }
}

/**
 * Converts:
 * Message (title)[some link], message
 *
 * to:
 * Message title, message
 */
fun String.removeLinks(): String = replace(Regex("\\[([^)]*)\\]\\([^\\]]*\\)"), "$1")

/**
 * Extract by format https://github.com/JetBrains/compose-multiplatform/blob/b32350459acceb9cca6b9e4422b7aaa051d9ae7d/.github/PULL_REQUEST_TEMPLATE.md?plain=1
 */
fun GitHubPullEntry.extractReleaseNotes(link: String): List<ChangelogEntry> {
    // extract body inside "## Release Notes"
    val relNoteBody = run {
        val after = body?.substringAfter("## Release Notes", "")?.ifBlank { null } ?:
            body?.substringAfter("## Release notes", "")?.ifBlank { null } ?:
            body?.substringAfter("## RelNote", "")?.ifBlank { null }

        val before = after?.substringBefore("\n## ", "")?.ifBlank { null } ?:
            after?.substringBefore("\n# ", "")?.ifBlank { null } ?:
            after

        before?.trim()
    }

    val list = mutableListOf<ChangelogEntry>()
    var section: String? = null
    var subsection: String? = null

    for (line in relNoteBody.orEmpty().split("\n")) {
        // parse "### Section - Subsection"
        if (line.startsWith("### ")) {
            val s = line.removePrefix("### ")
            section = s.substringBefore("-", "").trim().ifEmpty { null }
            subsection = s.substringAfter("-", "").trim().ifEmpty { null }
        } else if (section != null && line.isNotBlank()) {
            val isTopLevel = line.startsWith("-")
            val trimmedLine = line.trimEnd().removeSuffix(".")
            list.add(
                ChangelogEntry(
                    trimmedLine,
                    section,
                    subsection,
                    link.takeIf { isTopLevel }
                )
            )
        }
    }

    return list
}

/**
 * @param repo Example:
 *        JetBrains/compose-multiplatform-core
 */
fun entriesForRepo(repo: String): List<ChangelogEntry> {
    val pulls = (1..5)
        .flatMap {
            request<Array<GitHubPullEntry>>("https://api.github.com/repos/$repo/pulls?state=closed&per_page=100&page=$it").toList()
        }

    val pullNumberToPull = pulls.associateBy { it.number }
    val pullTitleToPull = pulls.associateBy { it.title }

    fun prForCommit(commit: GitHubCompareResponse.CommitEntry): GitHubPullEntry? {
        val (repoTitle, repoNumber) = repoTitleAndNumberForCommit(commit)
        return repoNumber?.let(pullNumberToPull::get) ?: pullTitleToPull[repoTitle]
    }

    fun changelogEntriesFor(
        pullRequest: GitHubPullEntry?
    ): List<ChangelogEntry> {
        return if (pullRequest != null) {
            val prTitle = pullRequest.title
            val prNumber = pullRequest.number
            val prLink = "https://github.com/$repo/pull/$prNumber"
            val prList = pullRequest.extractReleaseNotes(prLink)
            val changelogMessage = "- $prTitle"
            prList.ifEmpty {
                listOf(ChangelogEntry(changelogMessage, null, null, prLink))
            }
        } else {
            listOf()
        }
    }

    class CommitsResult(val commits: List<GitHubCompareResponse.CommitEntry>, val mergeBaseSha: String)

    fun fetchCommits(firsCommitSha: String, lastCommitSha: String): CommitsResult {
        lateinit var mergeBaseCommit: String
        val commits = fetchPagedUntilEmpty { page ->
            val result = request<GitHubCompareResponse>("https://api.github.com/repos/$repo/compare/$firsCommitSha...$lastCommitSha?per_page=1000&page=$page")
            mergeBaseCommit = result.merge_base_commit.sha
            result.commits
        }
        return CommitsResult(commits, mergeBaseCommit)
    }

    val main = fetchCommits(firstCommit, lastCommit)
    val previous = fetchCommits(main.mergeBaseSha, firstCommit)
    val pullRequests = main.commits.mapNotNull { prForCommit(it) }.toSet()
    val previousVersionPullRequests = previous.commits.mapNotNull { prForCommit(it) }.toSet()
    return (pullRequests - previousVersionPullRequests).flatMap { changelogEntriesFor(it) }
}

/**
 * Extract the PR number from the commit.
 */
fun repoTitleAndNumberForCommit(commit: GitHubCompareResponse.CommitEntry): Pair<String, Int?> {
    val commitTitle = commit.commit.message.substringBefore("\n")
    // check title similar to `Fix import android flavors with compose resources (#4319)`
    val title = commitTitle.substringBeforeLast(" (#")
    val number = commitTitle.substringAfterLast(" (#").substringBefore(")").toIntOrNull()
    return title to number
}

data class ChangelogEntry(
    val message: String,
    val section: String?,
    val subsection: String?,
    val link: String?,
)

fun ChangelogEntry.sectionOrder(): Int = sectionOrder.indexOf(section)
fun ChangelogEntry.subsectionOrder(): Int = subsectionOrder.indexOf(subsection)
fun ChangelogEntry.sectionName(): String = section ?: "Unknown"
fun ChangelogEntry.subsectionName(): String = subsection ?: "Unknown"

// example https://api.github.com/repos/JetBrains/compose-multiplatform-core/compare/v1.6.0-rc02...release/1.6.0
data class GitHubCompareResponse(val commits: List<CommitEntry>, val merge_base_commit: CommitEntry) {
    data class CommitEntry(val sha: String, val commit: Commit)
    data class Commit(val message: String)
}

// example https://api.github.com/repos/JetBrains/compose-multiplatform-core/pulls?state=closed
data class GitHubPullEntry(val number: Int, val title: String, val body: String?, val labels: List<Label>) {
    class Label(val name: String)
}

//region ========================================== UTILS =========================================

// from https://stackoverflow.com/a/41495542
fun String.runCommand(workingDir: File = File(".")) {
    ProcessBuilder(*split(" ").toTypedArray())
        .directory(workingDir)
        .redirectOutput(ProcessBuilder.Redirect.INHERIT)
        .redirectError(ProcessBuilder.Redirect.INHERIT)
        .start()
        .waitFor(5, TimeUnit.MINUTES)
}

fun String.execCommand(workingDir: File = File(".")): String? {
    try {
        val parts = this.split("\\s".toRegex())
        val proc = ProcessBuilder(*parts.toTypedArray())
            .directory(workingDir)
            .redirectOutput(ProcessBuilder.Redirect.PIPE)
            .redirectError(ProcessBuilder.Redirect.PIPE)
            .start()

        proc.waitFor(60, TimeUnit.MINUTES)
        return proc.inputStream.bufferedReader().readText()
    } catch (e: IOException) {
        e.printStackTrace()
        return null
    }
}

inline fun <reified T> request(
    url: String
): T = exponentialRetry {
    println("Request $url")
    val connection = URL(url).openConnection()
    connection.setRequestProperty("User-Agent", "Compose-Multiplatform-Script")
    if (token != null) {
        connection.setRequestProperty("Authorization", "Bearer $token")
    }
    connection.getInputStream().use {
        Gson().fromJson(
            it.bufferedReader(),
            T::class.java
        )
    }
}

fun <T> exponentialRetry(block: () -> T): T {
    val exception = IOException()
    val retriesMinutes = listOf(1, 5, 15, 30, 60)
    for (retriesMinute in retriesMinutes) {
        try {
            return block()
        } catch (e: IOException) {
            e.printStackTrace()
            exception.addSuppressed(e)
            println("Retry in $retriesMinute minutes")
            Thread.sleep(retriesMinute.toLong() * 60 * 1000)
        }
    }
    throw exception
}

inline fun <T> fetchPagedUntilEmpty(fetch: (page: Int) -> List<T>): MutableList<T> {
    val all = mutableListOf<T>()
    var page = 1
    do {
        val result = fetch(page++)
        all.addAll(result)
    } while (result.isNotEmpty())
    return all
}

//endregion