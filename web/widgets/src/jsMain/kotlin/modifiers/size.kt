package org.jetbrains.compose.common.ui

import org.jetbrains.compose.common.ui.unit.Dp
import org.jetbrains.compose.web.css.width
import org.jetbrains.compose.web.css.height
import org.jetbrains.compose.web.css.px
import org.jetbrains.compose.common.internal.castOrCreate

actual fun Modifier.size(width: Dp, height: Dp): Modifier = castOrCreate().apply {
    add {
        width(width.value.px)
        height(height.value.px)
    }
}
