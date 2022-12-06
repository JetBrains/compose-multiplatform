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

import androidx.compose.animation.core.InfiniteTransition
import androidx.compose.animation.tooling.ComposeAnimation
import androidx.compose.animation.tooling.ComposeAnimationType
import org.jetbrains.annotations.TestOnly

/**
 * [ComposeAnimation] of type [ComposeAnimationType.INFINITE_TRANSITION].
 */
internal class InfiniteTransitionComposeAnimation
private constructor(
    private val toolingState: ToolingState<Long>,
    override val animationObject: InfiniteTransition,
) : ComposeAnimation {
    override val type = ComposeAnimationType.INFINITE_TRANSITION

    override val states: Set<Any> = setOf(0)

    override val label: String = animationObject.label

    fun setTimeNanos(playTimeNanos: Long) {
        toolingState.value = playTimeNanos
    }

    companion object {

        /**
         * [ComposeAnimationType] from ANIMATABLE to UNSUPPORTED are not available in previous
         * versions of the library. To avoid creating non-existing enum,
         * [InfiniteTransitionComposeAnimation] should only be instantiated if
         * [ComposeAnimationType] API for INFINITE_TRANSITION enum is available.
         */
        var apiAvailable =
            enumValues<ComposeAnimationType>().any { it.name == "INFINITE_TRANSITION" }
            private set

        internal fun AnimationSearch.InfiniteTransitionSearchInfo.parse():
            InfiniteTransitionComposeAnimation? {
            if (!apiAvailable) return null
            return InfiniteTransitionComposeAnimation(
                toolingState, infiniteTransition
            )
        }

        /** This method is for testing only. */
        @TestOnly
        fun testOverrideAvailability(override: Boolean) {
            apiAvailable = override
        }
    }
}