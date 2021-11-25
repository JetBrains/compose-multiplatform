package org.jetbrains.compose.common.ui.unit

import org.jetbrains.compose.annotations.webWidgetsDeprecationMessage
import org.jetbrains.compose.common.ui.ExperimentalComposeWebWidgetsApi

@ExperimentalComposeWebWidgetsApi
@Deprecated(message = webWidgetsDeprecationMessage)
enum class TextUnitType {
    Unspecified,
    Em,
    Sp
}

@ExperimentalComposeWebWidgetsApi
@Deprecated(message = webWidgetsDeprecationMessage)
data class TextUnit(val value: Float, val unitType: TextUnitType) {
    companion object {
        @Deprecated(message = webWidgetsDeprecationMessage)
        val Unspecified = TextUnit(Float.NaN, TextUnitType.Unspecified)
    }
}
