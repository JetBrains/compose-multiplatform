package org.jetbrains.compose.common.ui.layout

import org.jetbrains.compose.common.ui.ExperimentalComposeWebWidgets
import org.jetbrains.compose.common.ui.Modifier
import org.jetbrains.compose.common.ui.unit.IntSize

@ExperimentalComposeWebWidgets
actual fun Modifier.onSizeChanged(
    onSizeChanged: (IntSize) -> Unit
): Modifier {
    return this
}
