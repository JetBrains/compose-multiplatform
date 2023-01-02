/*
 * Copyright 2020-2022 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.desktop.application.tasks

import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import org.jetbrains.compose.internal.utils.ioFile
import org.jetbrains.compose.internal.utils.notNullProperty
import java.io.File

abstract class AbstractNativeMacApplicationPackageDmgTask : AbstractNativeMacApplicationPackageTask() {
    @get:InputFile
    @get:PathSensitive(PathSensitivity.ABSOLUTE)
    val hdiutil: RegularFileProperty = objects.fileProperty().value { File("/usr/bin/hdiutil") }

    @get:InputFile
    @get:PathSensitive(PathSensitivity.ABSOLUTE)
    val osascript: RegularFileProperty = objects.fileProperty().value { File("/usr/bin/osascript") }

    @get:Input
    val installDir: Property<String> = objects.notNullProperty("/Applications")

    @get:InputDirectory
    val appDir: DirectoryProperty = objects.directoryProperty()

    override fun createPackage(destinationDir: File, workingDir: File) {
        val packageName = packageName.get()
        val fullPackageName = fullPackageName.get()
        val tmpImage = workingDir.resolve("$fullPackageName.tmp.dmg")
        val finalImage = destinationDir.resolve("$fullPackageName.dmg")

        createImage(volumeName = packageName, imageFile = tmpImage, srcDir = appDir.ioFile)
        val mounted = mountImage(volumeName = packageName, imageFile = tmpImage)
        try {
            runSetupScript(appName = packageName, mounted)
        } finally {
            unmountImage(mounted)
        }
        finalizeImage(tmpImage, finalImage)
        logger.lifecycle("The distribution is written to ${finalImage.canonicalPath}")
    }

    private fun createImage(volumeName: String, imageFile: File, srcDir: File) {
        var size = srcDir.walk().filter { it.isFile }.sumOf { it.length() }
        size += 10 * 1024 * 1024

        hdiutil(
            "create",
            "-srcfolder", srcDir.absolutePath,
            "-volname", volumeName,
            "-size", size.toString(),
            "-ov", imageFile.absolutePath,
            "-fs", "HFS+",
            "-format", "UDRW"
        )
    }

    private data class MountedImage(val device: String, val disk: String)
    private fun mountImage(volumeName: String, imageFile: File): MountedImage {
        val output = hdiutil(
            "attach",
            "-readwrite",
            "-noverify",
            "-noautoopen",
            imageFile.absolutePath
        )
        Thread.sleep(3000)
        var device: String? = null
        var volume: String? = null

        for (line in output.split("\n")) {
            if (!line.startsWith("/dev/")) continue

            val volumeIndex = line.lastIndexOf("/Volumes/$volumeName")
            if (volumeIndex <= 0) continue

            volume = line.substring(volumeIndex).trimEnd()
            device = line.substring(0, line.indexOfFirst(Char::isWhitespace))
        }
        check(device != null && volume != null) {
            "Could not parse mounted image's device ($device) & volume ($volume) from hdiutil output:" +
                    "\n=======\n" +
                    output +
                    "\n=======\n"
        }
        if (verbose.get()) {
            logger.info("Mounted DMG image '$imageFile': volume '$volume', device '$device'")
        }
        return MountedImage(device = device, disk = volume.removePrefix("/Volumes/"))
    }

    private fun unmountImage(mounted: MountedImage) {
        hdiutil("detach", mounted.device)
    }

    private fun finalizeImage(tmpImage: File, finalImage: File) {
        hdiutil(
            "convert",
            tmpImage.absolutePath,
            "-format", "UDZO",
            "-imagekey", "zlib-level=9",
            "-o", finalImage.absolutePath
        )
    }

    private fun hdiutil(vararg args: String): String {
        var resultStdout = ""
        val allArgs = args.toMutableList()
        if (verbose.get()) {
            allArgs.add("-verbose")
        }
        runExternalTool(tool = hdiutil.ioFile, args = allArgs, processStdout = { resultStdout = it })
        return resultStdout
    }

    private fun runSetupScript(appName: String, mounted: MountedImage) {
        val disk = mounted.disk
        val installDir = installDir.get()
        val setupScript = workingDir.ioFile.resolve("setup-dmg.scpt").apply {
            writeText("""
                   tell application "Finder"
                     tell disk "$disk"
                           open
                           set current view of container window to icon view
                           set toolbar visible of container window to false
                           set statusbar visible of container window to false
                           set the bounds of container window to {400, 100, 885, 430}
                           set theViewOptions to the icon view options of container window
                           set arrangement of theViewOptions to not arranged
                           set icon size of theViewOptions to 72
                           make new alias file at container window to POSIX file "$installDir" with properties {name:"$installDir"}
                           set position of item "$appName" of container window to {100, 100}
                           set position of item "$installDir" of container window to {375, 100}
                           update without registering applications
                           delay 5
                           close
                     end tell
                   end tell
            """.trimIndent())
        }
        runExternalTool(tool = osascript.ioFile, args = listOf(setupScript.absolutePath))
    }
}