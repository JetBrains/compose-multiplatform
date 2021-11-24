package org.jetbrains.compose.common.core.graphics

import org.jetbrains.compose.common.ui.ExperimentalComposeWebWidgetsApi

@ExperimentalComposeWebWidgetsApi
@Deprecated(message = "compose.web.web-widgets API is deprecated")
public data class Color(val red: Int, val green: Int, val blue: Int) {

    companion object {
        @Deprecated(message = "compose.web.web-widgets API is deprecated")
        val Black = Color(0, 0, 0)
        @Deprecated(message = "compose.web.web-widgets API is deprecated")
        val DarkGray = Color(0x44, 0x44, 0x44)
        @Deprecated(message = "compose.web.web-widgets API is deprecated")
        val Gray = Color(0x88, 0x88, 0x88)
        @Deprecated(message = "compose.web.web-widgets API is deprecated")
        val LightGray = Color(0xCC, 0xCC, 0xCC)
        @Deprecated(message = "compose.web.web-widgets API is deprecated")
        val White = Color(0xFF, 0xFF, 0xFF)
        @Deprecated(message = "compose.web.web-widgets API is deprecated")
        val Red = Color(0xFF, 0, 0)
        @Deprecated(message = "compose.web.web-widgets API is deprecated")
        val Green = Color(0, 0xFF, 0)
        @Deprecated(message = "compose.web.web-widgets API is deprecated")
        val Blue = Color(0, 0, 0xFF)
        @Deprecated(message = "compose.web.web-widgets API is deprecated")
        val Yellow = Color(0xFF, 0xFF, 0x00)
        @Deprecated(message = "compose.web.web-widgets API is deprecated")
        val Cyan = Color(0, 0xFF, 0xFF)
        @Deprecated(message = "compose.web.web-widgets API is deprecated")
        val Magenta = Color(0xFF, 0, 0xFF)
    }
}
