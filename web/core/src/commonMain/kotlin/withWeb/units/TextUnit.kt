package org.jetbrains.compose.common.ui.unit

enum class TextUnitType {
    Unspecified,
    Em,
    Sp
}

data class TextUnit(val value: Float, val unitType: TextUnitType) {
    companion object {
        val Unspecified = TextUnit(Float.NaN, TextUnitType.Unspecified)
    }
}
