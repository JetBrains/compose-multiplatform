/*
 * Copyright 2020-2021 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

import androidx.compose.ui.window.Window
import androidx.compose.runtime.remember
import platform.AppKit.NSApp
import platform.AppKit.NSApplication

fun main() {
    NSApplication.sharedApplication()
    Window("Compose/Native sample") {
        val game = remember { Game(width = 600, height = 600) }
        fallingBalls(game)
    }
    NSApp?.run()
}
