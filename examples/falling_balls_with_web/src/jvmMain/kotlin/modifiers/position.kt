package org.jetbrains.compose.common.demo

import androidx.compose.runtime.Composable
import org.jetbrains.compose.common.ui.Modifier
import org.jetbrains.compose.common.foundation.layout.offset
import org.jetbrains.compose.common.ui.unit.Dp
import org.jetbrains.compose.common.internal.castOrCreate
import org.jetbrains.compose.common.ui.unit.implementation
import androidx.compose.foundation.layout.offset

@Composable
actual fun Modifier.position(width: Dp, height: Dp): Modifier = castOrCreate().apply {
    modifier = modifier.offset(width.implementation, height.implementation)
}