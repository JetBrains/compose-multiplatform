package org.jetbrains.compose.common.ui

import org.jetbrains.compose.common.ui.unit.Dp
import org.jetbrains.compose.common.core.graphics.Color

@ExperimentalComposeWebWidgetsApi
interface Modifier {
    open class Element : Modifier
    companion object : Element()
}

@ExperimentalComposeWebWidgetsApi
expect fun Modifier.background(color: Color): Modifier

@ExperimentalComposeWebWidgetsApi
expect fun Modifier.padding(all: Dp): Modifier
