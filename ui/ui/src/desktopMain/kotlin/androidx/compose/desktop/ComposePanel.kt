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
import androidx.compose.ui.unit.Density
import java.awt.Graphics
import java.awt.GridLayout
import java.awt.event.ComponentAdapter
import java.awt.event.ComponentEvent
import javax.swing.JPanel

/**
 * ComposePanel is panel for building UI using Compose for Desktop.
 */
class ComposePanel : JPanel {
    constructor() : super() {
        setLayout(GridLayout(1, 1))
    }

    private var init: Boolean = false

    private var layer: ComposeLayer? = null
    private var content: (@Composable () -> Unit)? = null

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
            layer!!.setContent(
                parent = this,
                invalidate = this::needRedrawLayer,
                content = content!!
            )
        }
    }

    val density: Density
        get() = if (layer == null) {
            Density(graphicsConfiguration.defaultTransform.scaleX.toFloat(), 1f)
        } else {
            layer!!.density
        }

    internal var onDispose: (() -> Unit)? = null

    private fun needRedrawLayer() {
        if (isShowing) {
            if (!init) {
                layer!!.updateLayer()
                init = true
            }
            layer!!.needRedrawLayer()
        }
    }

    override fun addNotify() {
        super.addNotify()

        // After [super.addNotify] is called we can safely initialize the layer and composable
        // content.
        layer = ComposeLayer()
        add(layer!!.wrapped)
        addComponentListener(object : ComponentAdapter() {
            override fun componentResized(e: ComponentEvent) {
                layer?.reinit()
                needRedrawLayer()
            }
        })

        initContent()
    }

    override fun removeNotify() {
        super.removeNotify()

        onDispose?.invoke()
        if (layer != null) {
            remove(layer!!.wrapped)
            layer!!.dispose()
        }
        init = false
    }

    override fun requestFocus() {
        if (layer != null) {
            layer!!.wrapped.requestFocus()
        }
    }

    override fun paint(g: Graphics?) {
        needRedrawLayer()
    }
}
