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

val changelogFile = __FILE__.resolve("../../../CHANGELOG.md").canonicalFile
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
        println("To increase the rate limit, specify token (https://github.com/settings/tokens), adding token=yourtoken in the end\n")
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
    val androidxLibToRedirectionVersion = androidxLibToRedirectionVersion(versionCommit)

    fun formatAndroidxLibPreviousVersion(libName: String) =
        androidxLibToPreviousVersion?.get(libName) ?: "PLACEHOLDER".also {
            println("Can't find $libName previous version. Using PLACEHOLDER")
        }

    fun formatAndroidxLibVersion(libName: String) =
        androidxLibToVersion[libName] ?: "PLACEHOLDER".also {
            println("Can't find $libName version. Using PLACEHOLDER")
        }

    fun formatAndroidxLibRedirectingVersion(libName: String) =
        androidxLibToRedirectionVersion[libName] ?: "PLACEHOLDER".also {
            println("Can't find $libName redirection version. Using PLACEHOLDER")
        }

    val versionCompose = formatAndroidxLibVersion("COMPOSE")
    val versionComposeMaterial3 = formatAndroidxLibVersion("COMPOSE_MATERIAL3")
    val versionComposeMaterial3Adaptive = formatAndroidxLibVersion("COMPOSE_MATERIAL3_ADAPTIVE")
    val versionLifecycle = formatAndroidxLibVersion("LIFECYCLE")
    val versionNavigationEvent = formatAndroidxLibVersion("NAVIGATION_EVENT")
    val versionSavedstate = formatAndroidxLibVersion("SAVEDSTATE")
    val versionWindow = formatAndroidxLibVersion("WINDOW")
    val versionNavigation3 = formatAndroidxLibVersion("NAVIGATION_3")

    val versionRedirectingCompose = formatAndroidxLibRedirectingVersion("compose")
    val versionRedirectingComposeMaterial3 = formatAndroidxLibRedirectingVersion("compose.material3")
    val versionRedirectingComposeMaterial3Adaptive = formatAndroidxLibRedirectingVersion("compose.material3.adaptive")
    val versionRedirectingLifecycle = formatAndroidxLibRedirectingVersion("lifecycle")
    val versionRedirectingNavigationEvent = formatAndroidxLibRedirectingVersion("navigationevent")
    val versionRedirectingSavedstate = formatAndroidxLibRedirectingVersion("savedstate")
    val versionRedirectingWindow = formatAndroidxLibRedirectingVersion("window")
    val versionRedirectingNavigation3 = formatAndroidxLibRedirectingVersion("navigation3")

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

    fun getChangelog(firstCommit: String, lastCommit: String, firstVersion: String, lastVersion: String): String {
        val isPrerelease = lastVersion.contains("-")

        val entries = entriesForRepo("JetBrains/compose-multiplatform-core", firstCommit, lastCommit) +
                entriesForRepo("JetBrains/compose-multiplatform", firstCommit, lastCommit)

        return buildString {
            appendLine("# $lastVersion (${currentChangelogDate()})")

            appendLine()
            appendLine("_Changes since ${firstVersion}_")
            appendLine()

            entries
                .filter { isPrerelease || !it.isPrerelease }
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
                                appendLine(it.run { "- $title [#$prNumber]($link)" })
                                if (it.details != null) {
                                    if (!it.details.startsWith("-")) {
                                        appendLine()
                                    }
                                    appendLine(it.details.prependIndent("  "))
                                }
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
                      - [Foundation $versionRedirectingCompose](https://developer.android.com/jetpack/androidx/releases/compose-foundation#$versionRedirectingCompose)
                      - [Material $versionRedirectingCompose](https://developer.android.com/jetpack/androidx/releases/compose-material#$versionRedirectingCompose)
                      - [Material3 $versionRedirectingComposeMaterial3](https://developer.android.com/jetpack/androidx/releases/compose-material3#$versionRedirectingComposeMaterial3)

                    - Compose Material3 libraries `org.jetbrains.compose.material3:material3*:$versionComposeMaterial3`. Based on [Jetpack Compose Material3 $versionRedirectingComposeMaterial3](https://developer.android.com/jetpack/androidx/releases/compose-material3#$versionRedirectingComposeMaterial3)
                    - Compose Material3 Adaptive libraries `org.jetbrains.compose.material3.adaptive:adaptive*:$versionComposeMaterial3Adaptive`. Based on [Jetpack Compose Material3 Adaptive $versionRedirectingComposeMaterial3Adaptive](https://developer.android.com/jetpack/androidx/releases/compose-material3-adaptive#$versionRedirectingComposeMaterial3Adaptive)
                    - Lifecycle libraries `org.jetbrains.androidx.lifecycle:lifecycle-*:$versionLifecycle`. Based on [Jetpack Lifecycle $versionRedirectingLifecycle](https://developer.android.com/jetpack/androidx/releases/lifecycle#$versionRedirectingLifecycle)
                    - Navigation libraries `org.jetbrains.androidx.navigation:navigation-*:2.9.1`. Based on [Jetpack Navigation 2.9.4](https://developer.android.com/jetpack/androidx/releases/navigation#2.9.4)
                    - Navigation 3 libraries `org.jetbrains.androidx.navigation:navigation3-*:$versionNavigation3`. Based on [Jetpack Navigation 3](https://developer.android.com/jetpack/androidx/releases/navigation3#$versionRedirectingNavigation3)
                    - Navigation Event library `org.jetbrains.androidx.navigationevent:navigationevent-compose:$versionNavigationEvent`. Based on [Jetpack Navigation Event $versionRedirectingNavigationEvent](https://developer.android.com/jetpack/androidx/releases/navigationevent#$versionRedirectingNavigationEvent)
                    - Savedstate library `org.jetbrains.androidx.savedstate:savedstate*:$versionSavedstate`. Based on [Jetpack Savedstate $versionRedirectingSavedstate](https://developer.android.com/jetpack/androidx/releases/savedstate#$versionRedirectingSavedstate)
                    - WindowManager Core library `org.jetbrains.androidx.window:window-core:$versionWindow`. Based on [Jetpack WindowManager $versionRedirectingWindow](https://developer.android.com/jetpack/androidx/releases/window#$versionRedirectingWindow)

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

    val newChangelog = getChangelog(previousVersionCommit, versionCommit, previousVersion, versionName)

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

fun GitHubPullEntry.extractReleaseNotes() = extractReleaseNotes(body, number, htmlUrl)

fun GitHubPullEntry.unknownChangelogEntries() =
    listOf(ChangelogEntry("- $title", null, null, null, number, htmlUrl, false))

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

    // Check if the release notes contain only GitHub PR links
    val pullRequests = relNoteBody
        .split("\n")
        .map { it.trim() }
        .mapNotNull(PullRequestLink::parseOrNull)

    if (pullRequests.isNotEmpty()) return ReleaseNotes.CherryPicks(pullRequests)

    /**
     * Parses bodies like:
     * ```
     * ### Highlights - iOS
     * - Describe change 1
     * details (multiline)
     *
     * - Describe change 2
     *   details (multiline)
     * ```
     */
    fun parseChangelogEntries(sectionBody: StringList): List<ChangelogEntry> {
        val s = sectionBody.first().trimStart { it == '#' || it.isWhitespace() }
        val section = s.substringBefore("-", "").trim()
            .normalizeSectionName().ifEmpty { return emptyList() }
        val subsection = s.substringAfter("-", "").trim()
            .normalizeSubsectionName().ifEmpty { return emptyList() }

        return sectionBody
            .drop(1)
            .split { it.startsWith("-") }
            .map { entryBody ->
                val title = entryBody.first().trim().removePrefix("-").removeSuffix(".").trim()
                val details = entryBody.drop(1)
                    .dropWhile { it.isBlank() }
                    .dropLastWhile { it.isBlank() }
                    .joinToString("\n")
                    .trimIndent()
                    .ifBlank { null }
                val isPrerelease = title.contains("(prerelease fix)")
                ChangelogEntry(title, details, section, subsection, prNumber, prLink, isPrerelease)
            }
    }

    return ReleaseNotes.Specified(
        relNoteBody
            .split("\n")
            .split { it.trim().startsWith("#") }
            .flatMap(::parseChangelogEntries)
    )
}

/**
 * @param repo Example:
 *        JetBrains/compose-multiplatform-core
 */
fun entriesForRepo(repo: String, firstCommit: String, lastCommit: String): List<ChangelogEntry> {
    val pulls = (1..10)
        .flatMap {
            requestJson<Array<GitHubPullEntry>>("https://api.github.com/repos/$repo/pulls?state=closed&per_page=100&page=$it").toList()
        }

    val shaToPull = pulls.associateBy { it.mergeCommitSha }
    val numberToPull = pulls.associateBy { it.number }
    val pullToReleaseNotes = Cache(GitHubPullEntry::extractReleaseNotes)

    // if GitHubPullEntry is a cherry-picks PR (contains a list of links to other PRs), replace it by the original PRs
    fun List<GitHubPullEntry>.replaceCherryPicks(): List<GitHubPullEntry> = flatMap { pullRequest ->
        val releaseNotes = pullToReleaseNotes[pullRequest]
        if (releaseNotes is ReleaseNotes.CherryPicks) {
            releaseNotes.pullRequests
                .filter { it.repo.equals(repo, ignoreCase = true) }
                .mapNotNull { numberToPull[it.number] }
        } else {
            listOf(pullRequest)
        }
    }

    val repoFolder = githubClone(repo)

    // Commits that exist in [firstCommit] and not identified as cherry-picks by `git log`
    // We'll try to exclude them via manual links to cherry-picks
    val cherryPickedPrsInFirstCommit = gitLogShas(repoFolder, firstCommit, lastCommit, "--cherry-pick --left-only")
        .mapNotNull(shaToPull::get)
        .replaceCherryPicks()

    // Exclude the same entries for partial cherry-picked PRs (example https://github.com/JetBrains/compose-multiplatform-core/pull/2096)
    val cherryPickedIdsInFirstCommit = cherryPickedPrsInFirstCommit
        .flatMap {
            pullToReleaseNotes[it]?.entries.orEmpty()
        }
        .mapTo(mutableSetOf()) {
            it.id
        }

    return gitLogShas(repoFolder, firstCommit, lastCommit, "--cherry-pick --right-only")
        .reversed() // older changes are at the bottom
        .mapNotNull(shaToPull::get)
        .replaceCherryPicks()
        .minus(cherryPickedPrsInFirstCommit)
        .distinctBy { it.number }
        .flatMap {
            pullToReleaseNotes[it]?.entries ?: it.unknownChangelogEntries()
        }
        .filterNot { it.id in cherryPickedIdsInFirstCommit }
}

/**
 * Extract redirection versions from core repo, file gradle.properties
 *
 * Example
 * https://raw.githubusercontent.com/JetBrains/compose-multiplatform-core/v1.8.0%2Bdev1966/gradle.properties
 * artifactRedirecting.androidx.graphics.version=1.0.1
 */
fun androidxLibToRedirectionVersion(commit: String): Map<String, String> {
    val gradleProperties = githubContentOf("JetBrains/compose-multiplatform-core", "gradle.properties", commit)
    val regexV1 = Regex("artifactRedirecting\\.androidx\\.(.*)\\.version=(.*)")
    val regexV2 = Regex("artifactRedirection\\.version\\.androidx\\.(.*)=(.*)") // changed in https://github.com/JetBrains/compose-multiplatform-core/pull/1946/files#diff-3d103fc7c312a3e136f88e81cef592424b8af2464c468116545c4d22d6edcf19R100
    return listOf(regexV1, regexV2).flatMap { it.findAll(gradleProperties) }.associate { result ->
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
    val libraryKt = try {
        spaceContentOf(repo, file, commit)
    } catch (_: Exception) {
        ""
    }

    return if (libraryKt.isBlank()) {
        println("Can't find library versions in $repo for $commit. Either the format is changed, or you need to register your ssh key in https://jetbrains.team/m/me/authentication?tab=GitKeys")
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

/**
 * Return a list of shas between [firstCommit] and [lastCommit] in [folder]
 */
fun gitLogShas(folder: File, firstCommit: String, lastCommit: String, additionalArgs: String): List<String> {
    val absolutePath = folder.absolutePath
    val commits = pipeProcess("git -C $absolutePath log --oneline --format=%H $additionalArgs $firstCommit...$lastCommit").
            readText()
    return commits.split("\n")
}

/**
 * Clone or fetch GitHub repo into [result] folder
 */
fun githubClone(repo: String): File {
    val url = "https://github.com/$repo"
    val folder = File("build/github/$repo")
    val absolutePath = folder.absolutePath
    if (!folder.exists() || folder.listFiles()?.isEmpty() == true) {
        folder.mkdirs()
        println("Cloning $url into ${folder.absolutePath}")
        pipeProcess("git clone --bare $url $absolutePath").waitAndCheck()
    } else {
        println("Fetching $url into ${folder.absolutePath}")
        pipeProcess("git -C $absolutePath fetch --force --tags").waitAndCheck()
    }
    check(folder.listFiles()?.isNotEmpty() == true) {
        "Cloning $url failed"
    }
    return folder
}

data class PullRequestLink(val repo: String, val number: Int) {
    companion object {
        fun parseOrNull(link: String): PullRequestLink? {
            val (repo, number) = Regex("https://github\\.com/(.+)/pull/(\\d+)/?")
                .matchEntire(link)
                ?.destructured
                ?: return null
            return PullRequestLink(repo, number.toInt())
        }
    }
}

sealed interface ReleaseNotes {
    val entries: List<ChangelogEntry>

    object NA: ReleaseNotes {
        override val entries: List<ChangelogEntry> get() = emptyList()
    }

    class CherryPicks(val pullRequests: List<PullRequestLink>): ReleaseNotes {
        override val entries: List<ChangelogEntry> get() = emptyList()
    }

    class Specified(override val entries: List<ChangelogEntry>): ReleaseNotes
}

/**
 * Describes a single entry in a format:
 *
 * ### section - subsection
 * ...
 * - title (single line)
 * details (line 1)
 * details (line 2)
 * ...
 */
data class ChangelogEntry(
    val title: String, /**  */
    val details: String?,
    val section: String?,
    val subsection: String?,
    val prNumber: Int,
    val link: String,
    val isPrerelease: Boolean,
) {
    /**
     * Unique entry id used for excluding cherry-picked entries
     */
    val id: UUID = UUID.nameUUIDFromBytes((section + subsection + title + details).toByteArray(Charsets.UTF_8))
}

fun ChangelogEntry.sectionOrder(): Int = section?.let(standardSections::indexOf) ?: standardSections.size
fun ChangelogEntry.subsectionOrder(): Int = subsection?.let(standardSubsections::indexOf) ?: standardSubsections.size
fun ChangelogEntry.sectionName(): String = section ?: "Unknown"
fun ChangelogEntry.subsectionName(): String = subsection ?: "Unknown"
fun String.normalizeSectionName() = standardSections.find { it.lowercase() == this.lowercase() } ?: this
fun String.normalizeSubsectionName() = standardSubsections.find { it.lowercase() == this.lowercase() } ?: this

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

fun Process.waitAndCheck() {
    val exitCode = waitFor()
    if (exitCode != 0) {
        val message = errorStream.bufferedReader().use { it.readText() }
        error("Command failed with exit code $exitCode:\n$message")
    }
}

fun Process.readText(): String = inputStream.bufferedReader().use {
    it.readText().also {
        waitAndCheck()
    }
}

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


typealias StringList = List<String>

fun StringList.split(shouldSplit: (line: String) -> Boolean): List<StringList> =
    fold(initial = mutableListOf<MutableList<String>>()) { acc, it ->
        if (acc.isEmpty() || shouldSplit(it)) {
            acc.add(mutableListOf())
        }
        acc.last().add(it)
        acc
    }

class Cache<K, V>(private val create: (K) -> V) {
    private val map = mutableMapOf<K,V>()
    operator fun get(key: K): V = map.getOrPut(key) { create(key) }
}

//endregion
