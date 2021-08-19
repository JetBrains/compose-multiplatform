package org.jetbrains.compose.common.foundation.layout

import org.jetbrains.compose.common.ui.ExperimentalComposeWebWidgetsApi
import androidx.compose.foundation.layout.Arrangement as JArrangement

@ExperimentalComposeWebWidgetsApi
val Arrangement.Horizontal.implementation: JArrangement.Horizontal
    get() = when (this) {
        Arrangement.End -> JArrangement.End
        else -> JArrangement.Start
    }
