/*
 * Copyright 2020-2021 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

import androidx.compose.runtime.remember
import kotlinx.browser.document
import kotlinx.browser.window
import org.jetbrains.compose.web.css.*
import org.jetbrains.compose.web.dom.*
import org.jetbrains.compose.web.renderComposable
import org.w3c.dom.HTMLElement

fun main() {
    val root = document.getElementById("root") as HTMLElement

    renderComposable(root = root) {
        val game = remember { Game(width = 600, height = 600) }
        fallingBalls(game)
    }
}

actual fun now(): Long = window.performance.now().toLong()
