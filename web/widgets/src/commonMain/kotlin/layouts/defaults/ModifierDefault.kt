package org.jetbrains.compose.common.ui

import org.jetbrains.compose.common.ui.unit.Dp

@ExperimentalComposeWebWidgetsApi
fun Modifier.size(size: Dp): Modifier {
    return size(size, size)
}
