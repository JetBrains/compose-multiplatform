package org.jetbrains.compose.common.ui

import org.jetbrains.compose.annotations.webWidgetsDeprecationMessage
import org.jetbrains.compose.common.ui.unit.Dp

@ExperimentalComposeWebWidgetsApi
@Deprecated(message = webWidgetsDeprecationMessage)
fun Modifier.size(size: Dp): Modifier {
    return size(size, size)
}
