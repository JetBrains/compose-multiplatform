package org.jetbrains.compose.test

import java.io.File

fun File.modify(fn: (String) -> String) {
    val content = readText()
    val newContent = fn(content)
    writeText(newContent)
}

fun File.checkExists(): File = apply {
    check(exists()) { "File does not exist: $absolutePath" }
}