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

package androidx.compose.foundation.gestures

import androidx.compose.animation.core.AnimationConstants.UnspecifiedTime
import androidx.compose.animation.core.AnimationState
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.spring
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.MotionDurationScale
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract
import kotlin.coroutines.coroutineContext
import kotlin.math.absoluteValue
import kotlin.math.roundToLong

/**
 * Holds state for an [animation][animateToZero] that will continuously animate a float [value] to
 * zero.
 *
 * Unlike the standard [AnimationState], this class allows the value to be changed while the
 * animation is running. When that happens, the next frame will continue animating the new value
 * to zero as though the previous animation was interrupted and restarted with the new value. See
 * the docs on [animateToZero] for more information.
 *
 * An analogy for how this animation works is gravity – you can pick something up, and as soon as
 * you let it go it will start falling to the ground. If you catch it and raise it higher, it will
 * continue falling from the new height.
 *
 * Similar behavior could be achieved by using an [AnimationState] and creating a new copy and
 * launching a new coroutine to call `animateTo(0f)` every time the value changes. However, this
 * class doesn't require allocating a new state object and launching/cancelling a coroutine to
 * update the value, which makes for a more convenient API for this particular use case, and makes
 * it cheaper to update [value] on every frame.
 */
internal class UpdatableAnimationState {

    private var lastFrameTime = UnspecifiedTime
    private var lastVelocity = ZeroVector
    private var isRunning = false

    /**
     * The value to be animated. This property will be changed on every frame while [animateToZero]
     * is running, and will be set to exactly 0f before it returns. Unlike [AnimationState], this
     * property is mutable – it can be changed it any time during the animation, and the animation
     * will continue running from the new value on the next frame.
     *
     * Simply setting this property will not start the animation – [animateToZero] must be manually
     * invoked to kick off the animation, but once it's running it does not need to be called again
     * when this property is changed, until the animation finishes.
     */
    var value: Float = 0f

    /**
     * Starts animating [value] to 0f. This function will suspend until [value] actually reaches
     * 0f – e.g. if [value] is reset to a non-zero value on every frame, it will never return. When
     * this function does return, [value] will have been set to exactly 0f.
     *
     * If this function is called more than once concurrently, it will throw.
     *
     * @param beforeFrame Called _inside_ the choreographer callback on every frame with the
     * difference between the previous value and the new value. This corresponds to the typical
     * frame callback used in the other animation APIs and [withFrameNanos]. It runs before
     * composition, layout, and other passes for the frame.
     * @param afterFrame Called _outside_ the choreographer callback for every frame, _after_ the
     * composition and layout passes have finished running for that frame. This function allows the
     * caller to update [value] based on any layout changes performed in [beforeFrame].
     */
    @OptIn(ExperimentalContracts::class)
    suspend fun animateToZero(
        beforeFrame: (valueDelta: Float) -> Unit,
        afterFrame: () -> Unit,
    ) {
        contract { callsInPlace(beforeFrame) }
        check(!isRunning)

        val durationScale = coroutineContext[MotionDurationScale]?.scaleFactor ?: 1f
        isRunning = true

        try {
            // Don't rely on the animation's duration vs playtime to calculate completion since the
            // value could be updated after each frame, and if that happens we need to continue
            // running the animation.
            while (!value.isZeroish()) {
                withFrameNanos { frameTime ->
                    if (lastFrameTime == UnspecifiedTime) {
                        lastFrameTime = frameTime
                    }

                    val vectorizedCurrentValue = AnimationVector1D(value)
                    val playTime = if (durationScale == 0f) {
                        // The duration scale will be 0 when animations are disabled via a11y
                        // settings or developer settings.
                        RebasableAnimationSpec.getDurationNanos(
                            initialValue = AnimationVector1D(value),
                            targetValue = ZeroVector,
                            initialVelocity = lastVelocity
                        )
                    } else {
                        ((frameTime - lastFrameTime) / durationScale).roundToLong()
                    }
                    val newValue = RebasableAnimationSpec.getValueFromNanos(
                        playTimeNanos = playTime,
                        initialValue = vectorizedCurrentValue,
                        targetValue = ZeroVector,
                        initialVelocity = lastVelocity
                    ).value
                    lastVelocity = RebasableAnimationSpec.getVelocityFromNanos(
                        playTimeNanos = playTime,
                        initialValue = vectorizedCurrentValue,
                        targetValue = ZeroVector,
                        initialVelocity = lastVelocity
                    )
                    lastFrameTime = frameTime

                    val delta = value - newValue
                    value = newValue
                    beforeFrame(delta)
                }
                afterFrame()

                if (durationScale == 0f) {
                    // Never run more than one loop when animations are disabled.
                    break
                }
            }

            // The last iteration of the loop may have called block with a non-zero value due to
            // the visibility threshold, so ensure it gets called one last time with actual zero.
            if (value.absoluteValue != 0f) {
                withFrameNanos {
                    val delta = value
                    // Update the value before invoking the callback so that the callback will see
                    // the correct value if it looks at it.
                    value = 0f
                    beforeFrame(delta)
                }
                afterFrame()
            }
        } finally {
            lastFrameTime = UnspecifiedTime
            lastVelocity = ZeroVector
            isRunning = false
        }
    }

    private companion object {
        const val VisibilityThreshold = 0.01f
        val ZeroVector = AnimationVector1D(0f)

        /**
         * Only the spring spec actually supports the way this class runs the animation, so we
         * don't allow other specs to be passed in.
         */
        val RebasableAnimationSpec = spring<Float>().vectorize(Float.VectorConverter)

        fun Float.isZeroish() = absoluteValue < VisibilityThreshold
    }
}