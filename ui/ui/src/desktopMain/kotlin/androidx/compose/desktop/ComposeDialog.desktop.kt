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
package androidx.compose.desktop

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionContext
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.input.key.KeyEvent
import org.jetbrains.skiko.ClipComponent
import org.jetbrains.skiko.GraphicsApi
import org.jetbrains.skiko.SkiaLayer
import java.awt.Component
import java.awt.Window
import javax.swing.JDialog
import javax.swing.JLayeredPane

/**
 * ComposeDialog is a dialog for building UI using Compose for Desktop.
 * ComposeDialog inherits javax.swing.JDialog.
 */
class ComposeDialog(
    owner: Window? = null,
    modalityType: ModalityType = ModalityType.MODELESS
) : JDialog(owner, modalityType) {
    private var isDisposed = false
    internal val layer = ComposeLayer()
    private val pane = object : JLayeredPane() {
        override fun setBounds(x: Int, y: Int, width: Int, height: Int) {
            layer.wrapped.setSize(width, height)
            super.setBounds(x, y, width, height)
        }

        override fun add(component: Component): Component {
            val clipComponent = ClipComponent(component)
            clipMap.put(component, clipComponent)
            layer.wrapped.clipComponents.add(clipComponent)
            return add(component, Integer.valueOf(0))
        }

        override fun remove(component: Component) {
            layer.wrapped.clipComponents.remove(clipMap.get(component)!!)
            clipMap.remove(component)
            super.remove(component)
        }

        override fun getPreferredSize() = layer.wrapped.preferredSize
    }

    private val clipMap = mutableMapOf<Component, ClipComponent>()

    init {
        pane.layout = null
        pane.add(layer.component, Integer.valueOf(1))
        contentPane.add(pane)
    }

    override fun add(component: Component): Component {
        return pane.add(component)
    }

    override fun remove(component: Component) {
        pane.remove(component)
    }

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
     * @param onKeyEvent This callback is invoked when the user interacts with the hardware
     * keyboard. While implementing this callback, return true to stop propagation of this event.
     * If you return false, the key event will be sent to this [onKeyEvent]'s parent.
     * @param onPreviewKeyEvent This callback is invoked when the user interacts with the hardware
     * keyboard. It gives ancestors of a focused component the chance to intercept a [KeyEvent].
     * Return true to stop propagation of this event. If you return false, the key event will be
     * sent to this [onPreviewKeyEvent]'s child. If none of the children consume the event,
     * it will be sent back up to the root using the onKeyEvent callback.
     * @param content Composable content of the ComposeWindow.
     */
    fun setContent(
        parentComposition: CompositionContext? = null,
        onKeyEvent: ((KeyEvent) -> Boolean) = { false },
        onPreviewKeyEvent: ((KeyEvent) -> Boolean) = { false },
        content: @Composable () -> Unit
    ) {
        layer.setContent(
            parentComposition = parentComposition,
            onPreviewKeyEvent = onPreviewKeyEvent,
            onKeyEvent = onKeyEvent
        ) {
            CompositionLocalProvider(
                LocalLayerContainer provides pane
            ) {
                content()
            }
        }
    }

    override fun dispose() {
        if (!isDisposed) {
            layer.dispose()
            isDisposed = true
        }
        super.dispose()
    }

    override fun setVisible(value: Boolean) {
        if (value != isVisible) {
            super.setVisible(value)
            layer.component.requestFocus()
        }
    }

    /**
     * Registers a task to run when the rendering API changes.
     */
    fun onRenderApiChanged(action: () -> Unit) {
        layer.component.onStateChanged(SkiaLayer.PropertyKind.Renderer) {
            action()
        }
    }

    /**
     * Retrieve underlying platform-specific operating system handle for the root window where
     * ComposeDialog is rendered. Currently returns HWND on Windows, Display on X11 and NSWindow
     * on macOS.
     */
    val windowHandle: Long
        get() = layer.component.windowHandle

    /**
     * Returns low-level rendering API used for rendering in this ComposeDialog. API is
     * automatically selected based on operating system, graphical hardware and `SKIKO_RENDER_API`
     * environment variable.
     */
    val renderApi: GraphicsApi
        get() = layer.component.renderApi
}
