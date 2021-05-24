package org.jetbrains.compose.common.ui.unit

import androidx.compose.ui.unit.Dp as JDp

val Dp.implementation: JDp
    get() = JDp(value)