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

val firstCommit = args.getOrNull(0) ?: error("Please call this way: kotlin changelog.main.kts <firstCommit> <lastCommit>")
val lastCommit = args.getOrNull(1) ?: error("Please call this way: kotlin changelog.main.kts <firstCommit> <lastCommit>")
val token = args.getOrNull(2)

if (token == null) {
    println("To increase the rate limit, specify token (https://github.com/settings/tokens): kotlin changelog.main.kts <firstCommit> <lastCommit> TOKEN")
}

// commits that don't have a link to a PR (a link should be something like " (#454)")
val commitToPRLinkMapping = File("commit-to-pr-mapping.txt").readLines().associate {
    val splits = it.split(" ")
    val commit = splits[0]
    val prLink = splits[1]
    commit to prLink
}

val entries = entriesForRepo("JetBrains/compose-multiplatform-core") +
        entriesForRepo("JetBrains/compose-multiplatform")

fun List<ChangelogEntry>.ofType(type: Type) =
    filter { it.type == type }.sortedByDescending { it.platforms.sumOf { it.sorting } }

val highlighted = entries.ofType(Type.Highlighted)
val normal = entries.ofType(Type.Normal)
val prereleaseFixes = entries.ofType(Type.PrereleaseFix)
val unknown = entries.ofType(Type.Unknown)

println(
    buildString {
        append("_Changes since ${commitToVersion(firstCommit)}_\n")
        append("\n")

        if (highlighted.isNotEmpty())
            append(highlighted.joinToString("\n") { it.format() }).append("\n")

        if (normal.isNotEmpty())
            append(normal.joinToString("\n") { it.format() }).append("\n")

        if (prereleaseFixes.isNotEmpty())
            append(prereleaseFixes.joinToString("\n") { it.format() }).append("\n")

        if (unknown.isNotEmpty()) {
            append("\nUnknown changes for review:\n")
            append(unknown.joinToString("\n") { it.format() }).append("\n")
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

fun ChangelogEntry.format() = buildString {
    append("- ")
    if (type == Type.Highlighted) append("**")
    if (type == Type.PrereleaseFix) append("_(prerelease fix)_ ")
    append("[$title]($link)")
    if (type == Type.Highlighted) append("**")
    platforms.forEach {
        append(" <sub>$it</sub>")
    }
}

/**
 * @param repo Example:
 *        JetBrains/compose-multiplatform-core
 */
fun entriesForRepo(repo: String): List<ChangelogEntry> {
    val pullNumberToPull = (1..5)
        .flatMap {
            request<Array<GitHubPullEntry>>("https://api.github.com/repos/$repo/pulls?state=closed&per_page=100&page=$it").toList()
        }
        .associateBy { it.number }

    fun prForCommit(commit: GitHubCompareResponse.CommitEntry): GitHubPullEntry? {
        val repoNumber = repoNumberForCommit(commit)
        return pullNumberToPull[repoNumber]
    }

    fun changelogEntryFor(
        commit: GitHubCompareResponse.CommitEntry,
        pullRequest: GitHubPullEntry?
    ): ChangelogEntry {
        return if (pullRequest != null) {
            val prTitle = pullRequest.title
            val prNumber = pullRequest.number
            ChangelogEntry(
                prTitle,
                "https://github.com/$repo/pull/$prNumber",
                typeOf(pullRequest),
                platformsOf(pullRequest)
            )
        } else {
            val commitSha = commit.sha
            val commitTitle = commit.commit.message.substringBefore("\n")
            ChangelogEntry(commitTitle, "https://github.com/$repo/commit/$commitSha", Type.Unknown, emptyList())
        }
    }

    return request<GitHubCompareResponse>("https://api.github.com/repos/$repo/compare/$firstCommit...$lastCommit")
        .commits
        .map { changelogEntryFor(it, prForCommit(it)) }
}

/**
 * Extract the PR number from the commit.
 */
fun repoNumberForCommit(commit: GitHubCompareResponse.CommitEntry): Int? {
    val commitTitle = commit.commit.message.substringBefore("\n")
    // check title similar to `Fix import android flavors with compose resources (#4319)`
    return if (commitTitle.contains(" (#")) {
        commitTitle.substringAfter(" (#").substringBefore(")").toIntOrNull()
    } else {
        commitToPRLinkMapping[commit.sha]?.substringAfterLast("/")?.toIntOrNull()
    }
}

fun typeOf(pullRequest: GitHubPullEntry): Type {
    val labels = pullRequest.labels.mapTo(mutableSetOf()) { it.name.lowercase() }
    return when {
        labels.contains("changelog: highlight") -> Type.Highlighted
        labels.contains("changelog: normal") -> Type.Normal
        labels.contains("changelog: prerelease fix") -> Type.PrereleaseFix
        else -> Type.Unknown
    }
}

fun platformsOf(pullRequest: GitHubPullEntry) = pullRequest.labels.mapNotNull {
    when (it.name.lowercase()) {
        "ios" -> Platform.IOS
        "android" -> Platform.Android
        "desktop" -> Platform.Desktop
        "web" -> Platform.Web
        "common" -> Platform.Common
        else -> null
    }
}

data class ChangelogEntry(
    val title: String,
    val link: String,
    val type: Type,
    val platforms: List<Platform>,
)

enum class Type { Highlighted, Normal, PrereleaseFix, Unknown }

enum class Platform(val title: String, val sorting: Int) {
    Common("Common", 0x11111),
    IOS("iOS", 0x01000),
    Android("Android", 0x00100),
    Desktop("Desktop", 0x00010),
    Web("Web", 0x00001);

    override fun toString() = title
}

// example https://api.github.com/repos/JetBrains/compose-multiplatform-core/compare/v1.6.0-rc02...release/1.6.0
data class GitHubCompareResponse(val commits: List<CommitEntry>) {
    data class CommitEntry(val sha: String, val commit: Commit)
    data class Commit(val message: String)
}

// example https://api.github.com/repos/JetBrains/compose-multiplatform-core/pulls?state=closed
data class GitHubPullEntry(val number: Int, val title: String, val body: String, val labels: List<Label>) {
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

//endregion