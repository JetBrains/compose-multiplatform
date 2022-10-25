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

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.AnimationVector
import androidx.compose.animation.tooling.ComposeAnimation
import androidx.compose.animation.tooling.ComposeAnimationType
import org.jetbrains.annotations.TestOnly

/**
 * [ComposeAnimation] of type [ComposeAnimationType.ANIMATE_X_AS_STATE].
 */
internal class AnimateXAsStateComposeAnimation<T, V : AnimationVector>
private constructor(
    val toolingState: ToolingState<T>,
    val animationSpec: AnimationSpec<T>,
    override val animationObject: Animatable<T, V>,
) : ComposeAnimation {
    override val type = ComposeAnimationType.ANIMATE_X_AS_STATE

    override val states: Set<Any> = (animationObject.value as Any).let {
        it.javaClass.enumConstants?.toSet() ?: setOf(it)
    }

    override val label: String = animationObject.label

    @Suppress("UNCHECKED_CAST")
    fun setState(value: Any) {
        toolingState.value = value as T
    }

    companion object {

        /**
         * [ComposeAnimationType] from ANIMATABLE to UNSUPPORTED are not available in previous
         * versions of the library. To avoid creating non-existing enum,
         * [UnsupportedComposeAnimation] should only be instantiated if [ComposeAnimationType] API
         * for UNSUPPORTED enum is available.
         */
        var apiAvailable = enumValues<ComposeAnimationType>().any { it.name == "UNSUPPORTED" }
            private set

        internal fun <T, V : AnimationVector> AnimationSearch
        .AnimateXAsStateSearchInfo<T, V>.parse():
            AnimateXAsStateComposeAnimation<*, *>? {
            if (!apiAvailable) return null
            // Tooling can't control nullable Animatable with value set to null.
            if (animatable.value == null) return null
            return AnimateXAsStateComposeAnimation(
                toolingState, animationSpec, animatable
            )
        }

        /** This method is for testing only. */
        @TestOnly
        fun testOverrideAvailability(override: Boolean) {
            apiAvailable = override
        }
    }
}