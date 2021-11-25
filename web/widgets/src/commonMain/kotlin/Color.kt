package org.jetbrains.compose.common.core.graphics

import org.jetbrains.compose.annotations.webWidgetsDeprecationMessage
import org.jetbrains.compose.common.ui.ExperimentalComposeWebWidgetsApi

@ExperimentalComposeWebWidgetsApi
@Deprecated(message = webWidgetsDeprecationMessage)
public data class Color(val red: Int, val green: Int, val blue: Int) {

    companion object {
        @Deprecated(message = webWidgetsDeprecationMessage)
        val Black = Color(0, 0, 0)
        @Deprecated(message = webWidgetsDeprecationMessage)
        val DarkGray = Color(0x44, 0x44, 0x44)
        @Deprecated(message = webWidgetsDeprecationMessage)
        val Gray = Color(0x88, 0x88, 0x88)
        @Deprecated(message = webWidgetsDeprecationMessage)
        val LightGray = Color(0xCC, 0xCC, 0xCC)
        @Deprecated(message = webWidgetsDeprecationMessage)
        val White = Color(0xFF, 0xFF, 0xFF)
        @Deprecated(message = webWidgetsDeprecationMessage)
        val Red = Color(0xFF, 0, 0)
        @Deprecated(message = webWidgetsDeprecationMessage)
        val Green = Color(0, 0xFF, 0)
        @Deprecated(message = webWidgetsDeprecationMessage)
        val Blue = Color(0, 0, 0xFF)
        @Deprecated(message = webWidgetsDeprecationMessage)
        val Yellow = Color(0xFF, 0xFF, 0x00)
        @Deprecated(message = webWidgetsDeprecationMessage)
        val Cyan = Color(0, 0xFF, 0xFF)
        @Deprecated(message = webWidgetsDeprecationMessage)
        val Magenta = Color(0xFF, 0, 0xFF)
    }
}
