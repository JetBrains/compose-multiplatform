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
import java.awt.GridLayout
import javax.swing.JPanel
import javax.swing.SwingUtilities.isEventDispatchThread

/**
 * ComposePanel is panel for building UI using Compose for Desktop.
 */
class ComposePanel : JPanel() {
    init {
        check(isEventDispatchThread()) {
            "ComposePanel should be created inside AWT Event Dispatch Thread" +
                " (use SwingUtilities.invokeLater).\n" +
                "Creating from another thread isn't supported."
        }
        layout = GridLayout(1, 1)
    }

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
                content = content!!
            )
        }
    }

    override fun addNotify() {
        super.addNotify()

        // After [super.addNotify] is called we can safely initialize the layer and composable
        // content.
        layer = ComposeLayer()
        add(layer!!.component)

        initContent()
    }

    override fun removeNotify() {
        if (layer != null) {
            layer!!.dispose()
            remove(layer!!.component)
        }

        super.removeNotify()
    }

    override fun requestFocus() {
        if (layer != null) {
            layer!!.component.requestFocus()
        }
    }
}
