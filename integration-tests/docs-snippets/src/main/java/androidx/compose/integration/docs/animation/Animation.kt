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
    "MemberVisibilityCanBePrivate", "TransitionPropertiesLabel", "UpdateTransitionLabel",
    "UNUSED_PARAMETER"
)
@file:SuppressLint("ModifierInspectorInfo", "NewApi")

package androidx.compose.integration.docs.animation

import android.annotation.SuppressLint
import androidx.compose.animation.Animatable
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.EnterExitState
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.animateColor
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.AnimationVector2D
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.ExperimentalTransitionApi
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
import androidx.compose.animation.core.createChildTransition
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
import androidx.compose.animation.with
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.horizontalDrag
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.integration.docs.animation.UpdateTransitionEnumState.BoxState
import androidx.compose.material.Button
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Phone
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
import androidx.compose.ui.unit.IntSize
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
private fun AnimatedVisibilitySimple() {
    var editable by remember { mutableStateOf(true) }
    AnimatedVisibility(visible = editable) {
        Text(text = "Edit")
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
private fun AnimatedVisibilityWithEnterAndExit() {
    var visible by remember { mutableStateOf(true) }
    val density = LocalDensity.current
    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically {
            // Slide in from 40 dp from the top.
            with(density) { -40.dp.roundToPx() }
        } + expandVertically(
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

@OptIn(ExperimentalAnimationApi::class, ExperimentalTransitionApi::class)
@Composable
private fun AnimatedVisibilityMutable() {
    // Create a MutableTransitionState<Boolean> for the AnimatedVisibility.
    val state = remember {
        MutableTransitionState(false).apply {
            // Start the animation immediately.
            targetState = true
        }
    }
    Column {
        AnimatedVisibility(visibleState = state) {
            Text(text = "Hello, world!")
        }

        // Use the MutableTransitionState to know the current animation state
        // of the AnimatedVisibility.
        Text(
            text = when {
                state.isIdle && state.currentState -> "Visible"
                !state.isIdle && state.currentState -> "Disappearing"
                state.isIdle && !state.currentState -> "Invisible"
                else -> "Appearing"
            }
        )
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
private fun AnimatedVisibilityAnimateEnterExit(visible: Boolean) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        // Fade in/out the background and the foreground.
        Box(Modifier.fillMaxSize().background(Color.DarkGray)) {
            Box(
                Modifier
                    .align(Alignment.Center)
                    .animateEnterExit(
                        // Slide in/out the inner box.
                        enter = slideInVertically(),
                        exit = slideOutVertically()
                    )
                    .sizeIn(minWidth = 256.dp, minHeight = 64.dp)
                    .background(Color.Red)
            ) {
                // Content of the notification…
            }
        }
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
private fun AnimatedVisibilityTransition(visible: Boolean) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(),
        exit = fadeOut()
    ) { // this: AnimatedVisibilityScope
        // Use AnimatedVisibilityScope#transition to add a custom animation
        // to the AnimatedVisibility.
        val background by transition.animateColor { state ->
            if (state == EnterExitState.Visible) Color.Blue else Color.Gray
        }
        Box(modifier = Modifier.size(128.dp).background(background))
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
private fun AnimatedContentSimple() {
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
@Composable
private fun AnimatedContentTransitionSpec(count: Int) {
    AnimatedContent(
        targetState = count,
        transitionSpec = {
            // Compare the incoming number with the previous number.
            if (targetState > initialState) {
                // If the target number is larger, it slides up and fades in
                // while the initial (smaller) number slides up and fades out.
                slideInVertically { height -> height } + fadeIn() with
                    slideOutVertically { height -> -height } + fadeOut()
            } else {
                // If the target number is smaller, it slides down and fades in
                // while the initial number slides down and fades out.
                slideInVertically { height -> -height } + fadeIn() with
                    slideOutVertically { height -> height } + fadeOut()
            }.using(
                // Disable clipping since the faded slide-in/out should
                // be displayed out of bounds.
                SizeTransform(clip = false)
            )
        }
    ) { targetCount ->
        Text(text = "$targetCount")
    }
}

@OptIn(ExperimentalAnimationApi::class, ExperimentalMaterialApi::class)
@Composable
private fun AnimatedContentSizeTransform() {
    var expanded by remember { mutableStateOf(false) }
    Surface(
        color = MaterialTheme.colors.primary,
        onClick = { expanded = !expanded }
    ) {
        AnimatedContent(
            targetState = expanded,
            transitionSpec = {
                fadeIn(animationSpec = tween(150, 150)) with
                    fadeOut(animationSpec = tween(150)) using
                    SizeTransform { initialSize, targetSize ->
                        if (targetState) {
                            keyframes {
                                // Expand horizontally first.
                                IntSize(targetSize.width, initialSize.height) at 150
                                durationMillis = 300
                            }
                        } else {
                            keyframes {
                                // Shrink vertically first.
                                IntSize(initialSize.width, targetSize.height) at 150
                                durationMillis = 300
                            }
                        }
                    }
            }
        ) { targetExpanded ->
            if (targetExpanded) {
                Expanded()
            } else {
                ContentIcon()
            }
        }
    }
}

@Composable
private fun AnimateContentSizeSimple() {
    var message by remember { mutableStateOf("Hello") }
    Box(
        modifier = Modifier.background(Color.Blue).animateContentSize()
    ) {
        Text(text = message)
    }
}

@Composable
private fun CrossfadeSimple() {
    var currentPage by remember { mutableStateOf("A") }
    Crossfade(targetState = currentPage) { screen ->
        when (screen) {
            "A" -> Text("Page A")
            "B" -> Text("Page B")
        }
    }
}

@Composable
private fun AnimateAsStateSimple(enabled: Boolean) {
    val alpha: Float by animateFloatAsState(if (enabled) 1f else 0.5f)
    Box(
        Modifier.fillMaxSize()
            .graphicsLayer(alpha = alpha)
            .background(Color.Red)
    )
}

@Composable
private fun AnimatableSimple(ok: Boolean) {
    // Start out gray and animate to green/red based on `ok`
    val color = remember { Animatable(Color.Gray) }
    LaunchedEffect(ok) {
        color.animateTo(if (ok) Color.Green else Color.Red)
    }
    Box(Modifier.fillMaxSize().background(color.value))
}

private object UpdateTransitionEnumState {
    enum class BoxState {
        Collapsed,
        Expanded
    }
}

@Composable
private fun UpdateTransitionInstance() {
    var currentState by remember { mutableStateOf(BoxState.Collapsed) }
    val transition = updateTransition(currentState)
}

@Composable
private fun UpdateTransitionAnimationValues(transition: Transition<BoxState>) {
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
private fun UpdateTransitionTransitionSpec(transition: Transition<BoxState>) {
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
private fun UpdateTransitionMutableTransitionState() {
    // Start in collapsed state and immediately animate to expanded
    var currentState = remember { MutableTransitionState(BoxState.Collapsed) }
    currentState.targetState = BoxState.Expanded
    val transition = updateTransition(currentState)
    // ……
}

@OptIn(ExperimentalTransitionApi::class)
private object UpdateTransitionCreateChildTransition {

    enum class DialerState { DialerMinimized, NumberPad }

    @Composable
    fun DialerButton(isVisibleTransition: Transition<Boolean>) {
        // `isVisibleTransition` spares the need for the content to know
        // about other DialerStates. Instead, the content can focus on
        // animating the state change between visible and not visible.
    }

    @Composable
    fun NumberPad(isVisibleTransition: Transition<Boolean>) {
        // `isVisibleTransition` spares the need for the content to know
        // about other DialerStates. Instead, the content can focus on
        // animating the state change between visible and not visible.
    }

    @Composable
    fun Dialer(dialerState: DialerState) {
        val transition = updateTransition(dialerState)
        Box {
            // Creates separate child transitions of Boolean type for NumberPad
            // and DialerButton for any content animation between visible and
            // not visible
            NumberPad(
                transition.createChildTransition {
                    it == DialerState.NumberPad
                }
            )
            DialerButton(
                transition.createChildTransition {
                    it == DialerState.DialerMinimized
                }
            )
        }
    }
}

@OptIn(ExperimentalAnimationApi::class, ExperimentalMaterialApi::class)
@Composable
private fun UpdateTransitionAnimatedVisibility() {
    var selected by remember { mutableStateOf(false) }
    // Animates changes when `selected` is changed.
    val transition = updateTransition(selected)
    val borderColor by transition.animateColor { isSelected ->
        if (isSelected) Color.Magenta else Color.White
    }
    val elevation by transition.animateDp { isSelected ->
        if (isSelected) 10.dp else 2.dp
    }
    Surface(
        onClick = { selected = !selected },
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(2.dp, borderColor),
        elevation = elevation
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Text(text = "Hello, world!")
            // AnimatedVisibility as a part of the transition.
            transition.AnimatedVisibility(
                visible = { targetSelected -> targetSelected },
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Text(text = "It is fine today.")
            }
            // AnimatedContent as a part of the transition.
            transition.AnimatedContent { targetState ->
                if (targetState) {
                    Text(text = "Selected")
                } else {
                    Icon(imageVector = Icons.Default.Phone, contentDescription = "Phone")
                }
            }
        }
    }
}

private object UpdateTransitionEncapsulating {
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
private fun RememberInfiniteTransitionSimple() {
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
private fun TargetBasedAnimationSimple(someCustomCondition: () -> Boolean) {
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
private fun AnimationSpecTween(enabled: Boolean) {
    val alpha: Float by animateFloatAsState(
        targetValue = if (enabled) 1f else 0.5f,
        // Configure the animation duration and easing.
        animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing)
    )
}

@Composable
private fun AnimationSpecSpring() {
    val value by animateFloatAsState(
        targetValue = 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioHighBouncy,
            stiffness = Spring.StiffnessMedium
        )
    )
}

@Composable
private fun AnimationSpecTween() {
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
private fun AnimationSpecKeyframe() {
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
private fun AnimationSpecRepeatable() {
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
private fun AnimationSpecInfiniteRepeatable() {
    val value by animateFloatAsState(
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 300),
            repeatMode = RepeatMode.Reverse
        )
    )
}

@Composable
private fun AnimationSpecSnap() {
    val value by animateFloatAsState(
        targetValue = 1f,
        animationSpec = snap(delayMillis = 50)
    )
}

private object Easing {
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

private object AnimationVectorTwoWayConverter {
    val IntToVector: TwoWayConverter<Int, AnimationVector1D> =
        TwoWayConverter({ AnimationVector1D(it.toFloat()) }, { it.value.toInt() })
}

private object AnimationVectorCustomType {
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

private object GestureAndAnimationSimple {
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

private object GestureAndAnimationSwipeToDismiss {
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

private object Testing {
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
        // `assertAgainGolden` needs to be implemented in your code.
        rule.onRoot().captureToImage().assertAgainstGolden()
    }
}

/*
Fakes needed for snippets to build:
 */

private fun ImageBitmap.assertAgainstGolden() {
}

@Composable
private fun Circle(modifier: Modifier = Modifier) {
}

@Composable
private fun Expanded() {
}

@Composable
private fun ContentIcon() {
}
