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
 * Possible reasons for [Animatable]s to end.
 */
enum class AnimationEndReason {
    /**
     * Animation will be forced to end when its value reaches upper/lower bound (if they have
     * been defined, e.g. via [Animatable.updateBounds])
     *
     * Unlike [Finished], when an animation ends due to [BoundReached], it often falls short
     * from its initial target, and the remaining velocity is often non-zero. Both the end value
     * and the remaining velocity can be obtained via [AnimationResult].
     */
    BoundReached,
    /**
     * Animation has finished successfully without any interruption.
     */
    Finished
}