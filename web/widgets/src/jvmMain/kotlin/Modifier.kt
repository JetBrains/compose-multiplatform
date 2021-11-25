package org.jetbrains.compose.common.ui

import org.jetbrains.compose.common.ui.unit.Dp
import org.jetbrains.compose.common.ui.unit.implementation
import androidx.compose.foundation.background
import org.jetbrains.compose.common.core.graphics.Color
import org.jetbrains.compose.common.core.graphics.implementation
import org.jetbrains.compose.common.internal.castOrCreate
import androidx.compose.foundation.layout.padding
import org.jetbrains.compose.annotations.webWidgetsDeprecationMessage

@ExperimentalComposeWebWidgetsApi
@Deprecated(message = webWidgetsDeprecationMessage)
actual fun Modifier.background(color: Color): Modifier = castOrCreate().apply {
    modifier = modifier.background(color.implementation)
}

@ExperimentalComposeWebWidgetsApi
@Deprecated(message = webWidgetsDeprecationMessage)
actual fun Modifier.padding(all: Dp): Modifier = castOrCreate().apply {
    modifier = modifier.padding(all.implementation)
}

@ExperimentalComposeWebWidgetsApi
@Deprecated(message = webWidgetsDeprecationMessage)
val Modifier.implementation
    get() = castOrCreate().modifier
