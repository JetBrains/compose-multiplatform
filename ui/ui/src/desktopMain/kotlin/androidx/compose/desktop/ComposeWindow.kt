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
import androidx.compose.runtime.CompositionReference
import java.awt.event.ComponentAdapter
import java.awt.event.ComponentEvent
import javax.swing.JFrame

class ComposeWindow : JFrame {
    val parent: AppFrame
    internal val layer = ComposeLayer()

    val density get() = layer.density

    constructor(parent: AppFrame) : super() {
        this.parent = parent
        contentPane.add(layer.wrapped)

        addComponentListener(object : ComponentAdapter() {
            override fun componentResized(e: ComponentEvent) {
                layer.reinit()
                needRedrawLayer()
            }
        })
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
        parentComposition: CompositionReference? = null,
        content: @Composable () -> Unit
    ) {
        layer.setContent(
            parent = parent,
            invalidate = this::needRedrawLayer,
            parentComposition = parentComposition,
            content = content
        )
    }

    private fun updateLayer() {
        if (!isVisible) {
            return
        }
        layer.updateLayer()
    }

    internal fun needRedrawLayer() {
        if (!isVisible) {
            return
        }
        layer.needRedrawLayer()
    }

    override fun dispose() {
        layer.dispose()
        super.dispose()
    }

    override fun setVisible(value: Boolean) {
        if (value != isVisible) {
            super.setVisible(value)
            layer.wrapped.requestFocus()
            updateLayer()
            needRedrawLayer()
        }
    }
}

// Simple FPS tracker for debug purposes
internal class FPSTracker {
    private var t0 = 0L
    private val times = DoubleArray(155)
    private var timesIdx = 0

    fun track() {
        val t1 = System.nanoTime()
        times[timesIdx] = (t1 - t0) / 1000000.0
        t0 = t1
        timesIdx = (timesIdx + 1) % times.size
        println("FPS: ${1000 / times.takeWhile { it > 0 }.average()}")
    }
}
