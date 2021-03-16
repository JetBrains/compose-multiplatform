package org.jetbrains.compose.desktop.application.internal

import org.gradle.api.file.FileSystemLocation
import org.gradle.api.provider.Provider
import java.io.File

internal fun <T : Any?> MutableCollection<String>.cliArg(
    name: String,
    value: T?,
    fn: (T) -> String = defaultToString()
) {
    if (value is Boolean) {
        if (value) add(name)
    } else if (value != null) {
        add(name)
        add(fn(value))
    }
}

internal fun <T : Any?> MutableCollection<String>.cliArg(
    name: String,
    value: Provider<T>,
    fn: (T) -> String = defaultToString()
) {
    cliArg(name, value.orNull, fn)
}

private fun <T : Any?> defaultToString(): (T) -> String =
    {
        val asString = when (it) {
            is FileSystemLocation -> it.asFile.normalizedPath()
            is File -> it.normalizedPath()
            else -> it.toString()
        }
        "\"$asString\""
    }

internal fun File.normalizedPath() =
    if (currentOS == OS.Windows) absolutePath.replace("\\", "\\\\") else absolutePath