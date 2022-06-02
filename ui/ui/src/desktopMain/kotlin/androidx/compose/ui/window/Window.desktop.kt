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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.currentCompositionLocalContext
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCompositionContext
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.awt.ComposeWindow
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.ComponentUpdater
import androidx.compose.ui.util.makeDisplayable
import androidx.compose.ui.util.setIcon
import androidx.compose.ui.util.setPositionSafely
import androidx.compose.ui.util.setSizeSafely
import androidx.compose.ui.util.setUndecoratedSafely
import java.awt.Window
import java.awt.event.ComponentAdapter
import java.awt.event.ComponentEvent
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import javax.swing.JFrame
import javax.swing.JMenuBar

// TODO(demin): support focus management
/**
 * Composes platform window in the current composition. When Window enters the composition,
 * a new platform window will be created and receives the focus. When Window leaves the
 * composition, window will be disposed and closed.
 *
 * Initial size of the window is controlled by [WindowState.size].
 * Initial position of the window is controlled by [WindowState.position].
 *
 * Usage in single-window application ([ApplicationScope.exitApplication] will close all the
 * windows and stop all effects defined in [application]):
 * ```
 * fun main() = application {
 *     Window(onCloseRequest = ::exitApplication)
 * }
 * ```
 *
 * or if it only needed to close the main window without closing all other opened windows:
 * ```
 * fun main() = application {
 *     val isOpen by remember { mutableStateOf(true) }
 *     if (isOpen) {
 *         Window(onCloseRequest = { isOpen = false })
 *     }
 * }
 * ```
 *
 * @param onCloseRequest Callback that will be called when the user closes the window.
 * Usually in this callback we need to manually tell Compose what to do:
 * - change `isOpen` state of the window (which is manually defined)
 * - close the whole application (`onCloseRequest = ::exitApplication` in [ApplicationScope])
 * - don't close the window on close request (`onCloseRequest = {}`)
 * @param state The state object to be used to control or observe the window's state
 * When size/position/status is changed by the user, state will be updated.
 * When size/position/status of the window is changed by the application (changing state),
 * the native window will update its corresponding properties.
 * If application changes, for example [WindowState.placement], then after the next
 * recomposition, [WindowState.size] will be changed to correspond the real size of the window.
 * If [WindowState.position] is not [WindowPosition.isSpecified], then after the first show on the
 * screen [WindowState.position] will be set to the absolute values.
 * @param visible Is [Window] visible to user.
 * If `false`:
 * - internal state of [Window] is preserved and will be restored next time the window
 * will be visible;
 * - native resources will not be released. They will be released only when [Window]
 * will leave the composition.
 * @param title Title in the titlebar of the window
 * @param icon Icon in the titlebar of the window (for platforms which support this).
 * On macOs individual windows can't have a separate icon. To change the icon in the Dock,
 * set it via `iconFile` in build.gradle
 * (https://github.com/JetBrains/compose-jb/tree/master/tutorials/Native_distributions_and_local_execution#platform-specific-options)
 * @param undecorated Disables or enables decorations for this window.
 * @param transparent Disables or enables window transparency. Transparency should be set
 * only if window is undecorated, otherwise an exception will be thrown.
 * @param resizable Can window be resized by the user (application still can resize the window
 * changing [state])
 * @param enabled Can window react to input events
 * @param focusable Can window receive focus
 * @param alwaysOnTop Should window always be on top of another windows
 * @param onPreviewKeyEvent This callback is invoked when the user interacts with the hardware
 * keyboard. It gives ancestors of a focused component the chance to intercept a [KeyEvent].
 * Return true to stop propagation of this event. If you return false, the key event will be
 * sent to this [onPreviewKeyEvent]'s child. If none of the children consume the event,
 * it will be sent back up to the root using the onKeyEvent callback.
 * @param onKeyEvent This callback is invoked when the user interacts with the hardware
 * keyboard. While implementing this callback, return true to stop propagation of this event.
 * If you return false, the key event will be sent to this [onKeyEvent]'s parent.
 * @param content Content of the window
 */
@Composable
fun Window(
    onCloseRequest: () -> Unit,
    state: WindowState = rememberWindowState(),
    visible: Boolean = true,
    title: String = "Untitled",
    icon: Painter? = null,
    undecorated: Boolean = false,
    transparent: Boolean = false,
    resizable: Boolean = true,
    enabled: Boolean = true,
    focusable: Boolean = true,
    alwaysOnTop: Boolean = false,
    onPreviewKeyEvent: (KeyEvent) -> Boolean = { false },
    onKeyEvent: (KeyEvent) -> Boolean = { false },
    content: @Composable FrameWindowScope.() -> Unit
) {
    val currentState by rememberUpdatedState(state)
    val currentTitle by rememberUpdatedState(title)
    val currentIcon by rememberUpdatedState(icon)
    val currentUndecorated by rememberUpdatedState(undecorated)
    val currentTransparent by rememberUpdatedState(transparent)
    val currentResizable by rememberUpdatedState(resizable)
    val currentEnabled by rememberUpdatedState(enabled)
    val currentFocusable by rememberUpdatedState(focusable)
    val currentAlwaysOnTop by rememberUpdatedState(alwaysOnTop)
    val currentOnCloseRequest by rememberUpdatedState(onCloseRequest)

    val updater = remember(::ComponentUpdater)

    // the state applied to the window. exist to avoid races between WindowState changes and the state stored inside the native window
    val appliedState = remember {
        object {
            var size: DpSize? = null
            var position: WindowPosition? = null
            var placement: WindowPlacement? = null
            var isMinimized: Boolean? = null
        }
    }

    Window(
        visible = visible,
        onPreviewKeyEvent = onPreviewKeyEvent,
        onKeyEvent = onKeyEvent,
        create = {
            ComposeWindow().apply {
                // close state is controlled by WindowState.isOpen
                defaultCloseOperation = JFrame.DO_NOTHING_ON_CLOSE
                addWindowListener(object : WindowAdapter() {
                    override fun windowClosing(e: WindowEvent) {
                        currentOnCloseRequest()
                    }
                })
                addWindowStateListener {
                    currentState.placement = placement
                    currentState.isMinimized = isMinimized
                    appliedState.placement = currentState.placement
                    appliedState.isMinimized = currentState.isMinimized
                }
                addComponentListener(object : ComponentAdapter() {
                    override fun componentResized(e: ComponentEvent) {
                        // we check placement here and in windowStateChanged,
                        // because fullscreen changing doesn't
                        // fire windowStateChanged, only componentResized
                        currentState.placement = placement
                        currentState.size = DpSize(width.dp, height.dp)
                        appliedState.placement = currentState.placement
                        appliedState.size = currentState.size
                    }

                    override fun componentMoved(e: ComponentEvent) {
                        currentState.position = WindowPosition(x.dp, y.dp)
                        appliedState.position = currentState.position
                    }
                })
            }
        },
        dispose = ComposeWindow::dispose,
        update = { window ->
            updater.update {
                set(currentTitle, window::setTitle)
                set(currentIcon, window::setIcon)
                set(currentUndecorated, window::setUndecoratedSafely)
                set(currentTransparent, window::isTransparent::set)
                set(currentResizable, window::setResizable)
                set(currentEnabled, window::setEnabled)
                set(currentFocusable, window::setFocusableWindowState)
                set(currentAlwaysOnTop, window::setAlwaysOnTop)
            }
            if (state.size != appliedState.size) {
                window.setSizeSafely(state.size)
                appliedState.size = state.size
            }
            if (state.position != appliedState.position) {
                window.setPositionSafely(state.position)
                appliedState.position = state.position
            }
            if (state.placement != appliedState.placement) {
                window.placement = state.placement
                appliedState.placement = state.placement
            }
            if (state.isMinimized != appliedState.isMinimized) {
                window.isMinimized = state.isMinimized
                appliedState.isMinimized = state.isMinimized
            }
        },
        content = content
    )
}

/**
 * An entry point for the Compose application with single window.
 *
 * If you need to change attributes of the window in runtime, or need a custom closing logic, use
 * Composable `Window` in `application` entry point instead:
 * ```
 * application {
 *     Window(...) { }
 * }
 * ```
 *
 * Set [exitProcessOnExit] to `false`, if you need to execute some code after [singleWindowApplication] block, otherwise the code after it
 * won't be executed, as [singleWindowApplication] will exit the process.
 *
 * @param state The state object to be used to control or observe the window's state
 * When size/position/status is changed by the user, state will be updated.
 * When size/position/status of the window is changed by the application (changing state),
 * the native window will update its corresponding properties.
 * If application changes, for example [WindowState.placement], then after the next
 * recomposition, [WindowState.size] will be changed to correspond the real size of the window.
 * If [WindowState.position] is not [WindowPosition.isSpecified], then after the first show on the
 * screen [WindowState.position] will be set to the absolute values.
 * @param visible Is [Window] visible to user.
 * If `false`:
 * - internal state of [Window] is preserved and will be restored next time the window
 * will be visible;
 * - native resources will not be released. They will be released only when [Window]
 * will leave the composition.
 * @param title Title in the titlebar of the window
 * @param icon Icon in the titlebar of the window (for platforms which support this).
 * On macOs individual windows can't have a separate icon. To change the icon in the Dock,
 * set it via `iconFile` in build.gradle
 * (https://github.com/JetBrains/compose-jb/tree/master/tutorials/Native_distributions_and_local_execution#platform-specific-options)
 * @param undecorated Disables or enables decorations for this window.
 * @param transparent Disables or enables window transparency. Transparency should be set
 * only if window is undecorated, otherwise an exception will be thrown.
 * @param resizable Can window be resized by the user (application still can resize the window
 * changing [state])
 * @param enabled Can window react to input events
 * @param focusable Can window receive focus
 * @param alwaysOnTop Should window always be on top of another windows
 * @param onPreviewKeyEvent This callback is invoked when the user interacts with the hardware
 * keyboard. It gives ancestors of a focused component the chance to intercept a [KeyEvent].
 * Return true to stop propagation of this event. If you return false, the key event will be
 * sent to this [onPreviewKeyEvent]'s child. If none of the children consume the event,
 * it will be sent back up to the root using the onKeyEvent callback.
 * @param onKeyEvent This callback is invoked when the user interacts with the hardware
 * keyboard. While implementing this callback, return true to stop propagation of this event.
 * If you return false, the key event will be sent to this [onKeyEvent]'s parent.
 * @param exitProcessOnExit should `exitProcess(0)` be called after the window is closed.
 * exitProcess speedup process exit (instant instead of 1-4sec).
 * If `false`, the execution of the function will be unblocked after application is exited
 * (when the last window is closed, and all [LaunchedEffect] are complete).
 * @param content Content of the window
 */
fun singleWindowApplication(
    state: WindowState = WindowState(),
    visible: Boolean = true,
    title: String = "Untitled",
    icon: Painter? = null,
    undecorated: Boolean = false,
    transparent: Boolean = false,
    resizable: Boolean = true,
    enabled: Boolean = true,
    focusable: Boolean = true,
    alwaysOnTop: Boolean = false,
    onPreviewKeyEvent: (KeyEvent) -> Boolean = { false },
    onKeyEvent: (KeyEvent) -> Boolean = { false },
    exitProcessOnExit: Boolean = true,
    content: @Composable FrameWindowScope.() -> Unit
) = application(exitProcessOnExit = exitProcessOnExit) {
    Window(
        ::exitApplication,
        state,
        visible,
        title,
        icon,
        undecorated,
        transparent,
        resizable,
        enabled,
        focusable,
        alwaysOnTop,
        onPreviewKeyEvent,
        onKeyEvent,
        content
    )
}

/**
 * Compose [ComposeWindow] obtained from [create]. The [create] block will be called
 * exactly once to obtain the [ComposeWindow] to be composed, and it is also guaranteed to
 * be invoked on the UI thread (Event Dispatch Thread).
 *
 * Once Window leaves the composition, [dispose] will be called to free resources that
 * obtained by the [ComposeWindow].
 *
 * The [update] block can be run multiple times (on the UI thread as well) due to recomposition,
 * and it is the right place to set [ComposeWindow] properties depending on state.
 * When state changes, the block will be reexecuted to set the new properties.
 * Note the block will also be ran once right after the [create] block completes.
 *
 * Window is needed for creating window's that still can't be created with
 * the default Compose function [androidx.compose.ui.window.Window]
 *
 * @param visible Is [ComposeWindow] visible to user.
 * If `false`:
 * - internal state of [ComposeWindow] is preserved and will be restored next time the window
 * will be visible;
 * - native resources will not be released. They will be released only when [Window]
 * will leave the composition.
 * @param onPreviewKeyEvent This callback is invoked when the user interacts with the hardware
 * keyboard. It gives ancestors of a focused component the chance to intercept a [KeyEvent].
 * Return true to stop propagation of this event. If you return false, the key event will be
 * sent to this [onPreviewKeyEvent]'s child. If none of the children consume the event,
 * it will be sent back up to the root using the onKeyEvent callback.
 * @param onKeyEvent This callback is invoked when the user interacts with the hardware
 * keyboard. While implementing this callback, return true to stop propagation of this event.
 * If you return false, the key event will be sent to this [onKeyEvent]'s parent.
 * @param create The block creating the [ComposeWindow] to be composed.
 * @param dispose The block to dispose [ComposeWindow] and free native resources.
 * Usually it is simple `ComposeWindow::dispose`
 * @param update The callback to be invoked after the layout is inflated.
 * @param content Composable content of the creating window.
 */
@OptIn(ExperimentalComposeUiApi::class)
@Suppress("unused")
@Composable
fun Window(
    visible: Boolean = true,
    onPreviewKeyEvent: (KeyEvent) -> Boolean = { false },
    onKeyEvent: (KeyEvent) -> Boolean = { false },
    create: () -> ComposeWindow,
    dispose: (ComposeWindow) -> Unit,
    update: (ComposeWindow) -> Unit = {},
    content: @Composable FrameWindowScope.() -> Unit
) {
    val compositionLocalContext by rememberUpdatedState(currentCompositionLocalContext)
    val windowExceptionHandlerFactory by rememberUpdatedState(
        LocalWindowExceptionHandlerFactory.current
    )
    AwtWindow(
        visible = visible,
        create = {
            create().apply {
                this.compositionLocalContext = compositionLocalContext
                this.exceptionHandler = windowExceptionHandlerFactory.exceptionHandler(this)
                setContent(onPreviewKeyEvent, onKeyEvent, content)
            }
        },
        dispose = dispose,
        update = {
            it.compositionLocalContext = compositionLocalContext
            it.exceptionHandler = windowExceptionHandlerFactory.exceptionHandler(it)

            update(it)

            if (!it.isDisplayable) {
                it.makeDisplayable()
                it.contentPane.paint(it.graphics)
            }
        }
    )
}

/**
 * Receiver scope which is used by [androidx.compose.ui.window.Window].
 */
@Stable
interface FrameWindowScope : WindowScope {
    /**
     * [ComposeWindow] that was created inside [androidx.compose.ui.window.Window].
     */
    override val window: ComposeWindow
}

/**
 * Composes menu bar on the top of the window
 *
 * @param content content of the menu bar (list of menus)
 */
@Composable
fun FrameWindowScope.MenuBar(content: @Composable MenuBarScope.() -> Unit) {
    val parentComposition = rememberCompositionContext()

    DisposableEffect(Unit) {
        val menu = JMenuBar()
        val composition = menu.setContent(parentComposition, content)
        window.jMenuBar = menu
        composition to menu

        onDispose {
            window.jMenuBar = null
            composition.dispose()
        }
    }
}