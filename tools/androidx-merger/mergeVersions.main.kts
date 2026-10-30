import java.io.File
import java.io.FileOutputStream
import java.io.PrintStream
import java.nio.channels.Channels
import java.nio.channels.Pipe
import java.time.OffsetDateTime
import kotlin.concurrent.thread
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

val scriptDir = getScriptPathFromArgs()
val scriptLogFile = scriptDir.resolve("logScript.txt")
val buildLogFile = scriptDir.resolve("logBuild.txt")
buildLogFile.writeText("")

val scriptOutput = timestampedOutput(System.out)
System.setOut(scriptOutput)
System.setErr(scriptOutput)

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

    val hasConflict = !logOperation("Merging $branch") {
        val merged = run("git", "merge", "--no-commit", "--no-ff", branch)
        if (!merged) stageConflictingFiles(branch)
        check(run("git", "commit", "--no-edit")) { "Could not commit merge: $branch" }
        merged
    }

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
    check(logOperation("Checking out $currentCommit^1") {
        run("git", "switch", "--detach", "$currentCommit^1")
    }) { "Could not switch to the previous commit." }
    val previousCompiles = try {
        compile()
    } finally {
        val checkout = if (branchCode == 0) currentBranch else currentCommit
        val restored = logOperation("Restoring checkout $checkout") {
            if (branchCode == 0) {
                run("git", "switch", currentBranch)
            } else {
                run("git", "switch", "--detach", currentCommit)
            }
        }
        check(restored) { "Could not restore the original checkout at $currentCommit." }
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
    val hasConflict = !logOperation("Merging $commit") {
        val merged = run("git", "merge", "--no-commit", "--no-ff", commit)
        if (!merged) stageConflictingFiles(commit)
        setMergeTitle(commit)
        check(run("git", "commit", "--no-edit")) { "Could not commit merge: $commit" }
        merged
    }
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
    val commit = output("git", "rev-parse", "HEAD")
    val command = if (System.getProperty("os.name").startsWith("Windows")) {
        listOf("cmd", "/c", "gradlew")
    } else {
        listOf("./gradlew")
    }
    return logOperation("Compile at $commit") {
        execute(*command.toTypedArray(), "assemble", "compileTests", logFile = buildLogFile).first == 0
    }
}

fun solveWithMergeSolver(): Boolean {
    val url = airWebhookUrl?.takeIf(String::isNotEmpty) ?: return false
    val token = airWebhookToken?.takeIf(String::isNotEmpty) ?: return false
    val pushedCommit = output("git", "rev-parse", "HEAD")
    logOperation("Submitting $pushedCommit to Merge Solver") {
        check(run("git", "push"))
        check(run("git", "push", "origin", "refs/notes/commits"))
        val (code, result) = execute("curl", "--fail", "--silent", "--show-error", "--request", "POST", "--header", "Authorization: ApiKey $token", url)
        check(code == 0) { "JetBrains Air Automations webhook failed: $result" }
        true
    }
    if (!waitForMergeSolver(pushedCommit)) return false
    check(compile()) { "Build still fails after Merge Resolver." }
    return true
}

fun waitForMergeSolver(pushedCommit: String): Boolean {
    val timeout = 80.minutes
    val interval = 60.seconds
    return logOperation("Resolving $pushedCommit with Merge Solver (timeout: $timeout)") {
        repeat((timeout / interval).toInt()) {
            Thread.sleep(interval.inWholeMilliseconds)
            fetchNotes()
            if (hasMergeSolverNote(pushedCommit)) {
                check(hasMergeSolverNoteFixed(pushedCommit)) {
                    "Merge Solver did not fix $pushedCommit:\n${output("git", "notes", "show", pushedCommit)}"
                }
                check(pull()) { "Could not fast-forward to the Merge Solver result." }
                return@logOperation true
            }
        }
        false
    }
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
    if (compile()) {
        println("Skipping bisect; compilation passed.")
        return false
    }
    check(run("git", "bisect", "start", "--first-parent", "HEAD", lastKnownGood))
    while (true) {
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

fun logOperation(message: String, operation: () -> Boolean): Boolean {
    print("$message ... ")
    var success = false
    try {
        success = operation()
        return success
    } finally {
        println(if (success) "COMPLETED" else "FAILED")
    }
}

fun execute(vararg args: String, logFile: File? = null): Pair<Int, String> {
    val process = ProcessBuilder(*args).redirectErrorStream(true).start()
    val text = StringBuilder()
    process.inputStream.bufferedReader().useLines { lines ->
        if (logFile == null) {
            lines.forEach { text.appendLine(it) }
        } else {
            lines.onEach { text.appendLine(it) }.logTo(logFile)
        }
    }
    return process.waitFor() to text.toString().trim()
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

fun timestampedOutput(console: PrintStream): PrintStream {
    val pipe = Pipe.open()
    val output = PrintStream(Channels.newOutputStream(pipe.sink()), true, Charsets.UTF_8)
    val reader = thread(isDaemon = true) {
        Channels.newInputStream(pipe.source()).bufferedReader(Charsets.UTF_8).use { input ->
            sequence {
                var lineStart = true
                for (code in generateSequence(input::read).takeWhile { it != -1 }) {
                    val char = code.toChar()
                    if (lineStart) yield("${OffsetDateTime.now()} ")
                    yield(char.toString())
                    lineStart = char == '\n'
                }
                if (!lineStart) yield("\n")
            }.writeTo(scriptLogFile, console)
        }
    }
    Runtime.getRuntime().addShutdownHook(Thread {
        output.close()
        reader.join()
    })
    return output
}

fun Sequence<String>.logTo(file: File, console: PrintStream? = null) =
    map { "${OffsetDateTime.now()} $it\n" }.writeTo(file, console)

fun Sequence<String>.writeTo(file: File, console: PrintStream? = null) {
    FileOutputStream(file, true).bufferedWriter(Charsets.UTF_8).use { writer ->
        forEach { text ->
            synchronized(file) {
                console?.print(text)
                console?.flush()
                writer.write(text)
                writer.flush()
            }
        }
    }
}
