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

package androidx.compose.animation

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.Transition
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.util.fastForEach

/**
 * [Crossfade] allows to switch between two layouts with a crossfade animation.
 *
 * @sample androidx.compose.animation.samples.CrossfadeSample
 *
 * @param targetState is a key representing your target layout state. Every time you change a key
 * the animation will be triggered. The [content] called with the old key will be faded out while
 * the [content] called with the new key will be faded in.
 * @param modifier Modifier to be applied to the animation container.
 * @param animationSpec the [AnimationSpec] to configure the animation.
 * @param label An optional label to differentiate from other animations in Android Studio.
 */
@OptIn(ExperimentalAnimationApi::class)
@Composable
fun <T> Crossfade(
    targetState: T,
    modifier: Modifier = Modifier,
    animationSpec: FiniteAnimationSpec<Float> = tween(),
    label: String = "Crossfade",
    content: @Composable (T) -> Unit
) {
    val transition = updateTransition(targetState, label)
    transition.Crossfade(modifier, animationSpec, content = content)
}

@Deprecated(
    "Crossfade API now has a new label parameter added.",
    level = DeprecationLevel.HIDDEN
)
@OptIn(ExperimentalAnimationApi::class)
@Composable
fun <T> Crossfade(
    targetState: T,
    modifier: Modifier = Modifier,
    animationSpec: FiniteAnimationSpec<Float> = tween(),
    content: @Composable (T) -> Unit
) {
    val transition = updateTransition(targetState)
    transition.Crossfade(modifier, animationSpec, content = content)
}

/**
 * [Crossfade] allows to switch between two layouts with a crossfade animation. The target state of
 * this Crossfade will be the target state of the given Transition object. In other words, when
 * the Transition changes target, the [Crossfade] will fade in the target content while fading out
 * the current content.
 *
 * [content] is a mapping between the state and the composable function for the content of
 * that state. During the crossfade, [content] lambda will be invoked multiple times with different
 * state parameter such that content associated with different states will be fading in/out at the
 * same time.
 *
 * [contentKey] will be used to perform equality check for different states. For example, when two
 * states resolve to the same content key, there will be no animation for that state change.
 * By default, [contentKey] is the same as the state object. [contentKey] can be particularly useful
 * if target state object gets recreated across save & restore while a more persistent key is needed
 * to properly restore the internal states of the content.
 *
 * @param modifier Modifier to be applied to the animation container.
 * @param animationSpec the [AnimationSpec] to configure the animation.
 */
@ExperimentalAnimationApi
@Composable
fun <T> Transition<T>.Crossfade(
    modifier: Modifier = Modifier,
    animationSpec: FiniteAnimationSpec<Float> = tween(),
    contentKey: (targetState: T) -> Any? = { it },
    content: @Composable (targetState: T) -> Unit
) {
    val currentlyVisible = remember { mutableStateListOf<T>().apply { add(currentState) } }
    val contentMap = remember {
        mutableMapOf<T, @Composable () -> Unit>()
    }
    if (currentState == targetState) {
        // If not animating, just display the current state
        if (currentlyVisible.size != 1 || currentlyVisible[0] != targetState) {
            // Remove all the intermediate items from the list once the animation is finished.
            currentlyVisible.removeAll { it != targetState }
            contentMap.clear()
        }
    }
    if (!contentMap.contains(targetState)) {
        // Replace target with the same key if any
        val replacementId = currentlyVisible.indexOfFirst {
            contentKey(it) == contentKey(targetState)
        }
        if (replacementId == -1) {
            currentlyVisible.add(targetState)
        } else {
            currentlyVisible[replacementId] = targetState
        }
        contentMap.clear()
        currentlyVisible.fastForEach { stateForContent ->
            contentMap[stateForContent] = {
                val alpha by animateFloat(
                    transitionSpec = { animationSpec }
                ) { if (it == stateForContent) 1f else 0f }
                Box(Modifier.graphicsLayer { this.alpha = alpha }) {
                    content(stateForContent)
                }
            }
        }
    }

    Box(modifier) {
        currentlyVisible.fastForEach {
            key(contentKey(it)) {
                contentMap[it]?.invoke()
            }
        }
    }
}
