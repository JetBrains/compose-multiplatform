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

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.key
import androidx.compose.ui.graphics.toPainter
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Tray
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberTrayState
import androidx.compose.ui.window.rememberWindowState

fun main() = application {
    val windowState = rememberWindowState(
        size = DpSize(800.dp, 600.dp)
    )

    val trayState = rememberTrayState()

    if (AppState.isMainWindowOpen) {
        Tray(
            state = trayState,
            icon = AppState.image().toPainter(),
            menu = {
                ActionItems(trayState)
            }
        )

        Window(
            onCloseRequest = AppState::closeMainWindow,
            title = AppState.wndTitle.value,
            state = windowState,
            icon = AppState.image().toPainter(),
        ) {
            MainMenuBar(windowState, trayState)

            CompositionLocalProvider(
                LocalTest provides 42
            ) {
                Content(windowState, trayState)
            }
        }
    }

    for (id in AppState.secondaryWindowIds) {
        key(id) {
            SecondaryWindow(
                onCloseRequest = { AppState.closeSecondaryWindow(id) }
            )
        }
    }
}