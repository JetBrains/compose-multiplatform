package org.jetbrains.compose.common.ui

import org.jetbrains.compose.annotations.webWidgetsDeprecationMessage
import org.jetbrains.compose.common.ui.unit.Dp
import org.jetbrains.compose.common.core.graphics.Color

@ExperimentalComposeWebWidgetsApi
@Deprecated(message = webWidgetsDeprecationMessage)
interface Modifier {
    open class Element : Modifier
    @Deprecated(message = webWidgetsDeprecationMessage)
    companion object : Element()
}

@ExperimentalComposeWebWidgetsApi
@Deprecated(message = webWidgetsDeprecationMessage)
expect fun Modifier.background(color: Color): Modifier

@ExperimentalComposeWebWidgetsApi
@Deprecated(message = webWidgetsDeprecationMessage)
expect fun Modifier.padding(all: Dp): Modifier
