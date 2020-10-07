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

import androidx.compose.runtime.staticAmbientOf

val DesktopPlatformAmbient = staticAmbientOf(::identifyCurrentPlatform)

/**
 * Identify OS on which the application is currently running.
 *
 * If it is needed to know the current platform in @Composable function,
 * use [DesktopPlatformAmbient] instead of this function.
 *
 * identifyCurrentPlatform() should be used preferable only in initialization code.
 */
fun identifyCurrentPlatform(): DesktopPlatform {
    val name = System.getProperty("os.name")
    return when {
        name.startsWith("Linux") -> DesktopPlatform.Linux
        name.startsWith("Win") -> DesktopPlatform.Windows
        name == "Mac OS X" -> DesktopPlatform.MacOS
        else -> throw Error("Unsupported OS $name")
    }
}

enum class DesktopPlatform {
    Linux,
    Windows,
    MacOS
}