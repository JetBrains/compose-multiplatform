/*
 * Copyright 2020-2022 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.desktop.application.tasks

import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import org.gradle.api.tasks.Optional
import org.jetbrains.compose.desktop.application.internal.*
import org.jetbrains.compose.desktop.application.internal.InfoPlistBuilder
import org.jetbrains.compose.desktop.application.internal.PlistKeys
import org.jetbrains.compose.desktop.application.internal.ioFile
import org.jetbrains.compose.desktop.application.internal.notNullProperty
import java.io.File
import java.util.*

private const val KOTLIN_NATIVE_MIN_SUPPORTED_MAC_OS = "10.13"

abstract class AbstractNativeMacApplicationPackageAppDirTask : AbstractNativeMacApplicationPackageTask() {
    @get:InputFile
    @get:PathSensitive(PathSensitivity.ABSOLUTE)
    val executable: RegularFileProperty = objects.fileProperty()

    @get:InputFile
    @get:Optional
    @get:PathSensitive(PathSensitivity.ABSOLUTE)
    val iconFile: RegularFileProperty = objects.fileProperty()

    @get:Input
    val bundleID: Property<String> = objects.notNullProperty<String>().value(packageName)

    @get:Input
    @get:Optional
    val appCategory: Property<String?> = objects.nullableProperty()

    @get:Input
    @get:Optional
    val copyright: Property<String?> = objects.nullableProperty()

    override fun createPackage(destinationDir: File, workingDir: File) {
        val packageName = packageName.get()
        val appDir = destinationDir.resolve("$packageName.app").apply { mkdirs() }
        val contentsDir = appDir.resolve("Contents").apply { mkdirs() }
        val macOSDir = contentsDir.resolve("MacOS").apply { mkdirs() }
        val appResourcesDir = contentsDir.resolve("Resources").apply { mkdirs() }

        val appExecutableFile = macOSDir.resolve(packageName)
        executable.ioFile.copyTo(appExecutableFile)
        appExecutableFile.setExecutable(true)

        val appIconFile = appResourcesDir.resolve("$packageName.icns")
        iconFile.ioFile.copyTo(appIconFile)

        InfoPlistBuilder().apply {
            setupInfoPlist(executableName = appExecutableFile.name)
            writeToFile(contentsDir.resolve("Info.plist"))
        }
    }

    private fun InfoPlistBuilder.setupInfoPlist(executableName: String) {
        this[PlistKeys.LSMinimumSystemVersion] = KOTLIN_NATIVE_MIN_SUPPORTED_MAC_OS
        this[PlistKeys.CFBundleDevelopmentRegion] = "English"
        this[PlistKeys.CFBundleAllowMixedLocalizations] = "true"
        this[PlistKeys.CFBundleExecutable] = executableName
        this[PlistKeys.CFBundleIconFile] = iconFile.ioFile.name
        this[PlistKeys.CFBundleIdentifier] = bundleID.get()
        val packageVersion = packageVersion.get()
        this[PlistKeys.CFBundleShortVersionString] = packageVersion
        this[PlistKeys.CFBundleVersion] = packageVersion
        this[PlistKeys.LSApplicationCategoryType] = appCategory.orNull
        this[PlistKeys.NSHumanReadableCopyright] = copyright.orNull
        this[PlistKeys.NSSupportsAutomaticGraphicsSwitching] = "true"
        this[PlistKeys.NSHighResolutionCapable] = "true"
    }
}