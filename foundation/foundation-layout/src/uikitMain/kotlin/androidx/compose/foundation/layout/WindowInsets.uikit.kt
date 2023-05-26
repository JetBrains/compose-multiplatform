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

package androidx.compose.foundation.layout

import androidx.compose.runtime.Composable
import androidx.compose.runtime.InternalComposeApi
import androidx.compose.ui.uikit.*
import androidx.compose.ui.unit.dp

private val ZeroInsets = WindowInsets(0, 0, 0, 0)

/**
 * This insets represents iOS SafeAreas.
 */
private val WindowInsets.Companion.iosSafeArea: WindowInsets
    @Composable
    @OptIn(InternalComposeApi::class)
    get() = WindowInsets(
        top = LocalSafeAreaState.current.value.top,
        bottom = LocalSafeAreaState.current.value.bottom,
        left = LocalSafeAreaState.current.value.left,
        right = LocalSafeAreaState.current.value.right,
    )

/**
 * This insets represents iOS layoutMargins.
 */
private val WindowInsets.Companion.layoutMargins: WindowInsets
    @Composable
    @OptIn(InternalComposeApi::class)
    get() = WindowInsets(
        top = LocalLayoutMarginsState.current.value.top,
        bottom = LocalLayoutMarginsState.current.value.bottom,
        left = LocalLayoutMarginsState.current.value.left,
        right = LocalLayoutMarginsState.current.value.right,
    )

/**
 * An insets type representing the window of a caption bar.
 * It is useless for iOS.
 */
val WindowInsets.Companion.captionBar get() = ZeroInsets

/**
 * This [WindowInsets] represents the area with the display cutout (e.g. for camera).
 */
val WindowInsets.Companion.displayCutout: WindowInsets
    @Composable
    @OptIn(InternalComposeApi::class)
    get() = when (LocalInterfaceOrientationState.current.value) {
        InterfaceOrientation.Portrait -> iosSafeArea.only(WindowInsetsSides.Top)
        InterfaceOrientation.PortraitUpsideDown -> iosSafeArea.only(WindowInsetsSides.Bottom)
        InterfaceOrientation.LandscapeLeft -> iosSafeArea.only(WindowInsetsSides.Right)
        InterfaceOrientation.LandscapeRight -> iosSafeArea.only(WindowInsetsSides.Left)
    }

/**
 * An insets type representing the window of an "input method",
 * for iOS IME representing the software keyboard.
 *
 * TODO: Animation doesn't work on iOS yet
 */
val WindowInsets.Companion.ime: WindowInsets
    @Composable
    @OptIn(InternalComposeApi::class)
    get() = WindowInsets(bottom = LocalKeyboardOverlapHeightState.current.value.dp)

/**
 * These insets represents the space where system gestures have priority over application gestures.
 */
val WindowInsets.Companion.mandatorySystemGestures: WindowInsets
    @Composable
    get() = iosSafeArea.only(WindowInsetsSides.Top + WindowInsetsSides.Bottom)

/**
 * These insets represent where system UI places navigation bars.
 * Interactive UI should avoid the navigation bars area.
 */
val WindowInsets.Companion.navigationBars: WindowInsets
    @Composable
    get() = iosSafeArea.only(WindowInsetsSides.Bottom)

/**
 * These insets represents status bar.
 */
val WindowInsets.Companion.statusBars: WindowInsets
    @Composable
    @OptIn(InternalComposeApi::class)
    get() = when (LocalInterfaceOrientationState.current.value) {
        InterfaceOrientation.Portrait -> iosSafeArea.only(WindowInsetsSides.Top)
        else -> ZeroInsets
    }

/**
 * These insets represents all system bars.
 * Includes [statusBars], [captionBar] as well as [navigationBars], but not [ime].
 */
val WindowInsets.Companion.systemBars: WindowInsets
    @Composable
    get() = iosSafeArea

/**
 * The systemGestures insets represent the area of a window where system gestures have
 * priority and may consume some or all touch input, e.g. due to the system bar
 * occupying it, or it being reserved for touch-only gestures.
 */
val WindowInsets.Companion.systemGestures: WindowInsets
    @Composable
    get() = layoutMargins // the same as iosSafeArea.add(WindowInsets(left = 16.dp, right = 16.dp))

/**
 * Returns the tappable element insets.
 */
val WindowInsets.Companion.tappableElement: WindowInsets
    @Composable
    get() = iosSafeArea.only(WindowInsetsSides.Top)

/**
 * The insets for the curved areas in a waterfall display.
 * It is useless for iOS.
 */
val WindowInsets.Companion.waterfall: WindowInsets get() = ZeroInsets

/**
 * The insets that include areas where content may be covered by other drawn content.
 * This includes all [systemBars], [displayCutout], and [ime].
 */
val WindowInsets.Companion.safeDrawing
    @Composable
    get() = systemBars.union(ime).union(displayCutout)

/**
 * The insets that include areas where gestures may be confused with other input,
 * including [systemGestures], [mandatorySystemGestures], [waterfall], and [tappableElement].
 */
val WindowInsets.Companion.safeGestures: WindowInsets
    @Composable
    get() = tappableElement.union(mandatorySystemGestures).union(systemGestures).union(waterfall)

/**
 * The insets that include all areas that may be drawn over or have gesture confusion,
 * including everything in [safeDrawing] and [safeGestures].
 */
val WindowInsets.Companion.safeContent: WindowInsets
    @Composable
    get() = safeDrawing.union(safeGestures)

