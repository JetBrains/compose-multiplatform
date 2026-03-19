#!/usr/bin/env kotlin

import java.io.File
import java.util.concurrent.TimeUnit

println("--- Compose App Build and Size Measurement ---")

val rootDir = File(".").absoluteFile
val gradlew = if (System.getProperty("os.name").lowercase().contains("win")) "${rootDir.path}/gradlew.bat" else "${rootDir.path}/gradlew"

data class Target(
    val name: String,
    val task: String? = null,
    val customCommand: List<String>? = null,
    val outputPaths: List<String>,
    val description: String
)

data class Result(
    val name: String,
    val description: String,
    val success: Boolean,
    val size: String? = null
)

val targets = listOf(
    Target(
        "Android",
        task = ":composeApp:assembleRelease",
        outputPaths = listOf("composeApp/build/outputs/apk/release/composeApp-release.apk", "composeApp/build/outputs/apk/release/composeApp-release-unsigned.apk"),
        description = "Release APK"
    ),
    Target(
        "Desktop (UberJar)",
        task = ":composeApp:packageReleaseUberJarForCurrentOS",
        outputPaths = listOf("composeApp/build/compose/binaries/main/jar", "composeApp/build/compose/jars"),
        description = "Executable UberJar"
    ),
    Target(
        "Desktop (Installer)",
        task = ":composeApp:createReleaseDistributable",
        outputPaths = listOf(
            "composeApp/build/compose/binaries/main/dmg",
            "composeApp/build/compose/binaries/main/pkg",
            "composeApp/build/compose/binaries/main/deb",
            "composeApp/build/compose/binaries/main/rpm",
            "composeApp/build/compose/binaries/main/msi",
            "composeApp/build/compose/binaries/main/exe",
            "composeApp/build/compose/binaries/main-release/dmg",
            "composeApp/build/compose/binaries/main-release/pkg",
            "composeApp/build/compose/binaries/main-release/msi",
            "composeApp/build/compose/binaries/main-release/exe",
            "composeApp/build/compose/binaries/main-release/app"
        ),
        description = "Installation Package"
    ),
    Target(
        "iOS (Framework)",
        task = ":composeApp:linkReleaseFrameworkIosArm64",
        outputPaths = listOf("composeApp/build/bin/iosArm64/releaseFramework/ComposeApp.framework"),
        description = "Compiled Framework (ARM64)"
    ),
    Target(
        "iOS (App Bundle)",
        customCommand = listOf(
            "xcodebuild", "-project", "iosApp/iosApp.xcodeproj", 
            "-scheme", "iosApp", "-configuration", "Release", 
            "-sdk", "iphoneos", "-derivedDataPath", "iosApp/build",
            "-allowProvisioningUpdates", "CODE_SIGNING_ALLOWED=NO",
            "build"
        ),
        outputPaths = listOf("iosApp/build/Build/Products/Release-iphoneos/LazyGridImageCMPImpl.app"),
        description = "iOS App Bundle"
    ),
    Target(
        "Web (Wasm/JS)",
        task = ":composeApp:wasmJsBrowserDistribution",
        outputPaths = listOf("composeApp/build/dist/wasmJs/productionExecutable"),
        description = "Web Distribution"
    )
)

val results = mutableListOf<Result>()

var jsonOutputFile: String? = null
for (i in args.indices) {
    if (args[i] == "--json" && i + 1 < args.size) {
        jsonOutputFile = args[i + 1]
    }
}

for (target in targets) {
    println("\nBuilding ${target.name}...")
    val success = if (target.customCommand != null) {
        runCommand(*target.customCommand.toTypedArray())
    } else if (target.task != null) {
        runCommand(gradlew, target.task)
    } else {
        println("Error: No task or customCommand defined for ${target.name}")
        false
    }
    
    if (success) {
        println("Build successful for ${target.name}.")
        val foundOutputs = target.outputPaths.map { File(it) }.filter { it.exists() }
        
        if (foundOutputs.isEmpty()) {
            println("Warning: Build successful but no output found in expected locations:")
            target.outputPaths.forEach { println("  - $it") }
            results.add(Result(target.name, target.description, true, "N/A"))
        } else {
            // Find the first or best output to include in summary
            val mainOutput = foundOutputs.first()
            val size = calculateSize(mainOutput)
            results.add(Result(target.name, target.description, true, formatSize(size)))

            for (output in foundOutputs) {
                val individualSize = calculateSize(output)
                println("Resulting ${target.description} (${output.name}): ${formatSize(individualSize)}")
            }
        }
    } else {
        println("Build failed for ${target.name}. Skipping size calculation.")
        results.add(Result(target.name, target.description, false))
    }
}

println("\n" + "=".repeat(60))
println("%-20s | %-25s | %-10s".format("Target", "Description", "Size"))
println("-".repeat(60))
for (res in results) {
    val sizeStr = if (res.success) res.size ?: "N/A" else "FAILED"
    println("%-20s | %-25s | %-10s".format(res.name, res.description, sizeStr))
}
println("=".repeat(60))

if (jsonOutputFile != null) {
    val outputFile = File(jsonOutputFile)
    outputFile.parentFile?.mkdirs()
    val jsonContent = buildString {
        append("[\n")
        results.forEachIndexed { index, res ->
            append("  {\n")
            append("    \"name\": \"${res.name}\",\n")
            append("    \"description\": \"${res.description}\",\n")
            append("    \"success\": ${res.success}")
            if (res.size != null) {
                append(",\n    \"size\": \"${res.size}\"\n")
            } else {
                append("\n")
            }
            append("  }")
            if (index < results.size - 1) append(",")
            append("\n")
        }
        append("]\n")
    }
    File(jsonOutputFile).writeText(jsonContent)
    println("\nResults written to $jsonOutputFile")
}

fun runCommand(vararg command: String): Boolean {
    val process = ProcessBuilder(*command)
        .inheritIO()
        .start()
    
    val finished = process.waitFor(30, TimeUnit.MINUTES)
    return finished && process.exitValue() == 0
}

fun calculateSize(file: File): Long {
    if (file.isFile) return file.length()
    
    var totalSize = 0L
    val files = file.listFiles() ?: return 0L
    for (f in files) {
        totalSize += if (f.isFile) f.length() else calculateSize(f)
    }
    return totalSize
}

fun formatSize(bytes: Long): String {
    val kb = bytes / 1024.0
    val mb = kb / 1024.0
    return if (mb >= 1.0) {
        "%.2f MB".format(mb)
    } else {
        "%.2f KB".format(kb)
    }
}
