package org.jetbrains.compose.common.ui

import org.jetbrains.compose.common.ui.unit.Dp
import org.jetbrains.compose.common.core.graphics.Color

interface Modifier {
    open class Element : Modifier
    companion object : Element()
}

expect fun Modifier.background(color: Color): Modifier
expect fun Modifier.padding(all: Dp): Modifier