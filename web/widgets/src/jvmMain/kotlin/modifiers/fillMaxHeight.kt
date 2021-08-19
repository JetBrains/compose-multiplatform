package org.jetbrains.compose.common.foundation.layout

import org.jetbrains.compose.common.ui.Modifier
import org.jetbrains.compose.common.internal.castOrCreate
import androidx.compose.foundation.layout.fillMaxHeight
import org.jetbrains.compose.common.ui.ExperimentalComposeWebWidgetsApi

@ExperimentalComposeWebWidgetsApi
actual fun Modifier.fillMaxHeight(fraction: Float): Modifier = castOrCreate().apply {
    modifier = modifier.fillMaxHeight(fraction)
}
