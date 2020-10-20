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
package androidx.compose.ui.window

import androidx.compose.desktop.AppWindow
import androidx.compose.desktop.WindowEvents
import androidx.compose.desktop.AppWindowAmbient
import androidx.compose.runtime.Composable
import androidx.compose.runtime.emptyContent
import androidx.compose.runtime.onActive
import androidx.compose.runtime.onDispose
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import java.awt.image.BufferedImage

@Composable
fun Dialog(
    title: String = "JetpackDesktopDialog",
    size: IntSize = IntSize(400, 250),
    location: IntOffset = IntOffset.Zero,
    centered: Boolean = true,
    icon: BufferedImage? = null,
    menuBar: MenuBar? = null,
    undecorated: Boolean = false,
    events: WindowEvents = WindowEvents(),
    onDismissEvent: (() -> Unit)? = null,
    content: @Composable () -> Unit = emptyContent()
) {
    val attached = AppWindowAmbient.current
    if (attached?.pair != null) {
        return
    }

    val dialog = remember {
        AppWindow(
            attached = attached,
            title = title,
            size = size,
            location = location,
            centered = centered,
            icon = icon,
            menuBar = menuBar,
            undecorated = undecorated,
            events = events,
            onDismissEvent = onDismissEvent
        )
    }

    onActive {
        dialog.show {
            content()
        }
    }

    onDispose {
        if (!dialog.isClosed) {
            dialog.close()
        }
    }
}
