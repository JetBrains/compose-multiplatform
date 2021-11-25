package org.jetbrains.compose.common.ui

import org.jetbrains.compose.annotations.webWidgetsDeprecationMessage
import org.jetbrains.compose.common.ui.unit.Dp

@ExperimentalComposeWebWidgetsApi
@Deprecated(message = webWidgetsDeprecationMessage)
expect fun Modifier.size(width: Dp, height: Dp): Modifier
