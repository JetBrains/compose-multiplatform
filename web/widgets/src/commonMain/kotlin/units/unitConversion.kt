package org.jetbrains.compose.common.ui.unit

import org.jetbrains.compose.common.ui.ExperimentalComposeWebWidgets

// TODO: this have to be in a separate package otherwise there's an error for in cross-module usage (for JVM target)
@ExperimentalComposeWebWidgets
val Int.dp: Dp
    get() = Dp(this.toFloat())

@ExperimentalComposeWebWidgets
val Int.em: TextUnit
    get() = TextUnit(toFloat(), TextUnitType.Em)

@ExperimentalComposeWebWidgets
val Float.em: TextUnit
    get() = TextUnit(this, TextUnitType.Em)

@ExperimentalComposeWebWidgets
val Int.sp: TextUnit
    get() = TextUnit(toFloat(), TextUnitType.Sp)

@ExperimentalComposeWebWidgets
val Float.sp: TextUnit
    get() = TextUnit(this, TextUnitType.Sp)
