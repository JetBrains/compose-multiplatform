/*
 * Copyright 2020-2023 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.experimental.uikit.tasks

import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileCollection
import org.gradle.api.provider.Provider
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.*
import org.jetbrains.compose.experimental.uikit.internal.determineIosKonanTargetsFromEnv
import org.jetbrains.compose.experimental.uikit.internal.IosTargetResources
import java.io.File

abstract class SyncComposeResourcesForIosTask : AbstractComposeIosTask() {
    private fun missingTargetEnvAttributeError(attribute: String): Provider<Nothing> =
        providers.provider {
            error(
                "Could not infer iOS target $attribute. Make sure to build " +
                        "via XCode (directly or via Kotlin Multiplatform Mobile plugin for Android Studio)")
        }

    @get:Input
    val xcodeTargetPlatform: Provider<String> =
        providers.gradleProperty("compose.ios.resources.platform")
            .orElse(providers.gradleProperty("compose.ios.resources.platform"))
            .orElse(providers.environmentVariable("PLATFORM_NAME"))
            .orElse(missingTargetEnvAttributeError("platform"))

    @get:Input
    val xcodeTargetArchs: Provider<List<String>> =
        providers.gradleProperty("compose.ios.resources.archs")
            .orElse(providers.environmentVariable("ARCHS"))
            .orElse(missingTargetEnvAttributeError("architectures"))
            .map { it.split(",", " ").filter { it.isNotBlank() } }

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
        val outputDir = outputDir.get().asFile.apply { mkdirs() }
        val resourceFiles = resourceFiles.get().files
        for (file in resourceFiles) {
            val targetFile = outputDir.resolve(file.name)
            file.copyTo(targetFile, overwrite = true)
        }
        logger.info("Synced Compose resource files. Copied ${resourceFiles.size} files to $outputDir")
    }
}
