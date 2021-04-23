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

import androidx.compose.desktop.ComposeWindow
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Button
import androidx.compose.material.Checkbox
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusTarget
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.ApplicationScope
import androidx.compose.ui.window.AwtWindow
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.MenuBar
import androidx.compose.ui.window.Notification
import androidx.compose.ui.window.OwnerWindowScope
import androidx.compose.ui.window.Tray
import androidx.compose.ui.window.TrayState
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.WindowSize
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.awaitApplication
import androidx.compose.ui.window.launchApplication
import androidx.compose.ui.window.rememberNotification
import androidx.compose.ui.window.rememberWindowState
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.awt.Dimension
import java.awt.FileDialog
import java.awt.Frame
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import java.awt.image.BufferedImage
import java.lang.Thread.currentThread
import javax.imageio.ImageIO

@OptIn(DelicateCoroutinesApi::class)
fun helloWorld() = GlobalScope.launchApplication {
    Window(onCloseRequest = ::exitApplication) {
        Text("Hello, World!")
    }
}

@OptIn(DelicateCoroutinesApi::class)
fun suspendApplication() = GlobalScope.launch {
    println("Before application")

    awaitApplication {
        Window(onCloseRequest = ::exitApplication) {}
    }

    println("After application")
}

@OptIn(DelicateCoroutinesApi::class)
fun suspendBackgroundApplication() = GlobalScope.launch {
    println("Before application")

    awaitApplication {
        LaunchedEffect(Unit) {
            println("1")
            delay(1000)
            println("2")
            delay(1000)
            println("3")
        }
    }

    println("After application")
}

@OptIn(DelicateCoroutinesApi::class)
fun splashScreen() = GlobalScope.launchApplication {
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        delay(2000)
        isLoading = false
    }

    if (isLoading) {
        Window(onCloseRequest = ::exitApplication) {
            Text("Loading")
        }
    } else {
        Window(onCloseRequest = ::exitApplication) {
            Text("Hello, World!")
        }
    }
}

@OptIn(DelicateCoroutinesApi::class)
fun autoClose() = GlobalScope.launchApplication {
    var isOpen by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        delay(2000)
        isOpen = false
    }

    if (isOpen) {
        Window(onCloseRequest = {}) {
            Text("This window will be closed in 2 seconds")
        }
    }
}

@OptIn(DelicateCoroutinesApi::class)
fun openSecondWindow() = GlobalScope.launchApplication {
    var isMainWindowOpen by remember { mutableStateOf(true) }
    var isSecondWindowOpen by remember { mutableStateOf(false) }

    if (isMainWindowOpen) {
        Window(onCloseRequest = { isMainWindowOpen = false }) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(isSecondWindowOpen, { isSecondWindowOpen = !isSecondWindowOpen })
                Text("Second window")
            }
        }
    }

    if (isSecondWindowOpen) {
        Window(onCloseRequest = { isSecondWindowOpen = false }, title = "Nested window") {}
    }
}

@OptIn(DelicateCoroutinesApi::class)
fun closeToTray() = GlobalScope.launchApplication {
    var isVisible by remember { mutableStateOf(true) }

    Window(
        onCloseRequest = { isVisible = false },
        visible = isVisible,
        title = "Counter",
    ) {
        var counter by remember { mutableStateOf(0) }
        LaunchedEffect(Unit) {
            while (true) {
                counter++
                delay(1000)
            }
        }
        Text(counter.toString())
    }

    val icon = remember {
        runBlocking {
            loadIcon()
        }
    }

    if (!isVisible) {
        Tray(
            icon,
            hint = "Counter",
            onAction = { isVisible = true },
            menu = {
                Item("Exit", onClick = ::exitApplication)
            },
        )
    }
}

@OptIn(DelicateCoroutinesApi::class)
fun askToClose() = GlobalScope.launchApplication {
    var isAskingToClose by remember { mutableStateOf(false) }

    Window(onCloseRequest = { isAskingToClose = true }) {
        Text("Very important document")

        if (isAskingToClose) {
            Window(onCloseRequest = { isAskingToClose = false }, title = "Are you sure?") {
                Button(onClick = ::exitApplication) {
                    Text("Yes!")
                }
            }
        }
    }
}

@OptIn(DelicateCoroutinesApi::class)
fun customWindow() = GlobalScope.launchApplication {
    var isShowing by remember { mutableStateOf(true) }
    var titleNum by remember { mutableStateOf(0) }

    LaunchedEffect(Unit) {
        while (true) {
            titleNum++
            delay(1000)
        }
    }

    if (isShowing) {
        Window(
            create = {
                ComposeWindow().apply {
                    size = Dimension(200, 200)
                    addWindowListener(object : WindowAdapter() {
                        override fun windowClosing(e: WindowEvent) {
                            isShowing = false
                        }
                    })
                }
            },
            dispose = ComposeWindow::dispose,
            update = {
                it.title = "title$titleNum"
            }
        ) {}
    }
}

@OptIn(DelicateCoroutinesApi::class)
fun dialog() = GlobalScope.launchApplication {
    var isShowing by remember { mutableStateOf(true) }
    var isDialogShowing by remember { mutableStateOf(false) }

    if (isShowing) {
        Window(onCloseRequest = { isShowing = false }) {
            Button(onClick = { isDialogShowing = true }) {
                Text("Dialog")
            }

            if (isDialogShowing) {
                Dialog(onCloseRequest = { isDialogShowing = false }) {
                    Text("Dialog")
                }
            }
        }
    }
}

@OptIn(DelicateCoroutinesApi::class)
fun hideDialog() = GlobalScope.launchApplication {
    var isDialogVisible by remember { mutableStateOf(false) }

    Window(onCloseRequest = ::exitApplication) {
        Button(onClick = { isDialogVisible = true }) {
            Text("Dialog")
        }

        Dialog(
            onCloseRequest = { isDialogVisible = false },
            visible = isDialogVisible
        ) {
            var counter by remember { mutableStateOf(0) }
            LaunchedEffect(Unit) {
                while (true) {
                    counter++
                    delay(1000)
                }
            }
            Text(counter.toString())
        }
    }
}

@OptIn(DelicateCoroutinesApi::class)
fun customDialog() = GlobalScope.launchApplication {
    var isShowing by remember { mutableStateOf(true) }

    if (isShowing) {
        FileDialog(
            onDismissRequest = {
                isShowing = false
                println("Result $it")
            }
        )
    }
}

@Composable
private fun OwnerWindowScope.FileDialog(
    onDismissRequest: (result: String?) -> Unit
) = AwtWindow(
    create = {
        object : FileDialog(null as Frame?, "Choose a file", LOAD) {
            override fun setVisible(value: Boolean) {
                super.setVisible(value)
                if (value) {
                    onDismissRequest(file)
                }
            }
        }
    },
    dispose = FileDialog::dispose
)

@OptIn(DelicateCoroutinesApi::class)
fun setIcon() = GlobalScope.launchApplication {
    var icon: BufferedImage? by remember { mutableStateOf(null) }

    LaunchedEffect(Unit) {
        delay(1000)
        icon = loadIcon()
        delay(1000)
        icon = null
        delay(1000)
        icon = loadIcon()
    }

    Window(onCloseRequest = ::exitApplication, icon = icon) {}
}

@Suppress("BlockingMethodInNonBlockingContext")
private suspend fun loadIcon() = withContext(Dispatchers.IO) {
    val path = "androidx/compose/desktop/example/tray.png"
    ImageIO.read(currentThread().contextClassLoader.getResource(path))
}

@OptIn(DelicateCoroutinesApi::class)
fun setParameters() = GlobalScope.launchApplication {
    val state = rememberWindowState()
    Window(
        onCloseRequest = ::exitApplication,
        state = state, resizable = false, undecorated = true, alwaysOnTop = true
    ) {
        Button(onClick = ::exitApplication) {
            Text("Close")
        }
    }
}

@OptIn(DelicateCoroutinesApi::class)
fun setPosition() = GlobalScope.launchApplication {
    var isOpen by remember { mutableStateOf(true) }
    val state = rememberWindowState(position = WindowPosition(0.dp, 0.dp))

    if (isOpen) {
        Window(onCloseRequest = ::exitApplication, state) {}
    }

    Window(onCloseRequest = ::exitApplication) {
        Column {
            Text(state.position.toString())

            Button(
                onClick = {
                    val position = state.position
                    if (position is WindowPosition.Absolute) {
                        state.position = position.copy(x = state.position.x + 10.dp)
                    }
                }
            ) {
                Text("move")
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(isOpen, { isOpen = !isOpen })
                Text("isOpen")
            }
        }
    }
}

@OptIn(DelicateCoroutinesApi::class)
fun initiallyCenteredWindow() = GlobalScope.launchApplication {
    var isOpen by remember { mutableStateOf(true) }
    val state = rememberWindowState(position = WindowPosition(Alignment.Center))

    if (isOpen) {
        Window(onCloseRequest = ::exitApplication, state) {}
    }

    Window(onCloseRequest = ::exitApplication) {
        Column {
            Text(state.position.toString())

            Button(
                onClick = {
                    val position = state.position
                    if (position is WindowPosition.Absolute) {
                        state.position = position.copy(x = state.position.x + 10.dp)
                    }
                }
            ) {
                Text("move")
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(isOpen, { isOpen = !isOpen })
                Text("isOpen")
            }
        }
    }
}

@OptIn(DelicateCoroutinesApi::class)
fun setSize() = GlobalScope.launchApplication {
    var isOpen by remember { mutableStateOf(true) }
    val state = rememberWindowState(size = WindowSize(400.dp, 100.dp))

    if (isOpen) {
        Window(onCloseRequest = ::exitApplication, state) {}
    }

    Window(onCloseRequest = ::exitApplication) {
        Column {
            Text(state.size.toString())

            Button(
                onClick = {
                    state.size = state.size.copy(width = state.size.width + 10.dp)
                }
            ) {
                Text("resize")
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(isOpen, { isOpen = !isOpen })
                Text("isOpen")
            }
        }
    }
}

@OptIn(DelicateCoroutinesApi::class)
fun setStatus() = GlobalScope.launchApplication {
    val state = rememberWindowState(placement = WindowPlacement.Maximized)

    Window(onCloseRequest = ::exitApplication, state) {
        Column {
            Text(state.size.toString())

            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    state.placement == WindowPlacement.Fullscreen,
                    {
                        state.placement = if (it) {
                            WindowPlacement.Fullscreen
                        } else {
                            WindowPlacement.Floating
                        }
                    }
                )
                Text("isFullscreen")
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    state.placement == WindowPlacement.Fullscreen,
                    {
                        state.placement = if (it) {
                            WindowPlacement.Maximized
                        } else {
                            WindowPlacement.Floating
                        }
                    }
                )
                Text("isMaximized")
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(state.isMinimized, { state.isMinimized = !state.isMinimized })
                Text("isMinimized")
            }
        }
    }
}

@OptIn(DelicateCoroutinesApi::class)
fun hotKeys() = GlobalScope.launchApplication {
    val state = rememberWindowState()

    Window(onCloseRequest = ::exitApplication, state) {
        val focusRequester = remember(::FocusRequester)
        LaunchedEffect(Unit) {
            focusRequester.requestFocus()
        }

        Box(
            Modifier
                .focusRequester(focusRequester)
                .focusTarget()
                .onPreviewKeyEvent {
                    when (it.key) {
                        Key.Escape -> {
                            exitApplication()
                            true
                        }
                        else -> false
                    }
                }
        ) {
            TextField("Text", {})
        }
    }
}

fun saveWindowState() {
    // TODO
}

@OptIn(DelicateCoroutinesApi::class)
fun menu() = GlobalScope.launchApplication {
    var isSubmenuShowing by remember { mutableStateOf(false) }

    Window(
        onCloseRequest = ::exitApplication
    ) {
        MenuBar {
            Menu("File") {
                Item(
                    "Toggle submenu",
                    onClick = {
                        isSubmenuShowing = !isSubmenuShowing
                    }
                )
                if (isSubmenuShowing) {
                    Menu("Submenu") {
                        Item(
                            "item1",
                            onClick = {
                                println("item1")
                            }
                        )
                        Item(
                            "item2",
                            onClick = {
                                println("item2")
                            }
                        )
                    }
                }
                Separator()
                Item("Exit", onClick = this@launchApplication::exitApplication)
            }
        }
    }
}

@OptIn(DelicateCoroutinesApi::class)
fun trayAndNotifications() = GlobalScope.launchApplication {
    val state = remember(::AppState)

    Window(onCloseRequest = ::exitApplication, state.window) {
        TrayScreen(state)
    }

    Trays(state)
}

private class AppState {
    val window = WindowState()
    val tray = TrayState()
    var isTray2Visible by mutableStateOf(false)
}

@Composable
private fun TrayScreen(state: AppState) = Column(Modifier.padding(12.dp)) {
    val notification = rememberNotification(
        title = "Title",
        message = "Text",
        Notification.Type.Info
    )

    Button(
        modifier = Modifier.padding(8.dp),
        onClick = {
            state.isTray2Visible = !state.isTray2Visible
        }
    ) {
        Text("Toggle middle tray")
    }

    Button(
        modifier = Modifier.padding(8.dp),
        onClick = {
            state.tray.sendNotification(notification)
        }
    ) {
        Text("Show notification")
    }
}

@Composable
private fun ApplicationScope.Trays(state: AppState) {
    val icon = remember {
        runBlocking {
            loadIcon()
        }
    }

    Tray(icon, hint = "Tray1")

    if (state.isTray2Visible) {
        Tray(
            icon = icon,
            state = state.tray,
            hint = "Tray2",
            menu = {
                Menu("Submenu") {
                    Item(
                        "item1",
                        onClick = {
                            println("item1")
                        }
                    )
                    Item(
                        "item2",
                        onClick = {
                            println("item2")
                        }
                    )
                }

                Separator()

                Item(
                    "Exit",
                    onClick = ::exitApplication
                )
            }
        )
    }

    Tray(icon)
}
