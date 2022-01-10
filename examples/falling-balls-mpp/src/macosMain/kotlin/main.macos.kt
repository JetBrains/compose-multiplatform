/*
 * Copyright 2020-2021 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

import androidx.compose.ui.window.Window
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.dp
import platform.AppKit.NSApp
import platform.AppKit.NSApplication

object MacosTime : Time {
    override fun now(): Long = kotlin.system.getTimeNanos()
}

fun main() {
    NSApplication.sharedApplication()
    Window("Falling Balls") {
        val game = remember { Game(MacosTime) }
        FallingBalls(game)
    }
    NSApp?.run()
}
