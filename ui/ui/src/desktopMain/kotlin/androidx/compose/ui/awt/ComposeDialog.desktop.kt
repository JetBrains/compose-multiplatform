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
import androidx.compose.ui.window.DialogWindowScope
import androidx.compose.ui.window.UndecoratedWindowResizer
import org.jetbrains.skiko.GraphicsApi
import org.jetbrains.skiko.hostOs
import org.jetbrains.skiko.OS
import java.awt.Color
import java.awt.Component
import java.awt.Window
import java.awt.event.MouseListener
import java.awt.event.MouseMotionListener
import java.awt.event.MouseWheelListener
import javax.swing.JDialog

/**
 * ComposeDialog is a dialog for building UI using Compose for Desktop.
 * ComposeDialog inherits javax.swing.JDialog.
 */
class ComposeDialog(
    owner: Window? = null,
    modalityType: ModalityType = ModalityType.MODELESS
) : JDialog(owner, modalityType) {
    private val delegate = ComposeWindowDelegate(this)
    internal val layer get() = delegate.layer

    init {
        contentPane.add(delegate.pane)
    }

    override fun add(component: Component) = delegate.add(component)

    override fun remove(component: Component) = delegate.remove(component)

    /**
     * Composes the given composable into the ComposeDialog.
     *
     * @param content Composable content of the ComposeDialog.
     */
    @OptIn(ExperimentalComposeUiApi::class)
    fun setContent(
        content: @Composable DialogWindowScope.() -> Unit
    ) = setContent(
        parentComposition = null,
        onPreviewKeyEvent = { false },
        onKeyEvent = { false },
        content = content
    )

    /**
     * Composes the given composable into the ComposeDialog.
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
        onPreviewKeyEvent: ((KeyEvent) -> Boolean) = { false },
        onKeyEvent: ((KeyEvent) -> Boolean) = { false },
        content: @Composable DialogWindowScope.() -> Unit
    ) {
        val scope = object : DialogWindowScope {
            override val window: ComposeDialog get() = this@ComposeDialog
        }
        delegate.setContent(
            parentComposition,
            onPreviewKeyEvent,
            onKeyEvent,
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

    /**
     * `true` if background of the window is transparent, `false` otherwise
     * Transparency should be set only if window is not showing and `isUndecorated` is set to
     * `true`, otherwise AWT will throw an exception.
     */
    var isTransparent: Boolean
        get() = layer.component.transparency
        set(value) {
            if (value != layer.component.transparency) {
                check(isUndecorated) { "Window should be undecorated!" }
                check(!isDisplayable) {
                    "Cannot change transparency if window is already displayable."
                }
                layer.component.transparency = value
                if (value) {
                    if (hostOs != OS.Windows) {
                        background = Color(0, 0, 0, 0)
                    }
                } else {
                    background = null
                }
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
     * ComposeDialog is rendered. Currently returns HWND on Windows, Window on X11 and NSWindow
     * on macOS.
     */
    val windowHandle: Long get() = delegate.windowHandle

    /**
     * Returns low-level rendering API used for rendering in this ComposeDialog. API is
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
