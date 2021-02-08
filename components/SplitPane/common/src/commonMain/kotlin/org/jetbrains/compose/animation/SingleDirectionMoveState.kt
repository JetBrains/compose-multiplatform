package org.jetbrains.compose.animation

import androidx.compose.foundation.InteractionState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.structuralEqualityPolicy

class SingleDirectionMoveState(
    initial: Float,
    borders: ClosedFloatingPointRange<Float> = 0f..Float.POSITIVE_INFINITY,
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

    private var _maxValueState = mutableStateOf(borders.endInclusive, structuralEqualityPolicy())

    var minValue: Float
        get() = _minValueState.value
        internal set(newMin) {
            _minValueState.value = when {
                newMin < 0f -> 0f
                newMin > maxValue -> maxValue
                else -> newMin
            }
            if (value <= newMin) {
                value = newMin
            }
        }

    private var _minValueState = mutableStateOf(borders.start, structuralEqualityPolicy())

    private val singleDirectionMoveScope = object : SingleDirectionMoveScope {
        override fun moveBy(pixels: Float): Float {
            TODO("Not yet implemented")
        }
    }

    override suspend fun move(
        block: suspend SingleDirectionMoveScope.() -> Unit
    ) {

    }

    override val isMoveInProgress: Boolean
        get() = TODO("Not yet implemented")
}