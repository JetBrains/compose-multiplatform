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
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.window.MenuBar
import java.awt.image.BufferedImage

abstract class AppFrame {

    abstract val window: ComposeWindow

    internal var menuBar: MenuBar? = null

    var invoker: AppFrame? = null
        protected set

    val title: String
        get() = window.title

    val width: Int
        get() = window.width

    val height: Int
        get() = window.height

    val x: Int
        get() = window.x

    val y: Int
        get() = window.y

    var isClosed: Boolean = false
        internal set

    var icon: BufferedImage? = null
        internal set

    var events: WindowEvents = WindowEvents()
        internal set

    val onDismissEvents = mutableListOf<() -> Unit>()

    abstract fun setTitle(title: String)

    abstract fun setIcon(image: BufferedImage?)

    abstract fun setMenuBar(menuBar: MenuBar)

    abstract fun removeMenuBar()

    abstract fun setLocation(x: Int, y: Int)

    abstract fun setWindowCentered()

    abstract fun setSize(width: Int, height: Int)

    abstract fun show(content: @Composable () -> Unit)

    abstract fun close()

    internal abstract fun dispose()

    internal abstract fun connectPair(window: AppFrame)

    internal abstract fun disconnectPair()

    internal abstract fun lockWindow()

    internal abstract fun unlockWindow()
}

class WindowEvents(
    var onOpen: (() -> Unit)? = null,
    var onClose: (() -> Unit)? = null,
    var onMinimize: (() -> Unit)? = null,
    var onMaximize: (() -> Unit)? = null,
    var onRestore: (() -> Unit)? = null,
    var onFocusGet: (() -> Unit)? = null,
    var onFocusLost: (() -> Unit)? = null,
    var onResize: ((IntSize) -> Unit)? = null,
    var onRelocate: ((IntOffset) -> Unit)? = null
) {

    internal fun invokeOnOpen() {
        onOpen?.invoke()
    }

    internal fun invokeOnClose() {
        onClose?.invoke()
    }

    internal fun invokeOnMinimize() {
        onMinimize?.invoke()
    }

    internal fun invokeOnMaximize() {
        onMaximize?.invoke()
    }

    internal fun invokeOnRestore() {
        onRestore?.invoke()
    }

    internal fun invokeOnFocusGet() {
        onFocusGet?.invoke()
    }

    internal fun invokeOnFocusLost() {
        onFocusLost?.invoke()
    }

    internal fun invokeOnResize(size: IntSize) {
        onResize?.invoke(size)
    }

    internal fun invokeOnRelocate(location: IntOffset) {
        onRelocate?.invoke(location)
    }
}
