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
import androidx.compose.runtime.CompositionLocalProvider
import org.jetbrains.skiko.ClipComponent
import java.awt.Color
import java.awt.Component
import javax.swing.JLayeredPane
import javax.swing.SwingUtilities.isEventDispatchThread

/**
 * ComposePanel is a panel for building UI using Compose for Desktop.
 */
class ComposePanel : JLayeredPane() {
    init {
        check(isEventDispatchThread()) {
            "ComposePanel should be created inside AWT Event Dispatch Thread" +
                " (use SwingUtilities.invokeLater).\n" +
                "Creating from another thread isn't supported."
        }
        setBackground(Color.white)
        setLayout(null)
    }

    internal var layer: ComposeLayer? = null
    private val clipMap = mutableMapOf<Component, ClipComponent>()
    private var content: (@Composable () -> Unit)? = null

    override fun setBounds(x: Int, y: Int, width: Int, height: Int) {
        layer?.wrapped?.setSize(width, height)
        super.setBounds(x, y, width, height)
    }

    /**
     * Sets Compose content of the ComposePanel.
     *
     * @param content Composable content of the ComposePanel.
     */
    fun setContent(content: @Composable () -> Unit) {
        // The window (or root container) may not be ready to render composable content, so we need
        // to keep the lambda describing composable content and set the content only when
        // everything is ready to avoid accidental crashes and memory leaks on all supported OS
        // types.
        this.content = content
        initContent()
    }

    private fun initContent() {
        if (layer != null && content != null) {
            layer!!.setContent {
                CompositionLocalProvider(
                    LocalLayerContainer provides this,
                    content = content!!
                )
            }
        }
    }

    override fun add(component: Component): Component {
        if (layer == null) {
            return component
        }
        val clipComponent = ClipComponent(component)
        clipMap.put(component, clipComponent)
        layer!!.wrapped.clipComponents.add(clipComponent)
        return super.add(component, Integer.valueOf(0))
    }

    override fun remove(component: Component) {
        layer!!.wrapped.clipComponents.remove(clipMap.get(component)!!)
        clipMap.remove(component)
        super.remove(component)
    }

    override fun addNotify() {
        super.addNotify()

        // After [super.addNotify] is called we can safely initialize the layer and composable
        // content.
        layer = ComposeLayer()
        super.add(layer!!.component, Integer.valueOf(1))
        layer?.wrapped?.setSize(width, height)
        initContent()
    }

    override fun removeNotify() {
        if (layer != null) {
            layer!!.dispose()
            super.remove(layer!!.component)
        }

        super.removeNotify()
    }

    override fun requestFocus() {
        if (layer != null) {
            layer!!.component.requestFocus()
        }
    }
}
