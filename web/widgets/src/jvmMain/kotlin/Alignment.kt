package org.jetbrains.compose.common.ui

import org.jetbrains.compose.annotations.webWidgetsDeprecationMessage
import androidx.compose.ui.Alignment as JAlignment

@ExperimentalComposeWebWidgetsApi
@Deprecated(message = webWidgetsDeprecationMessage)
val Alignment.Vertical.implementation: JAlignment.Vertical
    get() = when (this) {
        Alignment.Top -> JAlignment.Top
        Alignment.CenterVertically -> JAlignment.CenterVertically
        else -> JAlignment.Bottom
    }
