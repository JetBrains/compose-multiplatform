package org.jetbrains.compose.common.foundation.layout

import org.jetbrains.compose.annotations.webWidgetsDeprecationMessage
import org.jetbrains.compose.common.ui.ExperimentalComposeWebWidgetsApi

@ExperimentalComposeWebWidgetsApi
@Deprecated(message = webWidgetsDeprecationMessage)
object Arrangement {
    @ExperimentalComposeWebWidgetsApi
    @Deprecated(message = webWidgetsDeprecationMessage)
    interface Horizontal

    @ExperimentalComposeWebWidgetsApi
    @Deprecated(message = webWidgetsDeprecationMessage)
    interface Vertical

    val End = object : Horizontal {}
    val Start = object : Horizontal {}
    val Top = object : Vertical {}
    val Bottom = object : Vertical {}
}
