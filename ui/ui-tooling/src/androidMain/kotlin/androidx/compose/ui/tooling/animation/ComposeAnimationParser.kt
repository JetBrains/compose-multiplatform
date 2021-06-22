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
 * Parses this [Transition] into a [ComposeAnimation] of type
 * [ComposeAnimationType.TRANSITION_ANIMATION].
 */
internal fun Transition<Any>.parse(): ComposeAnimation {
    Log.d("ComposeAnimationParser", "Transition subscribed")
    val initialState = segment.initialState
    val states = initialState.javaClass.enumConstants?.toSet() ?: setOf(initialState)
    val transitionLabel = label ?: initialState::class.simpleName
    val transition = this
    return object : ComposeAnimation {
        override val type: ComposeAnimationType
            get() = ComposeAnimationType.TRANSITION_ANIMATION

        override val animationObject: Any
            get() = transition

        override val states: Set<Any>
            get() = states

        override val label: String?
            get() = transitionLabel
    }
}
