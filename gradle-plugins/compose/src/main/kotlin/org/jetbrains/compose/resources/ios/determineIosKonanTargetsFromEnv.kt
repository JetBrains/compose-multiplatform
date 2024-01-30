/*
 * Copyright 2020-2023 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.resources.ios

import org.jetbrains.kotlin.konan.target.KonanTarget

// based on AppleSdk.kt from Kotlin Gradle Plugin
// See https://github.com/JetBrains/kotlin/blob/142421da5b966049b4eab44ce6856eb172cf122a/libraries/tools/kotlin-gradle-plugin/src/common/kotlin/org/jetbrains/kotlin/gradle/plugin/mpp/apple/AppleSdk.kt
internal fun determineIosKonanTargetsFromEnv(platform: String, archs: List<String>): List<KonanTarget> {
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