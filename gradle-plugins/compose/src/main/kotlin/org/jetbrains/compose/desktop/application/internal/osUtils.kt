package org.jetbrains.compose.desktop.application.internal

import org.gradle.api.tasks.Internal
import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import java.io.File

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
        "x86_64", "amd64" -> Arch.X64
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

internal fun executableName(nameWithoutExtension: String): String =
    if (currentOS == OS.Windows) "$nameWithoutExtension.exe" else nameWithoutExtension

internal fun javaExecutable(javaHome: String): String =
    File(javaHome).resolve("bin/${executableName("java")}").absolutePath

internal object MacUtils {
    val codesign: File by lazy {
        File("/usr/bin/codesign").checkExistingFile()
    }

    val security: File by lazy {
        File("/usr/bin/security").checkExistingFile()
    }

    val xcrun: File by lazy {
        File("/usr/bin/xcrun").checkExistingFile()
    }
}

@Internal
internal fun findOutputFileOrDir(dir: File, targetFormat: TargetFormat): File =
    when (targetFormat) {
        TargetFormat.AppImage -> dir
        else -> dir.walk().first { it.isFile && it.name.endsWith(targetFormat.fileExt) }
    }

internal fun File.checkExistingFile(): File =
    apply {
        check(isFile) { "'$absolutePath' does not exist" }
    }

internal val File.isJarFile: Boolean
    get() = name.endsWith(".jar", ignoreCase = true) && isFile
