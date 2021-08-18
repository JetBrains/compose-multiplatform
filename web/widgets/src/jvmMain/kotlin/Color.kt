package org.jetbrains.compose.common.core.graphics

import org.jetbrains.compose.common.ui.ExperimentalComposeWebWidgets
import androidx.compose.ui.graphics.Color as JColor

@ExperimentalComposeWebWidgets
val Color.implementation
    get() = JColor(red, green, blue)
