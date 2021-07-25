package org.jetbrains.compose.common.core.graphics

public data class Color(val red: Int, val green: Int, val blue: Int, val alpha: Float) {

    companion object {
        val Black = Color(0, 0, 0, 1f)
        val DarkGray = Color(0x44, 0x44, 0x44, 1f)
        val Gray = Color(0x88, 0x88, 0x88, 1f)
        val LightGray = Color(0xCC, 0xCC, 0xCC, 1f)
        val White = Color(0xFF, 0xFF, 0xFF, 1f)
        val Red = Color(0xFF, 0, 0, 1f)
        val Green = Color(0, 0xFF, 0, 1f)
        val Blue = Color(0, 0, 0xFF, 1f)
        val Yellow = Color(0xFF, 0xFF, 0x00, 1f)
        val Cyan = Color(0, 0xFF, 0xFF, 1f)
        val Magenta = Color(0xFF, 0, 0xFF, 1f)
        val Transparent = Color(0x00, 0x00, 0x00, 0f)
    }
}
