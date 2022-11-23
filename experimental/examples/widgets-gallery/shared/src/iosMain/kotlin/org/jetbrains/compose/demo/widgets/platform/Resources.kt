package org.jetbrains.compose.demo.widgets.platform

import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.painter.Painter

@Composable
internal actual fun painterResource(res: String): Painter =
    // TODO: use resource API
    object : Painter() {
        override val intrinsicSize: Size
            get() = Size(16f, 16f)

        override fun DrawScope.onDraw() {
            drawRect(color = Color.Red)
        }
    }
