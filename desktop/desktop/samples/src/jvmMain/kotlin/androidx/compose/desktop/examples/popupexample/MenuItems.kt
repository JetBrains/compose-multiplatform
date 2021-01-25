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
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.window.MenuItem
import androidx.compose.ui.window.KeyStroke
import androidx.compose.ui.window.Notifier

object MenuItems {
    val Exit = MenuItem(
        name = "Exit",
        onClick = { AppManager.exit() },
        shortcut = KeyStroke(Key.X)
    )

    val Notify = MenuItem(
        name = "Send tray notification",
        onClick = {
            Notifier().notify(
                title = "New Notification from JB",
                message = "JetBrains send you a message!"
            )
        },
        shortcut = KeyStroke(Key.N)
    )

    val Increment = MenuItem(
        name = "Increment amount",
        onClick = { AppState.amount.value++ },
        shortcut = KeyStroke(Key.A)
    )

    val About = MenuItem(
        name = "About app",
        onClick = {
            println("This is PopUpExampleApp")
        },
        shortcut = KeyStroke(Key.I)
    )

    val Update = MenuItem(
        name = "Check updates",
        onClick = {
            println("Application is up to date.")
        },
        shortcut = KeyStroke(Key.U)
    )

    val IsFullscreen = MenuItem(
        name = "Is fullscreen mode",
        onClick = {
            println("Fullscreen mode: ${AppManager.focusedWindow?.isFullscreen}")
        },
        shortcut = KeyStroke(Key.F)
    )
}