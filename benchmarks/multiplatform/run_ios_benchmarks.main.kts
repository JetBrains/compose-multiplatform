#!/usr/bin/env kotlin

import java.io.File
import java.io.InputStream
import java.util.*
import java.util.concurrent.TimeUnit

/**
 * run_ios_benchmarks.main.kts
 *
 * Builds the iosApp, installs it on a real device or simulator, then runs
 * every benchmark (from Benchmarks.kt) with parallel=true and parallel=false,
 * ATTEMPTS times each. Console output is saved to:
 *
 *   benchmarks_result/<device>_<ios>_parallel_<true|false>_<BenchmarkName>_<N>.txt
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
val OUTPUT_DIR = File(MULTIPLATFORM_DIR, "benchmarks_result")
val SCHEME = "iosApp"
val CONFIGURATION = "Release"
val ATTEMPTS = 5
val BUILD_DIR = File(MULTIPLATFORM_DIR, ".benchmark_build")

val BENCHMARKS = listOf(
    "AnimatedVisibility",
    "LazyGrid",
    "LazyGrid-ItemLaunchedEffect",
    "LazyGrid-SmoothScroll",
    "LazyGrid-SmoothScroll-ItemLaunchedEffect",
    "VisualEffects",
    "LazyList",
    "MultipleComponents",
    "MultipleComponents-NoVectorGraphics",
    "TextLayout",
    "CanvasDrawing",
    "HeavyShader"
)

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
        while (line != null) {
            println(line)
            writer.write(line)
            writer.newLine()
            line = reader.readLine()
        }
    }

    return process.waitFor()
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

val argUdid = args.getOrNull(0)
val deviceLine = if (argUdid != null) {
    allDeviceLines.find { it.contains(argUdid) } ?: run {
        println("Available devices and simulators:")
        allDeviceLines.forEach { println(it) }
        die("Device with UDID '$argUdid' not found among the above.")
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
println("    Prefix    : ${devicePrefix}_parallel_<true|false>_<Benchmark>_<N>.txt")

// ── 2. Build ───────────────────────────────────────────────────────────────────

println("\n==> [2/4] Building '$SCHEME' ($CONFIGURATION)...")
println("  $ mkdir -p $BUILD_DIR")
BUILD_DIR.mkdirs()

val xcodeLog = File(BUILD_DIR, "xcodebuild.log")

// Clean stale Kotlin Native build artifacts to avoid klib ABI version mismatches.
println("  $ ./gradlew clean")
execInheritIO("./gradlew", "clean")

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

val total = BENCHMARKS.size * 2 * ATTEMPTS
var current = 0

println("\n==> [4/4] Running $total benchmark sessions")
println("    ${BENCHMARKS.size} benchmarks  ×  2 parallel modes  ×  $ATTEMPTS attempts\n")

for (benchmark in BENCHMARKS) {
    for (parallel in listOf("true", "false")) {
        for (attempt in 1..ATTEMPTS) {
            current++

            val outFileName = "${devicePrefix}_parallel_${parallel}_${benchmark}_${attempt}.txt"
            val outFile = File(OUTPUT_DIR, outFileName)

            System.out.format("  [%3d/%3d]  %-52s  parallel=%-5s  attempt=%d\n",
                current, total, benchmark, parallel, attempt)
            println("            → ${outFile.name}")

            val runArgs = mutableListOf<String>()
            val exitCode = if (isSimulator) {
                runArgs.addAll(listOf(
                    "xcrun", "simctl", "launch", "--console", deviceId, bundleId,
                    "benchmarks=$benchmark",
                    "parallel=$parallel",
                    "warmupCount=100",
                    "modes=REAL",
                    "reportAtTheEnd=true"
                ))
                println("  $ ${runArgs.joinToString(" ")}")
                execWithTee(runArgs, outFile)
            } else {
                runArgs.addAll(listOf(
                    "xcrun", "devicectl", "device", "process", "launch", "--console", "--device", deviceId, bundleId,
                    "--",
                    "benchmarks=$benchmark",
                    "parallel=$parallel",
                    "warmupCount=100",
                    "modes=REAL",
                    "reportAtTheEnd=true"
                ))
                println("  $ ${runArgs.joinToString(" ")}")
                execWithTee(runArgs, outFile)
            }

            if (exitCode != 0) {
                System.out.format("            ⚠  WARNING: process exited with code %d\n", exitCode)
            } else {
                println("            ✓  done")
            }

            // Brief cooldown between runs so the device settles
            println("  $ sleep 3")
            Thread.sleep(3000)
        }
    }
}

println("\n==> All done!")
System.out.format("    %d output files written to: %s\n\n", total, OUTPUT_DIR.path)
