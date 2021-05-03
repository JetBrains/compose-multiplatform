package org.jetbrains.compose.common.demo

import androidx.compose.runtime.Composable
import org.jetbrains.compose.common.ui.Modifier
import org.jetbrains.compose.common.foundation.layout.offset
import org.jetbrains.compose.common.ui.unit.Dp
import org.jetbrains.compose.common.internal.castOrCreate
import androidx.compose.web.css.top
import androidx.compose.web.css.left
import androidx.compose.web.css.px
import androidx.compose.web.css.position
import androidx.compose.web.css.Position

@Composable
actual fun Modifier.position(width: Dp, height: Dp): Modifier  = castOrCreate().apply {
    add {
        position(Position.Relative)
        top(height.value.px)
        left(width.value.px)
    }
}