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

/**
 * Sub-extension to configure which platforms are built / published.
 */
open class PublishExtension {
    var android = Publish.UNSET
    var jvm = Publish.UNSET
    var js = Publish.UNSET
    var mac = Publish.UNSET
    var linux = Publish.UNSET
    var ios = Publish.UNSET

    /**
     * List of platforms names which should be published to maven. e.g. ["jvm", "js"]
     */
    val publishPlatforms: List<String>
        get() {
            val platforms = mutableListOf<String>()
            if (jvm.shouldPublish()) {
                platforms.add(JVM_PLATFORM)
            }
            if (js.shouldPublish()) {
                platforms.add(JS_PLATFORM)
            }
            if (mac.shouldPublish()) {
                platforms.addAll(macPlatforms)
            }
            if (linux.shouldPublish()) {
                platforms.addAll(linuxPlatforms)
            }
            if (ios.shouldPublish()) {
                platforms.addAll(iosPlatforms)
            }
            return platforms
        }
    private val allExtendedPlatforms
        get() = listOf(jvm, js, mac, linux, ios)
    private val allPlatforms
        get() = listOf(android) + allExtendedPlatforms
    private val activeExtendedPlatforms
        get() = allExtendedPlatforms.filter { it != Publish.UNSET }

    /**
     * Returns true if we need to publish any non-android platforms
     */
    fun shouldEnableMultiplatform() = activeExtendedPlatforms.isNotEmpty()
    fun shouldPublishAny() = allPlatforms.any {
        it.shouldPublish()
    }
    fun shouldReleaseAny() = allPlatforms.any {
        it.shouldRelease()
    }
    fun isPublishConfigured() = allPlatforms.any {
        it != Publish.UNSET
    }

    companion object {
        private const val JVM_PLATFORM = "jvm"
        private const val JS_PLATFORM = "js"
        private const val MAC_ARM_64 = "macosarm64"
        private const val MAC_OSX_64 = "macosx64"
        private const val LINUX_64 = "linuxx64"
        private const val IOS_SIMULATOR_ARM_64 = "iossimulatorarm64"
        private const val IOS_X_64 = "iosx64"
        private const val IOS_ARM_64 = "iosarm64"
        private val macPlatforms = listOf(MAC_ARM_64, MAC_OSX_64)
        private val linuxPlatforms = listOf(LINUX_64)
        private val iosPlatforms = listOf(IOS_SIMULATOR_ARM_64, IOS_ARM_64, IOS_X_64)
    }
}