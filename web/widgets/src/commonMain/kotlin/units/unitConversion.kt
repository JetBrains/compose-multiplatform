package org.jetbrains.compose.common.ui.unit

// TODO: this have to be in a separate package otherwise there's an error for in cross-module usage (for JVM target)
val Int.dp: Dp
    get() = Dp(this.toFloat())

val Int.em: TextUnit
    get() = TextUnit(toFloat(), TextUnitType.Em)

val Float.em: TextUnit
    get() = TextUnit(this, TextUnitType.Em)

val Int.sp: TextUnit
    get() = TextUnit(toFloat(), TextUnitType.Sp)

val Float.sp: TextUnit
    get() = TextUnit(this, TextUnitType.Sp)
