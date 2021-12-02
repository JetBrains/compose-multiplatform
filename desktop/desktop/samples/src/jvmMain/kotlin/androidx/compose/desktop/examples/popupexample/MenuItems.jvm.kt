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

import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyShortcut
import androidx.compose.ui.window.FrameWindowScope
import androidx.compose.ui.window.MenuBar
import androidx.compose.ui.window.MenuScope
import androidx.compose.ui.window.Notification
import androidx.compose.ui.window.TrayState
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.WindowState

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun FrameWindowScope.MainMenuBar(
    windowState: WindowState,
    trayState: TrayState
) = MenuBar {
    Menu("Actions") {
        ActionItems(trayState, withShortcuts = true)
    }
    Menu("About") {
        CheckboxItem(
            "Fullscreen",
            windowState.placement == WindowPlacement.Fullscreen,
            shortcut = KeyShortcut(Key.F, ctrl = true)
        ) { checked ->
            windowState.placement = if (checked) {
                WindowPlacement.Fullscreen
            } else {
                WindowPlacement.Floating
            }
        }
        Item("About", shortcut = KeyShortcut(Key.I, ctrl = true)) {
            println("This is PopUpExampleApp")
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun MenuScope.ActionItems(
    trayState: TrayState,
    withShortcuts: Boolean = false
) {
    Item(
        "Send tray notification",
        shortcut = KeyShortcut(Key.N, ctrl = true).takeIf { withShortcuts }
    ) {
        trayState.sendNotification(
            Notification("New Notification from JB", "JetBrains send you a message!")
        )
    }
    Item(
        "Increment amount",
        shortcut = KeyShortcut(Key.A, ctrl = true).takeIf { withShortcuts }
    ) {
        AppState.amount.value++
    }
    Item(
        "Exit",
        shortcut = KeyShortcut(Key.Escape).takeIf { withShortcuts },
        onClick = AppState::closeAll
    )
}