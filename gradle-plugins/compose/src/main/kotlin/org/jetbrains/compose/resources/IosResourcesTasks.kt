package org.jetbrains.compose.resources

import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileCollection
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Provider
import org.gradle.api.provider.ProviderFactory
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.jetbrains.kotlin.konan.target.KonanTarget
import javax.inject.Inject

internal abstract class SyncComposeResourcesForIosTask : DefaultTask() {

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

    @get:Inject
    protected abstract val providers: ProviderFactory

    @get:Inject
    protected abstract val objects: ObjectFactory

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
                .mapNotNull { konanTarget -> targetResources.getting(konanTarget.name).get() }
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
                logger.info("File '${dir.path}' is not a dir or doesn't exist")
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

        platform.startsWith("macosx") -> {
            targets.addAll(archs.map { arch ->
                when (arch) {
                    "arm64" -> KonanTarget.MACOS_ARM64
                    "x86_64" -> KonanTarget.MACOS_X64
                    else -> error("Unknown macOS arch: '$arch'")
                }
            })
        }

        else -> error("Unknown Apple platform: '$platform'")
    }

    return targets.toList()
}

/**
 * Since Xcode 15, there is a new default setting: `ENABLE_USER_SCRIPT_SANDBOXING = YES`
 * It's set in project.pbxproj
 *
 * SyncComposeResourcesForIosTask fails to work with it right now.
 *
 * Gradle attempts to create an output folder for SyncComposeResourcesForIosTask on our behalf,
 * so we can't handle an exception when it occurs. Therefore, we make SyncComposeResourcesForIosTask
 * depend on CheckCanAccessComposeResourcesDirectory, where we check ENABLE_USER_SCRIPT_SANDBOXING.
 */
internal abstract class CheckCanAccessComposeResourcesDirectory : DefaultTask() {
    @get:Input
    val enabled = project.providers.environmentVariable("ENABLE_USER_SCRIPT_SANDBOXING")
        .orElse("NOT_DEFINED")
        .map { it == "YES" }

    @TaskAction
    fun run() {
        if (enabled.get()) {
            logger.error("""
                Failed to sync compose resources!
                Please make sure ENABLE_USER_SCRIPT_SANDBOXING is set to 'NO' in 'project.pbxproj'
            """.trimIndent())
            throw IllegalStateException(
                "Sandbox environment detected (ENABLE_USER_SCRIPT_SANDBOXING = YES). It's not supported so far."
            )
        }
    }
}