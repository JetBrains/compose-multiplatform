package org.jetbrains.compose.common.foundation.layout

import org.jetbrains.compose.common.ui.unit.Dp
import org.jetbrains.compose.common.ui.Modifier
import org.jetbrains.compose.common.internal.castOrCreate
import androidx.compose.foundation.layout.offset
import org.jetbrains.compose.common.ui.ExperimentalComposeWebWidgets
import org.jetbrains.compose.common.ui.unit.implementation

@ExperimentalComposeWebWidgets
actual fun Modifier.offset(x: Dp, y: Dp): Modifier = castOrCreate().apply {
    modifier = modifier.offset(x.implementation, y.implementation)
}
