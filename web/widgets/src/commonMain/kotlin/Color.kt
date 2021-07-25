package org.jetbrains.compose.common.core.graphics

public data class Color(val red: Int, val green: Int, val blue: Int, val alpha: Int) {

    companion object {
        val Black = Color(0, 0, 0, 0xFF)
        val DarkGray = Color(0x44, 0x44, 0x44, 0xFF)
        val Gray = Color(0x88, 0x88, 0x88, 0xFF)
        val LightGray = Color(0xCC, 0xCC, 0xCC, 0xFF)
        val White = Color(0xFF, 0xFF, 0xFF, 0xFF)
        val Red = Color(0xFF, 0, 0, 0xFF)
        val Green = Color(0, 0xFF, 0, 0xFF)
        val Blue = Color(0, 0, 0xFF, 0xFF)
        val Yellow = Color(0xFF, 0xFF, 0x00, 0xFF)
        val Cyan = Color(0, 0xFF, 0xFF, 0xFF)
        val Magenta = Color(0xFF, 0, 0xFF, 0xFF)
        val Transparent = Color(0x00, 0x00, 0x00, 0x00)
    }
}

fun Color(red: Int, green: Int, blue: Int) = Color(red, green, blue, 255)