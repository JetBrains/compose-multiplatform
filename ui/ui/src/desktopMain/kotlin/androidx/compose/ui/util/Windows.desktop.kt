/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.compose.ui.util

import androidx.compose.ui.Alignment
import androidx.compose.ui.awt.ComposeWindow
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.isSpecified
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.density
import androidx.compose.ui.window.layoutDirection
import java.awt.Dialog
import java.awt.Dimension
import java.awt.Frame
import java.awt.Point
import java.awt.Toolkit
import java.awt.Window
import kotlin.math.roundToInt

/**
 * Ignore size updating if window is maximized or in fullscreen.
 * Otherwise we will reset maximized / fullscreen state.
 */
internal fun ComposeWindow.setSizeSafely(size: DpSize) {
    if (placement == WindowPlacement.Floating) {
        (this as Window).setSizeSafely(size)
    }
}

/**
 * Ignore position updating if window is maximized or in fullscreen.
 * Otherwise we will reset maximized / fullscreen state.
 */
internal fun ComposeWindow.setPositionSafely(
    position: WindowPosition,
    platformDefaultPosition: () -> Point?
) {
    if (placement == WindowPlacement.Floating) {
        (this as Window).setPositionSafely(position, platformDefaultPosition)
    }
}

/**
 * Limit the width and the height to a minimum of 0
 */
internal fun Window.setSizeSafely(size: DpSize) {
    val screenBounds by lazy { graphicsConfiguration.bounds }

    val isWidthSpecified = size.isSpecified && size.width.isSpecified
    val isHeightSpecified = size.isSpecified && size.height.isSpecified

    val width = if (isWidthSpecified) {
        size.width.value.roundToInt().coerceAtLeast(0)
    } else {
        screenBounds.width
    }

    val height = if (isHeightSpecified) {
        size.height.value.roundToInt().coerceAtLeast(0)
    } else {
        screenBounds.height
    }

    if (!isWidthSpecified || !isHeightSpecified) {
        preferredSize = Dimension(width, height)
        pack()
        // if we set null, getPreferredSize will return the default inner size determined by
        // the inner components (see the description of setPreferredSize)
        preferredSize = null
    }

    setSize(
        if (isWidthSpecified) width else preferredSize.width,
        if (isHeightSpecified) height else preferredSize.height,
    )
}

internal fun Window.setPositionSafely(
    position: WindowPosition,
    platformDefaultPosition: () -> Point?
) = when (position) {
    WindowPosition.PlatformDefault -> location = platformDefaultPosition()
    is WindowPosition.Aligned -> align(position.alignment)
    is WindowPosition.Absolute -> setLocation(
        position.x.value.roundToInt(),
        position.y.value.roundToInt()
    )
}

internal fun Window.align(alignment: Alignment) {
    val screenInsets = Toolkit.getDefaultToolkit().getScreenInsets(graphicsConfiguration)
    val screenBounds = graphicsConfiguration.bounds
    val size = IntSize(size.width, size.height)
    val screenSize = IntSize(
        screenBounds.width - screenInsets.left - screenInsets.right,
        screenBounds.height - screenInsets.top - screenInsets.bottom
    )
    val location = alignment.align(size, screenSize, LayoutDirection.Ltr)

    setLocation(
        screenBounds.x + screenInsets.left + location.x,
        screenBounds.y + screenInsets.top + location.y
    )
}

/**
 * We cannot call [Frame.setUndecorated] if window is showing - AWT will throw an exception.
 * But we can call [Frame.setUndecoratedSafely] if isUndecorated isn't changed.
 */
internal fun Frame.setUndecoratedSafely(value: Boolean) {
    if (this.isUndecorated != value) {
        this.isUndecorated = value
    }
}

/**
 * We cannot change call [Dialog.setUndecorated] if window is showing - AWT will throw an exception.
 * But we can call [Dialog.setUndecoratedSafely] if isUndecorated isn't changed.
 */
internal fun Dialog.setUndecoratedSafely(value: Boolean) {
    if (this.isUndecorated != value) {
        this.isUndecorated = value
    }
}

// In fact, this size doesn't affect anything on Windows/Linux, and isn't used by macOs (macOs
// doesn't have separate Window icons). We specify it to support Painter's with
// Unspecified intrinsicSize
private val iconSize = Size(32f, 32f)

internal fun Window.setIcon(painter: Painter?) {
    setIconImage(painter?.toAwtImage(density, layoutDirection, iconSize))
}

internal fun Window.makeDisplayable() {
    val oldPreferredSize = preferredSize
    val oldLocation = location
    preferredSize = size
    try {
        pack()
    } finally {
        preferredSize = oldPreferredSize
        // pack() messes with location in case of multiple displays with different densities,
        // so we restore it to the value before pack()
        location = oldLocation
    }
}