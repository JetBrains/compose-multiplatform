package org.jetbrains.compose.common.core.graphics

import androidx.compose.ui.graphics.Color as JColor

val Color.implementation
    get() = JColor(red, green, blue)
