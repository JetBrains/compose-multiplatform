package org.jetbrains.compose.common.foundation.layout

import org.jetbrains.compose.common.ui.ExperimentalComposeWebWidgets

@ExperimentalComposeWebWidgets
object Arrangement {
    @ExperimentalComposeWebWidgets
    interface Horizontal

    @ExperimentalComposeWebWidgets
    interface Vertical

    val End = object : Horizontal {}
    val Start = object : Horizontal {}
    val Top = object : Vertical {}
    val Bottom = object : Vertical {}
}
