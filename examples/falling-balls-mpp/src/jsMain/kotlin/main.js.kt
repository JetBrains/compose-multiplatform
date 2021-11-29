/*
 * Copyright 2020-2021 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

import androidx.compose.runtime.remember
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import org.jetbrains.skiko.wasm.onWasmReady

object JsTime : Time {
    override fun now(): Long = kotlinx.browser.window.performance.now().toLong()
}

fun main() {
    onWasmReady {
        Window("Falling Balls") {
            val game = remember { Game(JsTime).apply {
                // Ugly hack, properly pass geometry.
                width = 800.dp
                height = 800.dp
            }}
            FallingBalls(game)
        }
    }
}

