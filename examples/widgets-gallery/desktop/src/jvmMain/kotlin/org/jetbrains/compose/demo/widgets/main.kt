package org.jetbrains.compose.demo.widgets

import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowSize
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.application
import org.jetbrains.compose.demo.widgets.ui.MainView

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "Widgets Gallery",
        state = WindowState(size = WindowSize(800.dp, 600.dp))
    ) {
        MainView()
    }
}