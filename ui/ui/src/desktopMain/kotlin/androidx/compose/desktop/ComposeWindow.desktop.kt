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
import org.jetbrains.skiko.ClipComponent
import java.awt.Component
import javax.swing.JFrame
import javax.swing.JLayeredPane

/**
 * ComposeWindow is a window for building UI using Compose for Desktop.
 * ComposeWindow inherits javax.swing.JFrame.
 * @param parent The parent AppFrame that wraps the ComposeWindow.
 */
class ComposeWindow(val parent: AppFrame) : JFrame() {
    internal val layer = ComposeLayer()
    private val pane = object : JLayeredPane() {
        override fun setBounds(x: Int, y: Int, width: Int, height: Int) {
            layer.wrapped.setSize(width, height)
            super.setBounds(x, y, width, height)
        }
    }

    private val clipMap = mutableMapOf<Component, ClipComponent>()

    init {
        pane.setLayout(null)
        pane.add(layer.component, Integer.valueOf(1))
        contentPane.add(pane)
    }

    override fun add(component: Component): Component {
        val clipComponent = ClipComponent(component)
        clipMap.put(component, clipComponent)
        layer.wrapped.clipComponents.add(clipComponent)
        return pane.add(component, Integer.valueOf(0))
    }

    override fun remove(component: Component) {
        layer.wrapped.clipComponents.remove(clipMap.get(component)!!)
        clipMap.remove(component)
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
            content = content
        )
    }

    override fun dispose() {
        layer.dispose()
        super.dispose()
    }

    override fun setVisible(value: Boolean) {
        if (value != isVisible) {
            super.setVisible(value)
            layer.component.requestFocus()
        }
    }
}
