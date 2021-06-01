package org.jetbrains.compose.common.ui.draw

import org.jetbrains.compose.common.ui.Modifier
import jetbrains.compose.common.shapes.Shape
import jetbrains.compose.common.shapes.CircleShape
import org.jetbrains.compose.common.internal.castOrCreate
import org.jetbrains.compose.web.css.borderRadius
import org.jetbrains.compose.web.css.percent

actual fun Modifier.clip(shape: Shape): Modifier = castOrCreate().apply {
    when (shape) {
        CircleShape -> add {
            borderRadius(50.percent)
        }
    }
}
