package org.jetbrains.compose.common.demo

import androidx.compose.runtime.Composable
import org.jetbrains.compose.common.ui.Modifier
import org.jetbrains.compose.common.foundation.layout.offset
import org.jetbrains.compose.common.ui.unit.Dp
import org.jetbrains.compose.common.internal.castOrCreate
import org.jetbrains.compose.web.css.top
import org.jetbrains.compose.web.css.left
import org.jetbrains.compose.web.css.px
import org.jetbrains.compose.web.css.position
import org.jetbrains.compose.web.css.Position

@Composable
actual fun Modifier.position(width: Dp, height: Dp): Modifier  = castOrCreate().apply {
    add {
        position(Position.Absolute)
        top(height.value.px)
        left(width.value.px)
    }
}