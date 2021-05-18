package org.jetbrains.compose.common.foundation.layout

object Arrangement {
    interface Horizontal
    interface Vertical

    val End = object : Horizontal {}
    val Start = object : Horizontal {}
    val Top = object : Vertical {}
    val Bottom = object : Vertical {}
}
