/*
 * Copyright 2025 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.konan.target.*

description = "XCTest wrapper of Native kotlin.test"

plugins {
    alias(libs.plugins.kotlinMultiplatform)
}

repositories {
    mavenCentral()
}

fun frameworksPath(target: KonanTarget): String {
    fun getSdkPlatformPath(platform: String) =
        ProcessBuilder("xcrun", "--sdk", platform, "--show-sdk-platform-path").execute()
    val path = when (target) {
        KonanTarget.MACOS_ARM64, KonanTarget.MACOS_X64 -> getSdkPlatformPath("macosx")
        KonanTarget.IOS_SIMULATOR_ARM64, KonanTarget.IOS_X64 -> getSdkPlatformPath("iphonesimulator")
        KonanTarget.IOS_ARM64 -> getSdkPlatformPath("iphoneos")
        else -> error("Target $this is not supported")
    }
    return "${path}/Developer/Library/Frameworks/"
}

val nativeTargets = mutableListOf<KotlinNativeTarget>()

val hostManager = HostManager()
fun MutableList<KotlinNativeTarget>.addIfEnabledOnHost(target: KotlinNativeTarget) {
    if (hostManager.isEnabled(target.konanTarget)) add(target)
}

kotlin {
    with(nativeTargets) {
        addIfEnabledOnHost(macosX64())
        addIfEnabledOnHost(macosArm64())
        addIfEnabledOnHost(iosX64())
        addIfEnabledOnHost(iosArm64())
        addIfEnabledOnHost(iosSimulatorArm64())

        forEach {
            it.compilations.all {
                cinterops {
                    register("XCTest") {
                        val path = frameworksPath(it.konanTarget)
                        compilerOpts("-iframework", path)
                    }
                }
                compileTaskProvider.configure {
                    compilerOptions {
                        freeCompilerArgs.add("-Xdont-warn-on-error-suppression")
                    }
                }
            }
        }
    }
    sourceSets.all {
        languageSettings.apply {
            optIn("kotlinx.cinterop.BetaInteropApi")
            optIn("kotlinx.cinterop.ExperimentalForeignApi")
            optIn("kotlin.experimental.ExperimentalNativeApi")
        }
    }
}

private fun ProcessBuilder.execute(): String {
    return start().inputStream.bufferedReader().readLine()
}
