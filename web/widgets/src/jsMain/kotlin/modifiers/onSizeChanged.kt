package org.jetbrains.compose.common.ui.layout

import org.jetbrains.compose.common.ui.ExperimentalComposeWebWidgetsApi
import org.jetbrains.compose.common.ui.Modifier
import org.jetbrains.compose.common.ui.unit.IntSize

@ExperimentalComposeWebWidgetsApi
actual fun Modifier.onSizeChanged(
    onSizeChanged: (IntSize) -> Unit
): Modifier {
    return this
}
