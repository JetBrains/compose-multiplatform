/*
 * Copyright 2020-2021 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */


import androidx.compose.runtime.remember
import androidx.compose.ui.window.ComposeUIViewController
import platform.UIKit.UIViewController

object IosTime : Time {
    override fun now(): Long = kotlin.system.getTimeNanos()
}

fun MainViewController() : UIViewController = ComposeUIViewController {
    val game = remember { Game(IosTime) }
    FallingBalls(game)
}

