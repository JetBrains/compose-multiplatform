package org.jetbrains.compose.common.foundation.layout

import org.jetbrains.compose.common.ui.unit.Dp
import org.jetbrains.compose.common.ui.Modifier
import org.jetbrains.compose.common.internal.castOrCreate
import androidx.compose.foundation.layout.width
import org.jetbrains.compose.common.ui.ExperimentalComposeWebWidgets
import org.jetbrains.compose.common.ui.unit.implementation

@ExperimentalComposeWebWidgets
actual fun Modifier.width(size: Dp): Modifier = castOrCreate().apply {
    modifier = modifier.width(size.implementation)
}
