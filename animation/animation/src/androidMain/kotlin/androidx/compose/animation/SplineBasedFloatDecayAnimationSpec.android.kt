/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.compose.animation

import android.view.ViewConfiguration
import androidx.compose.animation.core.DecayAnimationSpec
import androidx.compose.animation.core.FloatDecayAnimationSpec
import androidx.compose.animation.core.generateDecayAnimationSpec
import androidx.compose.ui.unit.Density
import kotlin.math.sign

/**
 * A native Android fling curve decay.
 *
 * @param density density of the display
 */
class SplineBasedFloatDecayAnimationSpec(density: Density) : FloatDecayAnimationSpec {

    private val flingCalculator = AndroidFlingCalculator(
        ViewConfiguration.getScrollFriction(),
        density
    )

    override val absVelocityThreshold: Float get() = 0f

    private fun flingDistance(startVelocity: Float): Float =
        flingCalculator.flingDistance(startVelocity) * sign(startVelocity)

    override fun getTargetValue(initialValue: Float, initialVelocity: Float): Float =
        initialValue + flingDistance(initialVelocity)

    @Suppress("MethodNameUnits")
    override fun getValueFromNanos(
        playTimeNanos: Long,
        initialValue: Float,
        initialVelocity: Float
    ): Float {
        val playTimeMillis = playTimeNanos / 1_000_000L
        return initialValue + flingCalculator.flingInfo(initialVelocity).position(playTimeMillis)
    }

    @Suppress("MethodNameUnits")
    override fun getDurationNanos(initialValue: Float, initialVelocity: Float): Long =
        flingCalculator.flingDuration(initialVelocity) * 1_000_000L

    @Suppress("MethodNameUnits")
    override fun getVelocityFromNanos(
        playTimeNanos: Long,
        initialValue: Float,
        initialVelocity: Float
    ): Float {
        val playTimeMillis = playTimeNanos / 1_000_000L
        return flingCalculator.flingInfo(initialVelocity).velocity(playTimeMillis)
    }
}

/**
 * Creates a [DecayAnimationSpec] using the native Android fling decay. This can then be used to
 * animate any type [T].
 *
 * @param density density of the display
 */
fun <T> splineBasedDecay(density: Density): DecayAnimationSpec<T> =
    SplineBasedFloatDecayAnimationSpec(density).generateDecayAnimationSpec()