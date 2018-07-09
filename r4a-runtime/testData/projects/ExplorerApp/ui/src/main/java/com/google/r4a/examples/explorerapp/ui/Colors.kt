package com.google.r4a.examples.explorerapp.ui

import android.graphics.Color

object Colors {
    private val String.color get() = Color.parseColor(this)

    // Brand Colors
    val WHITE = "#FFFFFF".color
    val BLACK = "#212122".color
    val LIGHT_GRAY = "#dadada".color
    val DARK_GRAY = "#888888".color
    val ORANGE_RED = "#FF4500".color
    val MINT = "#0DD3BB".color
    val BLUE = "#24A0ED".color
    val ALIEN_BLUE = "#0079D3".color
    val TEAL = "#00A6A5".color
    val ORANGE = "#FF8717".color
    val MANGO = "#FFB000".color
    val YELLOW = "#FFCA00".color

    // Semantic Colors
    val TEXT_LIGHT = WHITE
    val TEXT_DARK = BLACK
    val TEXT_MUTED = DARK_GRAY
    val PRIMARY = ORANGE_RED
    val SECONDARY = MINT
    val DIVIDER = LIGHT_GRAY
}