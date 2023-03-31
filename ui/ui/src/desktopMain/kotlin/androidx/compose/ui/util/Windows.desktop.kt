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
import java.awt.Component
import java.awt.Dialog
import java.awt.Dimension
import java.awt.Frame
import java.awt.Point
import java.awt.Toolkit
import java.awt.Window
import java.awt.event.ComponentListener
import java.awt.event.WindowListener
import java.awt.event.WindowStateListener
import kotlin.math.roundToInt


/**
 * Sets the size of the window, given its placement.
 * If the window is already visible, then change the size only if it's floating, in order to
 * avoid resetting the maximized / fullscreen state.
 * If the window is not visible yet, we _do_ set its size so that:
 * - It will have an "un-maximized" size to go to when the user un-maximizes the window.
 * - To allow drawing the first frame (at the correct size) before the window is made visible.
 */
internal fun Window.setSizeSafely(size: DpSize, placement: WindowPlacement) {
    if (!isVisible || (placement == WindowPlacement.Floating)) {
        setSizeImpl(size)
    }
}

/**
 * Sets the position of the window, given its placement.
 * If the window is already visible, then change the position only if it's floating, in order to
 * avoid resetting the maximized / fullscreen state.
 * If the window is not visible yet, we _do_ set its size so that it will have an "un-maximized"
 * position to go to when the user un-maximizes the window.
 */
internal fun Window.setPositionSafely(
    position: WindowPosition,
    placement: WindowPlacement,
    platformDefaultPosition: () -> Point
) {
    if (!isVisible || (placement == WindowPlacement.Floating)) {
        setPositionImpl(position, platformDefaultPosition)
    }
}

private fun Window.setSizeImpl(size: DpSize) {
    val availableSize by lazy {
        val screenBounds = graphicsConfiguration.bounds
        val screenInsets = Toolkit.getDefaultToolkit().getScreenInsets(graphicsConfiguration)

        IntSize(
            width = screenBounds.width - screenInsets.left - screenInsets.right,
            height = screenBounds.height - screenInsets.top - screenInsets.bottom
        )
    }

    val isWidthSpecified = size.isSpecified && size.width.isSpecified
    val isHeightSpecified = size.isSpecified && size.height.isSpecified

    val width = if (isWidthSpecified) {
        size.width.value.roundToInt().coerceAtLeast(0)
    } else {
        availableSize.width
    }

    val height = if (isHeightSpecified) {
        size.height.value.roundToInt().coerceAtLeast(0)
    } else {
        availableSize.height
    }

    var computedPreferredSize: Dimension? = null
    if (!isWidthSpecified || !isHeightSpecified) {
        preferredSize = Dimension(width, height)
        pack()  // Makes it displayable

        // We set preferred size to null, and then call getPreferredSize, which will compute the
        // actual preferred size determined by the content (see the description of setPreferredSize)
        preferredSize = null
        computedPreferredSize = preferredSize
    }

    if (!isDisplayable) {
        // Pack to allow drawing the first frame
        preferredSize = Dimension(width, height)
        pack()
    }

    setSize(
        if (isWidthSpecified) width else computedPreferredSize!!.width,
        if (isHeightSpecified) height else computedPreferredSize!!.height,
    )
    revalidate()  // Calls doLayout on the ComposeLayer, causing it to update its size
}

internal fun Window.setPositionImpl(
    position: WindowPosition,
    platformDefaultPosition: () -> Point
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

// In fact, this size doesn't affect anything on Windows/Linux, and isn't used by macOS (macOS
// doesn't have separate Window icons). We specify it to support Painter's with
// Unspecified intrinsicSize
private val iconSize = Size(32f, 32f)

internal fun Window.setIcon(painter: Painter?) {
    setIconImage(painter?.toAwtImage(density, layoutDirection, iconSize))
}

internal class ListenerOnWindowRef<T>(
    private val register: Window.(T) -> Unit,
    private val unregister: Window.(T) -> Unit
) {
    private var value: T? = null

    fun registerWithAndSet(window: Window, listener: T) {
        window.register(listener)
        value = listener
    }

    fun unregisterFromAndClear(window: Window) {
        value?.let {
            window.unregister(it)
            value = null
        }
    }
}

internal fun windowStateListenerRef() = ListenerOnWindowRef<WindowStateListener>(
    register = Window::addWindowStateListener,
    unregister = Window::removeWindowStateListener
)

internal fun windowListenerRef() = ListenerOnWindowRef<WindowListener>(
    register = Window::addWindowListener,
    unregister = Window::removeWindowListener
)

internal fun componentListenerRef() = ListenerOnWindowRef<ComponentListener>(
    register = Component::addComponentListener,
    unregister = Component::removeComponentListener
)


