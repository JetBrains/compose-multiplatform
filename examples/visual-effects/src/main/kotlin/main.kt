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

fun mainNY() = application {
    NYWindow(::exitApplication)
}

fun main(args: Array<String>) {
    if (args.isEmpty()) return mainWords()
    when (val effect = args[0]) {
        "words" -> mainWords()
        "wave" -> mainWave(false)
        "wave-controls" -> mainWave(true)
        "NY" -> mainNY()
        else -> throw Error("Unknown effect: $effect")
    }
}
