#!/usr/bin/env kotlin

import java.io.File
import java.util.concurrent.TimeUnit

println("--- Compose Version Comparison ---")

if (args.size < 2) {
    println("Usage: compare_sizes.main.kts <version1> <version2> [--reuse-build]")
    System.exit(1)
}

val version1 = args[0]
val version2 = args[1]
val reuseBuild = args.contains("--reuse-build")

val libsVersionsFile = File("gradle/libs.versions.toml")
val originalContent = libsVersionsFile.readText()

data class ComparisonResult(
    val name: String,
    val description: String,
    val size1: String?,
    val size2: String?
)

fun parseSize(sizeStr: String?): Long? {
    if (sizeStr == null || sizeStr == "N/A" || sizeStr == "FAILED") return null
    val parts = sizeStr.split(" ")
    if (parts.size != 2) return null
    val value = parts[0].toDoubleOrNull() ?: return null
    return when (parts[1].uppercase()) {
        "MB" -> (value * 1024 * 1024).toLong()
        "KB" -> (value * 1024).toLong()
        "B" -> value.toLong()
        else -> null
    }
}

fun formatSize(bytes: Long): String {
    val kb = bytes / 1024.0
    val mb = kb / 1024.0
    return if (mb >= 1.0) "%.2f MB".format(mb)
    else if (kb >= 1.0) "%.2f KB".format(kb)
    else "$bytes B"
}

fun runMeasureSizes(version: String, reuse: Boolean): String {
    val buildDir = File("build")
    if (!buildDir.exists()) {
        buildDir.mkdirs()
    }
    val jsonFile = "build/sizes/res_$version.json"

    if (reuse && File(jsonFile).exists()) {
        println("\nReusing existing results for version $version ($jsonFile)")
        return jsonFile
    }
    
    // Update libs.versions.toml
    val newContent = originalContent.replace(
        Regex("""compose-multiplatform\s*=\s*".*""""),
        """compose-multiplatform = "$version""""
    )
    libsVersionsFile.writeText(newContent)

    println("\nMeasuring sizes for Compose version $version...")
    try {
        val cmdArgs = mutableListOf("kotlin", "measure_sizes.main.kts", "--json", jsonFile)
        
        val process = ProcessBuilder(*cmdArgs.toTypedArray())
            .inheritIO()
            .start()
        val finished = process.waitFor(60, TimeUnit.MINUTES)
        if (!finished || process.exitValue() != 0) {
            println("Error: measure_sizes.main.kts failed for version $version")
        }
    } finally {
        // Restore original content will be done at the very end
    }
    
    return jsonFile
}

try {
    val file1 = runMeasureSizes(version1, reuseBuild)
    val file2 = runMeasureSizes(version2, reuseBuild)

    // Restore original content
    libsVersionsFile.writeText(originalContent)

    println("\n" + "=".repeat(100))
    println("%-20s | %-20s | %-12s | %-12s | %-12s | %-8s".format("Target", "Description", version1, version2, "Diff", "Diff %"))
    println("-".repeat(100))

    val results1 = parseJson(File(file1))
    val results2 = parseJson(File(file2))

    val allNames = (results1.keys + results2.keys).toSet()

    for (name in allNames) {
        val res1 = results1[name]
        val res2 = results2[name]
        
        val size1Str = res1?.get("size") as? String
        val size2Str = res2?.get("size") as? String
        val description = (res1?.get("description") ?: res2?.get("description") ?: "") as String

        val size1 = parseSize(size1Str)
        val size2 = parseSize(size2Str)

        val diffStr: String
        val percentStr: String

        if (size1 != null && size2 != null) {
            val diff = size2 - size1
            diffStr = (if (diff >= 0) "+" else "") + formatSize(diff)
            percentStr = "%.2f%%".format((diff.toDouble() / size1.toDouble()) * 100.0)
        } else {
            diffStr = "N/A"
            percentStr = "N/A"
        }

        println("%-20s | %-20s | %-12s | %-12s | %-12s | %-8s".format(
            name, 
            description.take(20), 
            size1Str ?: "N/A", 
            size2Str ?: "N/A", 
            diffStr, 
            percentStr
        ))
    }
    println("=".repeat(100))

} catch (e: Exception) {
    e.printStackTrace()
    libsVersionsFile.writeText(originalContent)
}

fun parseJson(file: File): Map<String, Map<String, Any>> {
    if (!file.exists()) return emptyMap()
    val text = file.readText().trim()
    if (!text.startsWith("[") || !text.endsWith("]")) return emptyMap()
    
    val results = mutableMapOf<String, Map<String, Any>>()
    val objectStrings = text.substring(1, text.length - 1).split("},").map { it.trim() + "}" }.map { 
        if (it.endsWith("}}")) it.substring(0, it.length - 1) else it 
    }

    for (objStr in objectStrings) {
        val cleanObj = objStr.trim().removePrefix("{").removeSuffix("}")
        val pairs = cleanObj.split(",").map { it.trim() }
        val map = mutableMapOf<String, Any>()
        for (pair in pairs) {
            val keyValue = pair.split(":").map { it.trim().removeSurrounding("\"") }
            if (keyValue.size == 2) {
                val value: Any = when {
                    keyValue[1] == "true" -> true
                    keyValue[1] == "false" -> false
                    else -> keyValue[1]
                }
                map[keyValue[0]] = value
            }
        }
        val name = map["name"] as? String
        if (name != null) {
            results[name] = map
        }
    }
    return results
}
