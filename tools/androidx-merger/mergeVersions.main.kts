import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.io.PrintStream
import java.time.OffsetDateTime
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

val scriptDir = getScriptPathFromArgs()
val scriptLogFile = scriptDir.resolve("logScript.txt")
val buildLogFile = scriptDir.resolve("logBuild.txt")
scriptLogFile.writeText("")
buildLogFile.writeText("")

val stdoutLog = PrintStream(TimestampedOutputStream(scriptLogFile, System.out), true, Charsets.UTF_8)
val stderrLog = PrintStream(TimestampedOutputStream(scriptLogFile, System.err), true, Charsets.UTF_8)
val scriptOutput = PrintStream(TimestampedOutputStream(scriptLogFile), true, Charsets.UTF_8)
val buildOutput = PrintStream(TimestampedOutputStream(buildLogFile), true, Charsets.UTF_8)
System.setOut(stdoutLog)
System.setErr(stderrLog)
Runtime.getRuntime().addShutdownHook(Thread {
    listOf(stdoutLog, stderrLog, scriptOutput, buildOutput).forEach { it.close() }
})

val airWebhookFile = File(System.getProperty("user.home"), "androidxMergerAirWebhook.txt")
val compilationInterval = 1.days

val airWebhookUrl: String? = System.getenv("MERGER_SCRIPT_AIR_URL")
val airWebhookToken: String? = System.getenv("MERGER_SCRIPT_AIR_TOKEN")
if (airWebhookUrl.isNullOrEmpty()) {
    println("Create an Air Automation to automatically resolve merge and build failures. See AIR_AUTOMATION.md.")
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
check(pull()) { "Pull error" }
fetchNotes()

val head = output("git", "rev-parse", "HEAD")

when {
    hasMergeSolverNoteFixed(head) ->
        check(compile()) { "Build still fails after Merge Resolver." }
    needsMergeSolver(head) -> check(solveWithMergeSolver()) { "Merge Solver did not review $head" }
    else -> ensureInitialCompiles()
}

mergeJbMain()

if (!run("git", "remote", "get-url", "aosp")) {
    check(run("git", "remote", "add", "aosp", "https://android.googlesource.com/platform/frameworks/support")) {
        "git remote add aosp error"
    }
}
check(run("git", "fetch", "aosp")) { "git fetch error" }

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
var lastCompilationTime = commitTime(merged)

upstreamCommits.forEach { commit ->
    // Compile before a conflict, so Merge Solver doesn't have to fix unrelated build failures as well
    val hasConflict = !run("git", "merge-tree", "--write-tree", "HEAD", commit.hash)
    if (hasConflict || commit.timestamp - lastCompilationTime >= compilationInterval) {
        ensureCompiles()
    }
    mergeUpstreamCommit(commit.hash)
}
ensureCompiles()

check(run("git", "push")) { "git push error" }

fun needsMergeSolver(commit: String) =
    hasMergeScriptNote(commit) &&
        !hasMergeSolverNoteFixed(commit) &&
        Clock.System.now() - commitTime(commit) < 1.hours

fun mergeJbMain() {
    val branch = "origin/jb-main"
    if (run("git", "merge-base", "--is-ancestor", branch, "HEAD")) return

    println("Merging $branch")
    val hasConflict = !run("git", "merge", "--no-commit", "--no-ff", branch)
    if (hasConflict) stageConflictingFiles(branch)
    check(run("git", "commit", "--no-edit")) { "Could not commit merge: $branch" }
    println(if (hasConflict) "Merge committed with conflicts: $branch" else "Merge completed: $branch")

    if (hasConflict) {
        addMergeScriptNote("conflict")
    } else if (!compile()) {
        markCompilationFailure()
    } else {
        return
    }
    check(solveWithMergeSolver()) { "Merge Solver did not fix the merge of $branch" }
}

fun ensureInitialCompiles() {
    if (compile()) return

    val currentCommit = output("git", "rev-parse", "HEAD")
    val (branchCode, currentBranch) = execute("git", "symbolic-ref", "--quiet", "--short", "HEAD")
    println("Checking the previous commit: $currentCommit^1")
    check(run("git", "switch", "--detach", "$currentCommit^1")) { "Could not switch to the previous commit." }
    val previousCompiles = try {
        compile()
    } finally {
        val restored = if (branchCode == 0) {
            run("git", "switch", currentBranch)
        } else {
            run("git", "switch", "--detach", currentCommit)
        }
        check(restored) { "Could not restore the original checkout at $currentCommit." }
        println("Restored checkout: ${if (branchCode == 0) currentBranch else currentCommit}")
    }
    check(previousCompiles) { "Compilation failed on both the current and previous commits. Check the build environment." }
    markCompilationFailure()
    println("The last commit breaks compilation.")
    check(solveWithMergeSolver()) { "Merge Solver did not fix $currentCommit" }
}

fun ensureCompiles() {
    if (output("git", "rev-parse", "HEAD") == lastKnownGood) return
    if (!compile()) {
        if (bisect(lastKnownGood)) {
            markCompilationFailure()
            check(solveWithMergeSolver())
        }
    }
    lastKnownGood = output("git", "rev-parse", "HEAD")
    lastCompilationTime = commitTime(output("git", "merge-base", "HEAD", upstream))
}

fun mergeUpstreamCommit(commit: String) {
    println("Merging $commit")
    val hasConflict = !run("git", "merge", "--no-commit", "--no-ff", commit)
    if (hasConflict) stageConflictingFiles(commit)

    setMergeTitle(commit)
    check(run("git", "commit", "--no-edit")) { "Could not commit merge: $commit" }
    println(if (hasConflict) "Merge committed with conflicts: $commit" else "Merge completed: $commit")
    if (!hasConflict) return

    addMergeScriptNote("conflict")
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

fun compile(): Boolean {
    val commit = output("git", "rev-parse", "--short", "HEAD")
    println("Starting compilation at $commit: assemble compileTests")
    val command = if (System.getProperty("os.name").startsWith("Windows")) {
        listOf("cmd", "/c", "gradlew")
    } else {
        listOf("./gradlew")
    }
    val process = ProcessBuilder(command + listOf("assemble", "compileTests")).redirectErrorStream(true).start()
    process.inputStream.bufferedReader().useLines { lines -> lines.forEach(buildOutput::println) }
    val success = process.waitFor() == 0
    println("Compilation ${if (success) "succeeded" else "failed"} at $commit")
    return success
}

fun solveWithMergeSolver(): Boolean {
    val url = airWebhookUrl?.takeIf(String::isNotEmpty) ?: return false
    val token = airWebhookToken?.takeIf(String::isNotEmpty) ?: return false
    val pushedCommit = output("git", "rev-parse", "HEAD")
    println("Submitting $pushedCommit to Merge Solver")
    check(run("git", "push"))
    check(run("git", "push", "origin", "refs/notes/commits"))
    val (code, result) = execute("curl", "--fail", "--silent", "--show-error", "--request", "POST", "--header", "Authorization: ApiKey $token", url)
    check(code == 0) { "JetBrains Air Automations webhook failed: $result" }
    if (!waitForMergeSolver(pushedCommit)) return false
    check(compile()) { "Build still fails after Merge Resolver." }
    println("Merge Solver fix verified for $pushedCommit")
    return true
}

fun waitForMergeSolver(pushedCommit: String): Boolean {
    val timeout = 80.minutes
    val interval = 60.seconds
    println("Waiting for Merge Solver to review $pushedCommit (timeout: $timeout)")
    repeat((timeout / interval).toInt()) {
        Thread.sleep(interval.inWholeMilliseconds)
        fetchNotes()
        if (hasMergeSolverNote(pushedCommit)) {
            check(hasMergeSolverNoteFixed(pushedCommit)) {
                "Merge Solver did not fix $pushedCommit:\n${output("git", "notes", "show", pushedCommit)}"
            }
            println("Merge Solver reported a fix for $pushedCommit")
            check(pull()) { "Could not fast-forward to the Merge Solver result." }
            return true
        }
    }
    println("Timed out waiting for Merge Solver.")
    return false
}

// Not `git pull`: it merges FETCH_HEAD, which a concurrent fetch can fill with multiple branches,
// resulting in "Cannot fast-forward to multiple branches" (happened once)
fun pull() =
    run("git", "fetch", "origin", "--quiet") && run("git", "merge", "--ff-only", "--quiet", "@{u}")

fun fetchNotes() =
    run("git", "fetch", "origin", "+refs/notes/commits:refs/notes/commits", "--quiet",)

fun addMergeScriptNote(message: String) =
    check(run("git", "notes", "append", "-m", "Merge script: $message"))

fun markCompilationFailure() = addMergeScriptNote("compilation failure")

fun hasMergeScriptNote(commit: String) = hasNote(commit, "Merge script:")

fun hasMergeSolverNoteFixed(commit: String) = hasNote(commit, "Merge solver: fixed")

fun hasMergeSolverNote(commit: String) = hasNote(commit, "Merge solver:")

fun hasNote(commit: String, prefix: String): Boolean =
    execute("git", "notes", "show", commit)
        .let { (code, note) ->
            code == 0 && note.lineSequence().any { it.startsWith(prefix) }
        }

fun bisect(lastKnownGood: String): Boolean {
    println("Rechecking the suspected bad commit before bisect.")
    if (compile()) {
        println("Skipping bisect; continuing merges.")
        return false
    }
    println("Starting bisect after $lastKnownGood")
    check(run("git", "bisect", "start", "--first-parent", "HEAD", lastKnownGood))
    while (true) {
        println("Bisect testing commit ${output("git", "rev-parse", "HEAD")}")
        val result = if (compile()) "good" else "bad"
        val (code, text) = execute("git", "bisect", result)
        check(code == 0) { text }
        if ("is the first bad commit" in text) {
            val firstBad = output("git", "rev-parse", "refs/bisect/bad")
            check(run("git", "bisect", "reset"))
            check(run("git", "reset", "--hard", firstBad))
            println("Bisect found the first bad commit: $firstBad")
            return true
        }
    }
}

data class UpstreamCommit(val hash: String, val timestamp: Instant)

fun execute(vararg args: String): Pair<Int, String> {
    val process = ProcessBuilder(*args).redirectErrorStream(true).start()
    val text = process.inputStream.bufferedReader().use { it.readText().trim() }
    if (text.isNotEmpty()) scriptOutput.println(text)
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

class TimestampedOutputStream(
    private val file: File,
    private val console: PrintStream? = null,
) : OutputStream() {
    private val buffer = ByteArrayOutputStream()
    private val writer = FileOutputStream(file, true).bufferedWriter(Charsets.UTF_8)

    @Synchronized
    override fun write(value: Int) {
        if ((value and 0xff) != '\n'.code) {
            buffer.write(value)
            return
        }
        val message = buffer.toString(Charsets.UTF_8).removeSuffix("\r")
        buffer.reset()
        synchronized(file) {
            val line = "${OffsetDateTime.now()} $message\n"
            console?.print(line)
            writer.write(line)
            writer.flush()
        }
    }

    @Synchronized
    override fun close() {
        if (buffer.size() > 0) write('\n'.code)
        writer.close()
    }
}
