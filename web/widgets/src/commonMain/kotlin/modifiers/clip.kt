package org.jetbrains.compose.common.ui.draw

import org.jetbrains.compose.common.ui.Modifier
import jetbrains.compose.common.shapes.Shape

expect fun Modifier.clip(shape: Shape): Modifier
