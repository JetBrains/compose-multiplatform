import java.io.File
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.StandardCopyOption.ATOMIC_MOVE
import java.nio.file.StandardCopyOption.REPLACE_EXISTING
import java.text.SimpleDateFormat
import java.util.Date

val scriptDir = getScriptPathFromArgs().parentFile

val log = scriptDir.resolve("log.txt")
val lastMergedFile = scriptDir.resolve("lastMerged.txt")

// A process killed while a trial merge is running leaves MERGE_HEAD behind.
// Discard that uncommitted trial before starting the next one.
if (run("git", "rev-parse", "-q", "--verify", "MERGE_HEAD")) {
    check(run("git", "merge", "--abort")) {
        "Could not abort the unfinished merge. Inspect the working tree before rerunning."
    }
}

// Verify that the previous completed-merge checkpoint belongs to this branch.
if (lastMergedFile.exists()) {
    val savedCommit = lastMergedFile.readText().trim()
    val commitExistsInBranch = run("git", "merge-base", "--is-ancestor", savedCommit, "HEAD")
    check(commitExistsInBranch) {
        "Commit '$savedCommit' from lastMerged.txt does not exist in the current branch history. " +
            "You might be working on a different branch, or the commit was dropped/amended. " +
            "If you are sure that this is correct, remove the file manually."
    }
}

check(run("git", "fetch", "aosp"))

val remote = "aosp/androidx-main"

data class Version(val commit: String, val tag: String, val description: String)

val versions = scriptDir.resolve("versions.txt")
    .readLines()
    .mapNotNull { line ->
        val fields = line.trim().split(Regex("\\s+"))
        val commit = fields.getOrNull(2)
            ?.takeIf { it.matches(Regex("[0-9a-fA-F]{40}")) }
            ?: return@mapNotNull null
        Version(commit, "build ${fields[3]}", "${fields[0]} ${fields[1]}")
    }
    .map { version ->
        val base = output("git", "merge-base", version.commit, remote)
        version.copy(
            commit = base,
            tag = version.tag + if (base == version.commit) "" else " base",
        )
    }
    .distinct()

check(versions.isNotEmpty())

val versionCommits = versions.map { it.commit }.toSet()
val lastMerged = output("git", "merge-base", "HEAD", remote)
val datedVersions = output(
    "git",
    "show",
    "-s",
    "--no-walk",
    "--format=%H %ct",
    *(versionCommits + lastMerged).toTypedArray(),
).lineSequence()
    .filter { it.isNotBlank() }
    .map { line ->
        val (commit, timestamp) = line.split(" ", limit = 2)
        commit to timestamp.toLong()
    }
    .toList()
val lastMergedDate = datedVersions.first { it.first == lastMerged }.second
val newest = datedVersions
    .filter { it.first in versionCommits }
    .maxBy { it.second }
val commits = datedVersions
    .filter { it.first in versionCommits && it.second > lastMergedDate }
    .toMutableSet()
var day = lastMergedDate

output(
    "git",
    "rev-list",
    "--timestamp",
    "--first-parent",
    "--reverse",
    "$lastMerged..${newest.first}",
)
    .lineSequence()
    .filter { it.isNotBlank() }
    .forEach { line ->
        val (timestamp, commit) = line.split(" ", limit = 2)
        val d = timestamp.toLong()
        if (d - day >= 86400) {
            commits += commit to d
            day = d
        }
    }

if (newest.second > lastMergedDate) {
    commits += newest
}

val publishCommand = if (System.getProperty("os.name").startsWith("Windows")) {
    arrayOf(
        "cmd",
        "/c", 
        "gradlew",
        "assemble",
        "compileTest",
    )
} else {
    arrayOf(
        "./gradlew",
        "assemble",
        "compileTest",
    )
}

fun compile() = run(*publishCommand)

val dateFormat = SimpleDateFormat("dd-MM-yyyy")

fun merge(commit: String, timestamp: Long): Boolean {
    val matchingVersions = versions.filter { it.commit == commit }
    val optionalTag = matchingVersions.firstOrNull()?.tag
    val date = dateFormat.format(Date(timestamp * 1000))
    val text = "Merge $date ${optionalTag?.plus(" ") ?: ""}$commit"
    print(text)
    System.out.flush()
    return if (matchingVersions.isEmpty()) {
        run("git", "merge", "--no-commit", "--no-ff", commit)
    } else {
        run(
            "git",
            "merge",
            "--no-commit",
            "--no-ff",
            "-m",
            "Merge commit '$commit' ($optionalTag)\n\n${matchingVersions.joinToString("\n") { it.description }}",
            commit,
        )
    }
}

fun mergeBinary(to: String, timestamp: Long) {
    val from = output("git", "merge-base", "HEAD", remote)
    val range = output(
        "git",
        "rev-list",
        "--timestamp",
        "--first-parent",
        "--reverse",
        "$from..$to",
    )
        .lineSequence()
        .filter { it.isNotBlank() }
        .map { line ->
            val (time, commit) = line.split(" ", limit = 2)
            commit to time.toLong()
        }
        .toList()
    if (range.isEmpty()) {
        return
    }
    if (!merge(to, timestamp)) {
        println(" FAILED")
        if (range.size == 1) {
            val conflictingFiles = output(
                "git",
                "diff",
                "--name-only",
                "--diff-filter=U",
            ).lines().filter { it.isNotBlank() }
            check(conflictingFiles.isNotEmpty()) { "No conflicting files: $to" }
            check(
                run(
                    "git",
                    "add",
                    "-A",
                    "--",
                    *conflictingFiles.toTypedArray(),
                ),
            )
            check(run("git", "commit", "--no-edit"))
            saveLastMerged()
            error("Conflict committed: $to")
        }
        check(run("git", "merge", "--abort"))
    } else if (!compile()) {
        println(" FAILED")
        if (range.size == 1) {
            check(run("git", "commit", "--no-edit"))
            saveLastMerged()
            error("Compilation failed: $to")
        }
        check(run("git", "merge", "--abort"))
    } else {
        check(run("git", "commit", "--no-edit"))
        println()
        saveLastMerged()
        return
    }
    val middle = range[(range.size - 1) / 2]
    mergeBinary(middle.first, middle.second)
    mergeBinary(to, timestamp)
}

check(compile()) { "Compilation failed" }

commits.sortedBy { it.second }.forEach { (commit, timestamp) ->
    mergeBinary(commit, timestamp)
}

fun saveLastMerged() {
    writeAtomically(lastMergedFile, "${output("git", "rev-parse", "HEAD")}\n")
}

fun writeAtomically(file: File, content: String) {
    val temporaryFile = scriptDir.resolve("${file.name}.tmp")
    temporaryFile.writeText(content)
    try {
        Files.move(temporaryFile.toPath(), file.toPath(), ATOMIC_MOVE, REPLACE_EXISTING)
    } catch (_: AtomicMoveNotSupportedException) {
        Files.move(temporaryFile.toPath(), file.toPath(), REPLACE_EXISTING)
    }
}

fun execute(vararg args: String): Pair<Int, String> {
    val process = ProcessBuilder(*args).redirectErrorStream(true).start()
    val text = process.inputStream.bufferedReader().readText().trim()
    if (text.isNotEmpty()) {
        log.appendText("$text\n")
    }
    return process.waitFor() to text
}

fun output(vararg args: String): String {
    val (code, text) = execute(*args)
    check(code == 0) { text }
    return text
}

fun run(vararg args: String) = execute(*args).first == 0

// Avoid using __FILE__ here since it has a caching bug
// https://youtrack.jetbrains.com/issue/KT-77524/
fun getScriptPathFromArgs(): File =
    System.getProperty("sun.java.command")
        ?.split(" ")
        ?.find { it.endsWith(".kts") }
        ?.let { File(it).canonicalFile }
        ?: error("Can't find script path from args: ${System.getProperty("sun.java.command")}")
