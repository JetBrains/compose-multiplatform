package org.jetbrains.compose.movable

import androidx.compose.foundation.InteractionState
import androidx.compose.foundation.MutatePriority
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.structuralEqualityPolicy

class SplitterState(
    initial: Float,
    minValue: Float = 0f,
    maxValue: Float = Float.POSITIVE_INFINITY,
    interactionState: InteractionState? = null
) : SingleDirectionMovable {

    private var _position = mutableStateOf(initial, structuralEqualityPolicy())

    var position: Float
        get() = _position.value
        internal set(newPosition) {
            _position.value = newPosition.coerceIn(minPosition, maxPosition)
            val newPercent = (position - minPosition) / (maxPosition - minPosition)
            if (!newPercent.isNaN()) {
                percent = newPercent
            }
        }

    var maxPosition: Float
        get() = _maxPosition.value
        internal set(newMax) {
            _maxPosition.value = newMax
            _position.value = percentPosition
        }

    private var _maxPosition = mutableStateOf(maxValue, structuralEqualityPolicy())

    var minPosition: Float
        get() = _minPosition.value
        internal set(newMin) {
            _minPosition.value = newMin
            _position.value = percentPosition
        }

    private var _minPosition = mutableStateOf(minValue, structuralEqualityPolicy())

    private val singleDirectionMovableState = movableState { onMove(it) }

    internal fun onMove(delta: Float) {
        position = (position + delta).coerceIn(minPosition, maxPosition)
    }

    override suspend fun move(
        movePriority: MutatePriority,
        block: suspend SingleDirectionMoveScope.() -> Unit) = singleDirectionMovableState.move(movePriority,block)

    override val isMoveInProgress: Boolean
        get() = singleDirectionMovableState.isMoveInProgress

    private var percent: Float = 0f

    private val percentPosition: Float
        get() = ((maxPosition - minPosition) * percent).coerceIn(minPosition, maxPosition)

}

internal class SplitterPosition(
    position: Float = 0f,
    maxValue: Float = Float.POSITIVE_INFINITY,
    minValue: Float = 0f
) {
    var maxValue = maxValue
        set(newMaxValue) {
            field = newMaxValue
            if (position > newMaxValue) {
                position = newMaxValue
            }
        }

    var minValue = minValue
        set(newMinValue) {
            field = newMinValue
            if (position < newMinValue) {
                position = newMinValue
            }
        }

    var position = position
        set(newPosition) {
            field = newPosition.coerceIn(minValue,maxValue)
        }

    var percent: Float
        get() = (position - minValue) / (maxValue - minValue)
        set(newPercent) {
            if (newPercent in 0f..1f) {
                position = ((maxValue - minValue) * newPercent).coerceIn(minValue, maxValue)
            }
        }


}