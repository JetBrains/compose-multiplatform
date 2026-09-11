import java.io.File

val scriptDir = getScriptPathFromArgs()
val log = scriptDir.resolve("log.txt")
val lastMergedFile = scriptDir.resolve("lastMerged.txt")
val day = 24 * 60 * 60

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

if (lastMergedFile.exists()) {
    val lastMerged = lastMergedFile.readText().trim()
    check(run("git", "merge-base", "--is-ancestor", lastMerged, "HEAD")) {
        "Checkpoint $lastMerged is not in the current branch history."
    }
}

if (!run("git", "remote", "get-url", "aosp")) {
    check(run("git", "remote", "add", "aosp", "https://android.googlesource.com/platform/frameworks/support"))
}
check(run("git", "fetch", "aosp"))

val upstream = "aosp/androidx-main"
val merged = output("git", "merge-base", "HEAD", upstream)
val commits = output("git", "rev-list", "--timestamp", "--first-parent", "--reverse", "$merged..$upstream")
    .lineSequence()
    .filter(String::isNotBlank)
    .map { line ->
        val (timestamp, commit) = line.split(" ", limit = 2)
        commit to timestamp.toLong()
    }
    .toList()

check(compile()) { "Compilation failed" }

var lastKnownGood = output("git", "rev-parse", "HEAD")
var lastCompilationTime = output("git", "show", "-s", "--format=%ct", "HEAD").toLong()

commits.forEachIndexed { index, (commit, timestamp) ->
    println("Merge $commit")
    if (!run("git", "merge", "--no-commit", "--no-ff", commit)) {
        val conflictingFiles = output("git", "diff", "--name-only", "--diff-filter=U")
            .lineSequence()
            .filter(String::isNotBlank)
            .toList()
        check(conflictingFiles.isNotEmpty()) { "Merge failed without conflicts: $commit" }
        check(run("git", "add", "-A", "--", *conflictingFiles.toTypedArray()))
        check(run("git", "commit", "--no-edit")) { "Could not commit merge conflict: $commit" }
        saveLastMerged()
        error("Conflict committed: $commit")
    }
    check(run("git", "commit", "--no-edit")) { "Could not commit merge: $commit" }
    saveLastMerged()

    if (timestamp - lastCompilationTime >= day || index == commits.lastIndex) {
        if (!compile()) bisect(lastKnownGood)
        lastKnownGood = output("git", "rev-parse", "HEAD")
        lastCompilationTime = timestamp
    }
}

fun saveLastMerged() = lastMergedFile.writeText("${output("git", "rev-parse", "HEAD")}\n")

fun compile() = if (System.getProperty("os.name").startsWith("Windows")) {
    run("cmd", "/c", "gradlew", "assemble", "compileTest")
} else {
    run("./gradlew", "assemble", "compileTest")
}

fun bisect(lastKnownGood: String): Nothing {
    check(run("git", "bisect", "start", "--first-parent", "HEAD", lastKnownGood))
    while (true) {
        val result = if (compile()) "good" else "bad"
        val (code, text) = execute("git", "bisect", result)
        check(code == 0) { text }
        if ("is the first bad commit" in text) {
            val firstBad = output("git", "rev-parse", "HEAD")
            check(run("git", "bisect", "reset"))
            check(run("git", "reset", "--hard", firstBad))
            saveLastMerged()
            error("Build regression found. Reset at the first bad merge commit: $firstBad")
        }
    }
}

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
