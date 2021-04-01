/*
 * Copyright 2020-2021 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.test

import java.io.File

fun File.modify(fn: (String) -> String) {
    val content = readText()
    val newContent = fn(content)
    writeText(newContent)
}

fun File.checkExists(): File = apply {
    check(exists()) {
        buildString {
            appendln("Requested file does not exist: $absolutePath")
            parentFile?.listFiles()?.let { siblingFiles ->
                appendln("Other files in the same directory: ${parentFile.absolutePath}")
                siblingFiles.forEach {
                    appendln("  * ${it.name}")
                }
            }
        }
    }
}