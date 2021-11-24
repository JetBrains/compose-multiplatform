package org.jetbrains.compose.common.foundation.layout

import org.jetbrains.compose.common.ui.ExperimentalComposeWebWidgetsApi

@ExperimentalComposeWebWidgetsApi
@Deprecated(message = "compose.web.web-widgets API is deprecated")
object Arrangement {
    @ExperimentalComposeWebWidgetsApi
    @Deprecated(message = "compose.web.web-widgets API is deprecated")
    interface Horizontal

    @ExperimentalComposeWebWidgetsApi
    @Deprecated(message = "compose.web.web-widgets API is deprecated")
    interface Vertical

    val End = object : Horizontal {}
    val Start = object : Horizontal {}
    val Top = object : Vertical {}
    val Bottom = object : Vertical {}
}
