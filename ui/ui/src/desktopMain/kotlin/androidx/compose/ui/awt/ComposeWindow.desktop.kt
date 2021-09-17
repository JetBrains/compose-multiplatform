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
package androidx.compose.ui.awt

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionContext
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.window.FrameWindowScope
import androidx.compose.ui.window.UndecoratedWindowResizer
import androidx.compose.ui.window.WindowPlacement
import org.jetbrains.skiko.GraphicsApi
import java.awt.Component
import java.awt.event.MouseListener
import java.awt.event.MouseMotionListener
import java.awt.event.MouseWheelListener
import javax.swing.JFrame

/**
 * ComposeWindow is a window for building UI using Compose for Desktop.
 * ComposeWindow inherits javax.swing.JFrame.
 */
class ComposeWindow : JFrame() {
    private val delegate = ComposeWindowDelegate(this)
    internal val layer get() = delegate.layer

    init {
        contentPane.add(delegate.pane)
    }

    override fun add(component: Component) = delegate.add(component)

    override fun remove(component: Component) = delegate.remove(component)

    /**
     * Composes the given composable into the ComposeWindow.
     *
     * @param content Composable content of the ComposeWindow.
     */
    @OptIn(ExperimentalComposeUiApi::class)
    fun setContent(
        content: @Composable FrameWindowScope.() -> Unit
    ) = setContent(
        parentComposition = null,
        onPreviewKeyEvent = { false },
        onKeyEvent = { false },
        content = content
    )

    /**
     * Composes the given composable into the ComposeWindow.
     *
     * The new composition can be logically "linked" to an existing one, by providing a
     * [parentComposition]. This will ensure that invalidations and CompositionLocals will flow
     * through the two compositions as if they were not separate.
     *
     * @param parentComposition The parent composition reference to coordinate
     * scheduling of composition updates.
     * If null then default root composition will be used.
     * @param onPreviewKeyEvent This callback is invoked when the user interacts with the hardware
     * keyboard. It gives ancestors of a focused component the chance to intercept a [KeyEvent].
     * Return true to stop propagation of this event. If you return false, the key event will be
     * sent to this [onPreviewKeyEvent]'s child. If none of the children consume the event,
     * it will be sent back up to the root using the onKeyEvent callback.
     * @param onKeyEvent This callback is invoked when the user interacts with the hardware
     * keyboard. While implementing this callback, return true to stop propagation of this event.
     * If you return false, the key event will be sent to this [onKeyEvent]'s parent.
     * @param content Composable content of the ComposeWindow.
     */
    @ExperimentalComposeUiApi
    fun setContent(
        parentComposition: CompositionContext? = null,
        onPreviewKeyEvent: (KeyEvent) -> Boolean = { false },
        onKeyEvent: (KeyEvent) -> Boolean = { false },
        content: @Composable FrameWindowScope.() -> Unit
    ) {
        val scope = object : FrameWindowScope {
            override val window: ComposeWindow get() = this@ComposeWindow
        }
        delegate.setContent(
            parentComposition,
            onPreviewKeyEvent,
            onKeyEvent
        ) {
            scope.content()
        }
    }

    override fun dispose() {
        delegate.dispose()
        super.dispose()
    }

    private val undecoratedWindowResizer = UndecoratedWindowResizer(this, layer)

    override fun setUndecorated(value: Boolean) {
        super.setUndecorated(value)
        undecoratedWindowResizer.enabled = isUndecorated && isResizable
    }

    override fun setResizable(value: Boolean) {
        super.setResizable(value)
        undecoratedWindowResizer.enabled = isUndecorated && isResizable
    }

    var placement: WindowPlacement
        get() = when {
            isFullscreen -> WindowPlacement.Fullscreen
            isMaximized -> WindowPlacement.Maximized
            else -> WindowPlacement.Floating
        }
        set(value) {
            when (value) {
                WindowPlacement.Fullscreen -> {
                    isFullscreen = true
                }
                WindowPlacement.Maximized -> {
                    isMaximized = true
                }
                WindowPlacement.Floating -> {
                    isFullscreen = false
                    isMaximized = false
                }
            }
        }

    /**
     * `true` if the window is in fullscreen mode, `false` otherwise
     */
    private var isFullscreen: Boolean
        get() = layer.component.fullscreen
        set(value) {
            layer.component.fullscreen = value
        }

    /**
     * `true` if the window is maximized to fill all available screen space, `false` otherwise
     */
    private var isMaximized: Boolean
        get() = extendedState and MAXIMIZED_BOTH != 0
        set(value) {
            extendedState = if (value) {
                extendedState or MAXIMIZED_BOTH
            } else {
                extendedState and MAXIMIZED_BOTH.inv()
            }
        }

    /**
     * `true` if the window is minimized to the taskbar, `false` otherwise
     */
    var isMinimized: Boolean
        get() = extendedState and ICONIFIED != 0
        set(value) {
            extendedState = if (value) {
                extendedState or ICONIFIED
            } else {
                extendedState and ICONIFIED.inv()
            }
        }

    /**
     * Registers a task to run when the rendering API changes.
     */
    fun onRenderApiChanged(action: () -> Unit) {
        delegate.onRenderApiChanged(action)
    }

    /**
     * Retrieve underlying platform-specific operating system handle for the root window where
     * ComposeWindow is rendered. Currently returns HWND on Windows, Window on X11 and NSWindow
     * on macOS.
     */
    val windowHandle: Long get() = delegate.windowHandle

    /**
     * Returns low-level rendering API used for rendering in this ComposeWindow. API is
     * automatically selected based on operating system, graphical hardware and `SKIKO_RENDER_API`
     * environment variable.
     */
    val renderApi: GraphicsApi get() = delegate.renderApi

    // We need overridden listeners because we mix Swing and AWT components in the
    // org.jetbrains.skiko.SkiaLayer, they don't work well together.
    // TODO(demin): is it possible to fix that without overriding?

    override fun addMouseListener(listener: MouseListener) =
        delegate.addMouseListener(listener)

    override fun removeMouseListener(listener: MouseListener) =
        delegate.removeMouseListener(listener)

    override fun addMouseMotionListener(listener: MouseMotionListener) =
        delegate.addMouseMotionListener(listener)

    override fun removeMouseMotionListener(listener: MouseMotionListener) =
        delegate.removeMouseMotionListener(listener)

    override fun addMouseWheelListener(listener: MouseWheelListener) =
        delegate.addMouseWheelListener(listener)

    override fun removeMouseWheelListener(listener: MouseWheelListener) =
        delegate.removeMouseWheelListener(listener)
}
