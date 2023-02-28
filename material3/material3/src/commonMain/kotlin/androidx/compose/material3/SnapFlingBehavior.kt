/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.compose.material3

import androidx.compose.animation.core.AnimationScope
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.AnimationState
import androidx.compose.animation.core.AnimationVector
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.DecayAnimationSpec
import androidx.compose.animation.core.animateDecay
import androidx.compose.animation.core.animateTo
import androidx.compose.animation.core.calculateTargetValue
import androidx.compose.animation.core.copy
import androidx.compose.foundation.gestures.FlingBehavior
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.ScrollScope
import androidx.compose.foundation.lazy.LazyListItemInfo
import androidx.compose.foundation.lazy.LazyListLayoutInfo
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.ui.MotionDurationScale
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.fastSumBy
import kotlin.math.abs
import kotlin.math.absoluteValue
import kotlin.math.sign
import kotlinx.coroutines.withContext

/**
 * A [FlingBehavior] that snaps to the mostly visible item in the list of items. This behavior is
 * designed to be used when there is a single visible page.
 *
 * Note: This is a temporary fling behavior that will be removed once the framework's
 * `rememberSnapFlingBehavior` function is stable.
 *
 * @param lazyListState a [LazyListState]
 * @param decayAnimationSpec a [DecayAnimationSpec] that is used for the fling's decay animation
 * @param snapAnimationSpec an [AnimationSpec] that is used for the snap animation
 * @param density the current display [Density]
 */
// TODO(b/264687693): Replace with the framework's rememberSnapFlingBehavior ones it's stable.
@ExperimentalMaterial3Api
internal class SnapFlingBehavior(
    private val lazyListState: LazyListState,
    private val decayAnimationSpec: DecayAnimationSpec<Float>,
    private val snapAnimationSpec: AnimationSpec<Float>,
    private val density: Density
) : FlingBehavior {

    private val visibleItemsInfo: List<LazyListItemInfo>
        get() = lazyListState.layoutInfo.visibleItemsInfo

    private val itemSize: Float
        get() = if (visibleItemsInfo.isNotEmpty()) {
            visibleItemsInfo.fastSumBy { it.size } / visibleItemsInfo.size.toFloat()
        } else {
            0f
        }

    private val velocityThreshold = with(density) { MinFlingVelocityDp.toPx() }
    private var motionScaleDuration = object : MotionDurationScale {
        override val scaleFactor: Float
            get() = DefaultScrollMotionDurationScaleFactor
    }

    override suspend fun ScrollScope.performFling(initialVelocity: Float): Float {
        val (remainingOffset, remainingState) = fling(initialVelocity)

        // No remaining offset means we've used everything, no need to propagate velocity. Otherwise
        // we couldn't use everything (probably because we have hit the min/max bounds of the
        // containing layout) we should propagate the offset.
        return if (remainingOffset == 0f) 0f else remainingState.velocity
    }

    private suspend fun ScrollScope.fling(
        initialVelocity: Float
    ): AnimationResult<Float, AnimationVector1D> {
        // If snapping from scroll (short snap) or fling (long snap)
        val result = withContext(motionScaleDuration) {
            if (abs(initialVelocity) <= abs(velocityThreshold)) {
                shortSnap(initialVelocity)
            } else {
                longSnap(initialVelocity)
            }
        }

        return result
    }

    private suspend fun ScrollScope.shortSnap(
        velocity: Float
    ): AnimationResult<Float, AnimationVector1D> {

        val closestOffset = findClosestOffset(0f, lazyListState)

        val animationState = AnimationState(0f, velocity)
        return animateSnap(
            closestOffset,
            closestOffset,
            animationState,
            snapAnimationSpec
        )
    }

    private suspend fun ScrollScope.longSnap(
        initialVelocity: Float
    ): AnimationResult<Float, AnimationVector1D> {

        val offset =
            decayAnimationSpec.calculateTargetValue(0f, initialVelocity).absoluteValue

        val finalDecayOffset = (offset - itemSize).coerceAtLeast(0f)
        val initialOffset = if (finalDecayOffset == 0f) {
            finalDecayOffset
        } else {
            finalDecayOffset * initialVelocity.sign
        }

        val (remainingOffset, animationState) = runApproach(
            initialOffset,
            initialVelocity
        )

        return animateSnap(
            remainingOffset,
            remainingOffset,
            animationState.copy(value = 0f),
            snapAnimationSpec
        )
    }

    private suspend fun ScrollScope.runApproach(
        initialTargetOffset: Float,
        initialVelocity: Float
    ): AnimationResult<Float, AnimationVector1D> {
        val animationState = AnimationState(initialValue = 0f, initialVelocity = initialVelocity)
        val (_, currentAnimationState) = with(this) {
            animateDecay(initialTargetOffset, animationState, decayAnimationSpec)
        }
        val remainingOffset =
            findClosestOffset(currentAnimationState.velocity, lazyListState)
        // will snap the remainder
        return AnimationResult(remainingOffset, currentAnimationState)
    }

    override fun equals(other: Any?): Boolean {
        return if (other is SnapFlingBehavior) {
            other.snapAnimationSpec == this.snapAnimationSpec &&
                other.decayAnimationSpec == this.decayAnimationSpec &&
                other.lazyListState == this.lazyListState &&
                other.density == this.density
        } else {
            false
        }
    }

    override fun hashCode(): Int = 0
        .let { 31 * it + snapAnimationSpec.hashCode() }
        .let { 31 * it + decayAnimationSpec.hashCode() }
        .let { 31 * it + lazyListState.hashCode() }
        .let { 31 * it + density.hashCode() }

    private operator fun <T : Comparable<T>> ClosedFloatingPointRange<T>.component1(): T =
        this.start

    private operator fun <T : Comparable<T>> ClosedFloatingPointRange<T>.component2(): T =
        this.endInclusive

    private fun findClosestOffset(
        velocity: Float,
        lazyListState: LazyListState
    ): Float {

        fun Float.isValidDistance(): Boolean {
            return this != Float.POSITIVE_INFINITY && this != Float.NEGATIVE_INFINITY
        }

        fun calculateSnappingOffsetBounds(): ClosedFloatingPointRange<Float> {
            var lowerBoundOffset = Float.NEGATIVE_INFINITY
            var upperBoundOffset = Float.POSITIVE_INFINITY

            with(lazyListState.layoutInfo) {
                visibleItemsInfo.fastForEach { item ->
                    val offset =
                        calculateDistanceToDesiredSnapPosition(this, item)

                    // Find item that is closest to the center
                    if (offset <= 0 && offset > lowerBoundOffset) {
                        lowerBoundOffset = offset
                    }

                    // Find item that is closest to center, but after it
                    if (offset >= 0 && offset < upperBoundOffset) {
                        upperBoundOffset = offset
                    }
                }
            }

            return lowerBoundOffset.rangeTo(upperBoundOffset)
        }

        val (lowerBound, upperBound) = calculateSnappingOffsetBounds()

        val finalDistance = when (sign(velocity)) {
            0f -> {
                if (abs(upperBound) <= abs(lowerBound)) {
                    upperBound
                } else {
                    lowerBound
                }
            }

            1f -> upperBound
            -1f -> lowerBound
            else -> 0f
        }

        return if (finalDistance.isValidDistance()) {
            finalDistance
        } else {
            0f
        }
    }

    /**
     * Run a [DecayAnimationSpec] animation up to before [targetOffset] using [animationState]
     *
     * @param targetOffset The destination of this animation. Since this is a decay animation, we can
     * use this value to prevent the animation to run until the end.
     * @param animationState The previous [AnimationState] for continuation purposes.
     * @param decayAnimationSpec The [DecayAnimationSpec] that will drive this animation
     */
    private suspend fun ScrollScope.animateDecay(
        targetOffset: Float,
        animationState: AnimationState<Float, AnimationVector1D>,
        decayAnimationSpec: DecayAnimationSpec<Float>
    ): AnimationResult<Float, AnimationVector1D> {
        var previousValue = 0f

        fun AnimationScope<Float, AnimationVector1D>.consumeDelta(delta: Float) {
            val consumed = scrollBy(delta)
            if (abs(delta - consumed) > 0.5f) cancelAnimation()
        }

        animationState.animateDecay(
            decayAnimationSpec,
            sequentialAnimation = animationState.velocity != 0f
        ) {
            if (abs(value) >= abs(targetOffset)) {
                val finalValue = value.coerceToTarget(targetOffset)
                val finalDelta = finalValue - previousValue
                consumeDelta(finalDelta)
                cancelAnimation()
            } else {
                val delta = value - previousValue
                consumeDelta(delta)
                previousValue = value
            }
        }
        return AnimationResult(
            targetOffset - previousValue,
            animationState
        )
    }

    /**
     * Runs a [AnimationSpec] to snap the list into [targetOffset]. Uses [cancelOffset] to stop this
     * animation before it reaches the target.
     *
     * @param targetOffset The final target of this animation
     * @param cancelOffset If we'd like to finish the animation earlier we use this value
     * @param animationState The current animation state for continuation purposes
     * @param snapAnimationSpec The [AnimationSpec] that will drive this animation
     */
    private suspend fun ScrollScope.animateSnap(
        targetOffset: Float,
        cancelOffset: Float,
        animationState: AnimationState<Float, AnimationVector1D>,
        snapAnimationSpec: AnimationSpec<Float>
    ): AnimationResult<Float, AnimationVector1D> {
        var consumedUpToNow = 0f
        val initialVelocity = animationState.velocity
        animationState.animateTo(
            targetOffset,
            animationSpec = snapAnimationSpec,
            sequentialAnimation = (animationState.velocity != 0f)
        ) {
            val realValue = value.coerceToTarget(cancelOffset)
            val delta = realValue - consumedUpToNow
            val consumed = scrollBy(delta)
            // stop when unconsumed or when we reach the desired value
            if (abs(delta - consumed) > 0.5f || realValue != value) {
                cancelAnimation()
            }
            consumedUpToNow += consumed
        }

        // Always course correct velocity so they don't become too large.
        val finalVelocity = animationState.velocity.coerceToTarget(initialVelocity)
        return AnimationResult(
            targetOffset - consumedUpToNow,
            animationState.copy(velocity = finalVelocity)
        )
    }

    private fun Float.coerceToTarget(target: Float): Float {
        if (target == 0f) return 0f
        return if (target > 0) coerceAtMost(target) else coerceAtLeast(target)
    }

    private fun calculateDistanceToDesiredSnapPosition(
        layoutInfo: LazyListLayoutInfo,
        item: LazyListItemInfo
    ): Float {
        val containerSize =
            with(layoutInfo) { singleAxisViewportSize - beforeContentPadding - afterContentPadding }

        val desiredDistance =
            containerSize.toFloat() / 2 - item.size.toFloat() / 2 // snap to center

        val itemCurrentPosition = item.offset
        return itemCurrentPosition - desiredDistance
    }

    private val LazyListLayoutInfo.singleAxisViewportSize: Int
        get() = if (orientation == Orientation.Vertical) viewportSize.height else viewportSize.width

    private val DefaultScrollMotionDurationScaleFactor = 1f

    private val MinFlingVelocityDp = 400.dp
}

private class AnimationResult<T, V : AnimationVector>(
    val remainingOffset: T,
    val currentAnimationState: AnimationState<T, V>
) {
    operator fun component1(): T = remainingOffset
    operator fun component2(): AnimationState<T, V> = currentAnimationState
}
