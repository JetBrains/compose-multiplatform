#!/usr/bin/env kotlin
import java.io.File
import java.util.*

/**
 * Usage: ./find_degradation.main.kts <benchmarkName> <versionsFile> [platform=macos|desktop|web]
 * 
 * Performs a binary search to find the first version where degradation (>5%) 
 * was introduced compared to the first (baseline) version in the file.
 */

fun main(args: Array<String>) {
    val argMap = args.associate {
        val split = it.split("=", limit = 2)
        if (split.size == 2) split[0] to split[1] else it to ""
    }

    val benchmarkName = argMap["benchmarks"] ?: args.getOrNull(0)
    val versionsFileStr = argMap["versions"] ?: args.getOrNull(1)
    val platform = argMap["platform"] ?: "macos"

    if (benchmarkName == null || versionsFileStr == null) {
        println("Usage: find_degradation.main.kts benchmarks=<benchmarkName> versions=<versionsFile> [platform=macos|desktop|web]")
        return
    }

    val versionsFile = File(versionsFileStr)
    
    if (!versionsFile.exists()) {
        println("Versions file not found: ${versionsFile.absolutePath}")
        return
    }

    val versions = versionsFile.readLines().map { it.trim() }.filter { it.isNotEmpty() }
    
    if (versions.size < 2) {
        println("Need at least 2 versions in the file to find a degradation.")
        return
    }

    println("Finding degradation for benchmark: $benchmarkName")
    println("Versions to check: ${versions.joinToString(", ")}")
    println("Platform: $platform")

    val baselineVersion = versions.first()
    println("Baseline version: $baselineVersion")

    // We assume the first version is good and the last version might be bad.
    // If there's only 2 versions, we just compare them.
    
    var low = 1
    var high = versions.size - 1
    var firstDegradedIndex = -1

    while (low <= high) {
        val mid = (low + high) / 2
        val targetVersion = versions[mid]
        
        println("\n--- Checking version: $targetVersion (index $mid) ---")
        val isDegraded = checkDegradation(baselineVersion, targetVersion, benchmarkName, platform)
        
        if (isDegraded) {
            println("Degradation FOUND in $targetVersion")
            firstDegradedIndex = mid
            high = mid - 1 // Look for an earlier version that might also be degraded
        } else {
            println("No degradation in $targetVersion")
            low = mid + 1
        }
    }

    if (firstDegradedIndex != -1) {
        println("\n=== RESULT ===")
        println("Degradation was introduced in version: ${versions[firstDegradedIndex]}")
        println("Previous stable version: ${versions[firstDegradedIndex - 1]}")
    } else {
        println("\n=== RESULT ===")
        println("No degradation found in any of the provided versions compared to $baselineVersion")
    }
}

fun checkDegradation(v1: String, v2: String, benchmarkName: String, platform: String = "macos"): Boolean {
    // We call the comparison script and parse its output.
    // Alternatively, we could refactor the comparison script to be more "reusable" as a library,
    // but calling it as a separate process is safer for a script.
    
    val process = ProcessBuilder(
        "./compare_benchmarks.main.kts", "v1=$v1", "v2=$v2", "runs=3", "benchmarks=$benchmarkName", "platform=$platform"
    ).redirectErrorStream(true).start()
    
    val output = process.inputStream.bufferedReader().readText()
    process.waitFor()
    
    println(output)

    // The comparison script prints a line if diff > 5%.
    // We look for the benchmark name in the "Comparison Report" section.
    // Example line: Benchmark                      | 1.10.0 (avg ms) | 1.10.1 (avg ms) | Diff %
    // Example line: AnimationBenchmark             | 10.000          | 11.000          | +10.00%
    
    val lines = output.lines()
    val reportStartIndex = lines.indexOfFirst { it.contains("Comparison Report") }
    if (reportStartIndex == -1) return false
    
    for (i in reportStartIndex until lines.size) {
        val line = lines[i]
        if (line.contains(benchmarkName) && line.contains("%")) {
            // Found a diff report for our benchmark.
            // Check if it's positive (degradation) and > 5% (the script already filters by 5%).
            // Note: Lower time is better, so a positive percentage means it got slower.
            if (line.contains("+") && !line.contains("(OK)")) {
                return true
            }
        }
    }
    
    return false
}

main(args)
