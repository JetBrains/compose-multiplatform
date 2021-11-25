package org.jetbrains.compose.common.ui.unit

import org.jetbrains.compose.annotations.webWidgetsDeprecationMessage
import org.jetbrains.compose.common.ui.ExperimentalComposeWebWidgetsApi
import androidx.compose.ui.unit.Dp as JDp

@ExperimentalComposeWebWidgetsApi
@Deprecated(message = webWidgetsDeprecationMessage)
val Dp.implementation: JDp
    get() = JDp(value)
