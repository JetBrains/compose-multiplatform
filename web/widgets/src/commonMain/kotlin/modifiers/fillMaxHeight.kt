package org.jetbrains.compose.common.foundation.layout

import org.jetbrains.compose.common.ui.ExperimentalComposeWebWidgetsApi
import org.jetbrains.compose.common.ui.Modifier

@ExperimentalComposeWebWidgetsApi
expect fun Modifier.fillMaxHeight(fraction: Float): Modifier
