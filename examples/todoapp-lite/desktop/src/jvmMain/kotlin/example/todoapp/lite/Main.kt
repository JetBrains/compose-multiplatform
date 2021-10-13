package example.todoapp.lite

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.MaterialTheme
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import example.todoapp.lite.common.RootContent

fun main() {
    application {
        Window(
            onCloseRequest = ::exitApplication,
            title = "TodoApp Lite",
            state = rememberWindowState(
                position = WindowPosition(alignment = Alignment.Center),
            ),
        ) {
            MaterialTheme {
                RootContent(
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}
