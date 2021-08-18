package org.jetbrains.compose.common.ui.unit

import org.jetbrains.compose.common.ui.ExperimentalComposeWebWidgets

@ExperimentalComposeWebWidgets
enum class TextUnitType {
    Unspecified,
    Em,
    Sp
}

@ExperimentalComposeWebWidgets
data class TextUnit(val value: Float, val unitType: TextUnitType) {
    companion object {
        val Unspecified = TextUnit(Float.NaN, TextUnitType.Unspecified)
    }
}
