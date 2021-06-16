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
@file:OptIn(ExperimentalComposeUiApi::class)

package androidx.compose.desktop.examples.windowapi

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application

fun main() = application {
    Window(onCloseRequest = ::exitApplication) {
        Row {
            Column {
                Button("helloWorld", ::helloWorld)
                Button("suspendApplication", ::suspendApplication)
                Button("suspendBackgroundApplication", ::suspendBackgroundApplication)
                Button("splashScreen", ::splashScreen)
                Button("autoClose", ::autoClose)
                Button("openSecondWindow", ::openSecondWindow)
                Button("closeToTray", ::closeToTray)
                Button("askToClose", ::askToClose)
                Button("customWindow", ::customWindow)
                Button("dialog", ::dialog)
                Button("hideDialog", ::hideDialog)
                Button("customDialog", ::customDialog)
            }
            Column {
                Button("setIcon", ::setIcon)
                Button("setParameters", ::setParameters)
                Button("setPosition", ::setPosition)
                Button("initiallyCenteredWindow", ::initiallyCenteredWindow)
                Button("setSize", ::setSize)
                Button("setStatus", ::setStatus)
                Button("hotkeys", ::hotKeys)
                Button("saveWindowState", ::saveWindowState)
            }
            Column {
                Button("menu", ::menu)
                Button("trayAndNotifications", ::trayAndNotifications)
            }
        }
    }
}

@Composable
private fun Button(
    text: String = "",
    onClick: () -> Unit = {}
) = Button(
    onClick = onClick,
    modifier = Modifier.padding(2.dp)
) {
    Text(text)
}