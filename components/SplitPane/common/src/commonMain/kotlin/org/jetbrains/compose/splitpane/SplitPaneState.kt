package org.jetbrains.compose.splitpane

import androidx.compose.foundation.interaction.Interaction
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

@ExperimentalSplitPaneApi
class SplitPaneState(
    initialPositionPercentage: Float,
    moveEnabled: Boolean,
) {

    var moveEnabled by mutableStateOf(moveEnabled)
        internal set

    var positionPercentage by mutableStateOf(initialPositionPercentage)
        internal set

    internal var minPosition: Float = 0f

    internal var maxPosition: Float = Float.POSITIVE_INFINITY

    fun dispatchRawMovement(delta: Float) {
        val movableArea = maxPosition - minPosition
        if (movableArea > 0) {
            positionPercentage =
                ((movableArea * positionPercentage) + delta).coerceIn(minPosition, maxPosition) / movableArea
        }
    }

}