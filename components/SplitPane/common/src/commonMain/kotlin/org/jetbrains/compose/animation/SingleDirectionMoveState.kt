package org.jetbrains.compose.animation

import androidx.compose.animation.core.AnimationClockObservable
import androidx.compose.animation.core.AnimationEndReason
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.SpringSpec
import androidx.compose.foundation.InteractionState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.structuralEqualityPolicy

class SingleDirectionMoveState(
    initial: Float,
    borders: ClosedFloatingPointRange<Float> = 0f..Float.POSITIVE_INFINITY,
    animationClock: AnimationClockObservable,
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

    internal val singleDirectionMoveAnimationController =
        SingleDirectionMoveAnimationController(
            animationClockObservable = animationClock,
            consumeMoveDelta = {
                val absolute = (value + it)
                val newValue = absolute.coerceIn(minValue, maxValue)
                if (absolute != newValue) { stopAnimation() }
                val consumed = newValue - value
                value += consumed
                consumed
            },
            interactionState = interactionState
        )

    override suspend fun move(
        block: suspend SingleDirectionMoveScope.() -> Unit
    ): Unit = singleDirectionMoveAnimationController.move(block)

    fun stopAnimation() {
        singleDirectionMoveAnimationController.stopAnimation()
    }

    val isAnimationRunning
        get() = singleDirectionMoveAnimationController.isAnimationRunning

    fun smoothMoveTo(
        value: Float,
        spec: AnimationSpec<Float> = SpringSpec(),
        onEnd: (endReason: AnimationEndReason, finishValue: Float) -> Unit = { _, _->}
    ) {
        smoothMoveBy(value - this.value, spec, onEnd)
    }

    fun smoothMoveBy(
        value: Float,
        spec: AnimationSpec<Float> = SpringSpec(),
        onEnd: (endReason: AnimationEndReason, finishValue: Float) -> Unit = { _, _->}
    ) {
        singleDirectionMoveAnimationController.smoothMoveBy(value,spec, onEnd)
    }

    fun moveTo(value: Float) {
        this.value = value.coerceIn(minValue, maxValue)
    }

    fun moveBy(value: Float) {
        moveTo(this.value + value)
    }

}