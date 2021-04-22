/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.compose.ui.platform

import androidx.compose.runtime.staticCompositionLocalOf

// TODO(demin): changing DesktopPlatform is confusing.
//  Replace DesktopPlatform by DesktopLookAndFeel
//  (https://github.com/JetBrains/compose-jb/issues/303).
//  Or get rid of it completely
//  and provide appropriate Local's (LocalMouseScrollConfig, etc).
val LocalDesktopPlatform = staticCompositionLocalOf(DesktopPlatform::Current)

enum class DesktopPlatform {
    Linux,
    Windows,
    MacOS;

    companion object {
        /**
         * Identify OS on which the application is currently running.
         *
         * If it is needed to know the current platform in @Composable function,
         * use [LocalDesktopPlatform] instead of this function, so composable functions can
         * change their behaviour if we change platform in tests or in application settings
         * (some applications maybe will have this setting)
         */
        val Current: DesktopPlatform by lazy {
            val name = System.getProperty("os.name")
            when {
                name.startsWith("Linux") -> Linux
                name.startsWith("Win") -> Windows
                name == "Mac OS X" -> MacOS
                else -> throw Error("Unsupported OS $name")
            }
        }
    }
}