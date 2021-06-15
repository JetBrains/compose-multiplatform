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

// Ignore lint warnings in documentation snippets
@file:Suppress(
    "CanBeVal", "UNUSED_VARIABLE", "RemoveExplicitTypeArguments", "unused",
    "MemberVisibilityCanBePrivate"
)
@file:SuppressLint("ModifierInspectorInfo", "NewApi")

package androidx.compose.integration.docs.animation

import android.annotation.SuppressLint
import androidx.compose.animation.Animatable
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.animateColor
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.AnimationVector2D
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.TargetBasedAnimation
import androidx.compose.animation.core.Transition
import androidx.compose.animation.core.TwoWayConverter
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.animateDp
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateRect
import androidx.compose.animation.core.animateValueAsState
import androidx.compose.animation.core.calculateTargetValue
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.repeatable
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.splineBasedDecay
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.horizontalDrag
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.integration.docs.animation.UpdateTransitionEnumState.BoxState
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import org.junit.Rule
import org.junit.Test
import kotlin.math.absoluteValue
import kotlin.math.roundToInt

/**
 * This file lets DevRel track changes to snippets present in
 * https://developer.android.com/jetpack/compose/animation
 *
 * No action required if it's modified.
 */

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun AnimatedVisibilitySimple() {
    var editable by remember { mutableStateOf(true) }
    AnimatedVisibility(visible = editable) {
        Text(text = "Edit")
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun AnimatedVisibilityWithEnterAndExit() {
    var visible by remember { mutableStateOf(true) }
    val density = LocalDensity.current
    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically(
            // Slide in from 40 dp from the top.
            initialOffsetY = { with(density) { -40.dp.roundToPx() } }
        ) + expandVertically(
            // Expand from the top.
            expandFrom = Alignment.Top
        ) + fadeIn(
            // Fade in with the initial alpha of 0.3f.
            initialAlpha = 0.3f
        ),
        exit = slideOutVertically() + shrinkVertically() + fadeOut()
    ) {
        Text("Hello", Modifier.fillMaxWidth().height(200.dp))
    }
}

@Composable
fun AnimateContentSizeSimple() {
    var message by remember { mutableStateOf("Hello") }
    Box(
        modifier = Modifier.background(Color.Blue).animateContentSize()
    ) {
        Text(text = message)
    }
}

@Composable
fun CrossfadeSimple() {
    var currentPage by remember { mutableStateOf("A") }
    Crossfade(targetState = currentPage) { screen ->
        when (screen) {
            "A" -> Text("Page A")
            "B" -> Text("Page B")
        }
    }
}

@Composable
fun AnimateAsStateSimple(enabled: Boolean) {
    val alpha: Float by animateFloatAsState(if (enabled) 1f else 0.5f)
    Box(
        Modifier.fillMaxSize()
            .graphicsLayer(alpha = alpha)
            .background(Color.Red)
    )
}

@Composable
fun AnimatableSimple(ok: Boolean) {
    // Start out gray and animate to green/red based on `ok`
    val color = remember { Animatable(Color.Gray) }
    LaunchedEffect(ok) {
        color.animateTo(if (ok) Color.Green else Color.Red)
    }
    Box(Modifier.fillMaxSize().background(color.value))
}

object UpdateTransitionEnumState {
    enum class BoxState {
        Collapsed,
        Expanded
    }
}

@Composable
fun UpdateTransitionInstance() {
    var currentState by remember { mutableStateOf(BoxState.Collapsed) }
    val transition = updateTransition(currentState)
}

@Composable
fun UpdateTransitionAnimationValues(transition: Transition<BoxState>) {
    val rect by transition.animateRect { state ->
        when (state) {
            BoxState.Collapsed -> Rect(0f, 0f, 100f, 100f)
            BoxState.Expanded -> Rect(100f, 100f, 300f, 300f)
        }
    }
    val borderWidth by transition.animateDp { state ->
        when (state) {
            BoxState.Collapsed -> 1.dp
            BoxState.Expanded -> 0.dp
        }
    }
}

@Composable
fun UpdateTransitionTransitionSpec(transition: Transition<BoxState>) {
    val color by transition.animateColor(
        transitionSpec = {
            when {
                BoxState.Expanded isTransitioningTo BoxState.Collapsed ->
                    spring(stiffness = 50f)
                else ->
                    tween(durationMillis = 500)
            }
        }
    ) { state ->
        when (state) {
            BoxState.Collapsed -> MaterialTheme.colors.primary
            BoxState.Expanded -> MaterialTheme.colors.background
        }
    }
}

@Composable
fun UpdateTransitionMutableTransitionState() {
    // Start in collapsed state and immediately animate to expanded
    var currentState = remember { MutableTransitionState(BoxState.Collapsed) }
    currentState.targetState = BoxState.Expanded
    val transition = updateTransition(currentState)
    // ……
}

object UpdateTransitionEncapsulating {
    enum class BoxState { Collapsed, Expanded }

    @Composable
    fun AnimatingBox(boxState: BoxState) {
        val transitionData = updateTransitionData(boxState)
        // UI tree
        Box(
            modifier = Modifier
                .background(transitionData.color)
                .size(transitionData.size)
        )
    }

    // Holds the animation values.
    private class TransitionData(
        color: State<Color>,
        size: State<Dp>
    ) {
        val color by color
        val size by size
    }

    // Create a Transition and return its animation values.
    @Composable
    private fun updateTransitionData(boxState: BoxState): TransitionData {
        val transition = updateTransition(boxState)
        val color = transition.animateColor { state ->
            when (state) {
                BoxState.Collapsed -> Color.Gray
                BoxState.Expanded -> Color.Red
            }
        }
        val size = transition.animateDp { state ->
            when (state) {
                BoxState.Collapsed -> 64.dp
                BoxState.Expanded -> 128.dp
            }
        }
        return remember(transition) { TransitionData(color, size) }
    }
}

@Composable
fun RememberInfiniteTransitionSimple() {
    val infiniteTransition = rememberInfiniteTransition()
    val color by infiniteTransition.animateColor(
        initialValue = Color.Red,
        targetValue = Color.Green,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    Box(Modifier.fillMaxSize().background(color))
}

@Composable
fun TargetBasedAnimationSimple(someCustomCondition: () -> Boolean) {
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
            val animationValue = anim.getValueFromNanos(playTime)
        } while (someCustomCondition())
    }
}

@Composable
fun AnimationSpecTween(enabled: Boolean) {
    val alpha: Float by animateFloatAsState(
        targetValue = if (enabled) 1f else 0.5f,
        // Configure the animation duration and easing.
        animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing)
    )
}

@Composable
fun AnimationSpecSpring() {
    val value by animateFloatAsState(
        targetValue = 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioHighBouncy,
            stiffness = Spring.StiffnessMedium
        )
    )
}

@Composable
fun AnimationSpecTween() {
    val value by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(
            durationMillis = 300,
            delayMillis = 50,
            easing = LinearOutSlowInEasing
        )
    )
}

@Composable
fun AnimationSpecKeyframe() {
    val value by animateFloatAsState(
        targetValue = 1f,
        animationSpec = keyframes {
            durationMillis = 375
            0.0f at 0 with LinearOutSlowInEasing // for 0-15 ms
            0.2f at 15 with FastOutLinearInEasing // for 15-75 ms
            0.4f at 75 // ms
            0.4f at 225 // ms
        }
    )
}

@Composable
fun AnimationSpecRepeatable() {
    val value by animateFloatAsState(
        targetValue = 1f,
        animationSpec = repeatable(
            iterations = 3,
            animation = tween(durationMillis = 300),
            repeatMode = RepeatMode.Reverse
        )
    )
}

@Composable
fun AnimationSpecInfiniteRepeatable() {
    val value by animateFloatAsState(
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 300),
            repeatMode = RepeatMode.Reverse
        )
    )
}

@Composable
fun AnimationSpecSnap() {
    val value by animateFloatAsState(
        targetValue = 1f,
        animationSpec = snap(delayMillis = 50)
    )
}

object Easing {
    val CustomEasing = Easing { fraction -> fraction * fraction }

    @Composable
    fun EasingUsage() {
        val value by animateFloatAsState(
            targetValue = 1f,
            animationSpec = tween(
                durationMillis = 300,
                easing = CustomEasing
            )
        )
        // ……
    }
}

object AnimationVectorTwoWayConverter {
    val IntToVector: TwoWayConverter<Int, AnimationVector1D> =
        TwoWayConverter({ AnimationVector1D(it.toFloat()) }, { it.value.toInt() })
}

object AnimationVectorCustomType {
    data class MySize(val width: Dp, val height: Dp)

    @Composable
    fun MyAnimation(targetSize: MySize) {
        val animSize: MySize by animateValueAsState<MySize, AnimationVector2D>(
            targetSize,
            TwoWayConverter(
                convertToVector = { size: MySize ->
                    // Extract a float value from each of the `Dp` fields.
                    AnimationVector2D(size.width.value, size.height.value)
                },
                convertFromVector = { vector: AnimationVector2D ->
                    MySize(vector.v1.dp, vector.v2.dp)
                }
            )
        )
    }
}

object GestureAndAnimationSimple {
    @Composable
    fun Gesture() {
        val offset = remember { Animatable(Offset(0f, 0f), Offset.VectorConverter) }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    coroutineScope {
                        while (true) {
                            // Detect a tap event and obtain its position.
                            val position = awaitPointerEventScope {
                                awaitFirstDown().position
                            }
                            launch {
                                // Animate to the tap position.
                                offset.animateTo(position)
                            }
                        }
                    }
                }
        ) {
            Circle(modifier = Modifier.offset { offset.value.toIntOffset() })
        }
    }

    private fun Offset.toIntOffset() = IntOffset(x.roundToInt(), y.roundToInt())
}

object GestureAndAnimationSwipeToDismiss {
    fun Modifier.swipeToDismiss(
        onDismissed: () -> Unit
    ): Modifier = composed {
        val offsetX = remember { Animatable(0f) }
        pointerInput(Unit) {
            // Used to calculate fling decay.
            val decay = splineBasedDecay<Float>(this)
            // Use suspend functions for touch events and the Animatable.
            coroutineScope {
                while (true) {
                    // Detect a touch down event.
                    val pointerId = awaitPointerEventScope { awaitFirstDown().id }
                    val velocityTracker = VelocityTracker()
                    // Stop any ongoing animation.
                    offsetX.stop()
                    awaitPointerEventScope {
                        horizontalDrag(pointerId) { change ->
                            // Update the animation value with touch events.
                            launch {
                                offsetX.snapTo(
                                    offsetX.value + change.positionChange().x
                                )
                            }
                            velocityTracker.addPosition(
                                change.uptimeMillis,
                                change.position
                            )
                        }
                    }
                    // No longer receiving touch events. Prepare the animation.
                    val velocity = velocityTracker.calculateVelocity().x
                    val targetOffsetX = decay.calculateTargetValue(
                        offsetX.value,
                        velocity
                    )
                    // The animation stops when it reaches the bounds.
                    offsetX.updateBounds(
                        lowerBound = -size.width.toFloat(),
                        upperBound = size.width.toFloat()
                    )
                    launch {
                        if (targetOffsetX.absoluteValue <= size.width) {
                            // Not enough velocity; Slide back.
                            offsetX.animateTo(
                                targetValue = 0f,
                                initialVelocity = velocity
                            )
                        } else {
                            // The element was swiped away.
                            offsetX.animateDecay(velocity, decay)
                            onDismissed()
                        }
                    }
                }
            }
        }
            .offset { IntOffset(offsetX.value.roundToInt(), 0) }
    }
}

object Testing {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun testAnimationWithClock() {
        // Pause animations
        rule.mainClock.autoAdvance = false
        var enabled by mutableStateOf(false)
        rule.setContent {
            val color by animateColorAsState(
                targetValue = if (enabled) Color.Red else Color.Green,
                animationSpec = tween(durationMillis = 250)
            )
            Box(Modifier.size(64.dp).background(color))
        }

        // Initiate the animation.
        enabled = true

        // Let the animation proceed.
        rule.mainClock.advanceTimeBy(50L)

        // Compare the result with the image showing the expected result.
        rule.onRoot().captureToImage().assertAgainstGolden()
    }
}

private fun ImageBitmap.assertAgainstGolden() {
}

@Composable
private fun Circle(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(64.dp)
            .background(Color.Gray, CircleShape)
    )
}
