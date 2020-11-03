package org.jetbrains.compose.desktop.application.dsl

import org.jetbrains.compose.desktop.application.internal.OS
import org.jetbrains.compose.desktop.application.internal.currentOS

enum class TargetFormat(
    internal val id: String,
    private vararg val compatibleOSs: OS
) {
    AppImage("app-image", *OS.values()),
    Deb("deb", OS.Linux),
    Rpm("rpm", OS.Linux),
    Dmg("dmg", OS.MacOS),
    Pkg("pkg", OS.MacOS),
    Exe("exe", OS.Windows),
    Msi("msi", OS.Windows);

    val isCompatibleWithCurrentOS: Boolean by lazy { isCompatibleWith(currentOS) }

    internal fun isCompatibleWith(targetOS: OS): Boolean = targetOS in compatibleOSs

    val fileExt: String
        get() {
            check(this != AppImage) { "$this cannot have a file extension" }
            return ".$id"
        }
}