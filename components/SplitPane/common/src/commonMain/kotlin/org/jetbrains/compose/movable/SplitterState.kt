package org.jetbrains.compose.movable

import androidx.compose.foundation.Interaction
import androidx.compose.foundation.InteractionState
import androidx.compose.foundation.MutatePriority
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.structuralEqualityPolicy

class SplitPaneState(
    val splitterState: SplitterState,
    enabled: Boolean
) {
    private var _moveEnabled = mutableStateOf(enabled, structuralEqualityPolicy())

    var moveEnabled: Boolean
        get() = _moveEnabled.value
        set(newValue) {
            _moveEnabled.value = newValue
        }

}

class SplitterState(
    initialPosition: Float,
    minPosition: Float = 0f,
    maxPosition: Float = Float.POSITIVE_INFINITY,
    private val interactionState: InteractionState
) : SingleDirectionMovable {

    private var _position = mutableStateOf(initialPosition, structuralEqualityPolicy())

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

    private var _maxPosition = mutableStateOf(maxPosition, structuralEqualityPolicy())

    var minPosition: Float
        get() = _minPosition.value
        internal set(newMin) {
            _minPosition.value = newMin
            _position.value = percentPosition
        }

    private var _minPosition = mutableStateOf(minPosition, structuralEqualityPolicy())

    private val singleDirectionMovableState = movableState(this::onMove)

    private fun onMove(delta: Float) {
        interactionState.addInteraction(Interaction.Dragged)
        position = (position + delta).coerceIn(minPosition, maxPosition)
        interactionState.removeInteraction(Interaction.Dragged)
    }

    override suspend fun move(
        movePriority: MutatePriority,
        block: suspend SingleDirectionMoveScope.() -> Unit) = singleDirectionMovableState.move(movePriority,block)

    override fun dispatchRawMovement(delta: Float) = singleDirectionMovableState.dispatchRawMovement(delta)

    override val isMoveInProgress: Boolean
        get() = singleDirectionMovableState.isMoveInProgress

    private var percent: Float = 0f

    private val percentPosition: Float
        get() = ((maxPosition - minPosition) * percent).coerceIn(minPosition, maxPosition)

}