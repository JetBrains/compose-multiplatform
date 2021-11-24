package org.jetbrains.compose.common.ui.unit

import org.jetbrains.compose.common.ui.ExperimentalComposeWebWidgetsApi

// TODO: this have to be in a separate package otherwise there's an error for in cross-module usage (for JVM target)
@ExperimentalComposeWebWidgetsApi
@Deprecated(message = "compose.web.web-widgets API is deprecated")
val Int.dp: Dp
    get() = Dp(this.toFloat())

@ExperimentalComposeWebWidgetsApi
@Deprecated(message = "compose.web.web-widgets API is deprecated")
val Int.em: TextUnit
    get() = TextUnit(toFloat(), TextUnitType.Em)

@ExperimentalComposeWebWidgetsApi
@Deprecated(message = "compose.web.web-widgets API is deprecated")
val Float.em: TextUnit
    get() = TextUnit(this, TextUnitType.Em)

@ExperimentalComposeWebWidgetsApi
@Deprecated(message = "compose.web.web-widgets API is deprecated")
val Int.sp: TextUnit
    get() = TextUnit(toFloat(), TextUnitType.Sp)

@ExperimentalComposeWebWidgetsApi
@Deprecated(message = "compose.web.web-widgets API is deprecated")
val Float.sp: TextUnit
    get() = TextUnit(this, TextUnitType.Sp)
