package org.jetbrains.compose.common.ui.unit

import org.jetbrains.compose.common.ui.ExperimentalComposeWebWidgetsApi

// TODO: this have to be in a separate package otherwise there's an error for in cross-module usage (for JVM target)
@ExperimentalComposeWebWidgetsApi
val Int.dp: Dp
    get() = Dp(this.toFloat())

@ExperimentalComposeWebWidgetsApi
val Int.em: TextUnit
    get() = TextUnit(toFloat(), TextUnitType.Em)

@ExperimentalComposeWebWidgetsApi
val Float.em: TextUnit
    get() = TextUnit(this, TextUnitType.Em)

@ExperimentalComposeWebWidgetsApi
val Int.sp: TextUnit
    get() = TextUnit(toFloat(), TextUnitType.Sp)

@ExperimentalComposeWebWidgetsApi
val Float.sp: TextUnit
    get() = TextUnit(this, TextUnitType.Sp)
