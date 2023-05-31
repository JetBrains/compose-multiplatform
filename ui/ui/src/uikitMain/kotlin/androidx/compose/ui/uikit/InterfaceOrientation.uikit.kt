/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.compose.ui.uikit

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.InternalComposeApi
import androidx.compose.runtime.State
import androidx.compose.runtime.staticCompositionLocalOf
import platform.UIKit.*

/**
 * Wraps valid layout values for iOS UIInterfaceOrientation
 */
@InternalComposeApi
@Immutable
enum class InterfaceOrientation(private val rawValue: UIInterfaceOrientation) {
    Portrait(UIInterfaceOrientationPortrait),
    PortraitUpsideDown(UIInterfaceOrientationPortraitUpsideDown),
    LandscapeLeft(UIInterfaceOrientationLandscapeLeft),
    LandscapeRight(UIInterfaceOrientationLandscapeRight);

    companion object {
        fun getByRawValue(orientation: UIInterfaceOrientation): InterfaceOrientation? {
            return values().firstOrNull {
                it.rawValue == orientation
            }
        }
    }
}

/**
 * Composition local for [InterfaceOrientation]
 */
@InternalComposeApi
val LocalInterfaceOrientationState = staticCompositionLocalOf<State<InterfaceOrientation>> {
    error("CompositionLocal LocalInterfaceOrientationState not present")
}
