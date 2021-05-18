package org.jetbrains.compose.common.foundation

import org.jetbrains.compose.common.ui.unit.Dp
import org.jetbrains.compose.common.core.graphics.Color
import org.jetbrains.compose.common.ui.Modifier
import org.jetbrains.compose.common.internal.castOrCreate
import androidx.compose.web.css.px
import androidx.compose.web.css.LineStyle
import androidx.compose.web.css.border
import androidx.compose.web.css.Color.RGB

actual fun Modifier.border(size: Dp, color: Color): Modifier = castOrCreate().apply {
    add {
        border(size.value.px, LineStyle.Solid, RGB(color.red, color.green, color.blue))
    }
}
