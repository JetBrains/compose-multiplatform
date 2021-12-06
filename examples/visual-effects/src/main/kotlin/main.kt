package org.jetbrains.compose.demo.visuals

import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.application
import androidx.compose.ui.window.singleWindowApplication

fun mainWords() = singleWindowApplication(
    title = "Compose Demo", state = WindowState(size = DpSize(830.dp, 830.dp))
) {
    RotatingWords()
}

fun mainWave(controls: Boolean) = application {
    WaveEffect(::exitApplication, controls)
}

fun main(args: Array<String>) = when {
    args.isEmpty() -> mainWords()
    args[0] == "words" -> mainWords()
    args[0] == "wave" -> mainWave(false)
    args[0] == "wave-controls" -> mainWave(true)
    else -> throw Error("Unknown effect: ${args[0]}")
}
