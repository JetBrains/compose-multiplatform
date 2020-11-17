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

import androidx.compose.ui.unit.Density
import java.awt.Graphics
import java.awt.GridLayout
import java.awt.event.ComponentAdapter
import java.awt.event.ComponentEvent
import javax.swing.JFrame
import javax.swing.JPanel
import javax.swing.SwingUtilities.getRoot

/**
 * ComposePanel is panel for building UI using Compose for Desktop.
 */
class ComposePanel : JPanel {
    companion object {
        init {
            initCompose()
        }
    }

    private var init: Boolean = false

    internal val layer = ComposeLayer()

    val density get() = layer.density

    fun onDensityChanged(action: ((Density) -> Unit)?) {
        layer.onDensityChanged = action
    }

    internal var onDispose: (() -> Unit)? = null

    constructor() : super() {
        setLayout(GridLayout(1, 1))
        add(layer.wrapped)

        addComponentListener(object : ComponentAdapter() {
            override fun componentResized(e: ComponentEvent) {
                layer.reinit()
                needRedrawLayer()
            }
        })
    }

    internal fun needRedrawLayer() {
        if (isWindowReady()) {
            if (!init) {
                layer.updateLayer()
                init = true
            }
            layer.needRedrawLayer()
        }
    }

    override fun removeNotify() {
        remove(layer.wrapped)
        onDispose?.invoke()
        layer.dispose()
    }

    override fun requestFocus() {
        layer.wrapped.requestFocus()
    }

    private fun isWindowReady(): Boolean {
        val window = getRoot(this)
        return if (window is JFrame) window.isVisible else false
    }

    override fun paint(g: Graphics?) {
        needRedrawLayer()
    }
}
