package org.jetbrains.compose.common.core.graphics

import org.jetbrains.compose.annotations.webWidgetsDeprecationMessage
import org.jetbrains.compose.common.ui.ExperimentalComposeWebWidgetsApi
import androidx.compose.ui.graphics.Color as JColor

@ExperimentalComposeWebWidgetsApi
@Deprecated(message = webWidgetsDeprecationMessage)
val Color.implementation
    get() = JColor(red, green, blue)
