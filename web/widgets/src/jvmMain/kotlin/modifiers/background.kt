package modifiers

import androidx.compose.foundation.background
import org.jetbrains.compose.common.core.graphics.Color
import org.jetbrains.compose.common.core.graphics.implementation
import org.jetbrains.compose.common.internal.castOrCreate
import org.jetbrains.compose.common.ui.Modifier

actual fun Modifier.background(color: Color): Modifier = castOrCreate().apply {
    modifier = modifier.background(color.implementation)
}