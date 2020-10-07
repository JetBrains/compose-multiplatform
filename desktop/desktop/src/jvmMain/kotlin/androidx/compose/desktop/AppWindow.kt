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
import androidx.compose.runtime.Providers
import androidx.compose.runtime.ambientOf
import androidx.compose.runtime.emptyContent
import androidx.compose.ui.input.key.ExperimentalKeyInput
import androidx.compose.ui.platform.Keyboard
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import java.awt.Dimension
import java.awt.Toolkit
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import javax.swing.WindowConstants

val AppWindowAmbient = ambientOf<AppWindow?>()

fun Window(
    title: String = "JetpackDesktopDialog",
    size: IntSize = IntSize(1024, 768),
    position: IntOffset = IntOffset(0, 0),
    isCentered: Boolean = true,
    onDismissEvent: (() -> Unit)? = null,
    content: @Composable () -> Unit = emptyContent()
) {
    AppWindow(
        title = title,
        size = size,
        position = position,
        onDismissEvent = onDismissEvent,
        centered = isCentered
    ).show {
        content()
    }
}

class AppWindow : AppFrame {

    override val window: ComposeWindow

    init {
        window = ComposeWindow(parent = this)
        window.apply {
            defaultCloseOperation = WindowConstants.DISPOSE_ON_CLOSE
            addWindowListener(object : WindowAdapter() {
                override fun windowClosing(windowevent: WindowEvent) {
                    if (defaultCloseOperation != WindowConstants.DO_NOTHING_ON_CLOSE) {
                        onDismissEvents.forEach { it.invoke() }
                        AppManager.removeWindow(parent)
                        isClosed = true
                    }
                }
            })
        }
    }

    internal constructor(
        attached: AppFrame? = null,
        title: String = "JetpackDesktopWindow",
        size: IntSize = IntSize(1024, 768),
        position: IntOffset = IntOffset(0, 0),
        onDismissEvent: (() -> Unit)? = null,
        centered: Boolean = true
    ) : this(title, size, position, onDismissEvent, centered) {
        this.invoker = attached
    }

    constructor(
        title: String = "JetpackDesktopWindow",
        size: IntSize = IntSize(1024, 768),
        position: IntOffset = IntOffset(0, 0),
        onDismissEvent: (() -> Unit)? = null,
        centered: Boolean = true
    ) {
        this.title = title
        this.width = size.width
        this.height = size.height
        this.x = position.x
        this.y = position.y
        if (onDismissEvent != null) {
            onDismissEvents.add(onDismissEvent)
        }
        isCentered = centered

        AppManager.addWindow(this)
    }

    internal var pair: AppFrame? = null
    internal override fun connectPair(window: AppFrame) {
        pair = window
    }
    internal override fun disconnectPair() {
        pair = null
    }

    override fun setSize(width: Int, height: Int) {
        // better check min/max values of current window size
        var w = width
        if (w <= 0) {
            w = this.width
        }

        var h = height
        if (h <= 0) {
            h = this.height
        }
        this.width = w
        this.height = h
        window.setSize(w, h)
    }

    override fun setPosition(x: Int, y: Int) {
        this.x = x
        this.y = y
        window.setLocation(x, y)
    }

    override fun setWindowCentered() {
        val dim: Dimension = Toolkit.getDefaultToolkit().getScreenSize()
        this.x = dim.width / 2 - width / 2
        this.y = dim.height / 2 - height / 2
        window.setLocation(x, y)
    }

    private fun onCreate(content: @Composable () -> Unit) {
        window.setContent {
            Providers(
                AppWindowAmbient provides this,
                children = content
            )
        }
    }

    @OptIn(ExperimentalKeyInput::class)
    override fun show(content: @Composable () -> Unit) {
        if (invoker != null) {
            invoker!!.lockWindow()
            window.setAlwaysOnTop(true)
        }

        onCreate {
            window.owners?.keyboard = keyboard
            content()
        }

        if (isCentered) {
            setWindowCentered()
        }
        window.title = title
        window.setSize(width, height)
        window.setVisible(true)
    }

    override fun close() {
        window.dispatchEvent(WindowEvent(window, WindowEvent.WINDOW_CLOSING))
    }

    override fun dispose() {
        invoker?.unlockWindow()
    }

    override fun lockWindow() {
        window.apply {
            defaultCloseOperation = WindowConstants.DO_NOTHING_ON_CLOSE
            setFocusableWindowState(false)
            setResizable(false)
            setEnabled(false)
        }
        invoker?.connectPair(this)
    }

    override fun unlockWindow() {
        window.apply {
            defaultCloseOperation = WindowConstants.DISPOSE_ON_CLOSE
            setFocusableWindowState(true)
            setResizable(true)
            setEnabled(true)
            toFront()
            requestFocus()
        }
        disconnectPair()
    }

    @ExperimentalKeyInput
    val keyboard: Keyboard = Keyboard()
}
