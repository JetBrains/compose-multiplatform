/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.compose.animation.graphics.vector

import androidx.compose.animation.core.AnimationVector
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.AnimationVector2D
import androidx.compose.animation.core.AnimationVector3D
import androidx.compose.animation.core.AnimationVector4D
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.TwoWayConverter
import androidx.compose.animation.core.VectorizedFiniteAnimationSpec

private const val MillisToNanos = 1_000_000L

/**
 * Returns this [FiniteAnimationSpec] reversed.
 */
internal fun <T> FiniteAnimationSpec<T>.reversed(durationMillis: Int): FiniteAnimationSpec<T> {
    return ReversedSpec(this, durationMillis)
}

private class ReversedSpec<T>(
    private val spec: FiniteAnimationSpec<T>,
    private val durationMillis: Int
) : FiniteAnimationSpec<T> {
    override fun <V : AnimationVector> vectorize(
        converter: TwoWayConverter<T, V>
    ): VectorizedFiniteAnimationSpec<V> {
        return VectorizedReversedSpec(spec.vectorize(converter), durationMillis * MillisToNanos)
    }
}

private class VectorizedReversedSpec<V : AnimationVector>(
    private val animation: VectorizedFiniteAnimationSpec<V>,
    private val durationNanos: Long
) : VectorizedFiniteAnimationSpec<V> {

    override fun getValueFromNanos(
        playTimeNanos: Long,
        initialValue: V,
        targetValue: V,
        initialVelocity: V
    ): V {
        return animation.getValueFromNanos(
            durationNanos - playTimeNanos,
            targetValue,
            initialValue,
            initialVelocity
        )
    }

    override fun getVelocityFromNanos(
        playTimeNanos: Long,
        initialValue: V,
        targetValue: V,
        initialVelocity: V
    ): V {
        return animation.getVelocityFromNanos(
            durationNanos - playTimeNanos,
            targetValue,
            initialValue,
            initialVelocity
        ).reversed()
    }

    override fun getDurationNanos(initialValue: V, targetValue: V, initialVelocity: V): Long {
        return durationNanos
    }
}

/**
 * Creates a [FiniteAnimationSpec] that combine and run multiple [specs] based on the start time
 * (in milliseconds) specified as the first half of the pairs.
 */
internal fun <T> combined(
    specs: List<Pair<Int, FiniteAnimationSpec<T>>>
): FiniteAnimationSpec<T> {
    return CombinedSpec(specs)
}

private class CombinedSpec<T>(
    private val specs: List<Pair<Int, FiniteAnimationSpec<T>>>
) : FiniteAnimationSpec<T> {

    override fun <V : AnimationVector> vectorize(
        converter: TwoWayConverter<T, V>
    ): VectorizedFiniteAnimationSpec<V> {
        return VectorizedCombinedSpec(
            specs.map { (timeMillis, spec) ->
                timeMillis * MillisToNanos to spec.vectorize(converter)
            }
        )
    }
}

private class VectorizedCombinedSpec<V : AnimationVector>(
    private val animations: List<Pair<Long, VectorizedFiniteAnimationSpec<V>>>
) : VectorizedFiniteAnimationSpec<V> {

    private fun chooseAnimation(playTimeNanos: Long): Pair<Long, VectorizedFiniteAnimationSpec<V>> {
        return animations.lastOrNull { (timeNanos, _) ->
            timeNanos <= playTimeNanos
        } ?: animations.first()
    }

    override fun getValueFromNanos(
        playTimeNanos: Long,
        initialValue: V,
        targetValue: V,
        initialVelocity: V
    ): V {
        val (timeNanos, animation) = chooseAnimation(playTimeNanos)
        val internalPlayTimeNanos = playTimeNanos - timeNanos
        return animation.getValueFromNanos(
            internalPlayTimeNanos,
            initialValue,
            targetValue,
            initialVelocity
        )
    }

    override fun getVelocityFromNanos(
        playTimeNanos: Long,
        initialValue: V,
        targetValue: V,
        initialVelocity: V
    ): V {
        val (timeNanos, animation) = chooseAnimation(playTimeNanos)
        return animation.getVelocityFromNanos(
            playTimeNanos - timeNanos,
            initialValue,
            targetValue,
            initialVelocity
        )
    }

    override fun getDurationNanos(initialValue: V, targetValue: V, initialVelocity: V): Long {
        val (timeNanos, animation) = animations.last()
        return timeNanos + animation.getDurationNanos(initialValue, targetValue, initialVelocity)
    }
}

private fun <V : AnimationVector> V.reversed(): V {
    @Suppress("UNCHECKED_CAST")
    return when (this) {
        is AnimationVector1D -> AnimationVector1D(value * -1) as V
        is AnimationVector2D -> AnimationVector2D(v1 * -1, v2 * -1) as V
        is AnimationVector3D -> AnimationVector3D(v1 * -1, v2 * -1, v3 * -1) as V
        is AnimationVector4D -> AnimationVector4D(v1 * -1, v2 * -1, v3 * -1, v4 * -1) as V
        else -> throw RuntimeException("Unknown AnimationVector: $this")
    }
}
