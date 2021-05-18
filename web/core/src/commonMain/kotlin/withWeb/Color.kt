package org.jetbrains.compose.common.core.graphics

public data class Color(val red: Int, val green: Int, val blue: Int) {

    companion object {
        val Black = Color(0, 0, 0)
        val DarkGray = Color(0x44, 0x44, 0x44)
        val Gray = Color(0x88, 0x88, 0x88)
        val LightGray = Color(0xCC, 0xCC, 0xCC)
        val White = Color(0xFF, 0xFF, 0xFF)
        val Red = Color(0xFF, 0, 0)
        val Green = Color(0, 0xFF, 0)
        val Blue = Color(0, 0, 0xFF)
        val Yellow = Color(0xFF, 0xFF, 0x00)
        val Cyan = Color(0, 0xFF, 0xFF)
        val Magenta = Color(0xFF, 0, 0xFF)
    }
}
