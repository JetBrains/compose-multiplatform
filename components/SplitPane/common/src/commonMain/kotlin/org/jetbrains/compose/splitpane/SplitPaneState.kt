package org.jetbrains.compose.splitpane

import androidx.compose.foundation.Interaction
import androidx.compose.foundation.InteractionState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.structuralEqualityPolicy


class SplitPaneState(
    initialPositionPercentage: Float,
    moveEnabled: Boolean,
    private val interactionState: InteractionState
) {

    private var _moveEnabled = mutableStateOf(moveEnabled, structuralEqualityPolicy())

    var moveEnabled: Boolean
        get() = _moveEnabled.value
        set(newValue) {
            _moveEnabled.value = newValue
        }

    private val _positionPercentage = mutableStateOf(initialPositionPercentage, structuralEqualityPolicy())

    var positionPercentage: Float
        get() = _positionPercentage.value
        internal set(newPosition) {
            _positionPercentage.value = newPosition
        }

    internal var minPosition: Float = 0f

    internal var maxPosition: Float = Float.POSITIVE_INFINITY

    fun dispatchRawMovement(delta: Float) {
        interactionState.addInteraction(Interaction.Dragged)
        val movableArea = maxPosition - minPosition
        if (movableArea > 0) {
            positionPercentage =
                ((movableArea * positionPercentage) + delta).coerceIn(minPosition, maxPosition) / movableArea
        }
        interactionState.removeInteraction(Interaction.Dragged)
    }

}