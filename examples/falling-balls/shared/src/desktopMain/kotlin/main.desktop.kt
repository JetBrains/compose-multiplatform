/*
 * Copyright 2020-2021 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

object JvmTime : Time {
    override fun now(): Long = System.nanoTime()
}

@Composable
fun MainView() {
    val game = remember { Game(JvmTime) }
    FallingBalls(game)
}

@Preview
@Composable
fun GamePreview() {
    MainView()
}