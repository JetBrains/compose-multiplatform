package org.jetbrains.compose.common.ui

import org.jetbrains.compose.common.ui.unit.Dp
import org.jetbrains.compose.common.core.graphics.Color

@ExperimentalComposeWebWidgets
interface Modifier {
    open class Element : Modifier
    companion object : Element()
}

@ExperimentalComposeWebWidgets
expect fun Modifier.background(color: Color): Modifier

@ExperimentalComposeWebWidgets
expect fun Modifier.padding(all: Dp): Modifier
