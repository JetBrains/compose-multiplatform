/*
 * Copyright 2020-2023 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.resources.ios

import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileCollection
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.*
import org.jetbrains.compose.experimental.uikit.tasks.AbstractComposeIosTask
import org.jetbrains.kotlin.konan.target.KonanTarget

internal abstract class SyncComposeResourcesForIosTask : AbstractComposeIosTask() {

    private fun Provider<String>.orElseThrowMissingAttributeError(attribute: String): Provider<String> {
        val noProvidedValue = "__NO_PROVIDED_VALUE__"
        return this.orElse(noProvidedValue).map {
            if (it == noProvidedValue) {
                error(
                    "Could not infer iOS target $attribute. Make sure to build " +
                            "via XCode (directly or via Kotlin Multiplatform Mobile plugin for Android Studio)"
                )
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
            .map { str -> str.split(",", " ").filter { it.isNotBlank() } }

    @get:Internal
    internal abstract val targetResources: MapProperty<String, FileCollection>

    @get:PathSensitive(PathSensitivity.ABSOLUTE)
    @get:InputFiles
    val resourceFiles: Provider<FileCollection> =
        xcodeTargetPlatform.zip(xcodeTargetArchs, ::Pair).map { (xcodeTargetPlatform, xcodeTargetArchs) ->
            val allResources = getRequestedKonanTargetsByXcode(xcodeTargetPlatform, xcodeTargetArchs)
                .mapNotNull { konanTarget -> targetResources.getting(konanTarget.name).get().files }
            objects.fileCollection().from(*allResources.toTypedArray())
        }

    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty

    @TaskAction
    fun run() {
        val outputDir = outputDir.get().asFile
        outputDir.deleteRecursively()
        outputDir.mkdirs()
        logger.info("Clean ${outputDir.path}")

        resourceFiles.get().forEach { dir ->
            if (dir.exists() && dir.isDirectory) {
                logger.info("Copy '${dir.path}' to '${outputDir.path}'")
                dir.walkTopDown().filter { !it.isDirectory && !it.isHidden }.forEach { file ->
                    val targetFile = outputDir.resolve(file.relativeTo(dir))
                    if (targetFile.exists()) {
                        logger.info("Skip [already exists] '${file.path}'")
                    } else {
                        logger.info(" -> '${file.path}'")
                        file.copyTo(targetFile)
                    }
                }
            } else {
                logger.warn("File '${dir.path}' is not a dir or doesn't exist")
            }
        }
    }
}

// based on AppleSdk.kt from Kotlin Gradle Plugin
// See https://github.com/JetBrains/kotlin/blob/142421da5b966049b4eab44ce6856eb172cf122a/libraries/tools/kotlin-gradle-plugin/src/common/kotlin/org/jetbrains/kotlin/gradle/plugin/mpp/apple/AppleSdk.kt
private fun getRequestedKonanTargetsByXcode(platform: String, archs: List<String>): List<KonanTarget> {
    val targets: MutableSet<KonanTarget> = mutableSetOf()

    when {
        platform.startsWith("iphoneos") -> {
            targets.addAll(archs.map { arch ->
                when (arch) {
                    "arm64", "arm64e" -> KonanTarget.IOS_ARM64
                    "armv7", "armv7s" -> KonanTarget.IOS_ARM32
                    else -> error("Unknown iOS device arch: '$arch'")
                }
            })
        }

        platform.startsWith("iphonesimulator") -> {
            targets.addAll(archs.map { arch ->
                when (arch) {
                    "arm64", "arm64e" -> KonanTarget.IOS_SIMULATOR_ARM64
                    "x86_64" -> KonanTarget.IOS_X64
                    else -> error("Unknown iOS simulator arch: '$arch'")
                }
            })
        }

        else -> error("Unknown iOS platform: '$platform'")
    }

    return targets.toList()
}
