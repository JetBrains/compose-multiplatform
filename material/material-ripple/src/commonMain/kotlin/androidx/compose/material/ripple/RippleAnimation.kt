/*
 * Copyright 2019 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.compose.material.ripple

import androidx.compose.animation.OffsetPropKey
import androidx.compose.animation.core.AnimationClockObservable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.FloatPropKey
import androidx.compose.animation.core.InterruptionHandling
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.TransitionAnimation
import androidx.compose.animation.core.createAnimation
import androidx.compose.animation.core.transitionDefinition
import androidx.compose.animation.core.tween
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.inMilliseconds
import androidx.compose.ui.unit.milliseconds
import kotlin.math.max

/**
 * [RippleAnimation]s are drawn as part of [Ripple] as a visual indicator for an
 * different [androidx.compose.foundation.Interaction]s.
 *
 * Use [androidx.compose.foundation.clickable] or [androidx.compose.foundation.indication] to add a
 * ripple to your component, which contains a RippleAnimation for pressed states, and
 * a state layer for other states.
 *
 * This is a default implementation based on the Material Design specification.
 *
 * Draws a circular ripple effect with an origin starting at the input touch point and with a
 * radius expanding from 60% of the final value. The ripple origin animates to the center of its
 * target layout for the bounded version and stays in the center for the unbounded one.
 *
 * @param size The size of the target layout.
 * @param startPosition The position the animation will start from.
 * @param radius Effects grow up to this size.
 * @param clipped If true the effect should be clipped by the target layout bounds.
 * @param clock The animation clock observable that will drive this ripple effect
 * @param onAnimationFinished Call when the effect animation has been finished.
 */
internal class RippleAnimation(
    size: Size,
    startPosition: Offset,
    radius: Float,
    private val clipped: Boolean,
    clock: AnimationClockObservable,
    private val onAnimationFinished: (RippleAnimation) -> Unit
) {

    private val animation: TransitionAnimation<RippleTransition.State>
    private var transitionState = RippleTransition.State.Initial
    private var finishRequested = false
    private var animationPulse by mutableStateOf(0L)

    init {
        val surfaceSize = size
        val startRadius = getRippleStartRadius(surfaceSize)
        val targetRadius = radius

        val center = Offset(size.width / 2.0f, size.height / 2.0f)
        animation = RippleTransition.definition(
            startRadius = startRadius,
            endRadius = targetRadius,
            startCenter = startPosition,
            endCenter = center
        ).createAnimation(clock)
        animation.onUpdate = {
            // TODO We shouldn't need this animationPulse hack b/152631516
            animationPulse++
        }
        animation.onStateChangeFinished = { stage ->
            transitionState = stage
            if (transitionState == RippleTransition.State.Finished) {
                onAnimationFinished(this)
            }
        }
        // currently we are in Initial state, now we start the animation:
        animation.toState(RippleTransition.State.Revealed)
    }

    fun finish() {
        finishRequested = true
        animation.toState(RippleTransition.State.Finished)
    }

    fun DrawScope.draw(color: Color) {
        animationPulse // model read so we will be redrawn with the next animation values

        val alpha = if (transitionState == RippleTransition.State.Initial && finishRequested) {
            // if we still fading-in we should immediately switch to the final alpha.
            1f
        } else {
            animation[RippleTransition.Alpha]
        }

        val centerOffset = animation[RippleTransition.Center]
        val radius = animation[RippleTransition.Radius]

        val modulatedColor = color.copy(alpha = color.alpha * alpha)
        if (clipped) {
            clipRect {
                drawCircle(modulatedColor, radius, centerOffset)
            }
        } else {
            drawCircle(modulatedColor, radius, centerOffset)
        }
    }
}

/**
 * The Ripple transition specification.
 */
private object RippleTransition {

    enum class State {
        /** The starting state.  */
        Initial,
        /** User is still touching the surface.  */
        Revealed,
        /** User stopped touching the surface.  */
        Finished
    }

    private val FadeInDuration = 75.milliseconds
    private val RadiusDuration = 225.milliseconds
    private val FadeOutDuration = 150.milliseconds

    val Alpha = FloatPropKey()
    val Radius = FloatPropKey()
    val Center = OffsetPropKey()

    fun definition(
        startRadius: Float,
        endRadius: Float,
        startCenter: Offset,
        endCenter: Offset
    ) = transitionDefinition<State> {
        state(State.Initial) {
            this[Alpha] = 0f
            this[Radius] = startRadius
            this[Center] = startCenter
        }
        state(State.Revealed) {
            this[Alpha] = 1f
            this[Radius] = endRadius
            this[Center] = endCenter
        }
        state(State.Finished) {
            this[Alpha] = 0f
            // the rest are the same as for Revealed
            this[Radius] = endRadius
            this[Center] = endCenter
        }
        transition(State.Initial to State.Revealed) {
            Alpha using tween(
                durationMillis = FadeInDuration.inMilliseconds().toInt(),
                easing = LinearEasing
            )
            Radius using tween(
                durationMillis = RadiusDuration.inMilliseconds().toInt(),
                easing = FastOutSlowInEasing
            )
            Center using tween(
                durationMillis = RadiusDuration.inMilliseconds().toInt(),
                easing = LinearEasing
            )
            // we need to always finish the radius animation before starting fading out
            interruptionHandling = InterruptionHandling.UNINTERRUPTIBLE
        }
        transition(State.Revealed to State.Finished) {
            fun <T> toFinished() =
                tween<T>(
                    durationMillis = FadeOutDuration.inMilliseconds().toInt(),
                    easing = LinearEasing
                )

            Alpha using toFinished()
            Radius using toFinished()
            Center using toFinished()
        }
    }
}

/**
 * According to specs the starting radius is equal to 60% of the largest dimension of the
 * surface it belongs to.
 */
internal fun getRippleStartRadius(size: Size) =
    max(size.width, size.height) * 0.3f

/**
 * According to specs the ending radius
 * - expands to 10dp beyond the border of the surface it belongs to for bounded ripples
 * - fits within the border of the surface it belongs to for unbounded ripples
 */
internal fun Density.getRippleEndRadius(bounded: Boolean, size: Size): Float {
    val radiusCoveringBounds =
        (Offset(size.width, size.height).getDistance() / 2f)
    return if (bounded) {
        radiusCoveringBounds + BoundedRippleExtraRadius.toPx()
    } else {
        radiusCoveringBounds
    }
}

private val BoundedRippleExtraRadius = 10.dp
