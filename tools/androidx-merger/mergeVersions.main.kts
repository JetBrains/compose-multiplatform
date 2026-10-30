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

val airWebhookUrl = System.getenv("MERGER_SCRIPT_AIR_URL")
val airWebhookToken = System.getenv("MERGER_SCRIPT_AIR_TOKEN")
if (airWebhookUrl.isNullOrEmpty()) {
    println("Create an Air Automation to automatically resolve merge and build failures. See AIR_AUTOMATION.md.")
}

prepareForMerging()
mergeJbMain()
mergeUpstream()
check(run("git", "push")) { "git push error" }

fun prepareForMerging() {
    if (run("git", "rev-parse", "-q", "--verify", "MERGE_HEAD")) {
        check(run("git", "merge", "--abort")) {
            "Could not abort the unfinished merge. Inspect the working tree before rerunning."
        }
    }
    if (run("git", "rev-parse", "-q", "--verify", "refs/bisect/bad")) {
        check(run("git", "bisect", "reset")) { "Could not cancel the previous bisect." }
    }

    // Fail if the AI agent overwrote the last pushed commit.
    check(pull()) { "Pull error" }
    fetchNotes()

    val head = currentCommit()
    when {
        hasMergeSolverNoteFixed(head) -> check(compile()) { "Build still fails after Merge Resolver." }
        needsMergeSolver(head) -> check(solveWithMergeSolver()) { "Merge Solver did not review $head" }
        else -> ensureInitialCompiles()
    }
}

fun ensureInitialCompiles() {
    if (compile()) return

    val originalCommit = currentCommit()
    val (branchCode, originalBranch) = execute("git", "symbolic-ref", "--quiet", "--short", "HEAD")
    val originalCheckout = if (branchCode == 0) originalBranch else originalCommit
    check(logOperation("Checking out $originalCommit^1") {
        run("git", "switch", "--detach", "$originalCommit^1")
    }) { "Could not switch to the previous commit." }
    val previousCompiles = try {
        compile()
    } finally {
        check(logOperation("Restoring checkout $originalCheckout") {
            if (branchCode == 0) {
                run("git", "switch", originalBranch)
            } else {
                run("git", "switch", "--detach", originalCommit)
            }
        }) { "Could not restore the original checkout at $originalCommit." }
    }
    check(previousCompiles) { "Compilation failed on both the current and previous commits. Check the build environment." }
    markCompilationFailure()
    println("The last commit breaks compilation.")
    check(solveWithMergeSolver()) { "Merge Solver did not fix $originalCommit" }
}

fun mergeJbMain() {
    val branch = "origin/jb-main"
    if (run("git", "merge-base", "--is-ancestor", branch, "HEAD")) return

    when {
        !mergeCommit(branch) -> addMergeScriptNote("conflict")
        !compile() -> markCompilationFailure()
        else -> return
    }
    check(solveWithMergeSolver()) { "Merge Solver did not fix the merge of $branch" }
}

fun mergeUpstream() {
    if (!run("git", "remote", "get-url", "aosp")) {
        check(run("git", "remote", "add", "aosp", "https://android.googlesource.com/platform/frameworks/support")) {
            "git remote add aosp error"
        }
    }
    check(run("git", "fetch", "aosp")) { "git fetch error" }

    val upstream = "aosp/androidx-main"
    val merged = output("git", "merge-base", "HEAD", upstream)
    val commits = upstreamCommits(merged, upstream)
    var lastKnownGood = currentCommit()
    var lastCompilationTime = commitTime(merged)

    fun ensureCompiles() {
        if (currentCommit() == lastKnownGood) return
        if (!compile() && bisect(lastKnownGood)) {
            markCompilationFailure()
            check(solveWithMergeSolver())
        }
        lastKnownGood = currentCommit()
        lastCompilationTime = commitTime(output("git", "merge-base", "HEAD", upstream))
    }

    for (commit in commits) {
        // Compile before a conflict so Merge Solver only has to fix the conflicting merge.
        val hasConflict = !run("git", "merge-tree", "--write-tree", "HEAD", commit.hash)
        if (hasConflict || commit.timestamp - lastCompilationTime >= compilationInterval) {
            ensureCompiles()
        }
        mergeUpstreamCommit(commit.hash)
    }
    ensureCompiles()
}

data class UpstreamCommit(val hash: String, val timestamp: Instant)

fun upstreamCommits(merged: String, upstream: String): List<UpstreamCommit> =
    output("git", "rev-list", "--timestamp", "--first-parent", "--reverse", "$merged..$upstream")
        .lineSequence()
        .filter(String::isNotBlank)
        .map { line ->
            val (timestamp, commit) = line.split(" ", limit = 2)
            UpstreamCommit(commit, Instant.fromEpochSeconds(timestamp.toLong()))
        }
        .toList()

fun mergeUpstreamCommit(commit: String) {
    if (mergeCommit(commit, useAospTitle = true)) return
    addMergeScriptNote("conflict")
    check(solveWithMergeSolver())
}

fun mergeCommit(commit: String, useAospTitle: Boolean = false): Boolean =
    logOperation("Merging $commit") {
        val merged = run("git", "merge", "--no-commit", "--no-ff", commit)
        // Commit conflict markers too, so Merge Solver can fix them in a follow-up commit.
        if (!merged) stageConflictingFiles(commit)
        if (useAospTitle) setMergeTitle(commit)
        check(run("git", "commit", "--no-edit")) { "Could not commit merge: $commit" }
        merged
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
    val commit = currentCommit()
    val command = if (System.getProperty("os.name").startsWith("Windows")) {
        listOf("cmd", "/c", "gradlew")
    } else {
        listOf("./gradlew")
    }
    return logOperation("Compile at $commit") {
        execute(*command.toTypedArray(), "assemble", "compileTests", logFile = buildLogFile).first == 0
    }
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

fun needsMergeSolver(commit: String) =
    hasMergeScriptNote(commit) &&
        !hasMergeSolverNoteFixed(commit) &&
        Clock.System.now() - commitTime(commit) < 1.hours

fun solveWithMergeSolver(): Boolean {
    val url = airWebhookUrl
    val token = airWebhookToken
    if (url.isNullOrEmpty() || token.isNullOrEmpty()) return false

    val pushedCommit = currentCommit()
    logOperation("Submitting $pushedCommit to Merge Solver") {
        check(run("git", "push"))
        check(run("git", "push", "origin", "refs/notes/commits"))
        val (code, result) = execute(
            "curl", "--fail", "--silent", "--show-error",
            "--request", "POST",
            "--header", "Authorization: ApiKey $token",
            url
        )
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
    run("git", "fetch", "origin", "+refs/notes/commits:refs/notes/commits", "--quiet")

fun currentCommit() = output("git", "rev-parse", "HEAD")

fun commitTime(revision: String): Instant =
    Instant.fromEpochSeconds(output("git", "show", "-s", "--format=%ct", revision).toLong())

fun addMergeScriptNote(message: String) =
    check(run("git", "notes", "append", "-m", "Merge script: $message"))

fun markCompilationFailure() = addMergeScriptNote("compilation failure")

fun hasMergeScriptNote(commit: String) = hasNote(commit, "Merge script:")

fun hasMergeSolverNoteFixed(commit: String) = hasNote(commit, "Merge solver: fixed")

fun hasMergeSolverNote(commit: String) = hasNote(commit, "Merge solver:")

fun hasNote(commit: String, prefix: String): Boolean {
    val (code, note) = execute("git", "notes", "show", commit)
    return code == 0 && note.lineSequence().any { it.startsWith(prefix) }
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
