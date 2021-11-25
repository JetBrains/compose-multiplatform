package org.jetbrains.compose.common.ui.unit

import org.jetbrains.compose.annotations.webWidgetsDeprecationMessage
import org.jetbrains.compose.common.ui.ExperimentalComposeWebWidgetsApi

// TODO: this have to be in a separate package otherwise there's an error for in cross-module usage (for JVM target)
@ExperimentalComposeWebWidgetsApi
@Deprecated(message = webWidgetsDeprecationMessage)
val Int.dp: Dp
    get() = Dp(this.toFloat())

@ExperimentalComposeWebWidgetsApi
@Deprecated(message = webWidgetsDeprecationMessage)
val Int.em: TextUnit
    get() = TextUnit(toFloat(), TextUnitType.Em)

@ExperimentalComposeWebWidgetsApi
@Deprecated(message = webWidgetsDeprecationMessage)
val Float.em: TextUnit
    get() = TextUnit(this, TextUnitType.Em)

@ExperimentalComposeWebWidgetsApi
@Deprecated(message = webWidgetsDeprecationMessage)
val Int.sp: TextUnit
    get() = TextUnit(toFloat(), TextUnitType.Sp)

@ExperimentalComposeWebWidgetsApi
@Deprecated(message = webWidgetsDeprecationMessage)
val Float.sp: TextUnit
    get() = TextUnit(this, TextUnitType.Sp)
