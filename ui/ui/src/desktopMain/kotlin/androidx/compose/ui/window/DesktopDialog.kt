/*
 * Copyright 2019 The Android Open Source Project
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

import androidx.compose.desktop.AppWindow
import androidx.compose.desktop.AppWindowAmbient
import androidx.compose.desktop.WindowEvents
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.rememberCompositionReference
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import java.awt.image.BufferedImage

/**
 * Desktop specific properties to configure a dialog.
 *
 * @param title The title of the dialog window.
 * The title is displayed in the windows's native border.
 * @param size The initial size of the dialog window.
 * @param location The initial position of the dialog window in screen space. This parameter is
 * ignored if [center] is set to true.
 * @param centered Determines if the window is centered on startup. The default value for the
 * dialog is true.
 * @param icon The icon for the dialog window displayed on the system taskbar.
 * @param menuBar Window menu bar. The menu bar can be displayed inside a window (Windows,
 * Linux) or at the top of the screen (Mac OS).
 * @param undecorated Removes the native window border if set to true. The default value is false.
 * @param resizable Makes the window resizable if is set to true and unresizable if is set to
 * false. The default value is true.
 * @param events Allows to describe events of the window.
 * Supported events: onOpen, onClose, onMinimize, onMaximize, onRestore, onFocusGet, onFocusLost,
 * onResize, onRelocate.
 */
@Immutable
data class DesktopDialogProperties(
    val title: String = "JetpackDesktopDialog",
    val size: IntSize = IntSize(400, 250),
    val location: IntOffset = IntOffset.Zero,
    val centered: Boolean = true,
    val icon: BufferedImage? = null,
    val menuBar: MenuBar? = null,
    val undecorated: Boolean = false,
    val resizable: Boolean = true,
    val events: WindowEvents = WindowEvents()
) : DialogProperties

@Composable
internal actual fun ActualDialog(
    onDismissRequest: () -> Unit,
    properties: DialogProperties?,
    content: @Composable () -> Unit
) {
    var desktopProperties = DesktopDialogProperties()
    if (properties is DesktopDialogProperties) {
        desktopProperties = properties
    }

    val attached = AppWindowAmbient.current
    if (attached?.pair != null) {
        return
    }

    val parentComposition = rememberCompositionReference()
    val dialog = remember {
        AppWindow(
            attached = attached,
            title = desktopProperties.title,
            size = desktopProperties.size,
            location = desktopProperties.location,
            centered = desktopProperties.centered,
            icon = desktopProperties.icon,
            menuBar = desktopProperties.menuBar,
            undecorated = desktopProperties.undecorated,
            resizable = desktopProperties.resizable,
            events = desktopProperties.events,
            onDismissRequest = onDismissRequest
        )
    }

    DisposableEffect(Unit) {
        dialog.show(parentComposition) {
            content()
        }
        onDispose {
            if (!dialog.isClosed) {
                // There are two closing situations of dialog windows:
                // 1. by [onDismissRequest]
                // 2. directly closing the dialog by clicking the close button or calling
                // the [close ()] JFrame method.
                // In the first case [onDismissRequest] is called first, then [onDismissRequest]
                // calls [onDispose], [onDispose] invokes [dialog.close()], [dialog.close()]
                // calls [onDismissRequest] again.
                // To prevent double invocation of [onDismissRequest] we should clear
                // the [onDismiss (originally - onDismissRequest)] event of the dialog window.
                dialog.onDismiss = null

                // directly closes the Swing window
                dialog.close()
            }
        }
    }
}