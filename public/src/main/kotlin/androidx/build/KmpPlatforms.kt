/*
 * Copyright 2022 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.build

import java.util.Locale
import org.gradle.api.Project

/**
 * A comma separated list of KMP target platforms you wish to enable / disable.
 * e.g. '-jvm,+mac,+linux,+js'
 */
const val ENABLED_KMP_TARGET_PLATFORMS = "androidx.enabled.kmp.target.platforms"

enum class KmpPlatform {
    JVM,
    // Do _not_ enable unless you have read and understand this:
    // https://blog.jetbrains.com/kotlin/2021/10/important-ua-parser-js-exploit-and-kotlin-js/
    JS,
    MAC,
    LINUX;
    companion object {
        val native = listOf(MAC, LINUX)
        val enabledByDefault = listOf(JVM)
        private const val JVM_PLATFORM = "jvm"
        private const val JS_PLATFORM = "js"
        private const val MAC_ARM_64 = "macosarm64"
        private const val MAC_OSX_64 = "macosx64"
        private const val LINUX_64 = "linuxx64"
        private const val IOS_SIMULATOR_ARM_64 = "iossimulatorarm64"
        private const val IOS_X_64 = "iosx64"
        private const val IOS_ARM_64 = "iosarm64"
        val macPlatforms = listOf(MAC_ARM_64, MAC_OSX_64)
        val linuxPlatforms = listOf(LINUX_64)
        val iosPlatforms = listOf(IOS_SIMULATOR_ARM_64, IOS_ARM_64, IOS_X_64)
        val nativePlatforms = macPlatforms + linuxPlatforms + iosPlatforms
    }
}

object KmpFlagParser {
    fun parse(flag: String?): Set<KmpPlatform> {
        if (flag.isNullOrBlank()) {
            return KmpPlatform.enabledByDefault.toSortedSet()
        }
        val enabled = KmpPlatform.enabledByDefault.toMutableList()
        flag.split(",").forEach {
            val directive = it.firstOrNull() ?: ""
            val platform = it.drop(1)
            when (directive) {
                '+' -> enabled.addAll(matchingPlatforms(platform))
                '-' -> enabled.removeAll(matchingPlatforms(platform))
                else -> {
                    throw RuntimeException("Invalid value $flag for $ENABLED_KMP_TARGET_PLATFORMS")
                }
            }
        }
        return enabled.toSortedSet()
    }

    private fun matchingPlatforms(flag: String) = if (flag == "native") {
        KmpPlatform.native
    } else {
        listOf(KmpPlatform.valueOf(flag.uppercase(Locale.getDefault())))
    }
}

fun Project.enabledKmpPlatforms(): Set<KmpPlatform> {
    val enabledPlatformsFlag = project.findProperty(ENABLED_KMP_TARGET_PLATFORMS) as? String
    return KmpFlagParser.parse(enabledPlatformsFlag)
}

fun Project.enableJs(): Boolean = enabledKmpPlatforms().contains(KmpPlatform.JS)
fun Project.enableMac(): Boolean =
    enabledKmpPlatforms().contains(KmpPlatform.MAC) || Multiplatform.isKotlinNativeEnabled(this)
fun Project.enableLinux(): Boolean =
    enabledKmpPlatforms().contains(KmpPlatform.LINUX) || Multiplatform.isKotlinNativeEnabled(this)
fun Project.enableJvm(): Boolean = enabledKmpPlatforms().contains(KmpPlatform.JVM)
fun Project.enableNative(): Boolean = enableMac() && enableLinux()