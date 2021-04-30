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
import org.jetbrains.skiko.ClipComponent
import org.jetbrains.skiko.GraphicsApi
import java.awt.Component
import javax.swing.JFrame
import javax.swing.JLayeredPane

/**
 * ComposeWindow is a window for building UI using Compose for Desktop.
 * ComposeWindow inherits javax.swing.JFrame.
 */
class ComposeWindow : JFrame() {
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
    }

    private val clipMap = mutableMapOf<Component, ClipComponent>()

    init {
        pane.setLayout(null)
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
     * Sets Compose content of the ComposeWindow.
     *
     * @param parentComposition The parent composition reference to coordinate
     *        scheduling of composition updates.
     *        If null then default root composition will be used.
     * @param content Composable content of the ComposeWindow.
     */
    fun setContent(
        parentComposition: CompositionContext? = null,
        content: @Composable () -> Unit
    ) {
        layer.setContent(
            parentComposition = parentComposition,
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
     * Retrieve underlying platform-specific operating system handle for the window where ComposeWindow is rendered.
     * Currently returns HWND on Windows, Drawable on X11 and 0 on macOS.
     */
    val windowHandle: Long
        get() = layer.component.windowHandle

    /**
     * Returns low level rendering API used for rendering in this ComposeWindow. API is automatically selected based on
     * operating system, graphical hardware and `SKIKO_RENDER_API` environment variable.
     */
    val renderApi: GraphicsApi
        get() = layer.component.renderApi
}
