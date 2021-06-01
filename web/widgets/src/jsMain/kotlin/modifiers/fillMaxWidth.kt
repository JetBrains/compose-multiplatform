package org.jetbrains.compose.common.foundation.layout

import org.jetbrains.compose.common.ui.Modifier
import org.jetbrains.compose.common.internal.castOrCreate
import org.jetbrains.compose.web.css.width
import org.jetbrains.compose.web.css.percent

actual fun Modifier.fillMaxWidth(): Modifier = castOrCreate().apply {
    add {
        width(100.percent)
    }
}
