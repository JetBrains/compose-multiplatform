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
import androidx.compose.desktop.SwingPanel
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.preferredHeight
import androidx.compose.foundation.layout.preferredSize
import androidx.compose.foundation.layout.width
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Checkbox
import androidx.compose.material.RadioButton
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerMoveFilter
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.Notifier
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.Tray
import androidx.compose.ui.window.WindowDraggableArea
import java.awt.event.ActionListener
import java.awt.event.ActionEvent
import java.awt.Toolkit
import javax.swing.JButton

@Composable
fun content() {
    DisposableEffect(Unit) {
        val tray = Tray().apply {
            icon(AppState.image())
            menu(
                MenuItems.Notify,
                MenuItems.Increment,
                MenuItems.Exit
            )
        }
        onDispose {
            tray.remove()
        }
    }

    val dialogState = remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color(55, 55, 55)
    ) {
        Column {
            Row(
                modifier = Modifier.background(color = Color(75, 75, 75))
                    .fillMaxWidth()
                    .preferredHeight(30.dp)
                    .padding(start = 20.dp, end = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                WindowDraggableArea(
                    modifier = Modifier.weight(1f)
                ) {
                    TextBox(text = AppState.wndTitle.value)
                }
                Row {
                    Button(
                        color = Color(210, 210, 210),
                        size = IntSize(16, 16),
                        onClick = {
                            AppManager.focusedWindow?.makeFullscreen()
                        }
                    )
                    Spacer(modifier = Modifier.width(30.dp))
                    Button(
                        color = Color(232, 182, 109),
                        size = IntSize(16, 16),
                        onClick = {
                            AppManager.focusedWindow?.minimize()
                        }
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Button(
                        color = Color(150, 232, 150),
                        size = IntSize(16, 16),
                        onClick = {
                            AppManager.focusedWindow?.maximize()
                        }
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Button(
                        onClick = { AppManager.exit() },
                        color = Color(232, 100, 100),
                        size = IntSize(16, 16)
                    )
                }
            }
            Row {
                Column(modifier = Modifier.padding(start = 30.dp, top = 50.dp)) {
                    Button("Show Popup", { AppState.popupState.value = true }, Color(232, 182, 109))
                    Spacer(modifier = Modifier.height(30.dp))
                    Button("Open dialog", { dialogState.value = true })
                    Spacer(modifier = Modifier.height(30.dp))
                    Button(
                        text = "New window...",
                        onClick = {
                            AppWindow(
                                title = "Second window",
                                size = IntSize(400, 200),
                                undecorated = AppState.undecorated.value,
                                onDismissRequest = {
                                    println("Second window is dismissed.")
                                }
                            ).show {
                                WindowContent(
                                    amount = AppState.amount,
                                    onClose = {
                                        AppManager.focusedWindow?.close()
                                    }
                                )
                                DisposableEffect(Unit) {
                                    onDispose {
                                        println("Dispose composition")
                                    }
                                }
                            }
                        },
                        color = Color(26, 198, 188)
                    )
                    Spacer(modifier = Modifier.height(30.dp))
                    Button(
                        text = "Send notification",
                        onClick = {
                            val message = "There should be your message."
                            if (AppState.notify.value) {
                                Notifier().notify("Notification.", message)
                            } else if (AppState.warn.value) {
                                Notifier().warn("Warning.", message)
                            } else {
                                Notifier().error("Error.", message)
                            }
                        },
                        color = Color(196, 136, 255)
                    )
                    Spacer(modifier = Modifier.height(30.dp))
                    Button("Increment amount", { AppState.amount.value++ }, Color(150, 232, 150))
                    Spacer(modifier = Modifier.height(30.dp))
                    Button("Exit app", { AppManager.exit() }, Color(232, 100, 100))
                    Spacer(modifier = Modifier.height(30.dp))
                    SwingActionButton("JButton", { AppState.amount.value++ })
                }
                Column(
                    modifier = Modifier.padding(start = 30.dp, top = 50.dp, end = 30.dp)
                        .background(color = Color(255, 255, 255, 10))
                        .fillMaxWidth()
                ) {
                    Spacer(modifier = Modifier.height(130.dp))
                    Row {
                        Checkbox(
                            checked = AppState.undecorated.value,
                            onCheckedChange = {
                                AppState.undecorated.value = !AppState.undecorated.value
                            },
                            modifier = Modifier.height(35.dp).padding(start = 20.dp)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        TextBox(text = "- undecorated")
                    }
                    Spacer(modifier = Modifier.height(30.dp))
                    Row(modifier = Modifier.padding(start = 20.dp)) {
                        RadioButton(
                            text = "- notify",
                            state = AppState.notify
                        )
                        Spacer(modifier = Modifier.width(30.dp))
                        RadioButton(
                            text = "- warn",
                            state = AppState.warn
                        )
                        Spacer(modifier = Modifier.width(30.dp))
                        RadioButton(
                            text = "- error",
                            state = AppState.error
                        )
                    }
                    Spacer(modifier = Modifier.height(30.dp))
                    Row(modifier = Modifier.padding(start = 20.dp)) {
                        TextBox(text = "Amount: ${AppState.amount.value}")
                    }
                    Spacer(modifier = Modifier.height(60.dp))
                }
            }
        }
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.BottomCenter
    ) {
        Box(
            modifier = Modifier.background(color = Color(32, 32, 32))
                .fillMaxWidth()
                .preferredHeight(30.dp)
        ) {
            Row(modifier = Modifier.padding(start = 20.dp)) {
                TextBox(
                    text = "Size: ${AppState.wndSize.value}   Location: ${AppState.wndPos.value}"
                )
            }
        }
    }

    PopupSample(
        displayed = AppState.popupState.value,
        onDismiss = {
            AppState.popupState.value = false
            println("Popup is dismissed.")
        }
    )
    if (AppState.popupState.value) {
        // To make sure the popup is displayed on the top.
        Box(
            Modifier.fillMaxSize().background(color = Color(0, 0, 0, 200))
        )
    }

    if (dialogState.value) {
        val dismiss = {
            dialogState.value = false
            println("Dialog window is dismissed.")
        }
        Dialog(
            onDismissRequest = dismiss
        ) {
            WindowContent(
                AppState.amount,
                onClose = dismiss
            )
        }
    }
}

@Composable
fun PopupSample(displayed: Boolean, onDismiss: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        if (displayed) {
            Popup(
                alignment = Alignment.TopCenter,
                offset = IntOffset(0, 50),
                isFocusable = true,
                onDismissRequest = onDismiss
            ) {
                println("Ambient value is ${AmbientTest.current}.")
                PopupContent(onDismiss)
                DisposableEffect(Unit) {
                    onDispose {
                        println("onDispose inside Popup is called.")
                    }
                }
            }
        }
    }
}

@Composable
fun PopupContent(onDismiss: () -> Unit) {
    Box(
        Modifier.preferredSize(300.dp, 150.dp).background(color = Color(65, 65, 65)),
        contentAlignment = Alignment.Center
    ) {
        Column {
            TextBox(text = "Popup demo.")
            Spacer(modifier = Modifier.height(30.dp))
            Button("Dismiss", { onDismiss.invoke() })
        }
    }
}

@Composable
fun WindowContent(amount: MutableState<Int>, onClose: () -> Unit) {
    Box(
        Modifier.fillMaxSize().background(color = Color(55, 55, 55)),
        contentAlignment = Alignment.Center
    ) {
        Column {
            TextBox(text = "Increment amount?")
            Spacer(modifier = Modifier.height(30.dp))
            Row {
                Button(text = "Yes", onClick = { amount.value++ }, size = IntSize(100, 35))
                Spacer(modifier = Modifier.width(30.dp))
                Button(text = "Close", onClick = { onClose.invoke() }, size = IntSize(100, 35))
            }
        }
    }
}

@Composable
fun Button(
    text: String = "",
    onClick: () -> Unit = {},
    color: Color = Color(10, 162, 232),
    size: IntSize = IntSize(200, 35)
) {
    val buttonHover = remember { mutableStateOf(false) }
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            backgroundColor =
                if (buttonHover.value)
                    Color(color.red / 1.3f, color.green / 1.3f, color.blue / 1.3f)
                else
                    color
        ),
        modifier = Modifier
            .preferredSize(size.width.dp, size.height.dp)
            .hover(
                onEnter = {
                    buttonHover.value = true
                    false
                },
                onExit = {
                    buttonHover.value = false
                    false
                },
                onMove = { false }
            )
    ) {
        Text(text = text)
    }
}

@Composable
fun TextBox(text: String = "", modifier: Modifier = Modifier.height(30.dp)) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = Color(200, 200, 200)
        )
    }
}

@Composable
fun RadioButton(text: String, state: MutableState<Boolean>) {
    Box(
        modifier = Modifier.height(35.dp).padding(start = 20.dp, bottom = 5.dp),
        contentAlignment = Alignment.Center
    ) {
        RadioButton(
            selected = state.value,
            onClick = {
                state.value = !state.value
                AppState.diselectOthers(state)
            }
        )
    }
    Spacer(modifier = Modifier.width(5.dp))
    TextBox(text = text)
}

expect fun Modifier.hover(
    onEnter: () -> Boolean = { true },
    onExit: () -> Boolean = { true },
    onMove: (Offset) -> Boolean = { true }
): Modifier

actual fun Modifier.hover(
    onEnter: () -> Boolean,
    onExit: () -> Boolean,
    onMove: (Offset) -> Boolean
): Modifier = this.pointerMoveFilter(onEnter = onEnter, onExit = onExit, onMove = onMove)

private fun image(url: String): java.awt.Image {
    return Toolkit.getDefaultToolkit().getImage(url)
}

@Composable
fun SwingActionButton(text: String, action: (() -> Unit)? = null) {
    SwingPanel(
        background = Color(55, 55, 55),
        modifier = Modifier.preferredSize(200.dp, 35.dp),
        componentBlock = {
            JButton(text).apply {
                addActionListener(object : ActionListener {
                    public override fun actionPerformed(e: ActionEvent) {
                        action?.invoke()
                    }
                })
            }
        },
        update = { component ->
            component.setText("$text:${AppState.amount.value}")
        }
    )
}
