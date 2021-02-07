package org.jetbrains.compose.animation

import androidx.compose.animation.core.AnimatedFloat
import androidx.compose.animation.core.AnimationClockObservable
import androidx.compose.animation.core.Spring

// TODO possibly should be removed when|if @see androidx.compose.foundation.gestures.DeltaAnimatedFloat will be public
//    otherwise should become public instead
internal class DeltaAnimatedFloat(
    initial: Float,
    clock: AnimationClockObservable,
    private val onDelta: (Float) -> Float
) : AnimatedFloat(clock, Spring.DefaultDisplacementThreshold) {

    override var value = initial
        set(value) {
            if (isRunning) {
                val delta = value - field
                onDelta(delta)
            }
            field = value
        }
}