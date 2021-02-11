package org.jetbrains.compose.movable

import androidx.compose.foundation.InteractionState
import androidx.compose.foundation.MutatePriority
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.structuralEqualityPolicy

class SingleDirectionMoveState(
    initial: Float,
    minValue: Float = 0f,
    maxValue: Float = Float.POSITIVE_INFINITY,
    interactionState: InteractionState? = null
) : SingleDirectionMovable {

    var value by mutableStateOf(initial, structuralEqualityPolicy())
        internal set

    var maxValue: Float
        get() = _maxValueState.value
        internal set(newMax) {
            _maxValueState.value = newMax
            if (value > newMax) {
                value = newMax
            }
        }

    private var _maxValueState = mutableStateOf(maxValue, structuralEqualityPolicy())

    var minValue: Float
        get() = _minValueState.value
        internal set(newMin) {
            _minValueState.value = newMin
            if (value <= newMin) {
                value = newMin
            }
        }

    private var _minValueState = mutableStateOf(minValue, structuralEqualityPolicy())

    private val singleDirectionMovableState = movableState { onMove(it) }

    internal fun onMove(delta: Float): Float {
        TODO("Not yet implemented")
    }

    private val singleDirectionMoveScope = object : SingleDirectionMoveScope {
        override fun moveBy(pixels: Float): Float {
            TODO("Not yet implemented")
        }
    }

    override suspend fun move(movePriority: MutatePriority, block: suspend SingleDirectionMoveScope.() -> Unit) {
        TODO("Not yet implemented")
    }

    override val isMoveInProgress: Boolean
        get() = TODO("Not yet implemented")
}