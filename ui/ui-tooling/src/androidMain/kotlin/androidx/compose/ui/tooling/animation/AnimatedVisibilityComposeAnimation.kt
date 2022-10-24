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

package androidx.compose.ui.tooling.animation

import androidx.compose.animation.core.Transition
import androidx.compose.animation.tooling.ComposeAnimation
import androidx.compose.animation.tooling.ComposeAnimationType
import androidx.compose.ui.tooling.animation.states.AnimatedVisibilityState

/**
 * Parses the [Transition] into an [AnimatedVisibilityComposeAnimation].
 */
internal fun Transition<Boolean>.parseAnimatedVisibility(): AnimatedVisibilityComposeAnimation {
    return AnimatedVisibilityComposeAnimation(this, this.label ?: "AnimatedVisibility")
}

/**
 * [ComposeAnimation] of type [ComposeAnimationType.ANIMATED_VISIBILITY].
 */
internal class AnimatedVisibilityComposeAnimation(
    override val animationObject: Transition<Boolean>,
    override val label: String?
) :
    ComposeAnimation {
    override val type = ComposeAnimationType.ANIMATED_VISIBILITY
    override val states = setOf(AnimatedVisibilityState.Enter, AnimatedVisibilityState.Exit)

    // Important assumption: AnimatedVisibility has a single child transition, which is a
    // Transition<EnterExitState>.
    @Suppress("UNCHECKED_CAST")
    val childTransition: Transition<Any>?
        get() = animationObject.transitions.getOrNull(0) as? Transition<Any>
}