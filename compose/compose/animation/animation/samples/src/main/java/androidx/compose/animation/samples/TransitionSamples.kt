/*
 * Copyright 2021 The Android Open Source Project
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
import androidx.compose.animation.animateColor
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Transition
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material.Button
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp

private enum class ComponentState { Pressed, Released }

@Sampled
@Composable
fun GestureAnimationSample() {
    // enum class ComponentState { Pressed, Released }
    var useRed by remember { mutableStateOf(false) }
    var toState by remember { mutableStateOf(ComponentState.Released) }
    val modifier = Modifier.pointerInput(Unit) {
        detectTapGestures(
            onPress = {
                toState = ComponentState.Pressed
                tryAwaitRelease()
                toState = ComponentState.Released
            }
        )
    }

    // Defines a transition of `ComponentState`, and updates the transition when the provided
    // [targetState] changes. The tran
    // sition will run all of the child animations towards the new
    // [targetState] in response to the [targetState] change.
    val transition: Transition<ComponentState> = updateTransition(targetState = toState)
    // Defines a float animation as a child animation the transition. The current animation value
    // can be read from the returned State<Float>.
    val scale: Float by transition.animateFloat(
        // Defines a transition spec that uses the same low-stiffness spring for *all*
        // transitions of this float, no matter what the target is.
        transitionSpec = { spring(stiffness = 50f) }
    ) { state ->
        // This code block declares a mapping from state to value.
        if (state == ComponentState.Pressed) 3f else 1f
    }

    // Defines a color animation as a child animation of the transition.
    val color: Color by transition.animateColor(
        transitionSpec = {
            when {
                ComponentState.Pressed isTransitioningTo ComponentState.Released ->
                    // Uses spring for the transition going from pressed to released
                    spring(stiffness = 50f)
                else ->
                    // Uses tween for all the other transitions. (In this case there is
                    // only one other transition. i.e. released -> pressed.)
                    tween(durationMillis = 500)
            }
        }
    ) { state ->
        when (state) {
            // Similar to the float animation, we need to declare the target values
            // for each state. In this code block we can access theme colors.
            ComponentState.Pressed -> MaterialTheme.colors.primary
            // We can also have the target value depend on other mutableStates,
            // such as `useRed` here. Whenever the target value changes, transition
            // will automatically animate to the new value even if it has already
            // arrived at its target state.
            ComponentState.Released -> if (useRed) Color.Red else MaterialTheme.colors.secondary
        }
    }
    Column {
        Button(
            modifier = Modifier.padding(10.dp).align(Alignment.CenterHorizontally),
            onClick = { useRed = !useRed }
        ) {
            Text("Change Color")
        }
        Box(
            modifier.fillMaxSize().wrapContentSize(Alignment.Center)
                .size((100 * scale).dp).background(color)
        )
    }
}

@Sampled
@Composable
fun InfiniteTransitionSample() {
    @Composable
    fun InfinitelyPulsingHeart() {
        // Creates an [InfiniteTransition] instance for managing child animations.
        val infiniteTransition = rememberInfiniteTransition()

        // Creates a child animation of float type as a part of the [InfiniteTransition].
        val scale by infiniteTransition.animateFloat(
            initialValue = 3f,
            targetValue = 6f,
            animationSpec = infiniteRepeatable(
                // Infinitely repeating a 1000ms tween animation using default easing curve.
                animation = tween(1000),
                // After each iteration of the animation (i.e. every 1000ms), the animation will
                // start again from the [initialValue] defined above.
                // This is the default [RepeatMode]. See [RepeatMode.Reverse] below for an
                // alternative.
                repeatMode = RepeatMode.Restart
            )
        )

        // Creates a Color animation as a part of the [InfiniteTransition].
        val color by infiniteTransition.animateColor(
            initialValue = Color.Red,
            targetValue = Color(0xff800000), // Dark Red
            animationSpec = infiniteRepeatable(
                // Linearly interpolate between initialValue and targetValue every 1000ms.
                animation = tween(1000, easing = LinearEasing),
                // Once [TargetValue] is reached, starts the next iteration in reverse (i.e. from
                // TargetValue to InitialValue). Then again from InitialValue to TargetValue. This
                // [RepeatMode] ensures that the animation value is *always continuous*.
                repeatMode = RepeatMode.Reverse
            )
        )

        Box(Modifier.fillMaxSize()) {
            Icon(
                Icons.Filled.Favorite,
                contentDescription = null,
                modifier = Modifier.align(Alignment.Center)
                    .graphicsLayer(
                        scaleX = scale,
                        scaleY = scale
                    ),
                tint = color
            )
        }
    }
}
