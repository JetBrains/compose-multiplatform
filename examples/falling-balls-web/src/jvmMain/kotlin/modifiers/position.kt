package org.jetbrains.compose.common.demo

import androidx.compose.runtime.Composable
import org.jetbrains.compose.common.ui.Modifier
import org.jetbrains.compose.common.ui.unit.Dp
import org.jetbrains.compose.common.internal.castOrCreate
import org.jetbrains.compose.common.ui.unit.implementation
import androidx.compose.foundation.layout.offset
import org.jetbrains.compose.common.ui.ExperimentalComposeWebWidgetsApi

@Composable
@OptIn(ExperimentalComposeWebWidgetsApi::class)
actual fun Modifier.position(width: Dp, height: Dp): Modifier = castOrCreate().apply {
    modifier = modifier.offset(width.implementation, height.implementation)
}
