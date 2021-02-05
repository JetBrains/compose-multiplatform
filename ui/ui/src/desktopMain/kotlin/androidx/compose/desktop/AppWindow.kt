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
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.platform.Keyboard
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.window.MenuBar
import java.awt.Container
import java.awt.Frame
import java.awt.event.ComponentAdapter
import java.awt.event.ComponentEvent
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import java.awt.image.BufferedImage
import javax.swing.JFrame
import javax.swing.JMenuBar
import javax.swing.SwingUtilities
import javax.swing.WindowConstants

/**
 * Local composition of [AppWindow]. [AppWindow] is a high level window implementation. This local
 * composition is used to get the current [AppWindow].
 */
val LocalAppWindow = compositionLocalOf<AppWindow>()
internal val LocalLayerContainer = compositionLocalOf<Container>()

/**
 * Opens a window with the given content.
 *
 * @param title The title of the window.
 * The title is displayed in the windows's native border.
 * @param size The initial size of the window.
 * @param location The initial position of the window in screen space. This parameter is
 * ignored if [center] is set to true.
 * @param centered Determines if the window is centered on startup. The default value for the
 * window is true.
 * @param icon The icon for the window displayed on the system taskbar.
 * @param menuBar Window menu bar. The menu bar can be displayed inside a window (Windows,
 * Linux) or at the top of the screen (Mac OS).
 * @param undecorated Removes the native window border if set to true. The default value is false.
 * @param resizable Makes the window resizable if is set to true and unresizable if is set to
 * false. The default value is true.
 * @param events Allows to describe events of the window.
 * Supported events: onOpen, onClose, onMinimize, onMaximize, onRestore, onFocusGet, onFocusLost,
 * onResize, onRelocate.
 * @param onDismissRequest Executes when the user tries to close the Window.
 */
fun Window(
    title: String = "JetpackDesktopWindow",
    size: IntSize = IntSize(800, 600),
    location: IntOffset = IntOffset.Zero,
    centered: Boolean = true,
    icon: BufferedImage? = null,
    menuBar: MenuBar? = null,
    undecorated: Boolean = false,
    resizable: Boolean = true,
    events: WindowEvents = WindowEvents(),
    onDismissRequest: (() -> Unit)? = null,
    content: @Composable () -> Unit = { }
) = SwingUtilities.invokeLater {
    AppWindow(
        title = title,
        size = size,
        location = location,
        centered = centered,
        icon = icon,
        menuBar = menuBar,
        undecorated = undecorated,
        resizable = resizable,
        events = events,
        onDismissRequest = onDismissRequest
    ).show {
        content()
    }
}

/**
 * AppWindow is a class that represents a window.
 */
class AppWindow : AppFrame {

    /**
     * Gets ComposeWindow object.
     */
    override val window: ComposeWindow

    init {
        require(SwingUtilities.isEventDispatchThread()) {
            "AppWindow should be created inside AWT Event Thread (use SwingUtilities.invokeLater " +
                "or just dsl for creating window: Window { })"
        }
        window = ComposeWindow(parent = this)
        window.apply {
            defaultCloseOperation = WindowConstants.DISPOSE_ON_CLOSE
            addWindowListener(object : WindowAdapter() {
                override fun windowClosing(event: WindowEvent) {
                    if (defaultCloseOperation != WindowConstants.DO_NOTHING_ON_CLOSE) {
                        onDispose?.invoke()
                        onDismiss?.invoke()
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
                    // Dialogs should not receive a common application menu bar
                    if (invoker == null) {
                        window.setJMenuBar(parent.menuBar?.menuBar)
                    }
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
        resizable: Boolean = true,
        events: WindowEvents = WindowEvents(),
        onDismissRequest: (() -> Unit)? = null
    ) : this(
        title = title,
        size = size,
        location = location,
        centered = centered,
        icon = icon,
        menuBar = menuBar,
        undecorated = undecorated,
        resizable = resizable,
        events = events,
        onDismissRequest = onDismissRequest
    ) {
        this.invoker = attached
    }

    /**
     * Creates an instance of AppWindow. AppWindow is a class that represents a window.
     *
     * @param title The title of the window.
     * The title is displayed in the windows's native border.
     * @param size The initial size of the window.
     * @param location The initial position of the window in screen space. This parameter is
     * ignored if [center] is set to true.
     * @param centered Determines if the window is centered on startup. The default value for the
     * window is true.
     * @param icon The icon for the window displayed on the system taskbar.
     * @param menuBar Window menu bar. The menu bar can be displayed inside a window (Windows,
     * Linux) or at the top of the screen (Mac OS).
     * @param undecorated Removes the native window border if set to true. The default value is false.
     * @param resizable Makes the window resizable if is set to true and unresizable if is set to
     * false. The default value is true.
     * @param events Allows to describe events of the window.
     * Supported events: onOpen, onClose, onMinimize, onMaximize, onRestore, onFocusGet, onFocusLost,
     * onResize, onRelocate.
     * @param onDismissRequest Executes when the user tries to close the AppWindow.
     */
    constructor(
        title: String = "JetpackDesktopWindow",
        size: IntSize = IntSize(800, 600),
        location: IntOffset = IntOffset.Zero,
        centered: Boolean = true,
        icon: BufferedImage? = null,
        menuBar: MenuBar? = null,
        undecorated: Boolean = false,
        resizable: Boolean = true,
        events: WindowEvents = WindowEvents(),
        onDismissRequest: (() -> Unit)? = null
    ) {
        AppManager.addWindow(this)

        setTitle(title)
        setIcon(icon)
        setSize(size.width, size.height)
        this.resizable = resizable
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
        onDismiss = onDismissRequest
    }

    internal var pair: AppFrame? = null
    internal override fun connectPair(window: AppFrame) {
        pair = window
    }
    internal override fun disconnectPair() {
        pair = null
    }

    /**
     * Sets the title of the window.
     *
     * @param title Window title text.
     */
    override fun setTitle(title: String) {
        window.setTitle(title)
    }

    /**
     * Sets the image icon of the window.
     *
     * @param image Image of the icon.
     */
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

    /**
     * Sets the menu bar of the window. The menu bar can be displayed inside a window (Windows,
     * Linux) or at the top of the screen (Mac OS).
     *
     * @param manuBar Window menu bar.
     */
    override fun setMenuBar(menuBar: MenuBar) {
        this.menuBar = menuBar
        window.setJMenuBar(menuBar.menuBar)
    }

    /**
     * Removes the menu bar of the window.
     */
    override fun removeMenuBar() {
        this.menuBar = null
        window.setJMenuBar(JMenuBar())
    }

    /**
     * Returns true if the window is in fullscreen mode, false otherwise.
     */
    override val isFullscreen: Boolean
        get() = window.layer.wrapped.fullscreen

    /**
     * Switches the window to fullscreen mode if the window is resizable. If the window is in
     * fullscreen mode [minimize] and [maximize] methods are ignored.
     */
    override fun makeFullscreen() {
        if (!isFullscreen && resizable) {
            window.layer.wrapped.fullscreen = true
        }
    }

    /**
     * Minimizes the window to the taskbar. If the window is in fullscreen mode this method
     * is ignored.
     */
    override fun minimize() {
        if (!isFullscreen) {
            window.setExtendedState(JFrame.ICONIFIED)
        }
    }

    /**
     * Maximizes the window to fill all available screen space. If the window is in fullscreen mode
     * this method is ignored.
     */
    override fun maximize() {
        if (!isFullscreen) {
            window.setExtendedState(JFrame.MAXIMIZED_BOTH)
        }
    }

    /**
     * Restores the previous state and size of the window after
     * maximizing/minimizing/fullscreen mode.
     */
    override fun restore() {
        if (isFullscreen) {
            window.layer.wrapped.fullscreen = false
        }
        window.setExtendedState(JFrame.NORMAL)
    }

    private var _resizable: Boolean = true

    /**
     * Sets the ability to resize the window. True - the window can be resized,
     * false - the window cannot be resized. If the window is in fullscreen mode
     * setter of this property is ignored. If this property is true the [makeFullscreen()]
     * method is ignored.
     */
    override var resizable: Boolean
        get() {
            return window.isResizable()
        }
        set(value) {
            if (!isFullscreen) {
                _resizable = value
                window.setResizable(value)
            }
        }

    /**
     * Sets the new size of the window.
     *
     * @param width the new width of the window.
     * @param height the new height of the window.
     */
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

    /**
     * Sets the new position of the window on the screen.
     *
     * @param x the new x-coordinate of the window.
     * @param y the new y-coordinate of the window.
     */
    override fun setLocation(x: Int, y: Int) {
        window.setLocation(x, y)
    }

    /**
     * Sets the window to the center of the current screen.
     */
    override fun setWindowCentered() {
        val screenBounds = window.graphicsConfiguration.getBounds()
        val x = (screenBounds.width - width) / 2 + screenBounds.x
        val y = (screenBounds.height - height) / 2 + screenBounds.y
        window.setLocation(x, y)
    }

    private fun onCreate(
        parentComposition: CompositionContext? = null,
        content: @Composable () -> Unit
    ) {
        window.setContent(parentComposition) {
            CompositionLocalProvider(
                LocalAppWindow provides this,
                LocalLayerContainer provides window,
                content = content
            )
        }
    }

    /**
     * Shows a window with the given Compose content.
     *
     * @param parentComposition The parent composition reference to coordinate
     *        scheduling of composition updates.
     *        If null then default root composition will be used.
     * @param content Composable content of the window.
     */
    fun show(
        parentComposition: CompositionContext? = null,
        content: @Composable () -> Unit
    ) {
        if (invoker != null) {
            invoker!!.lockWindow()
            window.setAlwaysOnTop(true)
        }

        onCreate(parentComposition) {
            window.layer.owners.keyboard = keyboard
            content()
        }

        window.setVisible(true)
        events.invokeOnOpen()
    }

    /**
     * Closes the window.
     */
    override fun close() {
        window.dispatchEvent(WindowEvent(window, WindowEvent.WINDOW_CLOSING))
    }

    internal override fun dispose() {
        invoker?.unlockWindow()
    }

    internal override fun lockWindow() {
        window.apply {
            defaultCloseOperation = WindowConstants.DO_NOTHING_ON_CLOSE
            setFocusableWindowState(false)
            setResizable(false)
            setEnabled(false)
        }
        invoker?.connectPair(this)
    }

    internal override fun unlockWindow() {
        window.apply {
            defaultCloseOperation = WindowConstants.DISPOSE_ON_CLOSE
            setFocusableWindowState(true)
            setEnabled(true)
            toFront()
            requestFocus()
        }
        resizable = _resizable
        disconnectPair()
    }

    /**
     * Gets the Keyboard object of the window.
     */
    val keyboard: Keyboard = Keyboard()
}
