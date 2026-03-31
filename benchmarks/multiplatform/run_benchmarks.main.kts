#!/usr/bin/env kotlin
import java.io.File

/**
 * Usage: ./run_benchmarks.main.kts <platform> [runs=1] [benchmarks=<benchmarkName>] [version=<version>] [any other gradle args]
 *
 * Example: ./run_benchmarks.main.kts macos benchmarks=LazyList runs=3 version=1.10.0
 *
 * JSON results are archived to: benchmarks/build/benchmarks/archive/${platform}/${version}_run${runIndex}
 */
fun main(args: Array<String>) {
    if (args.isEmpty()) {
        println("Usage: ./run_benchmarks.main.kts <platform> [runs=1] [benchmarks=<benchmarkName>] [version=<version>] [any other gradle args]")
        println("Platforms: macos, desktop, web, ios")
        println("Arguments:")
        println("  runs=<number> (default: 1)")
        println("  benchmarks=<name1,name2,...>")
        println("  version=<version>")
        println("  separateProcess=true|false (default: false)")
        println("  any other gradle args")
        return
    }

    val platform = args[0]
    val remainingArgs = args.drop(1)
    
    val argMap = remainingArgs.associate {
        val split = it.split("=", limit = 2)
        if (split.size == 2) split[0] to split[1] else it to ""
    }.toMutableMap()

    val runs = argMap.remove("runs")?.toIntOrNull() ?: 1
    val versionArg = argMap.remove("version")
    val version = versionArg ?: getCurrentComposeVersion()
    val benchmarkName = argMap["benchmarks"]
    val separateProcess = argMap.remove("separateProcess")?.toBoolean() ?: false
    
    val runServer = platform == "web"
    val isWeb = platform == "web"
    val isIos = platform == "ios"

    println("Running benchmarks for platform: $platform")
    println("Compose version: $version")
    println("Number of runs: $runs")
    println("Separate process: $separateProcess")
    println("Run server: $runServer")
    benchmarkName?.let { println("Filtering by benchmark: $it") }

    if (runs <= 0) {
        println("Nothing to run (runs=$runs)")
        return
    }

    val tomlFile = File("gradle/libs.versions.toml")
    val originalTomlContent = if (versionArg != null && tomlFile.exists()) tomlFile.readText() else null

    try {
        if (versionArg != null) {
            updateComposeVersion(version)
        }

        for (i in 1..runs) {
            println("\nRun $i/$runs...")
            // Clean up previous results to ensure we don't archive old ones
            val resultsDir = File("benchmarks/build/benchmarks/json-reports")
            if (resultsDir.exists()) {
                resultsDir.listFiles { f -> f.extension == "json" }?.forEach { it.delete() }
            }
            
            executeBenchmarks(version, i, platform, runServer, isWeb, isIos, separateProcess, benchmarkName, argMap)
            collectResults(platform, version, i)
        }
    } finally {
        if (originalTomlContent != null) {
            tomlFile.writeText(originalTomlContent)
            println("Restored gradle/libs.versions.toml")
        }
    }
}

fun collectResults(platform: String, version: String, runIndex: Int) {
    val resultsDir = File("benchmarks/build/benchmarks/json-reports")
    if (!resultsDir.exists()) return

    resultsDir.listFiles { f -> f.extension == "json" }?.forEach { file ->
        val archiveDir = File("benchmarks/build/benchmarks/archive/${platform}/${version}_run${runIndex}")
        archiveDir.mkdirs()
        val targetFile = File(archiveDir, file.name)
        if (file.renameTo(targetFile)) {
            println("Archived ${file.name} to ${targetFile.path}")
        }
    }
}

fun getCurrentComposeVersion(): String {
    val tomlFile = File("gradle/libs.versions.toml")
    if (!tomlFile.exists()) return "unknown"
    val content = tomlFile.readText()
    val match = Regex("""compose-multiplatform\s*=\s*"([^"]+)"""").find(content)
    return match?.groupValues?.get(1) ?: "unknown"
}

fun updateComposeVersion(version: String) {
    val tomlFile = File("gradle/libs.versions.toml")
    val content = tomlFile.readText()
    val newContent = content.replace(Regex("""compose-multiplatform\s*=\s*"[^"]+""""), "compose-multiplatform = \"$version\"")
    tomlFile.writeText(newContent)
    println("Updated gradle/libs.versions.toml to version $version")
}

fun executeBenchmarks(
    version: String, 
    runIndex: Int,
    platform: String,
    runServer: Boolean, 
    isWeb: Boolean, 
    isIos: Boolean,
    separateProcess: Boolean,
    benchmarkName: String?,
    extraArgs: Map<String, String>
) {
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
        "versionInfo=${version}_run${runIndex}"
    )
    if (runServer) {
        runArgs.add("runServer=true")
    }

    // Add extra args provided by user, override defaults if necessary
    extraArgs.forEach { (k, v) ->
        if (v.isNotEmpty()) {
            runArgs.add("$k=$v")
        } else {
            runArgs.add(k)
        }
    }

    if (runServer) {
        println("Starting benchmark server in background...")
        // For web, the server is a desktop application with Config.runServer = true
        val serverRunArgs = mutableListOf("runServer=true")
        if (extraArgs.containsKey("saveStatsToJSON")) serverRunArgs.add("saveStatsToJSON=${extraArgs["saveStatsToJSON"]}")
        if (extraArgs.containsKey("saveStatsToCSV")) serverRunArgs.add("saveStatsToCSV=${extraArgs["saveStatsToCSV"]}")

        val processBuilder = ProcessBuilder(
            "./gradlew", ":benchmarks:run",
            "-PrunArguments=${serverRunArgs.joinToString(" ")}"
        ).redirectErrorStream(true)
        val serverProcess = processBuilder.start()
        
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
            executeBenchmarksWithPlatform(version, platform, task, runArgs, versionedExecutable, defaultExecutable, isWeb, isIos, separateProcess,
                serverStopped, benchmarkName)
        } finally {
            println("Stopping benchmark server...")
            serverProcess.destroy()
            // Make sure the server is really dead as it might have spawned other processes
            serverProcess.destroyForcibly()
            monitorThread.interrupt()
        }
    } else {
        executeBenchmarksWithPlatform(version, platform, task, runArgs, versionedExecutable, defaultExecutable, isWeb, isIos, separateProcess,
            null, benchmarkName)
    }
}

fun executeBenchmarksWithPlatform(
    version: String,
    platform: String,
    task: String,
    runArgs: List<String>,
    versionedExecutable: File?,
    defaultExecutable: File?,
    isWeb: Boolean,
    isIos: Boolean,
    separateProcess: Boolean,
    serverStopped: java.util.concurrent.atomic.AtomicBoolean?,
    benchmarksToRun: String?
) {
    if (separateProcess) {
        println("Running benchmarks in separate processes...")
        val benchmarks = if (!benchmarksToRun.isNullOrBlank()) {
            benchmarksToRun.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        } else {
            listBenchmarks()
        }
        for (benchmark in benchmarks) {
            println("\n--- Running benchmark: $benchmark ---")
            val benchmarkArgs = runArgs.toMutableList()
            // Replace or add benchmarks parameter
            benchmarkArgs.removeIf { it.startsWith("benchmarks=") }
            benchmarkArgs.add("benchmarks=$benchmark")
            executeBenchmarksOnce(version, platform, task, benchmarkArgs, versionedExecutable, defaultExecutable, isWeb, isIos, serverStopped)
        }
    } else {
        executeBenchmarksOnce(version, platform, task, runArgs, versionedExecutable, defaultExecutable, isWeb, isIos, serverStopped)
    }
}

fun listBenchmarks(): List<String> {
    println("Fetching list of benchmarks using desktop run task...")

    val process = ProcessBuilder(
        "./gradlew", ":benchmarks:run",
        "-PrunArguments=listBenchmarks=true"
    ).start()
    val output = process.inputStream.bufferedReader().readText()
    process.waitFor()
    
    val benchmarks = mutableListOf<String>()
    var inList = false
    output.lines().forEach { line ->
        if (line.contains("AVAILABLE_BENCHMARKS_START")) {
            inList = true
        } else if (line.contains("AVAILABLE_BENCHMARKS_END")) {
            inList = false
        } else if (inList && line.isNotBlank()) {
            benchmarks.add(line.trim())
        }
    }
    
    if (benchmarks.isEmpty()) {
        println("Warning: Could not fetch benchmarks list. Output: $output")
    }
    return benchmarks
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

main(args)
