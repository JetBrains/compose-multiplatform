package org.jetbrainsc.compose.common.demo

import org.jetbrains.compose.web.renderComposable
import kotlinx.browser.document
import org.w3c.dom.HTMLElement
import org.jetbrains.compose.demo.falling.views.fallingBalls
import org.jetbrains.compose.demo.falling.Game
import androidx.compose.runtime.remember
import kotlinx.browser.window
import org.jetbrains.compose.web.css.Style
import org.jetbrains.compose.web.ui.Styles

class JsGame : Game() {
    override fun now() = window.performance.now().toLong()
}

fun main() {
    val root = document.getElementById("root") as HTMLElement

    renderComposable(root = root) {
        Style(Styles)
        val game = remember {
            JsGame().apply {
              width = root.offsetWidth
              height = root.offsetHeight
            }
        }
        fallingBalls(game)
    }
}
