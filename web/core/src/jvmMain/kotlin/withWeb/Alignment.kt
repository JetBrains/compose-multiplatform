package org.jetbrains.compose.common.ui

import androidx.compose.ui.Alignment as JAlignment

val Alignment.Vertical.implementation: JAlignment.Vertical
    get() = when (this) {
        Alignment.Top -> JAlignment.Top
        Alignment.CenterVertically -> JAlignment.CenterVertically
        else -> JAlignment.Bottom
    }
