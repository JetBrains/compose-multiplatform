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

import androidx.compose.runtime.Composable
import androidx.compose.runtime.emptyContent
import androidx.compose.runtime.onActive
import androidx.compose.runtime.onDispose
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.ui.desktop.AppWindow
import androidx.ui.desktop.AppWindowAmbient

@Composable
fun Dialog(
    title: String = "JetpackDesktopDialog",
    size: IntSize = IntSize(1024, 768),
    position: IntOffset = IntOffset(0, 0),
    isCentered: Boolean = true,
    onDismissEvent: (() -> Unit)? = null,
    content: @Composable () -> Unit = emptyContent()
) {
    val attached = AppWindowAmbient.current
    val dialog = remember {
        AppWindow(
            attached = attached,
            title = title,
            size = size,
            position = position,
            onDismissEvent = onDismissEvent,
            centered = isCentered
        )
    }

    onActive {
        dialog.show {
            content()
        }
    }

    onDispose {
        dialog.close()
    }
}
