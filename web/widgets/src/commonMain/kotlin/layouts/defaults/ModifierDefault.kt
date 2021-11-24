package org.jetbrains.compose.common.ui

import org.jetbrains.compose.common.ui.unit.Dp

@ExperimentalComposeWebWidgetsApi
@Deprecated(message = "compose.web.web-widgets API is deprecated")
fun Modifier.size(size: Dp): Modifier {
    return size(size, size)
}
