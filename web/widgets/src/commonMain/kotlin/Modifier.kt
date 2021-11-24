package org.jetbrains.compose.common.ui

import org.jetbrains.compose.common.ui.unit.Dp
import org.jetbrains.compose.common.core.graphics.Color

@ExperimentalComposeWebWidgetsApi
@Deprecated(message = "compose.web.web-widgets API is deprecated")
interface Modifier {
    open class Element : Modifier
    @Deprecated(message = "compose.web.web-widgets API is deprecated")
    companion object : Element()
}

@ExperimentalComposeWebWidgetsApi
@Deprecated(message = "compose.web.web-widgets API is deprecated")
expect fun Modifier.background(color: Color): Modifier

@ExperimentalComposeWebWidgetsApi
@Deprecated(message = "compose.web.web-widgets API is deprecated")
expect fun Modifier.padding(all: Dp): Modifier
