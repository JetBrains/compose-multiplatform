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

package androidx.compose.ui.tooling

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.DecayAnimation
import androidx.compose.animation.core.FloatExponentialDecaySpec
import androidx.compose.animation.core.InfiniteTransition
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.StartOffset
import androidx.compose.animation.core.StartOffsetType
import androidx.compose.animation.core.TargetBasedAnimation
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.animateDp
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.material.Button
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Done
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

enum class CheckBoxState { Unselected, Selected }

@Preview("Single CheckBox")
@Composable
fun CheckBoxPreview() {
    CheckBox()
}

@Preview(name = "CheckBox + Scaffold")
@Composable
fun CheckBoxScaffoldPreview() {
    Scaffold {
        CheckBox()
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Preview(name = "AnimatedContent")
@Composable
fun AnimatedContentPreview() {
    Row {
        var count by remember { mutableStateOf(0) }
        Button(onClick = { count++ }) {
            Text("Add")
        }
        AnimatedContent(targetState = count) { targetCount ->
            // Make sure to use `targetCount`, not `count`.
            Text(text = "Count: $targetCount")
        }
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Preview(name = "AnimatedVisibility")
@Composable
fun AnimatedVisibilityPreview() {
    val editable by remember { mutableStateOf(true) }
    AnimatedVisibility(label = "My Animated Visibility", visible = editable) {
        Text(text = "Edit")
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Preview(name = "transition.AnimatedVisibility")
@Composable
fun TransitionAnimatedVisibilityPreview() {
    val editable by remember { mutableStateOf(CheckBoxState.Unselected) }
    val transition = updateTransition(targetState = editable, label = "transition.AV")
    transition.AnimatedVisibility(visible = { it == CheckBoxState.Selected }) {
        Text(text = "Edit")
    }
}

@Composable
private fun CheckBox() {
    val (selected, onSelected) = remember { mutableStateOf(false) }
    val transition = updateTransition(
        if (selected) CheckBoxState.Selected else CheckBoxState.Unselected,
        label = "checkBoxAnim"
    )

    val checkBoxCorner by transition.animateDp(
        transitionSpec = {
            tween(durationMillis = 1000, easing = LinearEasing)
        },
        label = "CheckBox Corner"
    ) {
        when (it) {
            CheckBoxState.Selected -> 28.dp
            CheckBoxState.Unselected -> 0.dp
        }
    }

    Surface(
        shape = MaterialTheme.shapes.large.copy(topStart = CornerSize(checkBoxCorner)),
        modifier = Modifier.toggleable(value = selected, onValueChange = onSelected)
    ) {
        Icon(imageVector = Icons.Filled.Done, contentDescription = null)
    }
}

@Preview(name = "AnimateAsStatePreview")
@Composable
fun AnimateAsStatePreview() {
    var showMenu by remember { mutableStateOf(true) }
    var message by remember { mutableStateOf("Hello") }

    val size: Dp by animateDpAsState(
        targetValue = if (showMenu) 0.dp else 10.dp,
        animationSpec = spring(Spring.DampingRatioHighBouncy, Spring.StiffnessHigh),
    )
    val offset by animateIntAsState(
        targetValue = if (showMenu) 2 else 1
    )

    Box(
        Modifier
            .padding(size)
            .offset(offset.dp)
            .pointerInput(Unit) {
                detectTapGestures {
                    showMenu = !showMenu
                    message += "!"
                }
            }) {
        Text(text = message)
    }
}

@Preview(name = "CrossFadePreview")
@Composable
fun CrossFadePreview() {
    var currentPage by remember { mutableStateOf("A") }
    Row {
        Button(onClick = {
            currentPage = when (currentPage) {
                "A" -> "B"
                "B" -> "A"
                else -> "A"
            }
        }) {
            Text("Switch Page")
        }
        Crossfade(targetState = currentPage) { screen ->
            when (screen) {
                "A" -> Text("Page A")
                "B" -> Text("Page B")
            }
        }
    }
}

@Preview(name = "AnimateContentSizePreview")
@Composable
fun AnimateContentSizePreview() {
    var message by remember { mutableStateOf("Hello") }
    Row {
        var count by remember { mutableStateOf(0) }
        Button(onClick = {
            count++
            message = "Count is $count"
        }) {
            Text("Add")
        }
        Box(
            modifier = Modifier
                .animateContentSize()
        ) {
            Text(text = message)
        }
    }
}

@Preview(name = "TargetBasedAnimationPreview")
@Composable
fun TargetBasedAnimationPreview() {
    val anim = remember {
        TargetBasedAnimation(
            animationSpec = tween(200),
            typeConverter = Float.VectorConverter,
            initialValue = 200f,
            targetValue = 1000f
        )
    }
    var playTime by remember { mutableStateOf(0L) }

    LaunchedEffect(anim) {
        val startTime = withFrameNanos { it }

        do {
            playTime = withFrameNanos { it } - startTime
        } while (playTime < 1_000_000L)
    }
    Box { Text(text = "Play time $playTime") }
}

@Preview(name = "DecayAnimationPreview")
@Composable
fun DecayAnimationPreview() {
    val anim = remember {
        DecayAnimation(
            animationSpec = FloatExponentialDecaySpec(),
            initialValue = 200f,
        )
    }
    var playTime by remember { mutableStateOf(0L) }

    LaunchedEffect(anim) {
        val startTime = withFrameNanos { it }

        do {
            playTime = withFrameNanos { it } - startTime
        } while (playTime < 1_000_000L)
    }
    Box { Text(text = "Play time $playTime") }
}

@Preview
@Composable
fun InfiniteTransitionPreview() {
    val infiniteTransition = rememberInfiniteTransition()
    Row {
        infiniteTransition.PulsingDot(StartOffset(0))
        infiniteTransition.PulsingDot(StartOffset(150, StartOffsetType.FastForward))
        infiniteTransition.PulsingDot(StartOffset(300, StartOffsetType.FastForward))
    }
}

@Composable
fun InfiniteTransition.PulsingDot(startOffset: StartOffset) {
    val scale by animateFloat(
        0.2f,
        1f,
        infiniteRepeatable(tween(600), RepeatMode.Reverse, initialStartOffset = startOffset)
    )
    Box(
        Modifier
            .padding(5.dp)
            .size(20.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .background(Color.Gray, shape = CircleShape)
    )
}