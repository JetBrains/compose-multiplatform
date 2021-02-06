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

package androidx.compose.foundation.gestures

import androidx.compose.animation.core.AnimationState
import androidx.compose.animation.core.DecayAnimationSpec
import androidx.compose.animation.core.animateDecay
import androidx.compose.animation.defaultDecayAnimationSpec
import androidx.compose.foundation.Interaction
import androidx.compose.foundation.InteractionState
import androidx.compose.foundation.MutatePriority
import androidx.compose.foundation.gestures.Orientation.Horizontal
import androidx.compose.foundation.gestures.Orientation.Vertical
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollDispatcher
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.consumePositionChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.platform.debugInspectorInfo
import androidx.compose.ui.unit.Velocity
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import kotlin.math.abs

/**
 * Configure touch scrolling and flinging for the UI element in a single [Orientation].
 *
 * Users should update their state themselves using default [ScrollableState] and its
 * `consumeScrollDelta` callback or by implementing [ScrollableState] interface manually and reflect
 * their own state in UI when using this component.
 *
 * If you don't need to have fling or nested scroll support, but want to make component simply
 * draggable, consider using [draggable].
 *
 * @sample androidx.compose.foundation.samples.ScrollableSample
 *
 * @param state [ScrollableState] state of the scrollable. Defines how scroll events will be
 * interpreted by the user land logic and contains useful information about on-going events.
 * @param orientation orientation of the scrolling
 * @param enabled whether or not scrolling in enabled
 * @param reverseDirection reverse the direction of the scroll, so top to bottom scroll will
 * behave like bottom to top and left to right will behave like right to left.
 * @param flingBehavior logic describing fling behavior when drag has finished with velocity. If
 * `null`, default from [ScrollableDefaults.flingBehavior] will be used.
 * @param interactionState [InteractionState] that will be updated when this draggable is
 * being dragged, using [Interaction.Dragged].
 */
fun Modifier.scrollable(
    state: ScrollableState,
    orientation: Orientation,
    enabled: Boolean = true,
    reverseDirection: Boolean = false,
    flingBehavior: FlingBehavior? = null,
    interactionState: InteractionState? = null
): Modifier = composed(
    inspectorInfo = debugInspectorInfo {
        name = "scrollable"
        properties["orientation"] = orientation
        properties["state"] = state
        properties["enabled"] = enabled
        properties["reverseDirection"] = reverseDirection
        properties["flingBehavior"] = flingBehavior
        properties["interactionState"] = interactionState
    },
    factory = {
        fun Float.reverseIfNeeded(): Float = if (reverseDirection) this * -1 else this
        touchScrollImplementation(
            interactionState,
            orientation,
            reverseDirection,
            state,
            flingBehavior,
            enabled
        ).mouseScrollable(orientation) {
            state.dispatchRawDelta(it.reverseIfNeeded())
        }
    }
)

/**
 * Contains the default values used by [scrollable]
 */
object ScrollableDefaults {

    /**
     * Create and remember default [FlingBehavior] that will represent natural fling curve.
     */
    @Composable
    fun flingBehavior(): FlingBehavior {
        val flingSpec = defaultDecayAnimationSpec()
        return remember(flingSpec) {
            DefaultFlingBehavior(flingSpec)
        }
    }
}

// TODO(demin): think how we can move touchScrollable/mouseScrollable into commonMain,
//  so Android can support mouse wheel scrolling, and desktop can support touch scrolling.
//  For this we need first to implement different types of PointerInputEvent
//  (to differentiate mouse and touch)
internal expect fun Modifier.mouseScrollable(
    orientation: Orientation,
    onScroll: (Float) -> Unit
): Modifier

@Suppress("ComposableModifierFactory")
@Composable
private fun Modifier.touchScrollImplementation(
    interactionState: InteractionState?,
    orientation: Orientation,
    reverseDirection: Boolean,
    controller: ScrollableState,
    flingBehavior: FlingBehavior?,
    enabled: Boolean
): Modifier {
    DisposableEffect(interactionState) {
        onDispose {
            interactionState?.removeInteraction(Interaction.Dragged)
        }
    }

    val nestedScrollDispatcher = remember { mutableStateOf(NestedScrollDispatcher()) }
    val scrollLogic = rememberUpdatedState(
        ScrollingLogic(
            orientation,
            reverseDirection,
            nestedScrollDispatcher,
            controller,
            flingBehavior ?: ScrollableDefaults.flingBehavior()
        )
    )
    val nestedScrollConnection = remember { scrollableNestedScrollConnection(scrollLogic) }
    val orientationState = rememberUpdatedState(orientation)
    val enabledState = rememberUpdatedState(enabled)
    val controllerState = rememberUpdatedState(controller)
    val interactionStateState = rememberUpdatedState(interactionState)
    return dragForEachGesture(
        orientation = orientationState,
        enabled = enabledState,
        scrollableState = controllerState,
        nestedScrollDispatcher = nestedScrollDispatcher,
        interactionState = interactionStateState,
        scrollLogic = scrollLogic
    ).nestedScroll(nestedScrollConnection, nestedScrollDispatcher.value)
}

@Suppress("ComposableModifierFactory")
@Composable
private fun Modifier.dragForEachGesture(
    orientation: State<Orientation>,
    enabled: State<Boolean>,
    scrollableState: State<ScrollableState>,
    nestedScrollDispatcher: State<NestedScrollDispatcher>,
    interactionState: State<InteractionState?>,
    scrollLogic: State<ScrollingLogic>
): Modifier {
    fun isVertical() = orientation.value == Vertical

    fun Offset.axisValue() = this.run { if (isVertical()) y else x }

    suspend fun PointerInputScope.initialDown(): Pair<PointerInputChange?, Float> {
        var initialDelta = 0f
        return awaitPointerEventScope {
            val down = awaitFirstDown(requireUnconsumed = false)
            if (!enabled.value) {
                null to initialDelta
            } else if (scrollableState.value.isScrollInProgress) {
                // since we start immediately we don't wait for slop and set initial delta to 0
                initialDelta = 0f
                down to initialDelta
            } else {
                val onSlopPassed = { event: PointerInputChange, overSlop: Float ->
                    event.consumePositionChange()
                    initialDelta = overSlop
                }
                val result = if (isVertical()) {
                    awaitVerticalTouchSlopOrCancellation(down.id, onSlopPassed)
                } else {
                    awaitHorizontalTouchSlopOrCancellation(down.id, onSlopPassed)
                }
                (if (enabled.value) result else null) to initialDelta
            }
        }
    }

    suspend fun PointerInputScope.mainDragCycle(
        drag: PointerInputChange,
        initialDelta: Float,
        velocityTracker: VelocityTracker,
    ): Boolean {
        var result = false
        try {
            scrollableState.value.scroll(MutatePriority.UserInput) {
                awaitPointerEventScope {
                    if (enabled.value) {
                        with(scrollLogic.value) {
                            dispatchScroll(initialDelta, NestedScrollSource.Drag)
                        }
                    }
                    velocityTracker.addPosition(drag.uptimeMillis, drag.position)
                    val dragTick = { event: PointerInputChange ->
                        velocityTracker.addPosition(event.uptimeMillis, event.position)
                        val delta = event.positionChange().axisValue()
                        if (enabled.value) {
                            with(scrollLogic.value) {
                                dispatchScroll(delta, NestedScrollSource.Drag)
                            }
                        }
                        event.consumePositionChange()
                    }
                    result = if (isVertical()) {
                        verticalDrag(drag.id, dragTick)
                    } else {
                        horizontalDrag(drag.id, dragTick)
                    }
                }
            }
        } catch (c: CancellationException) {
            result = false
        }
        return result
    }

    suspend fun fling(velocity: Velocity) {
        val preConsumedByParent = nestedScrollDispatcher.value.dispatchPreFling(velocity)
        val available = velocity - preConsumedByParent
        val velocityLeft = scrollLogic.value.doFlingAnimation(available)
        nestedScrollDispatcher.value.dispatchPostFling(available - velocityLeft, velocityLeft)
    }

    val scrollLambda: suspend PointerInputScope.() -> Unit = remember {
        {
            forEachGesture {
                val (startEvent, initialDelta) = initialDown()
                if (startEvent != null) {
                    val velocityTracker = VelocityTracker()
                    // remember enabled state when we add interaction to remove later if needed
                    val enabledWhenInteractionAdded = enabled.value
                    if (enabledWhenInteractionAdded) {
                        interactionState.value?.addInteraction(Interaction.Dragged)
                    }
                    val isDragSuccessful = mainDragCycle(startEvent, initialDelta, velocityTracker)
                    if (enabledWhenInteractionAdded) {
                        interactionState.value?.removeInteraction(Interaction.Dragged)
                    }
                    if (isDragSuccessful) {
                        nestedScrollDispatcher.value.coroutineScope.launch {
                            fling(velocityTracker.calculateVelocity())
                        }
                    }
                }
            }
        }
    }
    return pointerInput(scrollLambda, scrollLambda)
}

private class ScrollingLogic(
    val orientation: Orientation,
    val reverseDirection: Boolean,
    val nestedScrollDispatcher: State<NestedScrollDispatcher>,
    val scrollableState: ScrollableState,
    val flingBehavior: FlingBehavior
) {
    fun Float.toOffset(): Offset =
        if (orientation == Horizontal) Offset(this, 0f) else Offset(0f, this)

    fun Float.toVelocity(): Velocity =
        if (orientation == Horizontal) Velocity(this, 0f) else Velocity(0f, this)

    fun Offset.toFloat(): Float =
        if (orientation == Horizontal) this.x else this.y

    fun Velocity.toFloat(): Float =
        if (orientation == Horizontal) this.x else this.y

    fun Float.reverseIfNeeded(): Float = if (reverseDirection) this * -1 else this

    fun ScrollScope.dispatchScroll(scrollDelta: Float, source: NestedScrollSource): Float {
        val scrollOffset = scrollDelta.toOffset()
        val preConsumedByParent = nestedScrollDispatcher.value
            .dispatchPreScroll(scrollOffset, source)

        val scrollAvailable = scrollOffset - preConsumedByParent
        val consumed = scrollBy(scrollAvailable.toFloat().reverseIfNeeded())
            .reverseIfNeeded().toOffset()
        val leftForParent = scrollAvailable - consumed
        nestedScrollDispatcher.value.dispatchPostScroll(consumed, leftForParent, source)
        return leftForParent.toFloat()
    }

    fun performRawScroll(scroll: Offset): Offset {
        return if (scrollableState.isScrollInProgress) {
            Offset.Zero
        } else {
            scrollableState.dispatchRawDelta(scroll.toFloat().reverseIfNeeded())
                .reverseIfNeeded().toOffset()
        }
    }

    suspend fun doFlingAnimation(available: Velocity): Velocity {
        var result: Velocity = available
        // come up with the better threshold, but we need it since spline curve gives us NaNs
        if (abs(available.toFloat()) > 1f) scrollableState.scroll {
            val outerScopeScroll: (Float) -> Float =
                { delta -> this.dispatchScroll(delta, NestedScrollSource.Fling) }
            val scope = object : ScrollScope {
                override fun scrollBy(pixels: Float): Float {
                    return outerScopeScroll.invoke(pixels)
                }
            }
            with(scope) {
                with(flingBehavior) {
                    result = performFling(available.toFloat()).toVelocity()
                }
            }
        }
        return result
    }
}

private fun scrollableNestedScrollConnection(
    scrollLogic: State<ScrollingLogic>
): NestedScrollConnection = object : NestedScrollConnection {
    override fun onPostScroll(
        consumed: Offset,
        available: Offset,
        source: NestedScrollSource
    ): Offset = scrollLogic.value.performRawScroll(available)

    override suspend fun onPostFling(
        consumed: Velocity,
        available: Velocity
    ): Velocity {
        val velocityLeft = scrollLogic.value.doFlingAnimation(available)
        return available - velocityLeft
    }
}

private class DefaultFlingBehavior(
    private val flingDecay: DecayAnimationSpec<Float>
) : FlingBehavior {
    override suspend fun ScrollScope.performFling(initialVelocity: Float): Float {
        var velocityLeft = initialVelocity
        var lastValue = 0f
        AnimationState(
            initialValue = 0f,
            initialVelocity = initialVelocity,
        ).animateDecay(flingDecay) {
            val delta = value - lastValue
            val left = scrollBy(delta)
            lastValue = value
            velocityLeft = this.velocity
            // avoid rounding errors and stop if anything is unconsumed
            if (abs(left) > 0.5f) this.cancelAnimation()
        }
        return velocityLeft
    }
}