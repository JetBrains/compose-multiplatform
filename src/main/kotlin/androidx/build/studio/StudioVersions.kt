/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.build.studio

import androidx.build.SupportConfig

/**
 * Studio version information used for setting up the correct version of Android Studio.
 *
 * Note: Android Gradle Plugin version is needed as part of configuring the build, so it is
 * defined separately in build_dependencies.gradle.
 *
 * Once you have a chosen version of AGP to upgrade to, go to
 * https://developer.android.com/studio/archive and find the matching version of Studio. For example,
 * if you are upgrading to AGP 3.6 alpha 05, look for the Studio 3.6 canary 5 build. (alpha maps to
 * canary, beta to beta, rc to rc, and no suffix for stable in both cases)
 *
 * The download url should contain: ...ide-zips/3.6.0.5/android-studio-ide-191.5721125-linux...
 * From this, the first number (3.6.0.5) is [studioVersion], the first number in the filename (192)
 * is the [ideaMajorVersion] and the last number (5721125) is the [studioBuildNumber].
 *
 * [buildTxtOverride] is the optional overridden version string to write to build.txt, this is needed
 * for the UI project as the prebuilts have non-final version string with erroneous dashes, so we
 * generate a new one that the Intellij gradle plugin will accept.
 */
sealed class StudioVersions {
    abstract val studioVersion: String
    abstract val ideaMajorVersion: String
    abstract val studioBuildNumber: String
    abstract val buildTxtOverride: String

    companion object {
        /**
         * Gets the relevant [StudioVersions] for the current root project.
         */
        fun get() = if (SupportConfig.isUiProject()) {
            UiStudioVersions
        } else {
            RootStudioVersions
        }
    }
}

private object RootStudioVersions : StudioVersions() {
    override val studioVersion = "3.6.0.18"
    override val ideaMajorVersion = "192"
    override val studioBuildNumber = "6071332"
    override val buildTxtOverride: String = ""
}

private object UiStudioVersions : StudioVersions() {
    override val studioVersion = "4.0.0.0"
    override val ideaMajorVersion = "192"
    override val studioBuildNumber = "5959023"
    override val buildTxtOverride = "AI-$ideaMajorVersion.6817.14.36.$studioBuildNumber"
}
