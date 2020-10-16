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

package androidx.compose.material

import androidx.compose.animation.core.AnimatedValue
import androidx.compose.animation.core.AnimationVector

// TODO: b/171041025 replace if/when similar functionality is added to the AnimatedValue APIs
/**
 * A lazy wrapper around [AnimatedValue] that delays creating the [AnimatedValue] until the
 * initial value / target is known. This is similar to [androidx.compose.animation.animate], but
 * can be used outside of a Composable function.
 *
 * @property factory lazily invoked factory to create an [AnimatedValue] for the given target
 */
internal class LazyAnimatedValue<T, V : AnimationVector>(
    private val factory: (target: T) -> AnimatedValue<T, V>
) {
    private var animatedValue: AnimatedValue<T, V>? = null

    /**
     * @return a new [AnimatedValue] with an initial value equal to [targetValue], or the
     * existing [AnimatedValue] if it has already been created.
     */
    fun animatedValueForTarget(targetValue: T): AnimatedValue<T, V> {
        return animatedValue ?: factory(targetValue).also { animatedValue = it }
    }
}
