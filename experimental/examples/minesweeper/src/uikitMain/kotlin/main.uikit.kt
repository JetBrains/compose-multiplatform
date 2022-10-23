/*
 * Copyright 2020-2022 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Application
import androidx.compose.ui.main.defaultUIKitMain

fun main() {
    defaultUIKitMain("Minesweeper", Application("Minesweeper") {
        Column {
            // To skip upper part of screen.
            Box(modifier = Modifier
                .height(100.dp))
            Game()
        }
    })
}

@Composable
actual fun loadImage(src: String): Painter {
    // TODO Bundle pics and show images properly
    val color = when (src) {
        "assets/clock.png" -> Color.Blue
        "assets/flag.png" -> Color.Green
        "assets/mine.png" -> Color.Red
        else -> Color.White
    }

    return object : Painter() {
        override val intrinsicSize: Size
            get() = Size(16f, 16f)

        override fun DrawScope.onDraw() {
            drawRect(color = color)
        }
    }
}