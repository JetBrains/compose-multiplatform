package modifiers

import implementation
import org.jetbrains.compose.common.core.graphics.Color
import org.jetbrains.compose.common.internal.castOrCreate
import org.jetbrains.compose.common.ui.Modifier
import org.jetbrains.compose.web.css.backgroundColor

actual fun Modifier.background(color: Color): Modifier = castOrCreate().apply {
    add {
        backgroundColor(color.implementation)
    }
}