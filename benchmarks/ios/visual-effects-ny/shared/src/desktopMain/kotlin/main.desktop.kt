package org.jetbrains.compose.demo.visuals

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.*

@Composable
fun NYWindow(onCloseRequest: () -> Unit) {
    val windowState = remember { WindowState(width = width.dp, height = height.dp) }
    Window(onCloseRequest = onCloseRequest, undecorated = false, transparent = false, state = windowState) {
        NYContent()
    }
}

fun mainNY() = application {
    NYWindow(::exitApplication)
}


