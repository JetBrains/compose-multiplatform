#!/usr/bin/env kotlin
import java.io.File
import java.util.*

/**
 * Usage: ./compare_benchmarks.main.kts v1=<version1> v2=<version2> [runs=3] [benchmarks=<benchmarkName>] [platform=macos|desktop]
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
        println("Usage: ./compare_benchmarks.main.kts v1=<version1> v2=<version2> [runs=3] [benchmarks=<benchmarkName>] [platform=macos|desktop|web|ios]")
        return
    }

    val runs = (argMap["runs"] ?: args.getOrNull(2))?.toIntOrNull() ?: 3
    val benchmarkName = argMap["benchmarks"] ?: null
    val platform = argMap["platform"] ?: "macos"
    val runServer = platform == "web"
    val isWeb = platform == "web"
    val isIos = platform == "ios"

    println("Comparing Compose versions: $v1 and $v2")
    println("Number of runs: $runs")
    println("Platform: $platform")
    benchmarkName?.let { println("Filtering by benchmark: $it") }

    val resultsV1 = runBenchmarksForVersion(v1, runs, benchmarkName, platform, runServer, isWeb, isIos)
    val resultsV2 = runBenchmarksForVersion(v2, runs, benchmarkName, platform, runServer, isWeb, isIos)

    compareResults(v1, resultsV1, v2, resultsV2)
}

data class BenchmarkResult(val name: String, val totalMs: Double)

fun runBenchmarksForVersion(version: String, runs: Int, benchmarkName: String?, platform: String, runServer: Boolean, isWeb: Boolean, isIos: Boolean): Map<String, List<Double>> {
    println("\n=== Running benchmarks for version: $version ===")

    val allRunsResults = mutableMapOf<String, MutableList<Double>>()

    for (i in 1..runs) {
        println("Run $i/$runs...")
        executeBenchmarks(version, i, benchmarkName, platform, runServer, isWeb, isIos)
        val runResults = collectResults(version, i)
        runResults.forEach { (name, value) ->
            allRunsResults.getOrPut(name) { mutableListOf() }.add(value)
        }
    }

    return allRunsResults
}

fun updateComposeVersion(version: String) {
    val tomlFile = File("gradle/libs.versions.toml")
    val content = tomlFile.readText()
    val newContent = content.replace(Regex("""compose-multiplatform\s*=\s*"[^"]+""""), "compose-multiplatform = \"$version\"")
    tomlFile.writeText(newContent)
    println("Updated gradle/libs.versions.toml to version $version")
}

fun executeBenchmarks(version: String, runIndex: Int, benchmarkName: String?, platform: String, runServer: Boolean, isWeb: Boolean, isIos: Boolean) {
    val (versionedExecutable, defaultExecutable, task) = when (platform) {
        "macos" -> Triple(
            File("benchmarks/build/bin/macosArm64/releaseExecutable/benchmarks-$version.kexe"),
            File("benchmarks/build/bin/macosArm64/releaseExecutable/benchmarks.kexe"),
            ":benchmarks:runReleaseExecutableMacosArm64"
        )
        "desktop" -> Triple(
            null, 
            null, 
            ":benchmarks:run"
        )
        "web" -> Triple(
            null,
            null,
            ":benchmarks:wasmJsBrowserProductionRun"
        )
        "ios" -> Triple(
            null,
            null,
            "ios"
        )
        else -> throw IllegalArgumentException("Unsupported platform: $platform")
    }

    val runArgs = mutableListOf(
        "modes=SIMPLE",
        "saveStatsToJSON=true",
        "frameCount=1000",
        "versionInfo=${version}_run${runIndex}"
    )
    if (runServer) {
        runArgs.add("runServer=true")
    }
    if (benchmarkName != null) {
        runArgs.add("benchmarks=$benchmarkName")
    }

    if (runServer) {
        println("Starting benchmark server in background...")
        // For web, the server is a desktop application with Config.runServer = true
        val serverProcess = ProcessBuilder(
            "./gradlew", ":benchmarks:run",
            "-PrunArguments=runServer=true saveStatsToJson=true"
        ).redirectErrorStream(true).start()
        
        val serverOutput = serverProcess.inputStream.bufferedReader()
        val serverStopped = java.util.concurrent.atomic.AtomicBoolean(false)
        
        // Thread to monitor server output
        val monitorThread = Thread {
            try {
                var line: String?
                while (serverOutput.readLine().also { line = it } != null) {
                    println("[SERVER] $line")
                    if (line?.contains("Benchmark server stopped") == true) {
                        println("Detected server stop signal.")
                        serverStopped.set(true)
                        break
                    }
                }
            } catch (e: Exception) {
                // Ignore
            }
        }
        monitorThread.start()
        
        // Wait a bit for server to start
        Thread.sleep(5000)
        
        try {
            executeBenchmarksOnce(version, platform, task, runArgs, versionedExecutable, defaultExecutable, isWeb, isIos, serverStopped)
        } finally {
            println("Stopping benchmark server...")
            serverProcess.destroy()
            // Make sure the server is really dead as it might have spawned other processes
            serverProcess.destroyForcibly()
            monitorThread.interrupt()
        }
    } else {
        executeBenchmarksOnce(version, platform, task, runArgs, versionedExecutable, defaultExecutable, isWeb, isIos, null)
    }
}

fun executeBenchmarksOnce(
    version: String, 
    platform: String,
    task: String,
    runArgs: List<String>,
    versionedExecutable: File?,
    defaultExecutable: File?,
    isWeb: Boolean,
    isIos: Boolean,
    serverStopped: java.util.concurrent.atomic.AtomicBoolean?
) {
    if (isIos) {
        println("Running version $version on iOS...")
        updateComposeVersion(version)
        val processBuilder = ProcessBuilder(
            "./run_ios_benchmarks.main.kts",
            *runArgs.toTypedArray()
        ).inheritIO()
        val process = processBuilder.start()
        val exitCode = process.waitFor()
        if (exitCode != 0) {
            println("Warning: iOS Benchmark run failed with exit code $exitCode")
        }
        return
    }

    if (versionedExecutable != null && versionedExecutable.exists()) {
        println("Using existing executable for version $version: ${versionedExecutable.absolutePath}")

        val process = ProcessBuilder(versionedExecutable.absolutePath, *runArgs.toTypedArray())
            .directory(File("benchmarks")).inheritIO().start()
        
        val exitCode = process.waitFor()
        if (exitCode != 0) {
            println("Warning: Benchmark run failed with exit code $exitCode")
        }
    } else {
        if (versionedExecutable == null) {
            println("Running version $version via gradle task $task...")
        } else {
            println("Executable for version $version not found. Building...")
        }
        updateComposeVersion(version)
        
        val processBuilder = ProcessBuilder(
            "./gradlew", task,
            "-PrunArguments=${runArgs.joinToString(" ")}"
        ).inheritIO()
        
        val process = processBuilder.start()

        val exitCode = if (isWeb) {
            println("Waiting for benchmarks to complete (timeout 5m)...")
            // Try to find if results were already saved
            val startTime = System.currentTimeMillis()
            val timeout = 5 * 60 * 1000L
            var completed = false
            
            while (System.currentTimeMillis() - startTime < timeout) {
                if (process.waitFor(10, java.util.concurrent.TimeUnit.SECONDS)) {
                    completed = true
                    break
                }
                
                if (serverStopped?.get() == true) {
                    println("Server stopped signal received. Benchmarks should be finished.")
                    completed = true
                    break
                }
            }
            
            if (!completed) {
                println("Timeout reached or task is non-terminating. Stopping benchmark task...")
            } else {
                println("Stopping benchmark task...")
            }
            process.destroy()
            process.destroyForcibly()
            0 
        } else {
            process.waitFor()
        }

        if (exitCode != 0) {
            println("Warning: Benchmark run failed with exit code $exitCode")
        } else if (platform == "macos") {
            if (defaultExecutable != null && defaultExecutable.exists()) {
                if (defaultExecutable.renameTo(versionedExecutable!!)) {
                    println("Renamed executable to ${versionedExecutable.name}")
                } else {
                    println("Warning: Failed to rename executable to ${versionedExecutable.name}")
                }
            } else {
                println("Warning: Could not find produced executable at ${defaultExecutable?.absolutePath}")
            }
        }
    }
}

fun collectResults(version: String, runIndex: Int): Map<String, Double> {
    val resultsDir = File("benchmarks/build/benchmarks/json-reports")
    val resultMap = mutableMapOf<String, Double>()
    
    if (!resultsDir.exists()) {
        println("Warning: Results directory not found: ${resultsDir.absolutePath}")
        return resultMap
    }

    resultsDir.listFiles { f -> f.extension == "json" }?.forEach { file ->
        val content = file.readText()
        // Simple manual parsing of JSON to avoid dependencies in the script
        // Expecting something like: "TOTAL avg frame (ms)": "1.23" OR "totalTime": "1.23ms"
        // Based on Benchmarks.kt: FrameInfo has cpuTime and gpuTime. 
        // BenchmarkStats has averageFrameInfo: FrameInfo?
        // Let's try to find totalTime or cpuTime + gpuTime
        
        val totalMs = parseTotalMsFromJson(content)
        if (totalMs != null) {
            resultMap[file.nameWithoutExtension] = totalMs
        }
        
        // Move file to avoid conflict with next run
        val archiveDir = File("benchmarks/build/benchmarks/archive/${version}_run${runIndex}")
        archiveDir.mkdirs()
        file.renameTo(File(archiveDir, file.name))
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
