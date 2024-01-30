/*
 * Copyright 2020-2023 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.resources.ios

import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileCollection
import org.gradle.api.provider.Provider
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.*
import org.jetbrains.compose.experimental.uikit.tasks.AbstractComposeIosTask
import org.jetbrains.compose.internal.utils.clearDirs
import java.io.File
import kotlin.io.path.Path
import kotlin.io.path.pathString
import kotlin.io.path.relativeTo

abstract class SyncComposeResourcesForIosTask : AbstractComposeIosTask() {

    private fun Provider<String>.orElseThrowMissingAttributeError(attribute: String): Provider<String> {
        val noProvidedValue = "__NO_PROVIDED_VALUE__"
        return this.orElse(noProvidedValue).map {
            if (it == noProvidedValue) {
                error(
                    "Could not infer iOS target $attribute. Make sure to build " +
                            "via XCode (directly or via Kotlin Multiplatform Mobile plugin for Android Studio)")
            }
            it
        }
    }

    @get:Input
    val xcodeTargetPlatform: Provider<String> =
        providers.gradleProperty("compose.ios.resources.platform")
            .orElse(providers.environmentVariable("PLATFORM_NAME"))
            .orElseThrowMissingAttributeError("platform")


    @get:Input
    val xcodeTargetArchs: Provider<List<String>> =
        providers.gradleProperty("compose.ios.resources.archs")
            .orElse(providers.environmentVariable("ARCHS"))
            .orElseThrowMissingAttributeError("architectures")
            .map {
                it.split(",", " ").filter { it.isNotBlank() }
            }

    @get:Input
    internal val iosTargets: SetProperty<IosTargetResources> = objects.setProperty(IosTargetResources::class.java)

    @get:PathSensitive(PathSensitivity.ABSOLUTE)
    @get:InputFiles
    val resourceFiles: Provider<FileCollection> = xcodeTargetPlatform.zip(xcodeTargetArchs, ::Pair)
        .map { (xcodeTargetPlatform, xcodeTargetArchs) ->
            val allResources = objects.fileCollection()
            val activeKonanTargets = determineIosKonanTargetsFromEnv(xcodeTargetPlatform, xcodeTargetArchs)
                .mapTo(HashSet()) { it.name }
            val dirsToInclude = iosTargets.get()
                .filter { it.konanTarget.get() in activeKonanTargets }
                .flatMapTo(HashSet()) { it.dirs.get() }
            for (dirPath in dirsToInclude) {
                val fileTree = objects.fileTree().apply {
                    setDir(layout.projectDirectory.dir(dirPath))
                    include("**/*")
                }
                allResources.from(fileTree)
            }
            allResources
        }

    @get:OutputDirectory
    val outputDir: DirectoryProperty = objects.directoryProperty()

    @TaskAction
    fun run() {
        val outputDir = outputDir.get().asFile
        fileOperations.clearDirs(outputDir)
        val allResourceDirs = iosTargets.get().flatMapTo(HashSet()) { it.dirs.get().map { Path(it).toAbsolutePath() } }

        fun copyFileToOutputDir(file: File) {
            for (dir in allResourceDirs) {
                val path = file.toPath().toAbsolutePath()
                if (path.startsWith(dir)) {
                    val targetFile = outputDir.resolve(path.relativeTo(dir).pathString)
                    file.copyTo(targetFile, overwrite = true)
                    return
                }
            }

            error(
                buildString {
                    appendLine("Resource file '$file' does not belong to a known resource directory:")
                    allResourceDirs.forEach {
                        appendLine("* $it")
                    }
                }
            )
        }

        val resourceFiles = resourceFiles.get().files
        for (file in resourceFiles) {
            copyFileToOutputDir(file)
        }
        logger.info("Synced Compose resource files. Copied ${resourceFiles.size} files to $outputDir")
    }
}
