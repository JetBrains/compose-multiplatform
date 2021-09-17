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

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.TooltipArea
import androidx.compose.foundation.TooltipPlacement
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.window.WindowDraggableArea
import androidx.compose.material.AlertDialog
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Checkbox
import androidx.compose.material.CursorDropdownMenu
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.RadioButton
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.SwingPanel
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.ApplicationScope
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.Notification
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.TrayState
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.WindowScope
import androidx.compose.ui.window.WindowSize
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.rememberWindowState
import java.awt.event.ActionEvent
import java.awt.event.ActionListener
import javax.swing.JButton

@Composable
fun WindowScope.Content(
    windowState: WindowState,
    trayState: TrayState,
) {
    val dialogState = remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color(55, 55, 55)
    ) {
        Column {
            Row(
                modifier = Modifier.background(color = Color(75, 75, 75))
                    .fillMaxWidth()
                    .height(30.dp)
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
                            windowState.placement = WindowPlacement.Fullscreen
                        }
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Button(
                        color = Color(232, 182, 109),
                        size = IntSize(16, 16),
                        onClick = {
                            windowState.isMinimized = true
                        }
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Button(
                        color = Color(150, 232, 150),
                        size = IntSize(16, 16),
                        onClick = {
                            windowState.placement = WindowPlacement.Maximized
                        }
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Button(
                        onClick = AppState::closeMainWindow,
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
                        onClick = AppState::openSecondaryWindow,
                        color = Color(26, 198, 188)
                    )
                    Spacer(modifier = Modifier.height(30.dp))
                    Button(
                        text = "Send notification",
                        onClick = {
                            val message = "There should be your message."
                            when {
                                AppState.notify.value -> trayState.sendNotification(
                                    Notification("Notification.", message)
                                )
                                AppState.warn.value -> trayState.sendNotification(
                                    Notification("Warning.", message, Notification.Type.Warning)
                                )
                                else -> trayState.sendNotification(
                                    Notification("Error.", message, Notification.Type.Error)
                                )
                            }
                        },
                        color = Color(196, 136, 255)
                    )
                    Spacer(modifier = Modifier.height(30.dp))
                    Button("Increment amount", { AppState.amount.value++ }, Color(150, 232, 150))
                    Spacer(modifier = Modifier.height(30.dp))
                    Button("Exit app", AppState::closeAll, Color(232, 100, 100))
                    Spacer(modifier = Modifier.height(30.dp))
                    SwingActionButton("JButton", { AppState.amount.value++ })
                }
                Column(
                    modifier = Modifier.padding(start = 30.dp, top = 50.dp, end = 30.dp)
                        .background(color = Color(255, 255, 255, 10))
                        .fillMaxWidth()
                ) {
                    Row {
                        ContextMenu()
                        Spacer(modifier = Modifier.width(30.dp))
                        TextFieldWithSuggestions()
                    }
                    Spacer(modifier = Modifier.height(30.dp))
                    CheckBox(
                        text = "- alert dialog",
                        state = AppState.alertDialog,
                    )
                    Spacer(modifier = Modifier.height(30.dp))
                    CheckBox(
                        text = "- undecorated",
                        state = AppState.undecorated,
                    )
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
                .height(30.dp)
        ) {
            Row(modifier = Modifier.padding(start = 20.dp)) {
                TextBox(
                    text = "Size: ${windowState.size}   Location: ${windowState.position}"
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
        @OptIn(ExperimentalMaterialApi::class)
        if (AppState.alertDialog.value) {
            AlertDialog(
                onDismissRequest = dismiss,
                confirmButton = {
                    Button(text = "OK", onClick = { AppState.amount.value++ })
                },
                dismissButton = {
                    Button(text = "Cancel", onClick = dismiss)
                },
                title = {
                    TextBox(text = "Alert Dialog")
                },
                text = {
                    println("CompositionLocal value is ${LocalTest.current}.")
                    TextBox(text = "Increment amount?")
                    DisposableEffect(Unit) {
                        onDispose {
                            println("onDispose inside AlertDialog is called.")
                        }
                    }
                },
                shape = RoundedCornerShape(0.dp),
                backgroundColor = Color(70, 70, 70)
            )
        } else {
            Dialog(
                onCloseRequest = dismiss
            ) {
                WindowContent(
                    AppState.amount,
                    onClose = dismiss
                )
            }
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
                focusable = true,
                onDismissRequest = onDismiss
            ) {
                println("CompositionLocal value is ${LocalTest.current}.")
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
        Modifier.size(300.dp, 150.dp).background(color = Color(65, 65, 65)),
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
@OptIn(ExperimentalComposeUiApi::class)
fun Button(
    text: String = "",
    onClick: () -> Unit = {},
    color: Color = Color(10, 162, 232),
    size: IntSize = IntSize(200, 35)
) {
    @OptIn(ExperimentalFoundationApi::class)
    TooltipArea(
        tooltip = {
            Surface(
                color = Color(210, 210, 210),
                shape = RoundedCornerShape(4.dp)
            ) {
                Text(text = "Tooltip: [$text]", modifier = Modifier.padding(10.dp))
            }
        }
    ) {
        Button(
            onClick = onClick,
            colors = ButtonDefaults.buttonColors(
                backgroundColor = color
            ),
            modifier = Modifier
                .size(size.width.dp, size.height.dp)
        ) {
            Text(text = text)
        }
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
@OptIn(
    ExperimentalFoundationApi::class
)
fun ContextMenu() {
    val items = listOf("Item A", "Item B", "Item C", "Item D", "Item E", "Item F")
    val showMenu = remember { mutableStateOf(false) }
    val selectedIndex = remember { mutableStateOf(0) }
    TooltipArea(
        delayMillis = 100,
        tooltipPlacement = TooltipPlacement.ComponentRect(
            anchor = Alignment.TopStart,
            alignment = Alignment.TopEnd
        ),
        tooltip = {
            Surface(
                color = Color(210, 210, 210),
                shape = RoundedCornerShape(4.dp)
            ) {
                Text(text = "Tooltip: [ContextMenu]", modifier = Modifier.padding(10.dp))
            }
        }
    ) {
        Surface(
            color = Color(255, 255, 255, 40),
            shape = RoundedCornerShape(4.dp)
        ) {
            TextBox(
                text = "Selected: ${items[selectedIndex.value]}",
                modifier = Modifier
                    .height(35.dp)
                    .padding(start = 4.dp, end = 4.dp)
                    .clickable(onClick = { showMenu.value = true })
            )
            CursorDropdownMenu(
                expanded = showMenu.value,
                onDismissRequest = { showMenu.value = false }
            ) {
                items.forEachIndexed { index, name ->
                    DropdownMenuItem(
                        onClick = {
                            selectedIndex.value = index
                            showMenu.value = false
                        }
                    ) {
                        Text(text = name)
                    }
                }
            }
        }
    }
}

@Composable
fun TextFieldWithSuggestions() {
    Surface(
        color = Color(255, 255, 255, 40),
        shape = RoundedCornerShape(4.dp)
    ) {
        Box(
            modifier = Modifier.size(200.dp, 35.dp).padding(5.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            val text = remember { mutableStateOf("") }
            val words = remember { listOf("Hi!", "walking", "are", "home", "world") }
            val showMenu = remember { mutableStateOf(false) }
            BasicTextField(
                textStyle = TextStyle.Default.copy(color = Color(200, 200, 200)),
                value = text.value,
                singleLine = true,
                onValueChange = {
                    text.value = it
                    if (text.value.isNotEmpty())
                        showMenu.value = true
                    else
                        showMenu.value = false
                },
                modifier = Modifier.height(14.dp),
            )
            DropdownMenu(
                expanded = showMenu.value,
                onDismissRequest = {},
                focusable = false
            ) {
                words.forEach { name ->
                    DropdownMenuItem(onClick = { text.value += name }) {
                        Text(text = name)
                    }
                }
            }
        }
    }
}

@Composable
fun CheckBox(text: String, state: MutableState<Boolean>) {
    Row {
        Box(
            modifier = Modifier.height(35.dp),
            contentAlignment = Alignment.Center
        ) {
            Checkbox(
                checked = state.value,
                onCheckedChange = {
                    state.value = !state.value
                },
                modifier = Modifier.padding(start = 20.dp, bottom = 5.dp)
            )
        }
        Spacer(modifier = Modifier.width(5.dp))
        TextBox(text = text)
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

@Composable
fun SwingActionButton(text: String, action: (() -> Unit)? = null) {
    SwingPanel(
        background = Color(55, 55, 55),
        modifier = Modifier.size(200.dp, 35.dp),
        factory = {
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

@Composable
fun ApplicationScope.SecondaryWindow(onCloseRequest: () -> Unit) = Window(
    onCloseRequest = onCloseRequest,
    state = rememberWindowState(size = WindowSize(400.dp, 200.dp)),
    undecorated = AppState.undecorated.value,
) {
    WindowContent(
        amount = AppState.amount,
        onClose = onCloseRequest
    )
    DisposableEffect(Unit) {
        onDispose {
            println("Dispose composition")
        }
    }
}
