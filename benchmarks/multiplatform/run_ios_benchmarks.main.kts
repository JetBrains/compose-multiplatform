#!/usr/bin/env kotlin

import java.io.File
import java.io.InputStream
import java.util.*
import java.util.concurrent.TimeUnit

/**
 * run_ios_benchmarks.main.kts
 *
 * Builds the iosApp, installs it on a real device or simulator, then runs
 * every benchmark (from Benchmarks.kt).
 * Console output is saved to:
 *
 *   build/benchmarks/text-reports/<BenchmarkName>.txt
 *
 * Requirements:
 *   - Xcode 15+ (uses xcrun devicectl for real devices, xcrun simctl for simulators)
 *   - For real device: connected via USB and trusted, valid code-signing identity
 *   - For simulator: any booted or available simulator
 *
 * Usage:  ./run_ios_benchmarks.main.kts [<device-udid>]
 *
 *   If no UDID is provided the first connected real device is used.
 *   Pass a simulator UDID to target a simulator instead.
 */

// ── Configuration ──────────────────────────────────────────────────────────────

val ROOT_DIR = File(".").absoluteFile
val PROJECT_DIR = File(ROOT_DIR, "iosApp")
val MULTIPLATFORM_DIR = ROOT_DIR
val OUTPUT_DIR = File(MULTIPLATFORM_DIR, "benchmarks/build/benchmarks")
val TEXT_REPORTS_DIR = File(OUTPUT_DIR, "text-reports")
val JSON_REPORTS_DIR = File(OUTPUT_DIR, "json-reports")
val SCHEME = "iosApp"
val CONFIGURATION = "Release"
val BUILD_DIR = File(MULTIPLATFORM_DIR, ".benchmark_build")

// ── Argument Parsing ──────────────────────────────────────────────────────────

val parsedArgs = mutableMapOf<String, String>()
val rawArgs = if (args.isEmpty() || (args.size == 1 && !args[0].contains("="))) {
    val gradleProps = File(ROOT_DIR, "gradle.properties")
    if (gradleProps.exists()) {
        val runArgsLine = gradleProps.readLines().find { it.startsWith("runArguments=") }
        runArgsLine?.substringAfter("runArguments=")?.split(" ")?.filter { it.isNotBlank() } ?: emptyList()
    } else {
        emptyList()
    }
} else {
    args.toList()
}

// UDID can be passed as a standalone first argument if it doesn't contain '='
val argUdidFromArgs = if (args.isNotEmpty() && !args[0].contains("=") && !args[0].startsWith("-")) args[0] else null

rawArgs.forEach { arg ->
    if (arg.contains("=")) {
        val (key, value) = arg.split("=", limit = 2)
        parsedArgs[key.lowercase()] = value
    }
}

val benchmarksFromArgs = parsedArgs["benchmarks"]?.split(",")?.map { it.substringBefore("(").trim() }?.filter { it.isNotEmpty() }
val separateProcess = parsedArgs["separateprocess"]?.toBoolean() ?: false

// Arguments to pass to the app
val appArgs = parsedArgs.toMutableMap()
appArgs.remove("separateprocess")

// ── Helpers ────────────────────────────────────────────────────────────────────

fun die(message: String): Nothing {
    System.err.println("\nERROR: $message")
    System.exit(1)
    throw RuntimeException(message)
}

fun exec(vararg command: String, workingDir: File = ROOT_DIR, redirectStderr: Boolean = true): String {
    val process = ProcessBuilder(*command)
        .directory(workingDir)
        .redirectErrorStream(redirectStderr)
        .start()

    val output = process.inputStream.bufferedReader().readText()
    val exitCode = process.waitFor()
    if (exitCode != 0) {
        if (!redirectStderr) {
            val error = process.errorStream.bufferedReader().readText()
            System.err.println(error)
        }
        println(output)
        die("Command failed with exit code $exitCode: ${command.joinToString(" ")}")
    }
    return output
}

fun execInheritIO(vararg command: String, workingDir: File = ROOT_DIR) {
    val process = ProcessBuilder(*command)
        .directory(workingDir)
        .inheritIO()
        .start()

    val exitCode = process.waitFor()
    if (exitCode != 0) {
        die("Command failed with exit code $exitCode: ${command.joinToString(" ")}")
    }
}

fun execWithTee(command: List<String>, outputFile: File, workingDir: File = ROOT_DIR): Int {
    val process = ProcessBuilder(command)
        .directory(workingDir)
        .redirectErrorStream(true)
        .start()

    val reader = process.inputStream.bufferedReader()
    outputFile.parentFile.mkdirs()
    outputFile.bufferedWriter().use { writer ->
        var line: String? = reader.readLine()
        var capturingJson = false
        var capturedJson = StringBuilder()
        while (line != null) {
            println(line)
            writer.write(line)
            writer.newLine()

            if (line == "JSON_START") {
                capturingJson = true
                capturedJson = StringBuilder()
            } else if (line == "JSON_END") {
                capturingJson = false
                saveCapturedJson(capturedJson.toString())
            } else if (capturingJson) {
                capturedJson.append(line).append("\n")
            }

            line = reader.readLine()
        }
    }

    return process.waitFor()
}

fun saveCapturedJson(json: String) {
    try {
        val jsonNode = json.trim()
        if (jsonNode.isEmpty()) return
        
        // Extract benchmark name from JSON
        val nameRegex = Regex("\"name\":\\s*\"([^\"]+)\"")
        val match = nameRegex.find(jsonNode)
        val benchmarkName = match?.groupValues?.get(1) ?: "unknown"
        
        JSON_REPORTS_DIR.mkdirs()
        val jsonFile = File(JSON_REPORTS_DIR, "${benchmarkName}.json")
        jsonFile.writeText(jsonNode)
        println("            → Captured JSON saved to ${jsonFile.relativeTo(ROOT_DIR).path}")
    } catch (e: Exception) {
        println("            ⚠  Failed to save captured JSON: ${e.message}")
    }
}

// ── 1. Detect target device or simulator ──────────────────────────────────────

println("\n==> [1/4] Detecting target...")

println("  $ xcrun xctrace list devices")
val xctraceOut = try {
    exec("xcrun", "xctrace", "list", "devices")
} catch (e: Exception) {
    die("Failed to run xctrace: ${e.message}")
}

fun parseDevices(output: String, sectionName: String): List<String> {
    val lines = output.lines()
    val result = mutableListOf<String>()
    var inSection = false
    for (line in lines) {
        if (line.startsWith("== $sectionName ==")) {
            inSection = true
            continue
        }
        if (line.startsWith("== ")) {
            inSection = false
            continue
        }
        if (inSection && line.isNotBlank()) {
            // Filter out Mac and ensure it has a version number
            if (!line.contains(" Mac ") && line.contains(Regex("""\(\d+\.\d+"""))) {
                result.add(line.trim())
            }
        }
    }
    return result
}

val allRealLines = parseDevices(xctraceOut, "Devices")
val allSimLines = parseDevices(xctraceOut, "Simulators")
val allDeviceLines = allRealLines + allSimLines

if (allDeviceLines.isEmpty()) {
    println(xctraceOut)
    die("No iOS device or simulator found.")
}

val deviceLine = if (argUdidFromArgs != null) {
    allDeviceLines.find { it.contains(argUdidFromArgs) } ?: run {
        println("Available devices and simulators:")
        allDeviceLines.forEach { println(it) }
        die("Device with UDID '$argUdidFromArgs' not found among the above.")
    }
} else {
    if (allRealLines.isNotEmpty()) allRealLines.first() else allSimLines.first()
}

val isSimulator = allSimLines.contains(deviceLine)

// Parse "Device Name (iOS Version) (UDID)"
// UDID is the last parenthesised token on the line.
val udidRegex = Regex("""\(([0-9A-Fa-f-]+)\)""")
val deviceId = udidRegex.findAll(deviceLine).lastOrNull()?.groupValues?.get(1) ?: die("Could not parse UDID from: $deviceLine")

val versionRegex = Regex("""\(([0-9]+\.[0-9.]+)\)""")
val deviceIos = versionRegex.findAll(deviceLine).firstOrNull()?.groupValues?.get(1) ?: die("Could not parse iOS version from: $deviceLine")

val deviceName = deviceLine.substringBefore(" ($deviceIos").trim()

// Normalize for filenames: lowercase, spaces→underscores, keep only [a-z0-9._-]
val devicePrefix = "${deviceName}_$deviceIos"
    .lowercase()
    .replace(" ", "_")
    .filter { it in 'a'..'z' || it in '0'..'9' || it == '.' || it == '_' || it == '-' }

println("    Name      : $deviceName")
println("    iOS       : $deviceIos")
println("    UDID      : $deviceId")
println("    Simulator : $isSimulator")
println("    Filename  : <Benchmark>.txt")

// ── 2. Build ───────────────────────────────────────────────────────────────────

println("\n==> [2/4] Building '$SCHEME' ($CONFIGURATION)...")
println("  $ mkdir -p $BUILD_DIR")
BUILD_DIR.mkdirs()

val xcodeLog = File(BUILD_DIR, "xcodebuild.log")

// Clean stale Kotlin Native build artifacts to avoid klib ABI version mismatches.
//println("  $ ./gradlew clean")
//execInheritIO("./gradlew", "clean")
//
println("  $ xcodebuild build -project ${PROJECT_DIR.path}/iosApp.xcodeproj -scheme $SCHEME -configuration $CONFIGURATION -destination id=$deviceId ONLY_ACTIVE_ARCH=YES SYMROOT=${BUILD_DIR.path}")

val xcodebuildProcess = ProcessBuilder(
    "xcodebuild", "build",
    "-project", "${PROJECT_DIR.path}/iosApp.xcodeproj",
    "-scheme", SCHEME,
    "-configuration", CONFIGURATION,
    "-destination", "id=$deviceId",
    "ONLY_ACTIVE_ARCH=YES",
    "SYMROOT=${BUILD_DIR.path}"
).redirectErrorStream(true).start()

xcodeLog.bufferedWriter().use { writer ->
    xcodebuildProcess.inputStream.bufferedReader().forEachLine { line ->
        writer.write(line)
        writer.newLine()
    }
}
val buildExit = xcodebuildProcess.waitFor()

if (buildExit != 0) {
    println("Build failed. Last 50 lines of xcodebuild output:")
    println("----------------------------------------------------")
    val logLines = xcodeLog.readLines()
    logLines.takeLast(50).forEach { println(it) }
    println("----------------------------------------------------")
    println("Full log: ${xcodeLog.path}")
    System.exit(1)
}

val appPath = if (isSimulator) {
    File(BUILD_DIR, "${CONFIGURATION}-iphonesimulator/ComposeBenchmarks.app")
} else {
    File(BUILD_DIR, "${CONFIGURATION}-iphoneos/ComposeBenchmarks.app")
}

if (!appPath.exists()) die("App bundle not found at expected path: ${appPath.path}")

println("  $ /usr/libexec/PlistBuddy -c 'Print CFBundleIdentifier' ${appPath.path}/Info.plist")
val bundleId = exec("/usr/libexec/PlistBuddy", "-c", "Print CFBundleIdentifier", "${appPath.path}/Info.plist").trim()
println("    Build  : OK")
println("    Bundle : $bundleId")

// ── 3. Install ─────────────────────────────────────────────────────────────────

println("\n==> [3/4] Installing...")

if (isSimulator) {
    // Boot the simulator if it is not already running.
    val simctlList = exec("xcrun", "simctl", "list", "devices")
    val simStateLine = simctlList.lines().find { it.contains(deviceId) }
    val isBooted = simStateLine?.contains("(Booted)") == true
    
    if (!isBooted) {
        println("  $ xcrun simctl boot $deviceId")
        exec("xcrun", "simctl", "boot", deviceId)
    }
    println("  $ xcrun simctl install $deviceId ${appPath.path}")
    exec("xcrun", "simctl", "install", deviceId, appPath.path)
} else {
    println("  $ xcrun devicectl device install app --device $deviceId ${appPath.path}")
    exec("xcrun", "devicectl", "device", "install", "app", "--device", deviceId, appPath.path)
}
println("    Installed.")

println("  $ mkdir -p $OUTPUT_DIR")
OUTPUT_DIR.mkdirs()

// ── 4. Run benchmarks ──────────────────────────────────────────────────────────

fun fetchBenchmarks(): List<String> {
    try {
        val output = exec("./gradlew", ":benchmarks:run", "-PrunArguments=listBenchmarks=true")
        val lines = output.lines()
        val startIndex = lines.indexOf("AVAILABLE_BENCHMARKS_START")
        val endIndex = lines.indexOf("AVAILABLE_BENCHMARKS_END")
        if (startIndex != -1 && endIndex != -1 && endIndex > startIndex) {
            return lines.subList(startIndex + 1, endIndex).filter { it.isNotBlank() }
        }
    } catch (e: Exception) {
        println("            ⚠  Failed to fetch benchmarks list via Gradle: ${e.message}")
    }
    return emptyList()
}

val benchmarksToRun = if (benchmarksFromArgs == null || benchmarksFromArgs.isEmpty()) fetchBenchmarks() else benchmarksFromArgs

if (benchmarksToRun.isEmpty()) {
    die("No benchmarks found to run.")
}

val total = if (separateProcess) benchmarksToRun.size else 1

println("\n==> [4/4] Running $total benchmark sessions")
if (separateProcess) {
    println("    ${benchmarksToRun.size} benchmarks individually\n")
} else {
    println("    All benchmarks together\n")
}

if (separateProcess) {
    for ((index, benchmark) in benchmarksToRun.withIndex()) {
        val (finalOutputDir, outFileName) = TEXT_REPORTS_DIR to "${benchmark}.txt"
        val outFile = File(finalOutputDir, outFileName)

        println("  [%3d/%3d]  %-52s".format(index + 1, total, benchmark))
        println("            → ${outFile.name} in ${finalOutputDir.relativeTo(ROOT_DIR).path}")

        val finalAppArgs = appArgs.toMutableMap()
        finalAppArgs["benchmarks"] = benchmark

        val exitCode = runBenchmark(deviceId, bundleId, isSimulator, finalAppArgs, outFile)

        if (exitCode != 0) {
            println("            ⚠  WARNING: process exited with code $exitCode")
        } else {
            println("            ✓  done")
        }

        // Brief cooldown between runs so the device settles
        println("  $ sleep 3")
        Thread.sleep(3000)
    }
} else {
    val (finalOutputDir, outFileName) = TEXT_REPORTS_DIR to "all_benchmarks.txt"
    val outFile = File(finalOutputDir, outFileName)

    println("  [%3d/%3d]  %-52s".format(1, total, "All Benchmarks"))
    println("            → ${outFile.name} in ${finalOutputDir.relativeTo(ROOT_DIR).path}")

    val finalAppArgs = appArgs.toMutableMap()
    finalAppArgs["benchmarks"] = benchmarksToRun.joinToString(",")

    val exitCode = runBenchmark(deviceId, bundleId, isSimulator, finalAppArgs, outFile)

    if (exitCode != 0) {
        println("            ⚠  WARNING: process exited with code $exitCode")
    } else {
        println("            ✓  done")
    }
}

fun runBenchmark(deviceId: String, bundleId: String, isSimulator: Boolean, finalAppArgs: Map<String, String>, outFile: File): Int {
    val runArgs = mutableListOf<String>()
    val appArgsList = finalAppArgs.map { "${it.key}=${it.value}" }

    return if (isSimulator) {
        runArgs.addAll(listOf("xcrun", "simctl", "launch", "--console", deviceId, bundleId))
        runArgs.addAll(appArgsList)
        println("  $ ${runArgs.joinToString(" ")}")
        execWithTee(runArgs, outFile)
    } else {
        runArgs.addAll(listOf("xcrun", "devicectl", "device", "process", "launch", "--console", "--device", deviceId, bundleId, "--"))
        runArgs.addAll(appArgsList)
        println("  $ ${runArgs.joinToString(" ")}")
        execWithTee(runArgs, outFile)
    }
}

println("\n==> All done!")
println("    %d output files written to: %s\n".format(total, TEXT_REPORTS_DIR.relativeTo(ROOT_DIR).path))
