package jetbrains.compose.common.shapes

import androidx.compose.ui.graphics.Shape as JShape
import androidx.compose.foundation.shape.CircleShape as JCircleShape

val Shape.implementation: JShape
    get() = when (this) {
        CircleShape -> JCircleShape
        else -> throw ClassCastException("Currently supporting only circle shape")
    }
