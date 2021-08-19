package org.jetbrains.compose.common.foundation.layout

import org.jetbrains.compose.common.ui.ExperimentalComposeWebWidgetsApi
import org.jetbrains.compose.common.ui.unit.Dp
import org.jetbrains.compose.common.ui.Modifier

@ExperimentalComposeWebWidgetsApi
expect fun Modifier.width(size: Dp): Modifier
