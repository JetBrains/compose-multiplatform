package org.jetbrains.compose.common.ui

import org.jetbrains.compose.common.ui.unit.Dp

@ExperimentalComposeWebWidgetsApi
expect fun Modifier.size(width: Dp, height: Dp): Modifier
