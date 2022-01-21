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
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCompositionContext
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.platform.DesktopPlatform
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import java.awt.PopupMenu
import java.awt.SystemTray
import java.awt.TrayIcon

// In fact, this size doesn't affect anything on Windows/Linux, because they request what they
// need, and not what we provide. It only affects macOs. This size will be scaled in asAwtImage to
// support DPI=2.0
// Unfortunately I hadn't enough time to find sources from the official docs
private val iconSize = when (DesktopPlatform.Current) {
    // https://doc.qt.io/qt-5/qtwidgets-desktop-systray-example.html (search 22x22)
    DesktopPlatform.Linux -> Size(22f, 22f)
    // https://doc.qt.io/qt-5/qtwidgets-desktop-systray-example.html (search 16x16)
    DesktopPlatform.Windows -> Size(16f, 16f)
    // https://medium.com/@acwrightdesign/creating-a-macos-menu-bar-application-using-swiftui-54572a5d5f87
    DesktopPlatform.MacOS -> Size(22f, 22f)
    DesktopPlatform.Unknown -> Size(32f, 32f)
}

/**
 * `true` if the platform supports tray icons in the taskbar
 */
val isTraySupported: Boolean get() = SystemTray.isSupported()

// TODO(demin): add mouse click/double-click/right click listeners (can we use PointerInputEvent?)
/**
 * Adds tray icon to the platform taskbar if it is supported.
 *
 * If tray icon isn't supported by the platform, in the "standard" error output stream
 * will be printed an error.
 *
 * See [isTraySupported] to know if tray icon is supported
 * (for example to show/hide an option in the application settings)
 *
 * @param icon Icon of the tray
 * @param state State to control tray and show notifications
 * @param tooltip Hint/tooltip that will be shown to the user
 * @param menu Context menu of the tray that will be shown to the user on the mouse click (right
 * click on Windows, left click on macOs).
 * If it doesn't contain any items then context menu will not be shown.
 * @param onAction Action performed when user clicks on the tray icon (double click on Windows,
 * right click on macOs)
 */
@Suppress("unused")
@Composable
fun ApplicationScope.Tray(
    icon: Painter,
    state: TrayState = rememberTrayState(),
    tooltip: String? = null,
    onAction: () -> Unit = {},
    menu: @Composable MenuScope.() -> Unit = {}
) {
    if (!isTraySupported) {
        DisposableEffect(Unit) {
            // We should notify developer, but shouldn't throw an exception.
            // If we would throw an exception, some application wouldn't work on some platforms at
            // all, if developer doesn't check that application crashes.
            //
            // We can do this because we don't return anything in Tray function, and following
            // code doesn't depend on something that is created/calculated in this function.
            System.err.println(
                "Tray is not supported on the current platform. " +
                    "Use the global property `isTraySupported` to check."
            )
            onDispose {}
        }
        return
    }

    val currentOnAction by rememberUpdatedState(onAction)

    val awtIcon = remember(icon) {
        // We shouldn't use LocalDensity here because Tray's density doesn't equal it. It
        // equals to the density of the screen on which it shows. Currently Swing doesn't
        // provide us such information, it only requests an image with the desired width/height
        // (see MultiResolutionImage.getResolutionVariant). Resources like svg/xml should look okay
        // because they don't use absolute '.dp' values to draw, they use values which are
        // relative to their viewport.
        icon.toAwtImage(GlobalDensity, GlobalLayoutDirection, iconSize)
    }

    val tray = remember {
        TrayIcon(awtIcon).apply {
            isImageAutoSize = true

            addActionListener {
                currentOnAction()
            }
        }
    }
    val popupMenu = remember { PopupMenu() }
    val currentMenu by rememberUpdatedState(menu)

    SideEffect {
        if (tray.image != awtIcon) tray.image = awtIcon
        if (tray.toolTip != tooltip) tray.toolTip = tooltip
    }

    val composition = rememberCompositionContext()
    val coroutineScope = rememberCoroutineScope()

    DisposableEffect(Unit) {
        tray.popupMenu = popupMenu

        val menuComposition = popupMenu.setContent(composition) {
            currentMenu()
        }

        SystemTray.getSystemTray().add(tray)

        state.notificationFlow
            .onEach(tray::displayMessage)
            .launchIn(coroutineScope)

        onDispose {
            menuComposition.dispose()
            SystemTray.getSystemTray().remove(tray)
        }
    }
}

/**
 * Creates a [WindowState] that is remembered across compositions.
 */
@Composable
fun rememberTrayState() = remember {
    TrayState()
}

/**
 * A state object that can be hoisted to control tray and show notifications.
 *
 * In most cases, this will be created via [rememberTrayState].
 */
class TrayState {
    private val notificationChannel = Channel<Notification>(0)

    /**
     * Flow of notifications sent by [sendNotification].
     * This flow doesn't have a buffer, so all previously sent notifications will not appear in
     * this flow.
     */
    val notificationFlow: Flow<Notification>
        get() = notificationChannel.receiveAsFlow()

    /**
     * Send notification to tray. If [TrayState] is attached to [Tray], notification will be sent to
     * the platform. If [TrayState] is not attached then notification will be lost.
     */
    fun sendNotification(notification: Notification) {
        notificationChannel.trySend(notification)
    }
}

private fun TrayIcon.displayMessage(notification: Notification) {
    val messageType = when (notification.type) {
        Notification.Type.None -> TrayIcon.MessageType.NONE
        Notification.Type.Info -> TrayIcon.MessageType.INFO
        Notification.Type.Warning -> TrayIcon.MessageType.WARNING
        Notification.Type.Error -> TrayIcon.MessageType.ERROR
    }

    displayMessage(notification.title, notification.message, messageType)
}
