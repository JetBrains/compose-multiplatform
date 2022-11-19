/*
 * Copyright 2020-2021 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Application
import platform.UIKit.UIViewController

object IosTime : Time {
    override fun now(): Long = kotlin.system.getTimeNanos()
}

fun MainViewController() : UIViewController =
    Application("Falling Balls") {
        val game = remember { Game(IosTime) }
        Column {
            // To skip upper part of screen.
            Box(modifier = Modifier
                .height(100.dp))
            FallingBalls(game)
        }
}
