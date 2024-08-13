package org.jetbrains.compose.demo.widgets

import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.singleWindowApplication
import MainView

fun main() = singleWindowApplication(
    title = "Widgets Gallery", state = WindowState(size = DpSize(800.dp, 800.dp))
) {
    MainView()
}