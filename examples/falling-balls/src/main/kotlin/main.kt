package org.jetbrains.compose.demo.falling

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.singleWindowApplication

@OptIn(ExperimentalComposeUiApi::class)
fun main() = singleWindowApplication(
    title = "Falling Balls", state = WindowState(size = DpSize(800.dp, 800.dp))
) {
    FallingBallsGame()
}

