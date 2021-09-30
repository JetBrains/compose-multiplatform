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

@file:Suppress("DEPRECATION")

package androidx.compose.foundation.gestures

import androidx.compose.foundation.DesktopPlatform
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.input.mouse.MouseScrollOrientation
import androidx.compose.ui.input.mouse.MouseScrollUnit
import androidx.compose.ui.input.mouse.mouseScrollFilter
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import kotlin.math.sqrt

// TODO(demin): figure out how we can provide a public API for changing MouseScrollConfig.
//  There are two ways:
//  - provide as CompositionLocal
//  - provide as a parameter Modifier.scrollable(mouseScrollConfig=)
//  This should be done only after new Pointer Input API with mouse support
internal val LocalMouseScrollConfig = compositionLocalOf { MouseScrollableConfig.CurrentPlatform }

// TODO(demin): implement smooth scroll animation on Windows
// TODO(demin): implement touchpad bounce physics on MacOS
// TODO(demin): maybe we need to differentiate different linux environments (Gnome/KDE)
// TODO(demin): do we need support real line scrolling (i.e. scroll by 3 text lines)?
@OptIn(ExperimentalComposeUiApi::class)
internal actual fun Modifier.mouseScrollable(
    orientation: Orientation,
    onScroll: (Float) -> Unit
): Modifier = composed {
    val density = LocalDensity.current
    val config = LocalMouseScrollConfig.current

    mouseScrollFilter { event, bounds ->
        if (isOrientationEqual(orientation, event.orientation)) {
            val scrollBounds = when (orientation) {
                Orientation.Vertical -> bounds.height
                Orientation.Horizontal -> bounds.width
            }
            val scrollOffset = config.offsetOf(event.delta, scrollBounds, density)
            onScroll(-scrollOffset)
            true
        } else {
            false
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
private fun isOrientationEqual(
    orientation: Orientation,
    mouseOrientation: MouseScrollOrientation
): Boolean {
    return if (mouseOrientation == MouseScrollOrientation.Horizontal) {
        orientation == Orientation.Horizontal
    } else {
        orientation == Orientation.Vertical
    }
}

@OptIn(ExperimentalComposeUiApi::class)
private fun MouseScrollableConfig.offsetOf(
    unit: MouseScrollUnit,
    bounds: Int,
    density: Density
) = when (unit) {
    is MouseScrollUnit.Line -> offsetOf(unit.value, bounds, density)

    // TODO(demin): Chrome/Firefox on Windows scroll differently: value * 0.90f * bounds
    //
    // TODO(demin): How can we integrate page scrolling into MouseScrollConfig and new
    //  Pointer Input API with mouse support?
    // the formula was determined experimentally based on Windows Start behaviour
    is MouseScrollUnit.Page -> unit.value * bounds.toFloat()
}

internal interface MouseScrollableConfig {
    fun offsetOf(scrollDelta: Float, bounds: Int, density: Density): Float

    // TODO(demin): Chrome on Windows/Linux uses different scroll strategy
    //  (always the same scroll offset, bounds-independent).
    //  Figure out why and decide if we can use this strategy instead of the current one.
    companion object {
        // TODO(demin): is this formula actually correct? some experimental values don't fit
        //  the formula
        val LinuxGnome = object : MouseScrollableConfig {
            // the formula was determined experimentally based on Ubuntu Nautilus behaviour
            override fun offsetOf(scrollDelta: Float, bounds: Int, density: Density): Float {
                return scrollDelta * sqrt(bounds.toFloat())
            }
        }

        val WindowsWinUI = object : MouseScrollableConfig {
            // the formula was determined experimentally based on Windows Start behaviour
            override fun offsetOf(scrollDelta: Float, bounds: Int, density: Density): Float {
                return scrollDelta * bounds / 20f
            }
        }

        val MacOSCocoa = object : MouseScrollableConfig {
            // the formula was determined experimentally based on MacOS Finder behaviour
            // MacOS driver will send events with accelerating delta
            override fun offsetOf(scrollDelta: Float, bounds: Int, density: Density): Float {
                return with(density) {
                    scrollDelta * 10.dp.toPx()
                }
            }
        }

        val CurrentPlatform: MouseScrollableConfig = when (DesktopPlatform.Current) {
            DesktopPlatform.Linux -> LinuxGnome
            DesktopPlatform.Windows -> WindowsWinUI
            DesktopPlatform.MacOS -> MacOSCocoa
            DesktopPlatform.Unknown -> WindowsWinUI
        }
    }
}