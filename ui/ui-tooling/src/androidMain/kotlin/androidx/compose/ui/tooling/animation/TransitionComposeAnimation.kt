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

/**
 * Parses the [Transition] into a [TransitionComposeAnimation].
 * [Transition] can have nullable state.
 * Compose Tooling is not handling the case if [Transition.currentState] is null.
 */
internal fun Transition<*>.parse(): TransitionComposeAnimation<*>? {
    return currentState?.let { state ->
        val states = state.javaClass.enumConstants?.toSet() ?: setOf(state)
        TransitionComposeAnimation(this, states, label ?: state::class.simpleName)
    }
}

/**
 * [ComposeAnimation] of type [ComposeAnimationType.TRANSITION_ANIMATION].
 */
internal class TransitionComposeAnimation<T>(
    override val animationObject: Transition<T>,
    override val states: Set<Any>,
    override val label: String?
) : ComposeAnimation, TransitionBasedAnimation<T> {
    override val type = ComposeAnimationType.TRANSITION_ANIMATION
}