package org.jetbrains.compose.common.foundation.layout

import androidx.compose.foundation.layout.Arrangement as JArrangement

val Arrangement.Horizontal.implementation: JArrangement.Horizontal
    get() = when (this) {
        Arrangement.End -> JArrangement.End
        else -> JArrangement.Start
    }
