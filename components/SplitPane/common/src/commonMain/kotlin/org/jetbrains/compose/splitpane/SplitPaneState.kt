package org.jetbrains.compose.splitpane

import androidx.compose.foundation.Interaction
import androidx.compose.foundation.InteractionState
import androidx.compose.foundation.MutatePriority
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.structuralEqualityPolicy
import org.jetbrains.compose.movable.SingleDirectionMovable
import org.jetbrains.compose.movable.SingleDirectionMoveScope
import org.jetbrains.compose.movable.movableState

interface SplitterPositionState {
    val positionPercentage: Float
}

interface EnableMoveState {
    var moveEnabled: Boolean
}

interface SplitPaneState : SingleDirectionMovable, SplitterPositionState, EnableMoveState

internal interface PositionBorders {
    var minPosition: Float
    var maxPosition: Float
}

internal class SplitPaneStateImpl(
    initialPosition: Float,
    moveEnabled: Boolean,
    private val interactionState: InteractionState
) : SplitPaneState, PositionBorders {

    private var _moveEnabled = mutableStateOf(moveEnabled, structuralEqualityPolicy())

    override var moveEnabled: Boolean
        get() = _moveEnabled.value
        set(newValue) {
            _moveEnabled.value = newValue
        }

    private val _positionPercentage = mutableStateOf(initialPosition, structuralEqualityPolicy())

    override var positionPercentage: Float
        get() = _positionPercentage.value
        internal set(newPosition) {
            _positionPercentage.value = newPosition
        }

    override var minPosition: Float = 0f

    override var maxPosition: Float = Float.POSITIVE_INFINITY

    private val singleDirectionMovableState = movableState(this::onMove)

    private fun onMove(delta: Float) {
        interactionState.addInteraction(Interaction.Dragged)
        val movableArea = maxPosition - minPosition
        if (movableArea > 0) {
            positionPercentage = when(positionPercentage) {
                0f -> if (delta>0) delta / movableArea  else 0f
                1f -> if (delta<0) (movableArea + delta) / movableArea else 1f
                else -> ((movableArea * positionPercentage) + delta) / movableArea
            }
        }
        interactionState.removeInteraction(Interaction.Dragged)
    }

    override suspend fun move(
        movePriority: MutatePriority,
        block: suspend SingleDirectionMoveScope.() -> Unit) = singleDirectionMovableState.move(movePriority,block)

    override fun dispatchRawMovement(delta: Float) = singleDirectionMovableState.dispatchRawMovement(delta)

    override val isMoveInProgress: Boolean
        get() = singleDirectionMovableState.isMoveInProgress

    private var percent: Float = 0f

}