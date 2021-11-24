package org.jetbrains.compose.common.ui.unit

import androidx.compose.ui.unit.TextUnit as JTextUnit
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.common.ui.ExperimentalComposeWebWidgetsApi

@ExperimentalComposeWebWidgetsApi
@Deprecated(message = "compose.web.web-widgets API is deprecated")
val TextUnit.implementation: JTextUnit
    get() = when (unitType) {
        TextUnitType.Em -> (value).em
        TextUnitType.Sp -> (value).sp
        else -> JTextUnit.Unspecified
    }
