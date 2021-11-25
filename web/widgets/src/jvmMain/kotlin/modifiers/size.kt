package org.jetbrains.compose.common.ui

import org.jetbrains.compose.common.ui.unit.Dp
import org.jetbrains.compose.common.ui.unit.implementation
import androidx.compose.foundation.layout.size
import org.jetbrains.compose.annotations.webWidgetsDeprecationMessage
import org.jetbrains.compose.common.internal.castOrCreate

@ExperimentalComposeWebWidgetsApi
@Deprecated(message = webWidgetsDeprecationMessage)
actual fun Modifier.size(width: Dp, height: Dp): Modifier = castOrCreate().apply {
    modifier = modifier.size(width.implementation, height.implementation)
}
