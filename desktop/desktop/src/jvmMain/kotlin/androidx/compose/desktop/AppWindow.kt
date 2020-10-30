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
import androidx.compose.ui.window.MenuBar
import java.awt.Dimension
import java.awt.Frame
import java.awt.image.BufferedImage
import java.awt.Toolkit
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import java.awt.event.ComponentAdapter
import java.awt.event.ComponentEvent
import javax.swing.JMenuBar
import javax.swing.WindowConstants

val AppWindowAmbient = ambientOf<AppWindow?>()

fun Window(
    title: String = "JetpackDesktopWindow",
    size: IntSize = IntSize(800, 600),
    location: IntOffset = IntOffset.Zero,
    centered: Boolean = true,
    icon: BufferedImage? = null,
    menuBar: MenuBar? = null,
    undecorated: Boolean = false,
    events: WindowEvents = WindowEvents(),
    onDismissEvent: (() -> Unit)? = null,
    content: @Composable () -> Unit = emptyContent()
) {
    AppWindow(
        title = title,
        size = size,
        location = location,
        centered = centered,
        icon = icon,
        menuBar = menuBar,
        undecorated = undecorated,
        events = events,
        onDismissEvent = onDismissEvent
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
                override fun windowClosing(event: WindowEvent) {
                    if (defaultCloseOperation != WindowConstants.DO_NOTHING_ON_CLOSE) {
                        onDismissEvents.forEach { it.invoke() }
                        events.invokeOnClose()
                        AppManager.removeWindow(parent)
                        isClosed = true
                    }
                }
                override fun windowIconified(event: WindowEvent) {
                    events.invokeOnMinimize()
                }
                override fun windowDeiconified(event: WindowEvent) {
                    events.invokeOnRestore()
                }
            })
            addWindowFocusListener(object : WindowAdapter() {
                override fun windowGainedFocus(event: WindowEvent) {
                    window.setJMenuBar(parent.menuBar?.menuBar)
                    events.invokeOnFocusGet()
                }
                override fun windowLostFocus(event: WindowEvent) {
                    events.invokeOnFocusLost()
                }
            })
            addWindowStateListener(object : WindowAdapter() {
                override fun windowStateChanged(event: WindowEvent) {
                    val state = getState()
                    if (state != Frame.NORMAL && state != Frame.ICONIFIED) {
                        events.invokeOnMaximize()
                    }
                }
            })
            addComponentListener(object : ComponentAdapter() {
                override fun componentResized(e: ComponentEvent) {
                    val size = IntSize(width, height)
                    events.invokeOnResize(size)
                }
                override fun componentMoved(e: ComponentEvent) {
                    val location = IntOffset(x, y)
                    events.invokeOnRelocate(location)
                }
            })
        }
    }

    internal constructor(
        attached: AppFrame? = null,
        title: String = "JetpackDesktopWindow",
        size: IntSize = IntSize(800, 600),
        location: IntOffset = IntOffset.Zero,
        centered: Boolean = true,
        icon: BufferedImage? = null,
        menuBar: MenuBar? = null,
        undecorated: Boolean = false,
        events: WindowEvents = WindowEvents(),
        onDismissEvent: (() -> Unit)? = null
    ) : this(title, size, location, centered, icon, menuBar, undecorated, events, onDismissEvent) {
        this.invoker = attached
    }

    constructor(
        title: String = "JetpackDesktopWindow",
        size: IntSize = IntSize(800, 600),
        location: IntOffset = IntOffset.Zero,
        centered: Boolean = true,
        icon: BufferedImage? = null,
        menuBar: MenuBar? = null,
        undecorated: Boolean = false,
        events: WindowEvents = WindowEvents(),
        onDismissEvent: (() -> Unit)? = null
    ) {
        AppManager.addWindow(this)

        setTitle(title)
        setIcon(icon)
        setSize(size.width, size.height)
        if (centered) {
            setWindowCentered()
        } else {
            setLocation(location.x, location.y)
        }

        this.menuBar = menuBar

        if (this.menuBar == null && AppManager.sharedMenuBar != null) {
            this.menuBar = AppManager.sharedMenuBar!!
        }

        this.events = events

        window.setUndecorated(undecorated)
        if (onDismissEvent != null) {
            onDismissEvents.add(onDismissEvent)
        }
    }

    internal var pair: AppFrame? = null
    internal override fun connectPair(window: AppFrame) {
        pair = window
    }
    internal override fun disconnectPair() {
        pair = null
    }

    override fun setTitle(title: String) {
        window.setTitle(title)
    }

    override fun setIcon(image: BufferedImage?) {
        this.icon = image
        if (icon != null) {
            try {
                val taskbar = java.awt.Taskbar.getTaskbar()
                taskbar.setIconImage(icon)
            } catch (e: UnsupportedOperationException) {
                println("The os does not support: 'Taskbar.setIconImage'")
            }
            window.setIconImage(icon)
        }
    }

    override fun setMenuBar(menuBar: MenuBar) {
        this.menuBar = menuBar
        window.setJMenuBar(menuBar.menuBar)
    }

    override fun removeMenuBar() {
        this.menuBar = null
        window.setJMenuBar(JMenuBar())
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
        window.setSize(w, h)
    }

    override fun setLocation(x: Int, y: Int) {
        window.setLocation(x, y)
    }

    override fun setWindowCentered() {
        val dim: Dimension = Toolkit.getDefaultToolkit().getScreenSize()
        val x = dim.width / 2 - width / 2
        val y = dim.height / 2 - height / 2
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

        window.setVisible(true)
        events.invokeOnOpen()
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
