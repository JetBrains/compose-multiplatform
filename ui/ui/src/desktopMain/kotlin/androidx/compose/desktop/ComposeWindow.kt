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
import javax.swing.JFrame

class ComposeWindow(val parent: AppFrame) : JFrame() {
    internal val layer = ComposeLayer()

    init {
        contentPane.add(layer.component)
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
