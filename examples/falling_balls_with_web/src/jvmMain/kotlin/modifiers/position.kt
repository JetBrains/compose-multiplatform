package modifiers

import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import org.jetbrains.compose.common.internal.castOrCreate
import org.jetbrains.compose.common.ui.Modifier
import org.jetbrains.compose.common.ui.unit.Dp
import org.jetbrains.compose.common.ui.unit.implementation

@Composable
actual fun Modifier.position(width: Dp, height: Dp): Modifier = castOrCreate().apply {
    modifier = modifier.offset(width.implementation, height.implementation)
}