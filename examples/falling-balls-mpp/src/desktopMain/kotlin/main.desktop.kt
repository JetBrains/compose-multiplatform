/*
 * Copyright 2020-2021 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.singleWindowApplication

object JvmTime : Time {
    override fun now(): Long = System.nanoTime()
}

@OptIn(ExperimentalComposeUiApi::class)
fun main() =
    singleWindowApplication(
        title = "Falling Balls",
        state = WindowState(size = DpSize(800.dp, 800.dp))
    ) {
        val game = remember { Game(JvmTime) }
        FallingBalls(game)
    }

@Preview
@Composable
fun GamePreview() {
    val game = remember { Game(JvmTime) }
    FallingBalls(game)
}