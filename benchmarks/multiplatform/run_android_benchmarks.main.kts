#!/usr/bin/env kotlin

import java.io.File
import java.util.concurrent.TimeUnit
import kotlin.text.lines

/**
 * run_android_benchmarks.main.kts
 *
 * Builds the Android APK, installs it on a connected device or emulator, then runs
 * every benchmark (from Benchmarks.kt).
 * Console output is captured from logcat and saved to:
 *
 *   build/benchmarks/text-reports/<BenchmarkName>.txt
 *
 * Requirements:
 *   - Android SDK with `adb` on PATH
 *   - A connected Android device or running emulator
 *
 * Usage:  ./run_android_benchmarks.main.kts [device=<serial>] [benchmarks=...] [modes=REAL] ...
 *
 *   If no device serial is provided, the first connected device is used.
 */

// ── Configuration ──────────────────────────────────────────────────────────────

val ROOT_DIR = File(".").absoluteFile
val OUTPUT_DIR = File(ROOT_DIR, "benchmarks/build/benchmarks")
val TEXT_REPORTS_DIR = File(OUTPUT_DIR, "text-reports")
val JSON_REPORTS_DIR = File(OUTPUT_DIR, "json-reports")
val PACKAGE_NAME = "org.jetbrains.compose.benchmarks"
val ACTIVITY_NAME = "$PACKAGE_NAME.MainActivity"
val APK_PATH = File(ROOT_DIR, "benchmarks/build/outputs/apk/debug/benchmarks-debug.apk")

// ── Argument Parsing ──────────────────────────────────────────────────────────

val parsedArgs = mutableMapOf<String, String>()
val rawArgs = if (args.isEmpty()) {
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

rawArgs.forEach { arg ->
    if (arg.contains("=")) {
        val (key, value) = arg.split("=", limit = 2)
        parsedArgs[key.lowercase()] = value
    }
}

val deviceSerial = parsedArgs.remove("device")
val benchmarksFromArgs = parsedArgs["benchmarks"]?.split(",")?.map { it.substringBefore("(").trim() }?.filter { it.isNotEmpty() }
val separateProcess = parsedArgs.remove("separateprocess")?.toBoolean() ?: false

// Ensure REAL mode is set (only mode that works on Android)
if (!parsedArgs.containsKey("modes")) {
    parsedArgs["modes"] = "REAL"
}

// Ensure saveStatsToJSON is enabled for result capture
if (!parsedArgs.containsKey("savestatstojson")) {
    parsedArgs["savestatstojson"] = "true"
}

// App arguments (everything except script-specific args)
val appArgs = parsedArgs.toMutableMap()

// ── Android SDK resolution ─────────────────────────────────────────────────────

fun findAndroidSdkPath(): String? {
    // 1. local.properties
    val localProps = File(ROOT_DIR, "local.properties")
    if (localProps.exists()) {
        val sdkDir = localProps.readLines()
            .find { it.trimStart().startsWith("sdk.dir") }
            ?.substringAfter("=")?.trim()
        if (sdkDir != null && File(sdkDir).isDirectory) return sdkDir
    }
    // 2. ANDROID_HOME / ANDROID_SDK_ROOT env vars
    for (envVar in listOf("ANDROID_HOME", "ANDROID_SDK_ROOT")) {
        val path = System.getenv(envVar)
        if (path != null && File(path).isDirectory) return path
    }
    // 3. Default macOS path
    val defaultPath = File(System.getProperty("user.home"), "Library/Android/sdk")
    if (defaultPath.isDirectory) return defaultPath.absolutePath
    return null
}

val androidSdkPath = findAndroidSdkPath()
val emulatorBinary = if (androidSdkPath != null) {
    val emulatorInSdk = File(androidSdkPath, "emulator/emulator")
    if (emulatorInSdk.exists()) emulatorInSdk.absolutePath else "emulator"
} else {
    "emulator"
}
val adbBinary = if (androidSdkPath != null) {
    val adbInSdk = File(androidSdkPath, "platform-tools/adb")
    if (adbInSdk.exists()) adbInSdk.absolutePath else "adb"
} else {
    "adb"
}

// ── Helpers ────────────────────────────────────────────────────────────────────

fun die(message: String): Nothing {
    System.err.println("\nERROR: $message")
    System.exit(1)
    throw RuntimeException(message)
}

fun adbCmd(vararg extraArgs: String): Array<String> {
    val cmd = mutableListOf(adbBinary)
    if (deviceSerial != null) {
        cmd.add("-s")
        cmd.add(deviceSerial)
    }
    cmd.addAll(extraArgs)
    return cmd.toTypedArray()
}

fun exec(vararg command: String, workingDir: File = ROOT_DIR, redirectStderr: Boolean = true): String {
    val process = ProcessBuilder(*command)
        .directory(workingDir)
        .redirectErrorStream(redirectStderr)
        .start()

    val output = process.inputStream.bufferedReader().readText()
    val exitCode = process.waitFor()
    if (exitCode != 0) {
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

fun saveCapturedJson(json: String) {
    try {
        val jsonNode = json.trim()
        if (jsonNode.isEmpty()) return

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

// ── 1. Detect target device ───────────────────────────────────────────────────

fun listAvailableAvds(): List<String> {
    return try {
        val output = ProcessBuilder(emulatorBinary, "-list-avds")
            .redirectErrorStream(true)
            .start()
            .inputStream.bufferedReader().readText()
        output.lines().filter { it.isNotBlank() && !it.startsWith("INFO") }
    } catch (e: Exception) {
        println("    ⚠  Could not list AVDs: ${e.message}")
        emptyList()
    }
}

fun startEmulatorAndWait(avdName: String) {
    println("    Starting emulator '$avdName'...")
    ProcessBuilder(emulatorBinary, "-avd", avdName, "-no-snapshot-load")
        .redirectErrorStream(true)
        .start()

    // Wait for the device to come online and boot
    println("    Waiting for emulator to boot...")
    exec(adbBinary, "wait-for-device")

    // Wait for boot_completed
    val bootTimeout = 120_000L
    val bootStart = System.currentTimeMillis()
    while (System.currentTimeMillis() - bootStart < bootTimeout) {
        try {
            val bootCompleted = ProcessBuilder(adbBinary, "shell", "getprop", "sys.boot_completed")
                .redirectErrorStream(true)
                .start()
                .inputStream.bufferedReader().readText().trim()
            if (bootCompleted == "1") {
                println("    Emulator booted.")
                Thread.sleep(2000) // extra settle time
                return
            }
        } catch (_: Exception) {}
        Thread.sleep(2000)
    }
    die("Emulator '$avdName' did not finish booting within ${bootTimeout / 1000}s.")
}

println("\n==> [1/4] Detecting target device...")
println("    Using adb: $adbBinary")

var devicesOutput = exec(*adbCmd("devices", "-l"))

var deviceLines = devicesOutput.lines()
    .drop(1) // skip "List of devices attached"
    .filter { it.contains("device") }

if (deviceLines.isEmpty()) {
    println("    No connected device or running emulator found.")
    val avds = listAvailableAvds()
    if (avds.isEmpty()) {
        die("No Android device/emulator found and no AVDs available.\n" +
            "Please either:\n" +
            "  1. Connect a physical device via USB/WiFi, or\n" +
            "  2. Create an AVD: Android Studio → Device Manager → Create Device\n" +
            "     or via command line: avdmanager create avd -n Pixel_API_35 -k 'system-images;android-35;google_apis;arm64-v8a'")
    }
    println("    Available AVDs: ${avds.joinToString(", ")}")
    startEmulatorAndWait(avds.first())

    // Re-detect devices
    devicesOutput = exec(*adbCmd("devices", "-l"))
    deviceLines = devicesOutput.lines()
        .drop(1)
        .filter { it.contains("device") }

    if (deviceLines.isEmpty()) {
        die("Emulator started but no device detected by adb.")
    }
}

// Select device: prefer explicit serial, then try to find the most recently launched emulator
// (which is typically the one running in Android Studio / IntelliJ IDEA)
val targetDevice = if (deviceSerial != null) {
    deviceLines.find { it.startsWith(deviceSerial) }
        ?: die("Device with serial '$deviceSerial' not found.\nAvailable devices:\n${deviceLines.joinToString("\n")}")
} else if (deviceLines.size == 1) {
    deviceLines.first()
} else {
    // Multiple devices: prefer real (physical) devices over emulators
    val physicalLines = deviceLines.filter { !it.startsWith("emulator-") }
    val emulatorLines = deviceLines.filter { it.startsWith("emulator-") }
    val selected = if (physicalLines.isNotEmpty()) {
        physicalLines.first()
    } else {
        // Among emulators prefer the highest port number (most recently started)
        emulatorLines.sortedByDescending { it.split("\\s+".toRegex()).first() }.first()
    }
    println("    Multiple devices found (${deviceLines.size}), selected: ${selected.split("\\s+".toRegex()).first()}")
    selected
}

val actualSerial = targetDevice.split("\\s+".toRegex()).first()
val deviceModel = Regex("model:(\\S+)").find(targetDevice)?.groupValues?.get(1) ?: "unknown"

// Override deviceSerial for subsequent adb commands if it wasn't specified
val adbSerial = deviceSerial ?: actualSerial

fun adb(vararg extraArgs: String): Array<String> {
    val cmd = mutableListOf(adbBinary, "-s", adbSerial)
    cmd.addAll(extraArgs)
    return cmd.toTypedArray()
}

val androidVersion = exec(*adb("shell", "getprop", "ro.build.version.release")).trim()

println("    Serial    : $adbSerial")
println("    Model     : $deviceModel")
println("    Android   : $androidVersion")

// ── 2. Build ───────────────────────────────────────────────────────────────────

println("\n==> [2/4] Building APK...")
println("  $ ./gradlew :benchmarks:assembleDebug")

execInheritIO("./gradlew", ":benchmarks:assembleDebug")

if (!APK_PATH.exists()) {
    die("APK not found at expected path: ${APK_PATH.path}")
}
println("    Build  : OK")
println("    APK    : ${APK_PATH.relativeTo(ROOT_DIR).path}")

// ── 3. Install ─────────────────────────────────────────────────────────────────

println("\n==> [3/4] Installing...")
println("  $ adb -s $adbSerial install -r ${APK_PATH.path}")

exec(*adb("install", "-r", APK_PATH.path))
println("    Installed.")

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

fun runBenchmark(finalAppArgs: Map<String, String>, outFile: File): Int {
    // Clear logcat before launching
    exec(*adb("logcat", "-c"))

    // Force-stop any previous instance
    exec(*adb("shell", "am", "force-stop", PACKAGE_NAME))
    Thread.sleep(500)

    // Build the args string for the intent extra
    val argsString = finalAppArgs.map { "${it.key}=${it.value}" }.joinToString(" ")

    // Launch the activity
    val launchCmd = adb(
        "shell", "am", "start",
        "-n", "$PACKAGE_NAME/$ACTIVITY_NAME",
        "--es", "args", "\"\'$argsString\'\""
    )
    println("  $ ${launchCmd.joinToString(" ")}")
    exec(*launchCmd)

    // Give the app a moment to start, then get its PID for precise logcat filtering
    Thread.sleep(1000)
    val appPid = try {
        exec(*adb("shell", "pidof", PACKAGE_NAME)).trim()
    } catch (_: Exception) { "" }

    // Monitor logcat for output, capturing to file
    // Use --pid to filter by app process (works reliably on both real devices and emulators)
    // Use -v raw for clean output without logcat metadata
    val logcatArgs = if (appPid.isNotEmpty()) {
        println("    Filtering logcat by PID: $appPid")
        adb("logcat", "-v", "raw", "--pid=$appPid", "System.out:I", "*:S")
    } else {
        println("    ⚠  Could not determine app PID, using tag-based filtering")
        adb("logcat", "-v", "raw", "System.out:I", "*:S")
    }
    val logcatProcess = ProcessBuilder(*logcatArgs)
        .directory(ROOT_DIR)
        .redirectErrorStream(true)
        .start()

    outFile.parentFile.mkdirs()
    var exitCode = 0
    var capturingJson = false
    var capturedJson = StringBuilder()
    val timeoutMs = 30 * 60 * 1000L // 30 minutes timeout
    val startTime = System.currentTimeMillis()

    outFile.bufferedWriter().use { writer ->
        val reader = logcatProcess.inputStream.bufferedReader()
        try {
            while (true) {
                if (System.currentTimeMillis() - startTime > timeoutMs) {
                    println("            ⚠  Timeout reached (30 min)")
                    exitCode = 1
                    break
                }

                // Check if the app is still running
                val line = reader.readLine() ?: break

                // With -v raw, output is just the message text.
                // But strip any residual logcat prefix just in case.
                val message = if (line.contains("System.out:")) {
                    line.substringAfter("System.out:").trim()
                } else {
                    line.trim()
                }

                if (message.isEmpty()) continue

                println(message)
                writer.write(message)
                writer.newLine()

                if (message == "JSON_START") {
                    capturingJson = true
                    capturedJson = StringBuilder()
                } else if (message == "JSON_END") {
                    capturingJson = false
                    saveCapturedJson(capturedJson.toString())
                } else if (capturingJson) {
                    capturedJson.append(message).append("\n")
                }

                // Detect app completion - the activity calls finishAndRemoveTask()
                // We check if the process is still running
                if (message.contains("Completed!") || message.contains("All benchmarks done")) {
                    Thread.sleep(2000) // Give time for final output
                    break
                }
            }
        } catch (e: Exception) {
            // Reader closed
        }
    }

    logcatProcess.destroyForcibly()

    // Also check if app finished by checking if it's still running
    val psOutput = try {
        exec(*adb("shell", "pidof", PACKAGE_NAME))
    } catch (e: Exception) {
        "" // App not running = finished
    }

    if (psOutput.isBlank()) {
        println("            App has finished.")
    }

    return exitCode
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
        val outFile = File(TEXT_REPORTS_DIR, "${benchmark}.txt")

        println("  [%3d/%3d]  %-52s".format(index + 1, total, benchmark))
        println("            → ${outFile.name} in ${TEXT_REPORTS_DIR.relativeTo(ROOT_DIR).path}")

        val finalAppArgs = appArgs.toMutableMap()
        finalAppArgs["benchmarks"] = benchmark

        val exitCode = runBenchmark(finalAppArgs, outFile)

        if (exitCode != 0) {
            println("            ⚠  WARNING: process exited with code $exitCode")
        } else {
            println("            ✓  done")
        }

        // Brief cooldown between runs
        println("  $ sleep 3")
        Thread.sleep(3000)
    }
} else {
    val outFile = File(TEXT_REPORTS_DIR, "all_benchmarks.txt")

    println("  [%3d/%3d]  %-52s".format(1, total, "All Benchmarks"))
    println("            → ${outFile.name} in ${TEXT_REPORTS_DIR.relativeTo(ROOT_DIR).path}")

    val finalAppArgs = appArgs.toMutableMap()
    finalAppArgs["benchmarks"] = benchmarksToRun.joinToString(",")

    val exitCode = runBenchmark(finalAppArgs, outFile)

    if (exitCode != 0) {
        println("            ⚠  WARNING: process exited with code $exitCode")
    } else {
        println("            ✓  done")
    }
}

println("\n==> All done!")
println("    %d output files written to: %s".format(total, TEXT_REPORTS_DIR.relativeTo(ROOT_DIR).path))
println("    JSON reports in: %s\n".format(JSON_REPORTS_DIR.relativeTo(ROOT_DIR).path))
