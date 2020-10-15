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
import androidx.compose.foundation.Text
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.preferredHeight
import androidx.compose.foundation.layout.preferredSize
import androidx.compose.foundation.layout.preferredWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.ButtonConstants
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.Popup

@Composable
fun content() {

    val popupState = remember { mutableStateOf(false) }
    val dialogState = remember { mutableStateOf(false) }
    val amount = remember { mutableStateOf(0) }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color.White
    ) {
        Column {
            Spacer(modifier = Modifier.height(50.dp))
            Row(modifier = Modifier.preferredHeight(40.dp)) {
                Button("Popup", { popupState.value = true })
                Spacer(modifier = Modifier.width(50.dp))
                Button("Dialog window", { dialogState.value = true })
                Spacer(modifier = Modifier.width(50.dp))
                Button(
                    text = "Second window: ${amount.value}",
                    onClick = {
                        AppWindow(
                            title = "Second window",
                            size = IntSize(500, 300),
                            onDismissEvent = {
                                println("Second window is dismissed.")
                            }
                        ).show {
                            WindowContent(
                                amount,
                                onClose = {
                                    AppManager.getCurrentFocusedWindow()?.close()
                                }
                            )
                        }
                    }
                )
            }
        }
    }

    PopupSample(
        displayed = popupState.value,
        onDismiss = {
            popupState.value = false
            println("Popup is dismissed.")
        }
    )
    if (popupState.value) {
        // To make sure the popup is displayed on the top.
        Box(
            Modifier.fillMaxSize().background(Color(10, 162, 232, 200))
        )
    }

    if (dialogState.value) {
        val dismiss = {
            dialogState.value = false
            println("Dialog window is dismissed.")
        }
        Dialog(
            title = "Dialog window",
            size = IntSize(500 + amount.value * 10, 300 + amount.value * 10),
            onDismissEvent = dismiss
        ) {
            WindowContent(amount, onClose = dismiss)
        }
    }
}

@Composable
fun PopupSample(displayed: Boolean, onDismiss: () -> Unit) {
    Box(
        Modifier.fillMaxSize()
    ) {
        if (displayed) {
            Popup(
                alignment = Alignment.Center,
                offset = IntOffset(100, 100),
                isFocusable = true,
                onDismissRequest = onDismiss
            ) {
                PopupContent(onDismiss)
            }
        }
    }
}

@Composable
fun PopupContent(onDismiss: () -> Unit) {
    Box(
        Modifier.preferredSize(300.dp, 150.dp).background(Color.Gray, RoundedCornerShape(4.dp)),
        alignment = Alignment.Center
    ) {
        Column {
            Text(text = "Are you sure?")
            Spacer(modifier = Modifier.height(50.dp))
            Button(
                onClick = { onDismiss.invoke() },
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Text(text = "Yes")
            }
        }
    }
}

@Composable
fun WindowContent(amount: MutableState<Int>, onClose: () -> Unit) {
    Box(
        Modifier.fillMaxSize().background(Color.White),
        alignment = Alignment.Center
    ) {
        Box(
            Modifier.preferredSize(300.dp, 150.dp)
                .background(Color.Gray, RoundedCornerShape(4.dp)),
            alignment = Alignment.Center
        ) {
            Column() {
                Text(text = "Increment value?")
                Spacer(modifier = Modifier.height(50.dp))
                Row() {
                    Button(
                        onClick = { amount.value++ }
                    ) {
                        Text(text = "Yes")
                    }
                    Spacer(modifier = Modifier.width(30.dp))
                    Button(
                        onClick = { onClose.invoke() }
                    ) {
                        Text(text = "Close")
                    }
                }
            }
        }
    }
}

@Composable
fun Button(
    text: String = "Button",
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        colors = ButtonConstants.defaultButtonColors(
            backgroundColor = Color(10, 162, 232)
        ),
        modifier = Modifier.preferredHeight(40.dp)
            .preferredWidth(200.dp)
    ) {
        Text(text = text)
    }
}
