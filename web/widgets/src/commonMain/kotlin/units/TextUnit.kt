package org.jetbrains.compose.common.ui.unit

import org.jetbrains.compose.common.ui.ExperimentalComposeWebWidgetsApi

@ExperimentalComposeWebWidgetsApi
enum class TextUnitType {
    Unspecified,
    Em,
    Sp
}

@ExperimentalComposeWebWidgetsApi
data class TextUnit(val value: Float, val unitType: TextUnitType) {
    companion object {
        val Unspecified = TextUnit(Float.NaN, TextUnitType.Unspecified)
    }
}
