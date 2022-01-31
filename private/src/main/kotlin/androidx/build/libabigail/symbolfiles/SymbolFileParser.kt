/*
 * Copyright 2022 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.build.libabigail.symbolfiles

import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader

class ParseError(message: String) : RuntimeException(message)

typealias Tags = List<String>

data class Version(
    val name: String,
    val base: String? = null,
    val symbols: List<Symbol> = emptyList()
)

data class Symbol(val name: String)

/**
 * Class to parse version scripts based on the original python implementation but simplified to
 * ignore tags and some other unused parameters.
 *
 * https://android.googlesource.com/platform/build/soong/+/master/cc/symbolfile/__init__.py
 */
class SymbolFileParser(
    val input: InputStream
) {
    val reader = BufferedReader(InputStreamReader(input))
    lateinit var currentLine: String

    init {
        nextLine()
    }

    fun parse(): List<Version> {
        val versions = mutableListOf<Version>()
        do {
            if (currentLine.contains("{")) {
                versions.add(parseNextVersion())
            } else {
                throw ParseError("Unexpected contents at top level: $currentLine")
            }
        } while (nextLine().isNotBlank())
        return versions
    }

    /**
     * Parses a single version section and returns a Version object.
     */
    fun parseNextVersion(): Version {
        val name = currentLine.split("{").first().trim()
        val symbols = mutableListOf<Symbol>()
        var globalScope = true
        var cppSymbols = false
        while (nextLine().isNotBlank()) {
            if (currentLine.contains("}")) {
                // Line is something like '} BASE; # tags'. Both base and tags are optional here.
                val base = currentLine.split("}").last().split("#").first().trim()
                if (!base.endsWith(";")) {
                    throw ParseError("Unterminated version/export \"C++\" block (expected ;).")
                }
                if (cppSymbols) {
                    cppSymbols = false
                } else {
                    return Version(
                        name,
                        base.removeSuffix(";").trim().ifBlank { null },
                        symbols
                    )
                }
            } else if (currentLine.contains("extern \"C++\" {")) {
                cppSymbols = true
            } else if (!cppSymbols && currentLine.contains(":")) {
                val visibility = currentLine.split(':').first().trim()
                if (visibility == "local") {
                    globalScope = false
                } else if (visibility == "global") {
                    globalScope = true
                } else {
                    throw ParseError("Unknown visibility label: $visibility")
                }
            } else if (globalScope) {
                symbols.add(parseNextSymbol(cppSymbols))
            } else {
                // We're in a hidden scope. Ignore everything.
            }
        }
        throw ParseError("Unexpected EOF in version block.")
    }

    /**
     * Parses a single symbol line and returns a Symbol object.
     */
    fun parseNextSymbol(cppSymbol: Boolean): Symbol {
        val line = currentLine
        if (!line.contains(";")) {
            throw ParseError("Expected ; to terminate symbol: ${line.trim()}")
        }
        if (line.contains("*")) {
            throw ParseError("Wildcard global symbols are not permitted.")
        }
        // Line is now in the format "<symbol-name>; # tags"
        val name = line.trim().split(";").first().let {
            if (cppSymbol) {
              it.removeSurrounding("\"")
            } else {
                it
            }
        }
        return Symbol(name)
    }

    /**
     * Returns the next non-empty non-comment line.
     * A return value of '' indicates EOF.
     */
    fun nextLine(): String {
        var line: String? = reader.readLine()
        while (line != null && (line.isBlank() || line.trim().startsWith("#"))) {
            line = reader.readLine() ?: break
        }
        currentLine = line ?: ""
        return currentLine
    }
}
