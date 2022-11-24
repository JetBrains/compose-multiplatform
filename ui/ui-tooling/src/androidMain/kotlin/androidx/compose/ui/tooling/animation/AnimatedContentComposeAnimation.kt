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
import org.jetbrains.annotations.TestOnly

/**
 * [ComposeAnimation] of type [ComposeAnimationType.ANIMATED_CONTENT].
 */
internal class AnimatedContentComposeAnimation<T> private constructor(
    override val animationObject: Transition<T>,
    override val states: Set<Any>,
    override val label: String?
) : ComposeAnimation, TransitionBasedAnimation<T> {
    override val type = ComposeAnimationType.ANIMATED_CONTENT

    companion object {
        /**
         * [ComposeAnimationType] from ANIMATABLE to UNSUPPORTED are not available in previous
         * versions of the library. To avoid creating non-existing enum,
         * [AnimatedContentComposeAnimation] should only be instantiated if [ComposeAnimationType] API
         * for ANIMATED_CONTENT enum is available.
         */
        var apiAvailable = enumValues<ComposeAnimationType>().any { it.name == "ANIMATED_CONTENT" }
            private set

        /**
         * Parses the [Transition] into a [AnimatedContentComposeAnimation].
         * [Transition] can have nullable state.
         * Compose Tooling is not handling the case if [Transition.currentState] is null.
         */
        fun Transition<*>.parseAnimatedContent(): AnimatedContentComposeAnimation<*>? {
            if (!apiAvailable) return null
            return currentState?.let { state ->
                val states = state.javaClass.enumConstants?.toSet() ?: setOf(state)
                AnimatedContentComposeAnimation(this, states, label ?: state::class.simpleName)
            }
        }

        /** This method is for testing only. */
        @TestOnly
        fun testOverrideAvailability(override: Boolean) {
            apiAvailable = override
        }
    }
}