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

import java.io.InputStream
import java.io.OutputStream

/**
 * Class to generate a symbol list [output] from a given version script [input]. The generated
 * symbol list can be passed to abidw as an allow list of visible symbols to document.
 */
class SymbolListGenerator(val input: InputStream, val output: OutputStream) {
    fun generate() {
        val versions = SymbolFileParser(input).parse()
        write(versions)
    }

    fun write(versions: List<Version>) {
        writeLine("[abi_symbol_list]")
        versions.forEach { writeVersion(it) }
    }

    fun writeVersion(version: Version) {
        version.symbols.forEach { writeLine("  " + it.name) }
    }

    private fun writeLine(line: String) {
        output.write("$line\n".toByteArray())
    }
}
