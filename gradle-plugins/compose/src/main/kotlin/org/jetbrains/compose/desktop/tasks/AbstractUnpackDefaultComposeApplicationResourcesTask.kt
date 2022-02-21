/*
 * Copyright 2020-2022 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.desktop.tasks

import org.gradle.api.file.Directory
import org.gradle.api.file.FileSystemLocation
import org.gradle.api.file.RegularFile
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.jetbrains.compose.ComposeBuildConfig
import org.jetbrains.compose.desktop.application.internal.ioFile

abstract class AbstractUnpackDefaultComposeApplicationResourcesTask : AbstractComposeDesktopTask() {
    internal class DefaultResourcesProvider(resourcesRootDir: Provider<Directory>) {
        val macIcon: Provider<RegularFile> = resourcesRootDir.map { it.file("default-icon-mac.icns") }
        val windowsIcon: Provider<RegularFile> = resourcesRootDir.map { it.file("default-icon-windows.ico") }
        val linuxIcon: Provider<RegularFile> = resourcesRootDir.map { it.file("default-icon-linux.png") }
    }

    @OutputDirectory
    val destinationDir: Provider<Directory> = project.layout.buildDirectory.dir(
        "compose/default-resources/${ComposeBuildConfig.composeGradlePluginVersion}"
    )

    @get:Internal
    internal val resources = DefaultResourcesProvider(destinationDir)

    @TaskAction
    fun run() {
        cleanDirs(destinationDir)

        unpack(iconSourcePath("mac", "icns"), resources.macIcon)
        unpack(iconSourcePath("windows", "ico"), resources.windowsIcon)
        unpack(iconSourcePath("linux", "png"), resources.linuxIcon)
    }

    private fun iconSourcePath(platformName: String, iconExt: String): String =
        "default-compose-desktop-icon-$platformName.$iconExt"

    private fun unpack(from: String, to: Provider<out FileSystemLocation>) {
        val targetIoFile = to.ioFile.apply {
            if (exists()) {
                delete()
            } else {
                parentFile.mkdirs()
            }
            createNewFile()
        }

        val iconResourceStream = javaClass.classLoader.getResourceAsStream(from)
            ?: error("Could not find default resource: $from")
        iconResourceStream.use { input ->
            targetIoFile.outputStream().buffered().use { output ->
                input.copyTo(output)
            }
        }
    }
}