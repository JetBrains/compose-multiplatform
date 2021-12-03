package org.jetbrains.compose.common.demo

import androidx.compose.runtime.remember
import androidx.compose.ui.window.singleWindowApplication
import org.jetbrains.compose.demo.falling.Game
import org.jetbrains.compose.demo.falling.views.fallingBalls

class JvmGame : Game() {
    override fun now() = System.nanoTime()
}

fun main() {
    singleWindowApplication(title = "Demo") {
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
