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

package androidx.compose.animation.core

/**
 * TargetAnimation class defines how to animate to a given target value.
 *
 * @param target Target value for the animation to animate to
 * @param animation The animation that will be used to animate to the target destination. This
 *                  animation defaults to a Spring Animation unless specified.
 */
data class TargetAnimation(
    val target: Float,
    val animation: AnimationSpec<Float> = SpringSpec()
)

/**
 * Possible reasons with which DynamicTargetAnimation can finish
 */
enum class AnimationEndReason {
    /**
     * Animation has successfully reached the [BaseAnimatedValue.targetValue] value
     * and come to stop
     */
    TargetReached,
    /**
     * Animation was interrupted, e.g by another animation
     */
    Interrupted,
    /**
     * Animation will be forced to end when its value reaches upper/lower bound (if they have
     * been defined, e.g via [AnimatedFloat.setBounds])
     *
     * Unlike [TargetReached], when an animation ends due to [BoundReached], it often falls short
     * from its initial target, and the remaining velocity is often non-zero. Both the end value
     * and the remaining velocity can be obtained via `onEnd` param in [AnimatedFloat.fling]
     * callback
     */
    BoundReached
}