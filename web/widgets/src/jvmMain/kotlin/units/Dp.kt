package org.jetbrains.compose.common.ui.unit

import org.jetbrains.compose.common.ui.ExperimentalComposeWebWidgets
import androidx.compose.ui.unit.Dp as JDp

@ExperimentalComposeWebWidgets
val Dp.implementation: JDp
    get() = JDp(value)
