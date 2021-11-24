package org.jetbrains.compose.common.ui.unit

import org.jetbrains.compose.common.ui.ExperimentalComposeWebWidgetsApi

@ExperimentalComposeWebWidgetsApi
@Deprecated(message = "compose.web.web-widgets API is deprecated")
enum class TextUnitType {
    Unspecified,
    Em,
    Sp
}

@ExperimentalComposeWebWidgetsApi
@Deprecated(message = "compose.web.web-widgets API is deprecated")
data class TextUnit(val value: Float, val unitType: TextUnitType) {
    companion object {
        @Deprecated(message = "compose.web.web-widgets API is deprecated")
        val Unspecified = TextUnit(Float.NaN, TextUnitType.Unspecified)
    }
}
