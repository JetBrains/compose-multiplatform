/*
 * Copyright 2020-2022 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.window.Application
import androidx.compose.ui.main.defaultUIKitMain

fun main() {
    defaultUIKitMain("Minesweeper", Application("Minesweeper") {
        Game()
    })
}

@Composable
actual fun loadImage(src: String): Painter = loadImageAsColoredRect(src)

actual fun isMobileDevice() = true