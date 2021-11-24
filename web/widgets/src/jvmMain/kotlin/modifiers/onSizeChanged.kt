package org.jetbrains.compose.common.ui.layout

import org.jetbrains.compose.common.ui.Modifier
import org.jetbrains.compose.common.ui.unit.IntSize
import org.jetbrains.compose.common.internal.castOrCreate
import androidx.compose.ui.layout.onSizeChanged
import org.jetbrains.compose.common.ui.ExperimentalComposeWebWidgetsApi

@ExperimentalComposeWebWidgetsApi
@Deprecated(message = "compose.web.web-widgets API is deprecated")
actual fun Modifier.onSizeChanged(
    onSizeChanged: (IntSize) -> Unit
): Modifier = castOrCreate().apply {
    modifier = modifier.onSizeChanged {
        onSizeChanged(IntSize(it.width, it.height))
    }
}
