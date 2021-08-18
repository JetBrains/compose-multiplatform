package org.jetbrains.compose.common.ui

import org.jetbrains.compose.common.ui.unit.Dp

@ExperimentalComposeWebWidgets
fun Modifier.size(size: Dp): Modifier {
    return size(size, size)
}
