package org.jetbrains.compose.desktop.application.internal

internal enum class OS {
    Linux, Windows, MacOS
}

internal val currentOS: OS by lazy {
    val os = System.getProperty("os.name")
    when {
        os.equals("Mac OS X", ignoreCase = true) -> OS.MacOS
        os.startsWith("Win", ignoreCase = true) -> OS.Windows
        os.startsWith("Linux", ignoreCase = true) -> OS.Linux
        else -> error("Unknown OS name: $os")
    }
}