package org.jetbrains.compose.common.foundation

import org.jetbrains.compose.common.ui.unit.Dp
import org.jetbrains.compose.common.core.graphics.Color
import org.jetbrains.compose.common.ui.ExperimentalComposeWebWidgets
import org.jetbrains.compose.common.ui.Modifier

@ExperimentalComposeWebWidgets
expect fun Modifier.border(size: Dp, color: Color): Modifier
