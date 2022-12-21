package org.jetbrains.compose.demo.visuals

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.*

fun mainWords() = singleWindowApplication(
    title = "Compose Demo", state = WindowState(size = DpSize(830.dp, 830.dp))
) {
    RotatingWords()
}

@Composable
fun WaveEffect(onCloseRequest: () -> Unit, showControls: Boolean) {
    val windowState = remember { WindowState(width = 1200.dp, height = 800.dp) }
    Window(onCloseRequest = {}, undecorated = true, transparent = true, state = windowState) {
        Grid()
    }

    if (showControls) {
        Window(
            onCloseRequest = onCloseRequest,
            state = WindowState(width = 200.dp, height = 400.dp, position = WindowPosition(1400.dp, 200.dp))
        ) {
            Column {
                SettingsPanel(SettingsState.red, "Red")
                SettingsPanel(SettingsState.green, "Green")
                SettingsPanel(SettingsState.blue, "Blue")
            }
        }
    }
}

fun mainWave(controls: Boolean) = application {
    WaveEffect(::exitApplication, controls)
}

@Composable
fun NYWindow(onCloseRequest: () -> Unit) {
    val windowState = remember { WindowState(width = width.dp, height = height.dp) }
    Window(onCloseRequest = onCloseRequest, undecorated = true, transparent = true, state = windowState) {
        NYContent()
    }
}

fun mainNY() = application {
    NYWindow(::exitApplication)
}


fun allSamples() = application {
    val windowState = remember { WindowState(width = 1200.dp, height = 900.dp) }
    Window(onCloseRequest = ::exitApplication, title = "Visual effects", undecorated = false, transparent = false, state = windowState) {
        AllSamplesView()
    }
}

fun main(args: Array<String>) {
    if (args.isEmpty()) return allSamples()
    when (val effect = args[0]) {
        "words" -> mainWords()
        "wave" -> mainWave(false)
        "wave-controls" -> mainWave(true)
        "NY" -> mainNY()
        else -> throw Error("Unknown effect: $effect")
    }
}
