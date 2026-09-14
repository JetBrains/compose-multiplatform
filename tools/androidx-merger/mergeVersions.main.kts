import java.io.File
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

val scriptDir = getScriptPathFromArgs()
val log = scriptDir.resolve("log.txt")
val airWebhookFile = File(System.getProperty("user.home"), "androidxMergerAirWebhook.txt")
val compilationInterval = 1.days

val airWebhookUrl: String? = System.getenv("MERGER_SCRIPT_AIR_URL")
val airWebhookToken: String? = System.getenv("MERGER_SCRIPT_AIR_TOKEN")
if (airWebhookUrl.isNullOrEmpty()) {
    println("Create an Air Automation to automatically resolve merge and build failures. See AIR_AUTOMATION.md.")
}

if (log.exists()) {
    log.delete()
}

if (run("git", "rev-parse", "-q", "--verify", "MERGE_HEAD")) {
    check(run("git", "merge", "--abort")) {
        "Could not abort the unfinished merge. Inspect the working tree before rerunning."
    }
}

if (run("git", "rev-parse", "-q", "--verify", "refs/bisect/bad")) {
    check(run("git", "bisect", "reset")) { "Could not cancel the previous bisect." }
}

// --ff-only should fail if the AI agent overwrote the last pushed commit
check(run("git", "pull", "--ff-only"))
fetchNotes()

val head = output("git", "rev-parse", "HEAD")
when {
    hasMergeSolverNote(head) -> Unit
    needsMergeSolver(head) -> check(solveWithMergeSolver()) { "Merge Solver did not review $head" }
    else -> check(compile()) { "Compilation failed" }
}

if (!run("git", "remote", "get-url", "aosp")) {
    check(run("git", "remote", "add", "aosp", "https://android.googlesource.com/platform/frameworks/support"))
}
check(run("git", "fetch", "aosp"))

val upstream = "aosp/androidx-main"
val merged = output("git", "merge-base", "HEAD", upstream)
val upstreamCommits = output("git", "rev-list", "--timestamp", "--first-parent", "--reverse", "$merged..$upstream")
    .lineSequence()
    .filter(String::isNotBlank)
    .map { line ->
        val (timestamp, commit) = line.split(" ", limit = 2)
        UpstreamCommit(commit, Instant.fromEpochSeconds(timestamp.toLong()))
    }
    .toList()

var lastKnownGood = output("git", "rev-parse", "HEAD")
var lastCompilationTime = commitTime("HEAD")

upstreamCommits.forEachIndexed { index, commit ->
    mergeUpstreamCommit(commit.hash)

    if (index == upstreamCommits.lastIndex ||
        commit.timestamp - lastCompilationTime >= compilationInterval
    ) {
        if (!compile()) {
            println("Compilation failed. Bisecting.")
            bisect(lastKnownGood)
            markCompilationFailure()
            println("Build regression found.")
            check(solveWithMergeSolver())
        }
        lastKnownGood = output("git", "rev-parse", "HEAD")
        lastCompilationTime = commit.timestamp
    }
}

fun needsMergeSolver(commit: String) =
    hasMergeScriptNote(commit) &&
        !hasMergeSolverNote(commit) &&
        Clock.System.now() - commitTime(commit) < 1.hours

fun mergeUpstreamCommit(commit: String) {
    println("Merge $commit")
    val hasConflict = !run("git", "merge", "--no-commit", "--no-ff", commit)
    if (hasConflict) stageConflictingFiles(commit)

    setMergeTitle(commit)
    check(run("git", "commit", "--no-edit")) { "Could not commit merge: $commit" }
    if (!hasConflict) return

    addMergeScriptNote("conflict")
    println("Conflict committed: $commit")
    check(solveWithMergeSolver())
}

fun stageConflictingFiles(commit: String) {
    val conflictingFiles = output("git", "diff", "--name-only", "--diff-filter=U")
        .lineSequence()
        .filter(String::isNotBlank)
        .toList()
    check(conflictingFiles.isNotEmpty()) { "Merge failed without conflicts: $commit" }
    check(run("git", "add", "-A", "--", *conflictingFiles.toTypedArray()))
}

fun setMergeTitle(commit: String) {
    val originalTitle = output("git", "show", "-s", "--format=%s", commit)
    val messageFile = File(output("git", "rev-parse", "--git-path", "MERGE_MSG"))
    val shortCommit = commit.take(8)
    val title = "(AOSP $shortCommit) $originalTitle"
    messageFile.writeText("$title\n${messageFile.readText().substringAfter('\n')}")
}

fun compile() = if (System.getProperty("os.name").startsWith("Windows")) {
    run("cmd", "/c", "gradlew", "assemble", "compileTests")
} else {
    run("./gradlew", "assemble", "compileTests")
}

fun solveWithMergeSolver(): Boolean {
    val url = airWebhookUrl?.takeIf(String::isNotEmpty) ?: return false
    val token = airWebhookToken?.takeIf(String::isNotEmpty) ?: return false
    println("Solving with Merge Solver... ")
    val pushedCommit = output("git", "rev-parse", "HEAD")
    check(run("git", "push"))
    check(run("git", "push", "origin", "refs/notes/commits"))
    val (code, result) = execute("curl", "--fail", "--silent", "--show-error", "--request", "POST", "--header", "Authorization: ApiKey $token", url)
    check(code == 0) { "JetBrains Air Automations webhook failed: $result" }
    if (!waitForMergeSolver(pushedCommit)) return false
    check(compile()) { "Build still fails after Merge Resolver." }
    return true
}

fun waitForMergeSolver(pushedCommit: String): Boolean {
    val timeout = 45.minutes
    val interval = 30.seconds
    repeat((timeout / interval).toInt()) {
        Thread.sleep(interval.inWholeMilliseconds)
        output("git", "pull", "--ff-only", "--quiet")
        fetchNotes()
        if (hasMergeSolverNote(pushedCommit)) return true
    }
    println("Timed out waiting for Merge Solver.")
    return false
}

fun fetchNotes() =
    run("git", "fetch", "origin", "+refs/notes/commits:refs/notes/commits", "--quiet",)

fun addMergeScriptNote(message: String) =
    check(run("git", "notes", "append", "-m", "Merge script: $message"))

fun markCompilationFailure() = addMergeScriptNote("compilation failure")

fun hasMergeScriptNote(commit: String) = hasNote(commit, "Merge script:")

fun hasMergeSolverNote(commit: String) = hasNote(commit, "Merge solver:")

fun hasNote(commit: String, prefix: String): Boolean =
    execute("git", "notes", "show", commit)
        .let { (code, note) ->
            code == 0 && note.lineSequence().any { it.startsWith(prefix) }
        }

fun bisect(lastKnownGood: String) {
    check(run("git", "bisect", "start", "--first-parent", "HEAD", lastKnownGood))
    while (true) {
        println("Bisect ${output("git", "rev-parse", "HEAD")}")
        val result = if (compile()) "good" else "bad"
        val (code, text) = execute("git", "bisect", result)
        check(code == 0) { text }
        if ("is the first bad commit" in text) {
            val firstBad = output("git", "rev-parse", "HEAD")
            check(run("git", "bisect", "reset"))
            check(run("git", "reset", "--hard", firstBad))
            return
        }
    }
}

data class UpstreamCommit(val hash: String, val timestamp: Instant)

fun execute(vararg args: String): Pair<Int, String> {
    val process = ProcessBuilder(*args).redirectErrorStream(true).start()
    val text = process.inputStream.bufferedReader().readText().trim()
    if (text.isNotEmpty()) log.appendText("$text\n")
    return process.waitFor() to text
}

fun run(vararg args: String) = execute(*args).first == 0

fun output(vararg args: String): String {
    val (code, text) = execute(*args)
    check(code == 0) { text }
    return text
}

// Avoid using __FILE__ here since it has a caching bug
// https://youtrack.jetbrains.com/issue/KT-77524/
fun getScriptPathFromArgs(): File =
    System.getProperty("sun.java.command")
        ?.split(" ")
        ?.find { it.endsWith(".kts") }
        ?.let { File(it).canonicalFile.parentFile }
        ?: error("Can't find script path from args: ${System.getProperty("sun.java.command")}")

fun commitTime(revision: String): Instant =
    Instant.fromEpochSeconds(output("git", "show", "-s", "--format=%ct", revision).toLong())

fun logTimestamp() = Clock.System.now().toString()

fun println(message: Any?) = kotlin.io.println("${logTimestamp()} $message")

inline fun check(value: Boolean, lazyMessage: () -> Any = { "Check failed." }) =
    kotlin.check(value) { "${logTimestamp()} ${lazyMessage()}" }

fun error(message: Any): Nothing = kotlin.error("${logTimestamp()} $message")
