package modifiers

import androidx.compose.runtime.Composable
import androidx.compose.web.css.*
import org.jetbrains.compose.common.internal.castOrCreate
import org.jetbrains.compose.common.ui.Modifier
import org.jetbrains.compose.common.ui.unit.Dp

@Composable
actual fun Modifier.position(width: Dp, height: Dp): Modifier = castOrCreate().apply {
    add {
        position(Position.Absolute)
        top(height.value.px)
        left(width.value.px)
    }
}