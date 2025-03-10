/**
 * Script for creating a changelog. Call:
 * ```
 * kotlin changelog.main.kts v1.7.0+dev555
 * ```
 * or:
 * ```
 * kotlin changelog.main.kts v1.7.0..v1.7.1+dev555
 * ```
 * where:
 * v1.7.0+dev555 - the tag/branch of the version. The previous version will be read from CHANGELOG.md
 * v1.7.0..v1.7.1+dev555 - the range of tag/branches for the changelog
 *
 * It modifies CHANGELOG.md and adds new changes between the last version in CHANGELOG.md and the specified version.
 *
 * Changelog entries are generated from reading Release Notes in GitHub PR's.
 *
 * ## Checking PR description in a file
 * Not supposed to be called manually, used by GitHub workflow:
 * https://github.com/JetBrains/compose-multiplatform/blob/master/tools/changelog/check-release-notes-github-action/action.yml)
 * ```
 * kotlin changelog.main.kts action=checkPr prDescription.txt
 * ```
 *
 * compose-multiplatform - name of the GitHub repo
 * 5202 - PR number
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
import com.google.gson.annotations.SerializedName
import java.io.File
import java.io.IOException
import java.lang.ProcessBuilder.Redirect
import java.lang.System.err
import java.net.URL
import java.net.URLEncoder
import java.nio.charset.StandardCharsets.UTF_8
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.*
import kotlin.system.exitProcess

val changelogFile = __FILE__.resolve("../../CHANGELOG.md").canonicalFile
val prFormatFile = File("PR_FORMAT.md")
val prFormatLink = "https://github.com/JetBrains/compose-multiplatform/blob/master/tools/changelog/PR_FORMAT.md"

val argsKeyless = args
    .filter { !it.contains("=") }

val argsKeyToValue = args
    .filter { it.contains("=") }
    .associate { it.substringBefore("=") to it.substringAfter("=") }

val token = argsKeyToValue["token"]

// Parse sections from [PR_FORMAT.md]
fun parseSections(title: String) = prFormatFile
    .readText()
    .substringAfter(title)
    .substringAfter("```")
    .substringBefore("```")
    .split("\n")
    .map { it.trim().removePrefix("-").substringBefore("#").substringBefore("\n").trim() }
    .filter { it.isNotEmpty() }

val standardSections = parseSections("### Sections")
val standardSubsections = parseSections("### Subsections")

println()

when (argsKeyToValue["action"]) {
    "checkPr" -> checkPr()
    else -> generateChangelog()
}

fun generateChangelog() {
    if (token == null) {
        println("To increase the rate limit, specify token (https://github.com/settings/tokens), adding token=yourtoken in the end")
    }

    val commitsArg = argsKeyless.getOrNull(0) ?: "HEAD"

    var previousVersionCommitArg: String?
    var versionCommitArg: String
    if (commitsArg.contains("..")) {
        previousVersionCommitArg = commitsArg.substringBefore("..")
        versionCommitArg = commitsArg.substringAfter("..")
    } else {
        previousVersionCommitArg = null
        versionCommitArg = commitsArg
    }

    val versionCommit = versionCommitArg

    val androidxLibToPreviousVersion = previousVersionCommitArg?.let(::androidxLibToVersion)
    val androidxLibToVersion = androidxLibToVersion(versionCommit)
    val androidxLibToRedirectingVersion = androidxLibToRedirectingVersion(versionCommit)

    fun formatAndroidxLibPreviousVersion(libName: String) =
        androidxLibToPreviousVersion?.get(libName) ?: "PLACEHOLDER".also {
            println("Can't find $libName previous version. Using PLACEHOLDER")
        }

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

    var previousVersionCommit: String
    var previousVersion: String
    if (previousVersionCommitArg != null) {
        previousVersionCommit = previousVersionCommitArg!!
        previousVersion = formatAndroidxLibPreviousVersion("COMPOSE")
    } else {
        val previousVersionInChangelog = previousChangelog.substringAfter("# ").substringBefore(" (")
        previousVersionCommit = "v$previousVersionInChangelog"
        previousVersion = previousVersionInChangelog
    }

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
                                appendLine(it.run { "$message [#$prNumber]($link)" })
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

    println()
    println("Generating changelog between $previousVersion and $versionName")

    val newChangelog = getChangelog(previousVersionCommit, versionCommit, previousVersion)

    changelogFile.writeText(
        newChangelog + previousChangelog
    )

    println()
    println("CHANGELOG.md changed")
}

fun checkPr() {
    val filePath = argsKeyless.getOrNull(0) ?: error("Please specify a file that contains PR description as the first argument")

    val body = File(filePath).readText()
    val releaseNotes = extractReleaseNotes(body, 0, "https://github.com/JetBrains/compose-multiplatform/pull/0")

    val nonstandardSections = releaseNotes?.entries
        .orEmpty()
        .filter { it.section !in standardSections || it.subsection !in standardSubsections }
        .map { "${it.section} - ${it.subsection}" }
        .toSet()

    println()

    when {
        releaseNotes is ReleaseNotes.Specified && releaseNotes.entries.isEmpty() -> {
            err.println("""
                "## Release Notes" doesn't contain any items, or "### Section - Subsection" isn't specified
            
                See the format in $prFormatLink
            """.trimIndent())
            exitProcess(1)
        }
        releaseNotes is ReleaseNotes.Specified && nonstandardSections.isNotEmpty() -> {
            err.println("""
                "## Release Notes" contains nonstandard "Section - Subsection" pairs:
                ${nonstandardSections.joinToString(", ")}
            
                Allowed sections: ${standardSections.joinToString(", ")}
                Allowed subsections: ${standardSubsections.joinToString(", ")}
            
                See the full format in $prFormatLink
            """.trimIndent())
            exitProcess(1)
        }
        releaseNotes == null -> {
            err.println("""
                "## Release Notes" section is missing in the PR description
            
                See the format in $prFormatLink
            """.trimIndent())
            exitProcess(1)
        }
        else -> {
            println("\"## Release Notes\" are correct")
        }
    }
}

/**
 * September 2024
 */
fun currentChangelogDate() = LocalDate.now().format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH))

/**
 * Extract by format [PR_FORMAT.md]
 */
fun extractReleaseNotes(body: String?, prNumber: Int, prLink: String): ReleaseNotes? {
    fun String?.substringBetween(begin: String, end: String): String? {
        val after = this?.substringAfter(begin, "")?.ifBlank { null }
        return after?.substringBefore(end, "")?.ifBlank { null } ?: after
    }

    // extract body inside "# Release Notes"
    val relNoteBody = body
            ?.replace("# Release notes", "# Release Notes", ignoreCase = true)
            ?.replace("#Release notes", "# Release Notes", ignoreCase = true)
            ?.replace("# RelNote", "# Release Notes", ignoreCase = true)
            ?.run {
                substringBetween("# Release Notes", "\n# ")
                    ?: substringBetween("## Release Notes", "\n## ")
                    ?: substringBetween("### Release Notes", "\n### ")
                    ?: substringBetween("## Release Notes", "\n# ")
                    ?: substringBetween("### Release Notes", "\n## ")
                    ?: substringBetween("### Release Notes", "\n# ")
            }
            ?.trim()

    if (relNoteBody == null) return null
    if (relNoteBody.trim().lowercase() == "n/a") return ReleaseNotes.NA

    val list = mutableListOf<ChangelogEntry>()
    var section: String? = null
    var subsection: String? = null
    var isFirstLine = true
    var shouldPadLines = false

    for (line in relNoteBody.split("\n")) {
        // parse "## Section - Subsection"
        if (line.trim().startsWith("#")) {
            val s = line.trimStart { it == '#' || it.isWhitespace() }
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
                    prNumber,
                    prLink.takeIf { isTopLevel }
                )
            )
            isFirstLine = false
        }
    }

    return ReleaseNotes.Specified(list)
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

    val shaToPull = pulls.associateBy { it.mergeCommitSha }

    fun changelogEntriesFor(
        pullRequest: GitHubPullEntry?
    ): List<ChangelogEntry> {
        return if (pullRequest != null) {
            with(pullRequest) {
                extractReleaseNotes(body, number, htmlUrl)?.entries ?:
                    listOf(ChangelogEntry("- $title", null, null, number, htmlUrl))
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
            mergeBaseCommit = result.mergeBaseCommit.sha
            result.commits
        }
        return CommitsResult(commits, mergeBaseCommit)
    }

    val main = fetchCommits(firstCommit, lastCommit)
    val previous = fetchCommits(main.mergeBaseSha, firstCommit)
    val pullRequests = main.commits.mapNotNull { shaToPull[it.sha] }.toSet()
    val previousVersionPullRequests = previous.commits.mapNotNull { shaToPull[it.sha] }.toSet()
    return (pullRequests - previousVersionPullRequests).flatMap { changelogEntriesFor(it) }
}

/**
 * @param repo Example:
 *        JetBrains/compose-multiplatform-core
 */
fun pullRequest(repo: String, prNumber: String): GitHubPullEntry {
    return requestJson<GitHubPullEntry>("https://api.github.com/repos/$repo/pulls/$prNumber")
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

sealed interface ReleaseNotes {
    val entries: List<ChangelogEntry>

    object NA: ReleaseNotes {
        override val entries: List<ChangelogEntry> get() = emptyList()
    }

    class Specified(override val entries: List<ChangelogEntry>): ReleaseNotes
}

data class ChangelogEntry(
    val message: String,
    val section: String?,
    val subsection: String?,
    val prNumber: Int,
    val link: String?,
)

fun ChangelogEntry.sectionOrder(): Int = section?.let(standardSections::indexOf) ?: standardSections.size
fun ChangelogEntry.subsectionOrder(): Int = subsection?.let(standardSubsections::indexOf) ?: standardSubsections.size
fun ChangelogEntry.sectionName(): String = section ?: "Unknown"
fun ChangelogEntry.subsectionName(): String = subsection ?: "Unknown"
fun String.normalizeSectionName() = standardSections.find { it.lowercase() == this.lowercase() } ?: this
fun String.normalizeSubsectionName() = standardSubsections.find { it.lowercase() == this.lowercase() } ?: this

// example https://api.github.com/repos/JetBrains/compose-multiplatform-core/compare/v1.6.0-rc02...release/1.6.0
data class GitHubCompareResponse(
    val commits: List<CommitEntry>,
    @SerializedName("merge_base_commit") val mergeBaseCommit: CommitEntry
) {
    data class CommitEntry(val sha: String, val commit: Commit)
    data class Commit(val message: String)
}

// example https://api.github.com/repos/JetBrains/compose-multiplatform-core/pulls?state=closed
data class GitHubPullEntry(
    @SerializedName("html_url") val htmlUrl: String,
    val number: Int,
    val title: String,
    val body: String?,
    @SerializedName("merge_commit_sha") val mergeCommitSha: String?,
)

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
