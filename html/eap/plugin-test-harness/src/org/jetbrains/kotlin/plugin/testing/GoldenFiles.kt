/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.plugin.testing

import java.io.File

enum class GoldenMode {
    /** Never modifies expected output. A failure writes a sibling `<golden>.actual` file. */
    VERIFY,
    /** Explicitly accepts the current output, creating or replacing the golden. */
    UPDATE,
}

/** Golden file handling independent of compilation and of any particular test framework. */
object GoldenFiles {
    fun assertMatches(expectedFile: File, actual: String, mode: GoldenMode = GoldenMode.VERIFY) {
        val normalized = normalize(actual)
        val actualFile = File(expectedFile.path + ".actual")
        if (mode == GoldenMode.UPDATE) {
            expectedFile.absoluteFile.parentFile.mkdirs()
            expectedFile.writeText(normalized)
            actualFile.delete()
            return
        }

        val expected = if (expectedFile.isFile) normalize(expectedFile.readText()) else null
        if (expected == normalized) {
            actualFile.delete()
            return
        }

        actualFile.absoluteFile.parentFile.mkdirs()
        actualFile.writeText(normalized)
        val difference = if (expected == null) {
            "Golden file is missing."
        } else {
            val expectedLines = expected.lines()
            val actualLines = normalized.lines()
            val index = (0 until maxOf(expectedLines.size, actualLines.size)).first {
                expectedLines.getOrNull(it) != actualLines.getOrNull(it)
            }
            "First difference at line ${index + 1}:\n" +
                    "- ${expectedLines.getOrNull(index) ?: "<EOF>"}\n" +
                    "+ ${actualLines.getOrNull(index) ?: "<EOF>"}"
        }
        throw AssertionError(
            "$difference\nExpected: ${expectedFile.absolutePath}\nActual: ${actualFile.absolutePath}\n" +
                    "Review the actual file, then copy it to the golden or explicitly use GoldenMode.UPDATE."
        )
    }

    // Do not trim indentation or trailing spaces: they may be significant in source and string constants.
    private fun normalize(text: String): String = text.replace("\r\n", "\n").replace('\r', '\n').trimEnd('\n') + "\n"
}
