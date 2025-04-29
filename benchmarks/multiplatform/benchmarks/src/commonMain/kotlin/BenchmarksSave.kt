/*
 * Copyright 2020-2025 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.format.FormatStringsInDatetimeFormats
import kotlinx.datetime.format.byUnicodePattern
import kotlinx.datetime.toLocalDateTime
import kotlinx.io.IOException
import kotlinx.io.RawSink
import kotlinx.io.RawSource
import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.io.readByteArray

fun saveBenchmarkStatsOnDisk(name: String, stats: BenchmarkStats) {
    try {
        if (Config.saveStatsToCSV) {
            val path = Path("build/benchmarks/$name.csv")

            val keyToValue = mutableMapOf<String, String>()
            keyToValue.put("Date", currentFormattedDate)
            stats.putFormattedValuesTo(keyToValue)

            var text = if (SystemFileSystem.exists(path)) {
                SystemFileSystem.source(path).readText()
            } else {
                keyToValue.keys.joinToString(",") + "\n"
            }

            fun escapeForCSV(value: String) = value.replace(",", ";")
            text += keyToValue.values.joinToString(",", transform = ::escapeForCSV) + "\n"

            SystemFileSystem.createDirectories(path.parent!!)
            SystemFileSystem.sink(path).writeText(text)
            println("CSV results saved to ${SystemFileSystem.resolve(path)}")
            println()
        } else if (Config.saveStatsToJSON) {
            val jsonString = stats.toJsonString()
            val jsonPath = Path("build/benchmarks/json-reports/$name.json")

            SystemFileSystem.createDirectories(jsonPath.parent!!)
            SystemFileSystem.sink(jsonPath).writeText(jsonString)
            println("JSON results saved to ${SystemFileSystem.resolve(jsonPath)}")
            println()
        }
    } catch (_: IOException) {
        // IOException "Read-only file system" is thrown on iOS without writing permissions
    } catch (_: UnsupportedOperationException) {
        // UnsupportedOperationException is thrown in browser
    }
}

private fun RawSource.readText() = use {
    it.buffered().readByteArray().decodeToString()
}

private fun RawSink.writeText(text: String) = use {
    it.buffered().apply {
        write(text.encodeToByteArray())
        flush()
    }
}

@OptIn(FormatStringsInDatetimeFormats::class)
private val currentFormattedDate: String get() {
    val currentTime = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
    return LocalDateTime.Format {
        byUnicodePattern("dd-MM-yyyy HH:mm:ss")
    }.format(currentTime)
}