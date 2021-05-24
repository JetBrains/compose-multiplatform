package org.jetbrains.compose.common.foundation.layout

import org.jetbrains.compose.common.ui.unit.Dp
import org.jetbrains.compose.common.ui.Modifier
import org.jetbrains.compose.common.internal.castOrCreate
import androidx.compose.web.css.marginTop
import androidx.compose.web.css.marginLeft
import androidx.compose.web.css.px

actual fun Modifier.offset(x: Dp, y: Dp): Modifier = castOrCreate().apply {
    add {
        marginLeft(x.value.px)
        marginTop(y.value.px)
    }
}
