package org.jetbrains.compose.common.ui.draw

import org.jetbrains.compose.common.ui.Modifier
import jetbrains.compose.common.shapes.Shape
import org.jetbrains.compose.common.ui.ExperimentalComposeWebWidgetsApi

@ExperimentalComposeWebWidgetsApi
expect fun Modifier.clip(shape: Shape): Modifier
