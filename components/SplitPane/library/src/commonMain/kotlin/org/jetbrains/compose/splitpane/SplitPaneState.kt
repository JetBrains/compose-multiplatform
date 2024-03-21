package org.jetbrains.compose.splitpane

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

@ExperimentalSplitPaneApi
class SplitPaneState(
    initialPositionPercentage: Float,
    moveEnabled: Boolean,
) {

    var moveEnabled by mutableStateOf(moveEnabled)

    private var _positionPercentage by mutableStateOf(initialPositionPercentage)
    var positionPercentage: Float
        get() = _positionPercentage
        set(value) {
            if (maxPosition == Float.POSITIVE_INFINITY) {
                _positionPercentage = value
            } else {
                val movableArea = maxPosition - minPosition
                _positionPercentage = (movableArea * value).coerceIn(0f, movableArea) / movableArea
            }
        }

    internal var minPosition: Float = 0f

    internal var maxPosition: Float = Float.POSITIVE_INFINITY

    fun dispatchRawMovement(delta: Float) {
        val movableArea = maxPosition - minPosition
        if (movableArea > 0) {
            positionPercentage =
                ((movableArea * positionPercentage) + delta).coerceIn(0f, movableArea) / movableArea
        }
    }

}