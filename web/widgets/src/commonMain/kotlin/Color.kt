package org.jetbrains.compose.common.core.graphics

public data class Color(val red: Int, val green: Int, val blue: Int, val alpha: Float) {

    companion object {
        val Black = Color(0, 0, 0, 1.0f)
        val DarkGray = Color(0x44, 0x44, 0x44, 1.0f)
        val Gray = Color(0x88, 0x88, 0x88, 1.0f)
        val LightGray = Color(0xCC, 0xCC, 0xCC, 1.0f)
        val White = Color(0xFF, 0xFF, 0xFF, 1.0f)
        val Red = Color(0xFF, 0, 0, 1.0f)
        val Green = Color(0, 0xFF, 0, 1.0f)
        val Blue = Color(0, 0, 0xFF, 1.0f)
        val Yellow = Color(0xFF, 0xFF, 0x00, 1.0f)
        val Cyan = Color(0, 0xFF, 0xFF, 1.0f)
        val Magenta = Color(0xFF, 0, 0xFF, 1.0f)
        val Transparent = Color(0x00, 0x00, 0x00, 0.0f)
    }
}

fun Color(red: Int, green: Int, blue: Int) = Color(red, green, blue, 1.0f)