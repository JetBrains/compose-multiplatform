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