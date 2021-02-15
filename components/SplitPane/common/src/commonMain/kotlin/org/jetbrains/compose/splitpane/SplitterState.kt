package org.jetbrains.compose.splitpane

import androidx.compose.foundation.Interaction
import androidx.compose.foundation.InteractionState
import androidx.compose.foundation.MutatePriority
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.structuralEqualityPolicy
import org.jetbrains.compose.movable.SingleDirectionMovable
import org.jetbrains.compose.movable.SingleDirectionMoveScope
import org.jetbrains.compose.movable.movableState

class SplitterState(
    initialPosition: Float,
    minPosition: Float = 0f,
    maxPosition: Float = Float.POSITIVE_INFINITY,
    private val interactionState: InteractionState
) : SingleDirectionMovable {

    private val _position = mutableStateOf(initialPosition, structuralEqualityPolicy())

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

    private val _maxPosition = mutableStateOf(maxPosition, structuralEqualityPolicy())

    var minPosition: Float
        get() = _minPosition.value
        internal set(newMin) {
            _minPosition.value = newMin
            _position.value = percentPosition
        }

    private val _minPosition = mutableStateOf(minPosition, structuralEqualityPolicy())

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