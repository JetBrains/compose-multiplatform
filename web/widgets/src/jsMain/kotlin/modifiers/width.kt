package org.jetbrains.compose.common.foundation.layout

import org.jetbrains.compose.common.ui.unit.Dp
import org.jetbrains.compose.common.ui.Modifier
import org.jetbrains.compose.common.internal.castOrCreate
import androidx.compose.web.css.px
import androidx.compose.web.css.width

actual fun Modifier.width(size: Dp): Modifier = castOrCreate().apply {
    add {
        width(size.value.px)
    }
}
