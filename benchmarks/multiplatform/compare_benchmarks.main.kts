#!/usr/bin/env kotlin
import java.io.File
import java.util.*

/**
 * Usage: ./compare_benchmarks.main.kts v1=<version1> v2=<version2> [runs=3] [benchmarks=<benchmarkName>] [platform=macos|desktop|web|ios] [skipExisting=true]
 *
 * Note: to run the script, you need to install Kotlin as described here: https://kotlinlang.org/docs/command-line.html#install-the-compiler
 */
fun main(args: Array<String>) {
    val argMap = args.associate {
        val split = it.split("=", limit = 2)
        if (split.size == 2) split[0] to split[1] else it to ""
    }

    val v1 = argMap["v1"] ?: args.getOrNull(0)
    val v2 = argMap["v2"] ?: args.getOrNull(1)
    
    if (v1 == null || v2 == null) {
        println("Usage: ./compare_benchmarks.main.kts v1=<version1> v2=<version2> [runs=3] [benchmarks=<benchmarkName>] [platform=macos|desktop|web|ios] [skipExisting=true] [separateProcess=true]")
        return
    }

    val runs = (argMap["runs"] ?: args.getOrNull(2))?.toIntOrNull() ?: 3
    val benchmarkName = argMap["benchmarks"] ?: null
    val platform = argMap["platform"] ?: "macos"
    val skipExisting = argMap["skipExisting"]?.toBoolean() ?: false
    val separateProcess = argMap["separateProcess"]?.toBoolean() ?: false
    val runServer = platform == "web"
    val isWeb = platform == "web"
    val isIos = platform == "ios"

    println("Comparing Compose versions: $v1 and $v2")
    println("Number of runs: $runs")
    println("Platform: $platform")
    if (skipExisting) {
        println("Skip existing results: true")
    }
    if (separateProcess) {
        println("Separate process: true")
    }
    benchmarkName?.let { println("Filtering by benchmark: $it") }

    val resultsV1 = runBenchmarksForVersion(v1, runs, benchmarkName, platform, runServer, isWeb, isIos, skipExisting, separateProcess)
    val resultsV2 = runBenchmarksForVersion(v2, runs, benchmarkName, platform, runServer, isWeb, isIos, skipExisting, separateProcess)

    compareResults(v1, resultsV1, v2, resultsV2)
}

data class BenchmarkResult(val name: String, val totalMs: Double)

fun runBenchmarksForVersion(version: String, runs: Int, benchmarkName: String?, platform: String, runServer: Boolean, isWeb: Boolean, isIos: Boolean, skipExisting: Boolean, separateProcess: Boolean): Map<String, List<Double>> {
    println("\n=== Running benchmarks for version: $version ===")

    val allRunsResults = mutableMapOf<String, MutableList<Double>>()

    var allExist = true
    if (skipExisting) {
        for (i in 1..runs) {
            val archiveDir = File("benchmarks/build/benchmarks/archive/${platform}/${version}_run${i}")
            if (!archiveDir.exists() || archiveDir.listFiles { f -> f.extension == "json" }?.isEmpty() != false) {
                allExist = false
                break
            }
        }
    } else {
        allExist = false
    }

    if (allExist) {
        println("All $runs runs already exist for version $version, skipping benchmarks execution.")
    } else {
        val runArgs = mutableListOf<String>()
        runArgs.add(platform)
        runArgs.add("runs=$runs")
        runArgs.add("version=$version")
        runArgs.add("modes=SIMPLE")
        runArgs.add("saveStatsToJSON=true")
        runArgs.add("frameCount=1000")
        if (separateProcess) {
            runArgs.add("separateProcess=true")
        }
        if (benchmarkName != null) {
            runArgs.add("benchmarks=$benchmarkName")
        }

        val processBuilder = ProcessBuilder(
            "./run_benchmarks.main.kts",
            *runArgs.toTypedArray()
        ).inheritIO()
        
        val process = processBuilder.start()
        val exitCode = process.waitFor()

        if (exitCode != 0) {
            println("Warning: run_benchmarks.main.kts failed with exit code $exitCode")
        }
    }

    for (i in 1..runs) {
        val runResults = collectResults(platform, version, i)
        runResults.forEach { (name, value) ->
            allRunsResults.getOrPut(name) { mutableListOf() }.add(value)
        }
    }

    return allRunsResults
}

fun collectResults(platform: String, version: String, runIndex: Int): Map<String, Double> {
    val archiveDir = File("benchmarks/build/benchmarks/archive/${platform}/${version}_run${runIndex}")
    val resultMap = mutableMapOf<String, Double>()
    
    if (!archiveDir.exists()) {
        println("Warning: Results directory not found: ${archiveDir.absolutePath}")
        return resultMap
    }

    archiveDir.listFiles { f -> f.extension == "json" }?.forEach { file ->
        val content = file.readText()
        val totalMs = parseTotalMsFromJson(content)
        if (totalMs != null) {
            resultMap[file.nameWithoutExtension] = totalMs
        }
    }
    
    return resultMap
}

fun parseTotalMsFromJson(json: String): Double? {
    // Example: "averageFrameInfo": { "cpuTime": "10.5ms", "gpuTime": "2.1ms" }
    // Duration might be serialized as ISO-8601 string: "PT0.0105S"
    
    fun extractDurationMs(key: String): Double? {
        val pattern = Regex(""""$key":\s*"([^"]+)"""")
        val match = pattern.find(json) ?: return null
        val value = match.groupValues[1]
        
        // Handle ISO-8601 duration (e.g. PT0.0105S)
        if (value.startsWith("PT") && value.endsWith("S")) {
            return value.substring(2, value.length - 1).toDoubleOrNull()?.let { it * 1000.0 }
        }

        return when {
            value.endsWith("ms") -> value.removeSuffix("ms").toDoubleOrNull()
            value.endsWith("s") && !value.endsWith("ms") -> value.removeSuffix("s").toDoubleOrNull()?.let { it * 1000.0 }
            value.endsWith("ns") -> value.removeSuffix("ns").toDoubleOrNull()?.let { it / 1_000_000.0 }
            else -> value.toDoubleOrNull() // Assume ms if no unit?
        }
    }

    val cpu = extractDurationMs("cpuTime") ?: 0.0
    val gpu = extractDurationMs("gpuTime") ?: 0.0
    
    if (cpu == 0.0 && gpu == 0.0) {
        // Try another format just in case
        val total = extractDurationMs("totalTime")
        if (total != null) return total
        return null
    }
    
    return cpu + gpu
}

fun compareResults(v1: String, res1: Map<String, List<Double>>, v2: String, res2: Map<String, List<Double>>) {
    println("\n=== Comparison Report (Diff > 5%) ===")
    println(String.format("%-30s | %-15s | %-15s | %-10s", "Benchmark", v1 + " (avg ms)", v2 + " (avg ms)", "Diff %"))
    println("-".repeat(75))

    val allBenchmarks = (res1.keys + res2.keys).sorted()

    for (name in allBenchmarks) {
        val times1 = res1[name]
        val times2 = res2[name]

        if (times1 == null || times2 == null) {
            println(String.format("%-30s | %-15s | %-15s | Missing data", name, 
                times1?.average()?.let { String.format("%.3f", it) } ?: "N/A",
                times2?.average()?.let { String.format("%.3f", it) } ?: "N/A"))
            continue
        }

        val avg1 = times1.average()
        val avg2 = times2.average()
        val diff = if (avg1 != 0.0) (avg2 - avg1) / avg1 * 100.0 else 0.0

        if (Math.abs(diff) > 5.0) {
            println(String.format("%-30s | %-15.3f | %-15.3f | %+.2f%%", name, avg1, avg2, diff))
        } else {
            // Optional: still print but maybe mark as "OK" or just skip
             println(String.format("%-30s | %-15.3f | %-15.3f | %+.2f%% (OK)", name, avg1, avg2, diff))
        }
    }
}

fun List<Double>.average(): Double = if (isEmpty()) 0.0 else sum() / size

main(args)
