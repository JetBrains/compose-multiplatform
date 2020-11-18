package org.jetbrains.compose.desktop.application.internal

internal enum class OS(val id: String) {
    Linux("linux"),
    Windows("windows"),
    MacOS("macos")
}

internal enum class Arch(val id: String) {
    X64("x64"),
    Arm64("arm64")
}

internal data class Target(val os: OS, val arch: Arch) {
    val id: String
        get() = "${os.id}-${arch.id}"
}

internal val currentTarget by lazy {
    Target(currentOS, currentArch)
}

internal val currentArch by lazy {
    val osArch = System.getProperty("os.arch")
    when (osArch) {
        "x86_64" -> Arch.X64
        "aarch64" -> Arch.Arm64
        else -> error("Unknown OS arch: $osArch")
    }
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