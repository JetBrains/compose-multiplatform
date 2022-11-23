package org.jetbrains.codeviewer.util

import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout

fun Modifier.withoutWidthConstraints() = layout { measurable, constraints ->
    val placeable = measurable.measure(constraints.copy(maxWidth = Int.MAX_VALUE))
    layout(constraints.maxWidth, placeable.height) {
        placeable.place(0, 0)
    }
}