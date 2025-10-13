/*
 * Copyright 2020-2022 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.desktop.application.tasks

import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import org.gradle.api.tasks.Optional
import org.jetbrains.compose.desktop.application.internal.InfoPlistBuilder
import org.jetbrains.compose.desktop.application.internal.MacAssetsTool
import org.jetbrains.compose.desktop.application.internal.PlistKeys
import org.jetbrains.compose.internal.utils.ioFile
import org.jetbrains.compose.internal.utils.notNullProperty
import org.jetbrains.compose.internal.utils.nullableProperty
import java.io.File
import kotlin.getValue

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

    @get:Input
    @get:Optional
    val minimumSystemVersion: Property<String?> = objects.nullableProperty()

    @get:InputFiles
    @get:Optional
    @get:PathSensitive(PathSensitivity.ABSOLUTE)
    val composeResourcesDirs: ConfigurableFileCollection = objects.fileCollection()

    @get:InputDirectory
    @get:Optional
    internal val macLayeredIcons: DirectoryProperty = objects.directoryProperty()

    private val macAssetsTool by lazy { MacAssetsTool(runExternalTool, logger) }

    override fun createPackage(destinationDir: File, workingDir: File) {
        val packageName = packageName.get()
        val appDir = destinationDir.resolve("$packageName.app").apply { mkdirs() }
        val contentsDir = appDir.resolve("Contents").apply { mkdirs() }
        val macOSDir = contentsDir.resolve("MacOS").apply { mkdirs() }
        val appResourcesDir = contentsDir.resolve("Resources").apply { mkdirs() }

        val appExecutableFile = macOSDir.resolve(packageName)
        executable.ioFile.copyTo(appExecutableFile)
        appExecutableFile.setExecutable(true)

        macLayeredIcons.orNull?.let {
            runCatching {
                macAssetsTool.compileAssets(
                    iconDir = it.asFile,
                    workingDir = workingDir,
                    minimumSystemVersion = minimumSystemVersion.getOrElse(KOTLIN_NATIVE_MIN_SUPPORTED_MAC_OS)
                )
            }.onFailure { error ->
                logger.warn("Can not compile layered icon: ${error.message}")
            }
        }

        val appIconFile = appResourcesDir.resolve("$packageName.icns")
        iconFile.ioFile.copyTo(appIconFile)

        InfoPlistBuilder().apply {
            setupInfoPlist(executableName = appExecutableFile.name)
            writeToFile(contentsDir.resolve("Info.plist"))
        }

        if (!composeResourcesDirs.isEmpty) {
            fileOperations.copy { copySpec ->
                copySpec.from(composeResourcesDirs)
                copySpec.into(appResourcesDir.resolve("compose-resources").apply { mkdirs() })
            }
        }

        macAssetsTool.assetsFile(workingDir).let {
            if (it.exists()) {
                fileOperations.copy { copySpec ->
                    copySpec.from(it)
                    copySpec.into(appResourcesDir)
                }
            }
        }
    }

    private fun InfoPlistBuilder.setupInfoPlist(executableName: String) {
        this[PlistKeys.LSMinimumSystemVersion] = minimumSystemVersion.getOrElse(KOTLIN_NATIVE_MIN_SUPPORTED_MAC_OS)
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

        if (macAssetsTool.assetsFile(workingDir.ioFile).exists()) {
            macLayeredIcons.orNull?.let { this[PlistKeys.CFBundleIconName] = it.asFile.name.removeSuffix(".icon") }
        }
    }
}