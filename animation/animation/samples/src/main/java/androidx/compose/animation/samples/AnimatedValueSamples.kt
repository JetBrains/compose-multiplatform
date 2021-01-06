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
import androidx.compose.animation.animate
import androidx.compose.animation.animateAsState
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.AnimationVector2D
import androidx.compose.animation.core.TwoWayConverter
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.preferredSize
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp

@Suppress("DEPRECATION")
@Sampled
@Composable
fun VisibilityTransitionSample() {
    @Composable
    fun VisibilityTransition(visible: Boolean) {
        val alpha = animate(if (visible) 0f else 1f)
        Text("Visibility Transition", modifier = Modifier.alpha(alpha))
    }
}

@Suppress("DEPRECATION")
@Sampled
@Composable
fun ColorTransitionSample() {
    @Composable
    fun ColorTransition(enabled: Boolean) {
        val textColor = animate(if (enabled) Color.Black else Color.Gray)
        Text("Visibility Transition", color = textColor)
    }
}

data class MySize(val width: Dp, val height: Dp)

@Suppress("DEPRECATION")
@Sampled
@Composable
fun ArbitraryValueTypeTransitionSample() {
    @Composable
    fun ArbitraryValueTypeTransition(enabled: Boolean) {
        val mySize = remember(enabled) {
            if (enabled) {
                MySize(500.dp, 500.dp)
            } else {
                MySize(100.dp, 100.dp)
            }
        }
        val animSize = animate<MySize, AnimationVector2D>(
            mySize,
            TwoWayConverter(
                convertToVector = { AnimationVector2D(it.width.value, it.height.value) },
                convertFromVector = { MySize(it.v1.dp, it.v2.dp) }
            )
        )
        Box(Modifier.preferredSize(animSize.width, animSize.height).background(color = Color.Red))
    }
}

@Suppress("DEPRECATION")
@Sampled
@Composable
fun DpAnimationSample() {
    @Composable
    fun HeightAnimation(collapsed: Boolean) {
        val height: Dp = animate(if (collapsed) 10.dp else 20.dp)
        Box(Modifier.fillMaxWidth().height(height).background(color = Color.Red))
    }
}

@Sampled
@Composable
@Suppress("UNUSED_VARIABLE", "DEPRECATION")
fun AnimateOffsetSample() {
    @Composable
    fun OffsetTransition(selected: Boolean) {
        val offset: Offset = animate(
            if (selected) Offset(0f, 0f) else Offset(20f, 20f)
        )

        val intOffset: IntOffset = animate(
            if (selected) IntOffset(0, 0) else IntOffset(50, 50)
        )
    }
}

@Sampled
@Composable
fun ColorAnimationSample() {
    @Composable
    fun ColorAnimation(primary: Boolean) {
        // Animates to primary or secondary color, depending on whether [primary] is true
        // [animateState] returns the current animation value in a State<Color> in this example. We
        // use the State<Color> object as a property delegate here.
        val color: Color by animateAsState(
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