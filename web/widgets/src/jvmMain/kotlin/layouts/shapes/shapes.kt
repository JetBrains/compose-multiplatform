package jetbrains.compose.common.shapes

import org.jetbrains.compose.common.ui.ExperimentalComposeWebWidgets
import androidx.compose.ui.graphics.Shape as JShape
import androidx.compose.foundation.shape.CircleShape as JCircleShape

@ExperimentalComposeWebWidgets
val Shape.implementation: JShape
    get() = when (this) {
        CircleShape -> JCircleShape
        else -> throw ClassCastException("Currently supporting only circle shape")
    }
