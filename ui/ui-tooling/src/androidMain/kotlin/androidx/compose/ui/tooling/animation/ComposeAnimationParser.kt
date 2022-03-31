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

package androidx.compose.ui.tooling.animation

import android.util.Log
import androidx.compose.animation.core.Transition
import androidx.compose.animation.tooling.ComposeAnimation
import androidx.compose.animation.tooling.ComposeAnimationType

// TODO(b/160126628): support other animation types, e.g. single animated value
/**
 * Parses this [Transition] into a [TransitionComposeAnimation].
 */
internal fun Transition<Any>.parse(): TransitionComposeAnimation {
    Log.d("ComposeAnimationParser", "Transition subscribed")
    val initialState = segment.initialState
    val states = initialState.javaClass.enumConstants?.toSet() ?: setOf(initialState)
    return TransitionComposeAnimation(this, states, label ?: initialState::class.simpleName)
}

/**
 * Parses this [Transition] into an [AnimatedVisibilityComposeAnimation].
 */
internal fun Transition<Any>.parseAnimatedVisibility(): AnimatedVisibilityComposeAnimation {
    Log.d("ComposeAnimationParser", "AnimatedVisibility transition subscribed")
    return AnimatedVisibilityComposeAnimation(this, this.label ?: "AnimatedVisibility")
}

/**
 * [ComposeAnimation] of type [ComposeAnimationType.TRANSITION_ANIMATION].
 */
internal class TransitionComposeAnimation(
    transition: Transition<Any>,
    transitionStates: Set<Any>,
    transitionLabel: String?
) : ComposeAnimation {
    override val type = ComposeAnimationType.TRANSITION_ANIMATION
    override val animationObject: Transition<Any> = transition
    override val states = transitionStates
    override val label = transitionLabel
}

/**
 * [ComposeAnimation] of type [ComposeAnimationType.ANIMATED_VISIBILITY].
 */
internal class AnimatedVisibilityComposeAnimation(parent: Transition<Any>, parentLabel: String?) :
    ComposeAnimation {
    override val type = ComposeAnimationType.ANIMATED_VISIBILITY
    override val animationObject: Transition<Any> = parent
    override val states = setOf(AnimatedVisibilityState.Enter, AnimatedVisibilityState.Exit)
    override val label = parentLabel
    // Important assumption: AnimatedVisibility has a single child transition, which is a
    // Transition<EnterExitState>.
    @Suppress("UNCHECKED_CAST")
    val childTransition: Transition<Any>?
        get() = animationObject.transitions.getOrNull(0) as? Transition<Any>
}

/**
 * Represents the states of [AnimatedVisibilityComposeAnimation]s.
 */
@JvmInline
internal value class AnimatedVisibilityState private constructor(val value: String) {

    override fun toString() = value

    companion object {
        val Enter = AnimatedVisibilityState("Enter")
        val Exit = AnimatedVisibilityState("Exit")
    }
}