package org.jetbrains.compose.desktop.application.internal

import org.gradle.api.Project
import org.gradle.api.file.RegularFile
import org.gradle.api.provider.Provider
import org.jetbrains.compose.ComposeBuildConfig

internal object DefaultIcons {
    fun forLinux(project: Project): Provider<RegularFile> =
        unpackIconIfNeeded(project, platformName = "linux", iconExt = "png")

    fun forWindows(project: Project): Provider<RegularFile> =
        unpackIconIfNeeded(project, platformName = "windows", iconExt = "ico")

    fun forMac(project: Project): Provider<RegularFile> =
        unpackIconIfNeeded(project, platformName = "mac", iconExt = "icns")

    private fun unpackIconIfNeeded(project: Project, platformName: String, iconExt: String): Provider<RegularFile> {
        val iconsDir = project.layout.buildDirectory.dir("compose/default-icons/${ComposeBuildConfig.composeVersion}")
        val targetFile = iconsDir.map { it.file("icon-$platformName.$iconExt") }
        val targetIoFile = targetFile.ioFile
        val sourceIconName = "default-compose-desktop-icon-$platformName.$iconExt"

        if (targetIoFile.exists()) return targetFile

        val iconResourceStream = DefaultIcons.javaClass.classLoader.getResourceAsStream(sourceIconName)
            ?: error("Could not find default icon resource: $sourceIconName")
        iconResourceStream.use { input ->
            targetIoFile.parentFile.mkdirs()
            targetIoFile.createNewFile()
            targetIoFile.outputStream().buffered().use { output ->
                input.copyTo(output)
            }
        }

        return targetFile
    }
}