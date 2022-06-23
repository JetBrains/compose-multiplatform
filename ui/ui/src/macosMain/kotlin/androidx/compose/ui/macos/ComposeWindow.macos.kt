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

import platform.AppKit.*
import platform.Cocoa.*
import platform.Foundation.*
import platform.CoreGraphics.*
import kotlinx.cinterop.*

internal actual class ComposeWindow actual constructor() {
    val layer = ComposeLayer(
        layer = createSkiaLayer(),
        showSoftwareKeyboard = {},
        hideSoftwareKeyboard = {},
    )

    val title: String
        get() = "TODO: get a title from SkiaWindow"

    actual fun setTitle(title: String) {
        println("TODO: set title to SkiaWindow")
    }

    val windowStyle =
        NSWindowStyleMaskTitled or
        NSWindowStyleMaskMiniaturizable or
        NSWindowStyleMaskClosable or
        NSWindowStyleMaskResizable

    private val contentRect = NSMakeRect(0.0, 0.0, 640.0, 480.0)

    private val nsWindow = NSWindow(
        contentRect = contentRect,
        styleMask = windowStyle,
        backing =  NSBackingStoreBuffered,
        defer =  true
    )

    init {
        layer.layer.attachTo(nsWindow)
        nsWindow.orderFrontRegardless()
        contentRect.useContents {
            layer.setSize(size.width.toInt(), size.height.toInt())
        }
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
