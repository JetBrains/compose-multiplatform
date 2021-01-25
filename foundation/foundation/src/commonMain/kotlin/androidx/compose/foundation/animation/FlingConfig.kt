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

@file:Suppress("DEPRECATION")

package androidx.compose.foundation.animation

import androidx.compose.animation.core.AnimatedFloat
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.FloatDecayAnimationSpec
import androidx.compose.animation.core.FloatExponentialDecaySpec
import androidx.compose.animation.core.OnAnimationEnd
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.TargetAnimation
import androidx.compose.animation.core.fling
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import kotlin.math.abs

/**
 * Class to specify fling behavior.
 *
 * When drag has ended, this class specifies what to do given the velocity
 * with which drag ended and AnimatedFloat instance to perform fling on and read current value.
 *
 * Config that provides natural fling with customizable behaviour
 * e.g fling friction or result target adjustment.
 *
 * If you want to only be able to drag/animate between predefined set of values,
 * consider using [FlingConfig] function with anchors to generate such behaviour.
 *
 * @param decayAnimation the animation to control fling behaviour
 * @param adjustTarget callback to be called at the start of fling
 * so the final value for fling can be adjusted
 */
@Immutable
data class FlingConfig(
    val decayAnimation: FloatDecayAnimationSpec,
    val adjustTarget: (Float) -> TargetAnimation? = { null }
)

/**
 * Default [FlingConfig] curve.
 *
 * @param adjustTarget callback to be called at the start of fling so the final value for fling
 * can be adjusted
 */
@Composable
fun defaultFlingConfig(
    adjustTarget: (Float) -> TargetAnimation? = { null }
): FlingConfig = actualFlingConfig(adjustTarget)

@Composable
internal expect fun actualFlingConfig(adjustTarget: (Float) -> TargetAnimation?): FlingConfig

/**
 * Create fling config with anchors will make sure that after drag has ended,
 * the value will be animated to one of the points from the predefined list.
 *
 * It takes velocity into account, though value will be animated to the closest
 * point in provided list considering velocity.
 *
 * @param anchors set of anchors to animate to
 * @param animationSpec animation which will be used for animations
 * @param decayAnimation decay animation to be used to calculate closest point in the anchors set
 * considering velocity.
 */
fun FlingConfig(
    anchors: List<Float>,
    animationSpec: AnimationSpec<Float> = SpringSpec(),
    decayAnimation: FloatDecayAnimationSpec = FloatExponentialDecaySpec()
): FlingConfig {
    val adjustTarget: (Float) -> TargetAnimation? = { target ->
        val point = anchors.minByOrNull { abs(it - target) }
        val adjusted = point ?: target
        TargetAnimation(adjusted, animationSpec)
    }
    return FlingConfig(decayAnimation, adjustTarget)
}

/**
 * Starts a fling animation with the specified starting velocity and fling configuration.
 *
 * @param startVelocity Starting velocity of the fling animation
 * @param config configuration that specifies fling behaviour
 * @param onAnimationEnd callback to be invoked when fling finishes by decay
 * or being interrupted by gesture input.
 * Consider second boolean param "cancelled" to know what happened.
 */
internal fun AnimatedFloat.fling(
    startVelocity: Float,
    config: FlingConfig,
    onAnimationEnd: OnAnimationEnd? = null
) {
    fling(
        startVelocity,
        config.decayAnimation,
        config.adjustTarget,
        onAnimationEnd
    )
}