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

package androidx.compose.animation.samples

import androidx.annotation.Sampled
import androidx.compose.animation.Animatable
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

@Sampled
@Composable
fun ColorAnimationSample() {
    @Composable
    fun ColorAnimation(primary: Boolean) {
        // Animates to primary or secondary color, depending on whether [primary] is true
        // [animateState] returns the current animation value in a State<Color> in this example. We
        // use the State<Color> object as a property delegate here.
        val color: Color by animateColorAsState(
            if (primary) MaterialTheme.colors.primary else MaterialTheme.colors.secondary
        )
        Box(modifier = Modifier.background(color))
    }
}

@Sampled
fun AnimatableColor() {
    @Composable
    fun animate(
        targetValue: Color,
        animationSpec: AnimationSpec<Color>,
        onFinished: (Color) -> Unit
    ): Color {
        // Creates an Animatable of Color, and remembers it.
        val color = remember { Animatable(targetValue) }
        val finishedListener = rememberUpdatedState(onFinished)
        // Launches a new coroutine whenever the target value or animation spec has changed. This
        // automatically cancels the previous job/animation.
        LaunchedEffect(targetValue, animationSpec) {
            color.animateTo(targetValue, animationSpec)
            // Invokes finished listener. This line will not be executed if the job gets canceled
            // halfway through an animation.
            finishedListener.value(targetValue)
        }
        return color.value
    }
}