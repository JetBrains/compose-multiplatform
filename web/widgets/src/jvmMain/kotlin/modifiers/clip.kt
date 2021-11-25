package org.jetbrains.compose.common.ui.draw

import org.jetbrains.compose.common.ui.Modifier
import jetbrains.compose.common.shapes.Shape
import jetbrains.compose.common.shapes.implementation
import org.jetbrains.compose.common.internal.castOrCreate
import androidx.compose.ui.draw.clip
import org.jetbrains.compose.annotations.webWidgetsDeprecationMessage
import org.jetbrains.compose.common.ui.ExperimentalComposeWebWidgetsApi

@ExperimentalComposeWebWidgetsApi
@Deprecated(message = webWidgetsDeprecationMessage)
actual fun Modifier.clip(shape: Shape): Modifier = castOrCreate().apply {
    modifier = modifier.clip(shape.implementation)
}
