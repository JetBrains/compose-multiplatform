package org.jetbrains.compose.common.foundation

import org.jetbrains.compose.common.ui.unit.Dp
import org.jetbrains.compose.common.core.graphics.Color
import org.jetbrains.compose.common.ui.Modifier
import org.jetbrains.compose.common.internal.castOrCreate
import org.jetbrains.compose.common.core.graphics.implementation
import org.jetbrains.compose.common.ui.unit.implementation
import androidx.compose.foundation.border

actual fun Modifier.border(size: Dp, color: Color): Modifier = castOrCreate().apply {
    modifier = modifier.border(size.implementation, color.implementation)
}
