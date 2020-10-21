package org.jetbrains.compose.desktop.application.dsl

import org.jetbrains.compose.desktop.application.internal.OS

enum class TargetFormat(
    internal val id: String,
    internal val os: OS
) {
    Deb("deb", OS.Linux),
    Rpm("rpm", OS.Linux),
    App("app-image", OS.MacOS),
    Dmg("dmg", OS.MacOS),
    Pkg("pkg", OS.MacOS),
    Exe("exe", OS.Windows),
    Msi("msi", OS.Windows)
}