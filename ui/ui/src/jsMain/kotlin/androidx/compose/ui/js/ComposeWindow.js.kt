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

package androidx.compose.ui.window

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionContext
import androidx.compose.ui.createSkiaLayer
import androidx.compose.ui.native.ComposeLayer
import kotlinx.browser.document
import org.w3c.dom.HTMLCanvasElement

internal actual class ComposeWindow actual constructor(){
    val layer = ComposeLayer(
        layer = createSkiaLayer(),
        showSoftwareKeyboard = {
            println("TODO showSoftwareKeyboard in JS")
        },
        hideSoftwareKeyboard = {
            println("TODO hideSoftwareKeyboard in JS")
        },
    )

    val title: String
        get() = "TODO: get a title from SkiaWindow"

    actual fun setTitle(title: String) {
        println("TODO: set title to SkiaWindow")
    }

    // TODO: generalize me.
    val canvas = document.getElementById("ComposeTarget") as HTMLCanvasElement

    init {
        layer.layer.attachTo(canvas)
        canvas.setAttribute("tabindex", "0")
        layer.layer.needRedraw()

        val scale = layer.layer.contentScale
        layer.setSize(
            (canvas.width / scale).toInt(),
            (canvas.height / scale).toInt()
        )
    }

    /**
     * Sets Compose content of the ComposeWindow.
     *
     * @param content Composable content of the ComposeWindow.
     */
    actual fun setContent(
        content: @Composable () -> Unit
    ) {
        println("ComposeWindow.setContent")
        layer.setContent(
            content = content
        )
    }

    // TODO: need to call .dispose() on window close.
    actual fun dispose() {
        layer.dispose()
    }
}
