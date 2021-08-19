package org.jetbrains.compose.common.foundation.layout

import org.jetbrains.compose.common.ui.ExperimentalComposeWebWidgetsApi

@ExperimentalComposeWebWidgetsApi
object Arrangement {
    @ExperimentalComposeWebWidgetsApi
    interface Horizontal

    @ExperimentalComposeWebWidgetsApi
    interface Vertical

    val End = object : Horizontal {}
    val Start = object : Horizontal {}
    val Top = object : Vertical {}
    val Bottom = object : Vertical {}
}
