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
package androidx.compose.desktop.examples.popupexample

import androidx.compose.desktop.AppManager
import androidx.compose.desktop.AppWindow
import androidx.compose.desktop.WindowEvents
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize

fun main() {
    AppManager.onEvent(
        onAppStart = { println("OnAppStart") },
        onAppExit = { println("OnAppExit") },
        onWindowsEmpty = onCloseAppEvent
    )

    AppWindow(
        title = AppState.wndTitle.value,
        events = WindowEvents(
            onOpen = { println("OnOpen") },
            onClose = { println("OnClose") },
            onMinimize = { println("OnMinimize") },
            onMaximize = { println("OnMaximize") },
            onRestore = { println("OnRestore") },
            onFocusGet = { println("OnFocusGet") },
            onFocusLost = { println("OnFocusLost") },
            onResize = { size ->
                AppState.wndSize.value = size
            },
            onRelocate = { location ->
                AppState.wndPos.value = location
            }
        ),
        size = IntSize(800, 600),
        location = IntOffset(200, 200),
        icon = AppState.image()
    ).show {
        content()
    }
}

val onCloseAppEvent = {
    println("App exit.")
    System.exit(0)
}
