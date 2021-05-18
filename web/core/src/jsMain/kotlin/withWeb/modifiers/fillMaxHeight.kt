package org.jetbrains.compose.common.foundation.layout

import org.jetbrains.compose.common.ui.Modifier
import org.jetbrains.compose.common.internal.castOrCreate
import androidx.compose.web.css.height
import androidx.compose.web.css.percent

actual fun Modifier.fillMaxHeight(fraction: Float): Modifier = castOrCreate().apply {
    add {
        height((100 * fraction).percent)
    }
}
