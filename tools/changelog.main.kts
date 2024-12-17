/**
 * Script for creating a changelog. Call:
 * ```
 * kotlin changelog.main.kts 1.7.0-dev555
 * ```
 * or:
 * ```
 * kotlin changelog.main.kts 1.7.0..1.7.1-dev555
 * ```
 * where:
 * 1.7.0-dev555 - the tag/branch of the version. The previous version will be read from CHANGELOG.md
 * 1.7.0..1.7.1-dev555 - the range of tag/branches for the changelog
 *
 * It modifies CHANGELOG.md and adds new changes between the last version in CHANGELOG.md and the specified version.
 *
 * Changelog entries are generated from reading Release Notes in GitHub PR's.
 *
 * ## How to run Kotlin scripts
 * Option 1 - via Command line
 * 1. Download https://github.com/JetBrains/kotlin/releases/tag/v1.9.22 and add `bin` to PATH
 *
 * Option 2 - via IntelliJ:
 * 1. Right click on the script
 * 2. More Run/Debug
 * 3. Modify Run Configuration...
 * 4. Add Program arguments
 * 5. Clear all "Before launch" tasks (you can edit the system-wide template as well)
 * 6. OK
 */

@file:Repository("https://repo1.maven.org/maven2/")
@file:DependsOn("com.google.code.gson:gson:2.10.1")

import com.google.gson.Gson
import java.io.IOException
import java.lang.ProcessBuilder.Redirect
import java.net.URL
import java.net.URLEncoder
import java.nio.charset.StandardCharsets.UTF_8
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.*
import kotlin.text.substringAfterLast

//region ========================================== CONSTANTS =========================================

// Sections from the template https://github.com/JetBrains/compose-multiplatform/blob/master/.github/PULL_REQUEST_TEMPLATE.md?plain=1
// Changelog should contain only these categories

val standardSections = listOf(
    "Highlights",
    "Known Issues",
    "Breaking Changes",
    "Features",
    "Fixes",
)

val standardSubsections = listOf(
    "Multiple Platforms",
    "iOS",
    "Desktop",
    "Web",
    "Android",
    "Resources",
    "Gradle Plugin",
    "Lifecycle",
    "Navigation",
)

val changelogFile = __FILE__.resolve("../../CHANGELOG.md").canonicalFile

//endregion

val argsKeyless = args
    .filter { !it.contains("=") }

val argsKeyToValue = args
    .filter { it.contains("=") }
    .associate { it.substringBefore("=") to it.substringAfter("=") }

val commitsArg = argsKeyless.getOrNull(0) ?: "HEAD"

var previousVersionCommitArg: String?
var versionCommitArg: String
if (commitsArg.contains("..")) {
    previousVersionCommitArg = commitsArg.substringBefore("..")
    versionCommitArg = commitsArg.substringAfter("..",)
} else {
    previousVersionCommitArg = null
    versionCommitArg = commitsArg
}

val versionCommit = versionCommitArg

val token = argsKeyToValue["token"]

println("Note. The script supports optional arguments: kotlin changelog.main.kts [previousVersionCommit..versionCommit] [token=githubToken]")
if (token == null) {
    println("To increase the rate limit, specify token (https://github.com/settings/tokens)")
}
println()

val androidxLibToPreviousVersion = previousVersionCommitArg?.let(::androidxLibToVersion)
val androidxLibToVersion = androidxLibToVersion(versionCommit)
val androidxLibToRedirectingVersion = androidxLibToRedirectingVersion(versionCommit)

fun formatAndroidxLibVersion(libName: String) =
    androidxLibToVersion[libName] ?: "PLACEHOLDER".also {
        println("Can't find $libName version. Using PLACEHOLDER")
    }

fun formatAndroidxLibRedirectingVersion(libName: String) =
    androidxLibToRedirectingVersion[libName] ?: "PLACEHOLDER".also {
        println("Can't find $libName redirecting version. Using PLACEHOLDER")
    }

val versionCompose = formatAndroidxLibVersion("COMPOSE")
val versionComposeMaterial3Adaptive = formatAndroidxLibVersion("COMPOSE_MATERIAL3_ADAPTIVE")
val versionLifecycle = formatAndroidxLibVersion("LIFECYCLE")
val versionNavigation = formatAndroidxLibVersion("NAVIGATION")

val versionRedirectingCompose = formatAndroidxLibRedirectingVersion("compose")
val versionRedirectingComposeFoundation = formatAndroidxLibRedirectingVersion("compose.foundation")
val versionRedirectingComposeMaterial = formatAndroidxLibRedirectingVersion("compose.material")
val versionRedirectingComposeMaterial3 = formatAndroidxLibRedirectingVersion("compose.material3")
val versionRedirectingComposeMaterial3Adaptive = formatAndroidxLibRedirectingVersion("compose.material3.adaptive")
val versionRedirectingLifecycle = formatAndroidxLibRedirectingVersion("lifecycle")
val versionRedirectingNavigation = formatAndroidxLibRedirectingVersion("navigation")

val versionName = versionCompose

val currentChangelog = changelogFile.readText()
val previousChangelog =
    if (currentChangelog.startsWith("# $versionName ")) {
        val nextChangelogIndex = currentChangelog.indexOf("\n# ")
        currentChangelog.substring(nextChangelogIndex).removePrefix("\n")
    } else {
        currentChangelog
    }

val previousVersionInChangelog = previousChangelog.substringAfter("# ").substringBefore(" (")
val previousVersionCommit = previousVersionCommitArg ?: "v$previousVersionInChangelog"
val previousVersion = androidxLibToPreviousVersion?.get("COMPOSE") ?: previousVersionInChangelog

println()
println("Generating changelog between $previousVersion and $versionName")

val newChangelog = getChangelog(previousVersionCommit, versionCommit, previousVersion)

changelogFile.writeText(
    newChangelog + previousChangelog
)

println()
println("CHANGELOG.md changed")


fun getChangelog(firstCommit: String, lastCommit: String, firstVersion: String): String {
    val entries = entriesForRepo("JetBrains/compose-multiplatform-core", firstCommit, lastCommit) +
            entriesForRepo("JetBrains/compose-multiplatform", firstCommit, lastCommit)

    return buildString {
        appendLine("# $versionName (${currentChangelogDate()})")

        appendLine()
        appendLine("_Changes since ${firstVersion}_")
        appendLine()

        entries
            .sortedBy { it.sectionOrder() }
            .groupBy { it.sectionName() }
            .forEach { (section, sectionEntries) ->
                appendLine("## $section")
                appendLine()

                sectionEntries
                    .sortedBy { it.subsectionOrder() }
                    .groupBy { it.subsectionName() }
                    .forEach { (subsection, subsectionEntries) ->
                        appendLine("### $subsection")
                        appendLine()
                        subsectionEntries.forEach {
                            appendLine(it.format())
                        }
                        appendLine()
                    }
            }

        append(
            """
                ## Dependencies
        
                - Gradle Plugin `org.jetbrains.compose`, version `$versionCompose`. Based on Jetpack Compose libraries:
                  - [Runtime $versionRedirectingCompose](https://developer.android.com/jetpack/androidx/releases/compose-runtime#$versionRedirectingCompose)
                  - [UI $versionRedirectingCompose](https://developer.android.com/jetpack/androidx/releases/compose-ui#$versionRedirectingCompose)
                  - [Foundation $versionRedirectingComposeFoundation](https://developer.android.com/jetpack/androidx/releases/compose-foundation#$versionRedirectingComposeFoundation)
                  - [Material $versionRedirectingComposeMaterial](https://developer.android.com/jetpack/androidx/releases/compose-material#$versionRedirectingComposeMaterial)
                  - [Material3 $versionRedirectingComposeMaterial3](https://developer.android.com/jetpack/androidx/releases/compose-material3#$versionRedirectingComposeMaterial3)

                - Lifecycle libraries `org.jetbrains.androidx.lifecycle:lifecycle-*:$versionLifecycle`. Based on [Jetpack Lifecycle $versionRedirectingLifecycle](https://developer.android.com/jetpack/androidx/releases/lifecycle#$versionRedirectingLifecycle)
                - Navigation libraries `org.jetbrains.androidx.navigation:navigation-*:$versionNavigation`. Based on [Jetpack Navigation $versionRedirectingNavigation](https://developer.android.com/jetpack/androidx/releases/navigation#$versionRedirectingNavigation)
                - Material3 Adaptive libraries `org.jetbrains.compose.material3.adaptive:adaptive*:$versionComposeMaterial3Adaptive`. Based on [Jetpack Material3 Adaptive $versionRedirectingComposeMaterial3Adaptive](https://developer.android.com/jetpack/androidx/releases/compose-material3-adaptive#$versionRedirectingComposeMaterial3Adaptive)
        
                ---
            """.trimIndent()
        )

        appendLine()
        appendLine()

        val nonstandardSectionEntries = entries
            .filter {
                it.section != null && it.subsection != null
                        && it.section !in standardSections && it.subsection !in standardSubsections
            }

        if (nonstandardSectionEntries.isNotEmpty()) {
            println()
            println("WARNING! Changelog contains nonstandard sections. Please change them to the standard ones, or enhance the list in the PR template.")

            for (entry in nonstandardSectionEntries) {
                println("${entry.section} - ${entry.subsection} in ${entry.link}")
            }
        }
    }
}

/**
 * September 2024
 */
fun currentChangelogDate() = LocalDate.now().format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH))

/**
 * Formats:
 * - A new approach to implementation of `platformLayers`. Now extra layers (such as Dialogs and Popups) drawing is merged into a single screen size canvas.
 *
 * to:
 * - [A new approach to implementation of `platformLayers`](link). Now extra layers (such as Dialogs and Popups) drawing is merged into a single screen size canvas.
 */
fun ChangelogEntry.format(): String {
    return try {
        tryFormat()
    } catch (e: Exception) {
        throw RuntimeException("Formatting error of ChangelogEntry. Message:\n$message", e)
    }
}

fun ChangelogEntry.tryFormat(): String {
    return if (link != null) {
        val prefixRegex = "^[-\\s]*"   // "- "
        val tagRegex1 = "\\(.*\\)\\s*" // "(something) "
        val tagRegex2 = "\\[.*\\]\\s*" // "[something] "
        val tagRegex3 = "_.*_\\s*"     // "_something_ "
        val linkStartIndex = maxOf(
            message.endIndexOfFirstGroup(Regex("($prefixRegex).*"))?.plus(1) ?: 0,
            message.endIndexOfFirstGroup(Regex("($prefixRegex$tagRegex1).*"))?.plus(1) ?: 0,
            message.endIndexOfFirstGroup(Regex("($prefixRegex$tagRegex2).*"))?.plus(1) ?: 0,
            message.endIndexOfFirstGroup(Regex("($prefixRegex$tagRegex3).*"))?.plus(1) ?: 0,
        )
        val linkLastIndex = message.indexOfAny(listOf(". ", " ("), linkStartIndex).ifNegative { message.length }

        val beforeLink = message.substring(0, linkStartIndex)
        val inLink = message.substring(linkStartIndex, linkLastIndex).removeLinks()
        val afterLink = message.substring(linkLastIndex, message.length)

        "$beforeLink[$inLink]($link)$afterLink"
    } else {
        message
    }
}

fun Int.ifNegative(value: () -> Int): Int = if (this < 0) value() else this

fun String.endIndexOfFirstGroup(regex: Regex): Int? =
    regex.find(this)?.groups?.toList()?.getOrNull(1)?.range?.endInclusive

/**
 * Converts:
 * Message (title)[some link], message
 *
 * to:
 * Message title, message
 */
fun String.removeLinks(): String = replace(Regex("\\[([^)]*)\\]\\([^\\]]*\\)"), "$1")

/**
 * Extract by format https://github.com/JetBrains/compose-multiplatform/blob/master/.github/PULL_REQUEST_TEMPLATE.md?plain=1
 */
fun GitHubPullEntry.extractReleaseNotes(link: String): List<ChangelogEntry> {
    // extract body inside "## Release Notes"
    val relNoteBody = run {
        val after = body?.substringAfter("## Release Notes", "")?.ifBlank { null }
            ?: body?.substringAfter("## Release notes", "")?.ifBlank { null } ?: body?.substringAfter(
                "## RelNote",
                ""
            )?.ifBlank { null }

        val before = after?.substringBefore("\n## ", "")?.ifBlank { null } ?: after?.substringBefore("\n# ", "")
            ?.ifBlank { null } ?: after

        before?.trim()
    }

    if (relNoteBody?.trim()?.lowercase() == "n/a") return emptyList()

    val list = mutableListOf<ChangelogEntry>()
    var section: String? = null
    var subsection: String? = null
    var isFirstLine = true
    var shouldPadLines = false

    for (line in relNoteBody.orEmpty().split("\n")) {
        // parse "### Section - Subsection"
        if (line.startsWith("### ")) {
            val s = line.removePrefix("### ")
            section = s.substringBefore("-", "").trim().normalizeSectionName().ifEmpty { null }
            subsection = s.substringAfter("-", "").trim().normalizeSubsectionName().ifEmpty { null }
            isFirstLine = true
            shouldPadLines = false
        } else if (section != null && line.isNotBlank()) {
            var lineFixed = line

            if (isFirstLine && !lineFixed.startsWith("-")) {
                lineFixed = "- $lineFixed"
                shouldPadLines = true
            }
            if (!isFirstLine && shouldPadLines) {
                lineFixed = "  $lineFixed"
            }
            lineFixed = lineFixed.trimEnd().removeSuffix(".")

            val isTopLevel = lineFixed.startsWith("-")
            list.add(
                ChangelogEntry(
                    lineFixed,
                    section,
                    subsection,
                    link.takeIf { isTopLevel }
                )
            )
            isFirstLine = false
        }
    }

    return list
}

/**
 * @param repo Example:
 *        JetBrains/compose-multiplatform-core
 */
fun entriesForRepo(repo: String, firstCommit: String, lastCommit: String): List<ChangelogEntry> {
    val pulls = (1..5)
        .flatMap {
            requestJson<Array<GitHubPullEntry>>("https://api.github.com/repos/$repo/pulls?state=closed&per_page=100&page=$it").toList()
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
            val result = requestJson<GitHubCompareResponse>("https://api.github.com/repos/$repo/compare/$firsCommitSha...$lastCommitSha?per_page=1000&page=$page")
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

/**
 * Extract redirecting versions from core repo, file gradle.properties
 *
 * Example
 * https://raw.githubusercontent.com/JetBrains/compose-multiplatform-core/v1.8.0%2Bdev1966/gradle.properties
 * artifactRedirecting.androidx.graphics.version=1.0.1
 */
fun androidxLibToRedirectingVersion(commit: String): Map<String, String> {
    val gradleProperties = githubContentOf("JetBrains/compose-multiplatform-core", "gradle.properties", commit)
    val regex = Regex("artifactRedirecting\\.androidx\\.(.*)\\.version=(.*)")
    return regex.findAll(gradleProperties).associate { result ->
        result.groupValues[1].trim() to result.groupValues[2].trim()
    }
}

/**
 * Extract versions from CI config, file .teamcity/compose/Library.kt
 *
 * Example
 * https://jetbrains.team/p/ui/repositories/compose-teamcity-config/files/8f8408ccd05a9188895969b1fa0243050716baad/.teamcity/compose/Library.kt?tab=source&line=37&lines-count=1
 * Library.CORE_BUNDLE -> "1.1.0-alpha01"
 */
fun androidxLibToVersion(commit: String): Map<String, String> {
    val repo = "ssh://git@git.jetbrains.team/ui/compose-teamcity-config.git"
    val file = ".teamcity/compose/Library.kt"
    val libraryKt = spaceContentOf(repo, file, commit)

    return if (libraryKt.isBlank()) {
        println("Can't clone $repo to know library versions. Please register your ssh key in https://jetbrains.team/m/me/authentication?tab=GitKeys")
        emptyMap()
    } else {
        val regex = Regex("Library\\.(.*)\\s*->\\s*\"(.*)\"")
        return regex.findAll(libraryKt).associate { result ->
            result.groupValues[1].trim() to result.groupValues[2].trim()
        }
    }
}

fun githubContentOf(repo: String, path: String, commit: String): String {
    val commitEncoded = URLEncoder.encode(commit, UTF_8)
    return requestPlain("https://raw.githubusercontent.com/$repo/$commitEncoded/$path")
}

fun spaceContentOf(repoUrl: String, path: String, tagName: String): String {
    return pipeProcess("git archive --remote=$repoUrl $tagName $path")
        .pipeTo("tar -xO $path")
        .readText()
}

data class ChangelogEntry(
    val message: String,
    val section: String?,
    val subsection: String?,
    val link: String?,
)

fun ChangelogEntry.sectionOrder(): Int = section?.let(standardSections::indexOf) ?: standardSections.size
fun ChangelogEntry.subsectionOrder(): Int = subsection?.let(standardSubsections::indexOf) ?: standardSubsections.size
fun ChangelogEntry.sectionName(): String = section ?: "Unknown"
fun ChangelogEntry.subsectionName(): String = subsection ?: "Unknown"
fun String.normalizeSectionName() = standardSections.find { it.lowercase() == this.lowercase() } ?: this
fun String.normalizeSubsectionName() = standardSubsections.find { it.lowercase() == this.lowercase() } ?: this

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
fun pipeProcess(command: String) = ProcessBuilder(command.split(" "))
    .redirectOutput(Redirect.PIPE)
    .redirectError(Redirect.PIPE)
    .start()!!

fun Process.pipeTo(command: String): Process = pipeProcess(command).also {
    inputStream.use { input ->
        it.outputStream.use { out ->
            input.copyTo(out)
        }
    }
}

fun Process.readText(): String = inputStream.bufferedReader().use { it.readText() }

inline fun <reified T> requestJson(url: String): T =
    Gson().fromJson(requestPlain(url), T::class.java)

fun requestPlain(url: String): String = exponentialRetry {
    println("Request $url")
    val connection = URL(url).openConnection()
    connection.setRequestProperty("User-Agent", "Compose-Multiplatform-Script")
    if (token != null) {
        connection.setRequestProperty("Authorization", "Bearer $token")
    }
    connection.getInputStream().use {
        it.bufferedReader().readText()
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