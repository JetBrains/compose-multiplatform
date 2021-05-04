package org.jetbrains.compose.common.demo

import androidx.compose.desktop.Window
import androidx.compose.ui.unit.IntSize
import org.jetbrains.compose.demo.falling.views.fallingBalls
import org.jetbrains.compose.demo.falling.Game
import androidx.compose.runtime.remember

class JvmGame : Game() {
    override fun now() = System.nanoTime()
}

fun main() {
    Window(title = "Demo", size = IntSize(600, 400)) {
        fallingBalls(
            remember {
                JvmGame().apply {
                    width = 600
                    height = 400
                }
            }
        )
    }
}
