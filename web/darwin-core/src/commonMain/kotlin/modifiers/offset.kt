package org.jetbrains.compose.common.foundation.layout

import org.jetbrains.compose.common.ui.unit.Dp
import org.jetbrains.compose.common.ui.Modifier

expect fun Modifier.offset(x: Dp, y: Dp): Modifier
