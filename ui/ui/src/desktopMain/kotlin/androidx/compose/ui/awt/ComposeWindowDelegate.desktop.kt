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
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.window.LocalWindow
import org.jetbrains.skiko.ClipComponent
import org.jetbrains.skiko.GraphicsApi
import org.jetbrains.skiko.SkiaLayer
import java.awt.Component
import java.awt.Window
import java.awt.event.MouseListener
import java.awt.event.MouseMotionListener
import java.awt.event.MouseWheelListener
import javax.swing.JLayeredPane

internal class ComposeWindowDelegate(private val window: Window) {
    private var isDisposed = false

    val layer = ComposeLayer()
    val pane = object : JLayeredPane() {
        override fun setBounds(x: Int, y: Int, width: Int, height: Int) {
            layer.component.setSize(width, height)
            super.setBounds(x, y, width, height)
        }

        override fun add(component: Component): Component {
            val clipComponent = ClipComponent(component)
            clipMap[component] = clipComponent
            layer.component.clipComponents.add(clipComponent)
            return add(component, Integer.valueOf(0))
        }

        override fun remove(component: Component) {
            layer.component.clipComponents.remove(clipMap[component]!!)
            clipMap.remove(component)
            super.remove(component)
        }

        override fun addNotify() {
            super.addNotify()
            layer.component.requestFocus()
        }

        override fun getPreferredSize() = layer.component.preferredSize
    }

    private val clipMap = mutableMapOf<Component, ClipComponent>()

    init {
        pane.layout = null
        pane.add(layer.component, Integer.valueOf(1))
    }

    fun add(component: Component): Component {
        return pane.add(component)
    }

    fun remove(component: Component) {
        pane.remove(component)
    }

    fun setContent(
        onPreviewKeyEvent: (KeyEvent) -> Boolean = { false },
        onKeyEvent: (KeyEvent) -> Boolean = { false },
        content: @Composable () -> Unit
    ) {
        layer.setContent(
            onPreviewKeyEvent = onPreviewKeyEvent,
            onKeyEvent = onKeyEvent,
        ) {
            CompositionLocalProvider(
                LocalWindow provides window,
                LocalLayerContainer provides pane
            ) {
                content()
            }
        }
    }

    fun dispose() {
        if (!isDisposed) {
            layer.dispose()
            isDisposed = true
        }
    }

    fun onRenderApiChanged(action: () -> Unit) {
        layer.component.onStateChanged(SkiaLayer.PropertyKind.Renderer) {
            action()
        }
    }

    val windowHandle: Long
        get() = layer.component.windowHandle

    val renderApi: GraphicsApi
        get() = layer.component.renderApi

    fun addMouseListener(listener: MouseListener) {
        layer.component.addMouseListener(listener)
    }

    fun removeMouseListener(listener: MouseListener) {
        layer.component.removeMouseListener(listener)
    }

    fun addMouseMotionListener(listener: MouseMotionListener) {
        layer.component.addMouseMotionListener(listener)
    }

    fun removeMouseMotionListener(listener: MouseMotionListener) {
        layer.component.removeMouseMotionListener(listener)
    }

    fun addMouseWheelListener(listener: MouseWheelListener) {
        layer.component.addMouseWheelListener(listener)
    }

    fun removeMouseWheelListener(listener: MouseWheelListener) {
        layer.component.removeMouseWheelListener(listener)
    }
}