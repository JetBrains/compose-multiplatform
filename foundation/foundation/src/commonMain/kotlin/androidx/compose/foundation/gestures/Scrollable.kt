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
import androidx.compose.animation.rememberSplineBasedDecay
import androidx.compose.foundation.MutatePriority
import androidx.compose.foundation.gestures.Orientation.Horizontal
import androidx.compose.foundation.gestures.Orientation.Vertical
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollDispatcher
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.NestedScrollSource.Companion.Drag
import androidx.compose.ui.input.nestedscroll.NestedScrollSource.Companion.Fling
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.layout.onRelocationRequest
import androidx.compose.ui.platform.debugInspectorInfo
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.toSize
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
 * @param interactionSource [MutableInteractionSource] that will be used to emit
 * drag events when this scrollable is being dragged.
 */
fun Modifier.scrollable(
    state: ScrollableState,
    orientation: Orientation,
    enabled: Boolean = true,
    reverseDirection: Boolean = false,
    flingBehavior: FlingBehavior? = null,
    interactionSource: MutableInteractionSource? = null
): Modifier = scrollable(
    state = state,
    orientation = orientation,
    enabled = enabled,
    reverseDirection = reverseDirection,
    flingBehavior = flingBehavior,
    interactionSource = interactionSource,
    overScrollController = null
)

internal fun Modifier.scrollable(
    state: ScrollableState,
    orientation: Orientation,
    overScrollController: OverScrollController?,
    enabled: Boolean = true,
    reverseDirection: Boolean = false,
    flingBehavior: FlingBehavior? = null,
    interactionSource: MutableInteractionSource? = null
): Modifier = composed(
    inspectorInfo = debugInspectorInfo {
        name = "scrollable"
        properties["orientation"] = orientation
        properties["state"] = state
        properties["overScrollController"] = overScrollController
        properties["enabled"] = enabled
        properties["reverseDirection"] = reverseDirection
        properties["flingBehavior"] = flingBehavior
        properties["interactionSource"] = interactionSource
    },
    factory = {
        val overscrollModifier =
            if (overScrollController != null) {
                Modifier.overScroll(overScrollController)
            } else {
                Modifier
            }

        fun Float.reverseIfNeeded(): Float = if (reverseDirection) this * -1 else this
        relocationScrollable(state, orientation, reverseDirection)
            .then(overscrollModifier)
            .touchScrollable(
                interactionSource,
                orientation,
                reverseDirection,
                state,
                flingBehavior,
                overScrollController,
                enabled
            )
            .mouseScrollable(orientation) {
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
        val flingSpec = rememberSplineBasedDecay<Float>()
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
private fun Modifier.touchScrollable(
    interactionSource: MutableInteractionSource?,
    orientation: Orientation,
    reverseDirection: Boolean,
    controller: ScrollableState,
    flingBehavior: FlingBehavior?,
    overScrollController: OverScrollController?,
    enabled: Boolean
): Modifier {
    val fling = flingBehavior ?: ScrollableDefaults.flingBehavior()
    val nestedScrollDispatcher = remember { mutableStateOf(NestedScrollDispatcher()) }
    val scrollLogic = rememberUpdatedState(
        ScrollingLogic(
            orientation,
            reverseDirection,
            nestedScrollDispatcher,
            controller,
            fling,
            overScrollController
        )
    )
    val nestedScrollConnection = remember(enabled) {
        scrollableNestedScrollConnection(scrollLogic, enabled)
    }
    val draggableState = remember { ScrollDraggableState(scrollLogic) }

    return draggable(
        { draggableState },
        orientation = orientation,
        enabled = enabled,
        interactionSource = interactionSource,
        reverseDirection = false,
        startDragImmediately = { scrollLogic.value.shouldScrollImmediately() },
        onDragStopped = { velocity ->
            nestedScrollDispatcher.value.coroutineScope.launch {
                scrollLogic.value.onDragStopped(velocity)
            }
        },
        canDrag = { down -> down.type != PointerType.Mouse }
    ).nestedScroll(nestedScrollConnection, nestedScrollDispatcher.value)
}

private class ScrollingLogic(
    val orientation: Orientation,
    val reverseDirection: Boolean,
    val nestedScrollDispatcher: State<NestedScrollDispatcher>,
    val scrollableState: ScrollableState,
    val flingBehavior: FlingBehavior,
    val overScrollController: OverScrollController?
) {
    fun Float.toOffset(): Offset = when {
        this == 0f -> Offset.Zero
        orientation == Horizontal -> Offset(this, 0f)
        else -> Offset(0f, this)
    }

    fun Float.toVelocity(): Velocity =
        if (orientation == Horizontal) Velocity(this, 0f) else Velocity(0f, this)

    fun Offset.toFloat(): Float =
        if (orientation == Horizontal) this.x else this.y

    fun Velocity.toFloat(): Float =
        if (orientation == Horizontal) this.x else this.y

    fun Velocity.update(newValue: Float): Velocity =
        if (orientation == Horizontal) copy(x = newValue) else copy(y = newValue)

    fun Float.reverseIfNeeded(): Float = if (reverseDirection) this * -1 else this

    fun ScrollScope.dispatchScroll(
        scrollDelta: Float,
        pointerPosition: Offset?,
        source: NestedScrollSource
    ): Float {
        val overScrollPreConsumed =
            overScrollController
                ?.consumePreScroll(scrollDelta.toOffset(), pointerPosition, source)
                ?.toFloat()
                ?: 0f
        val afterPreOverscroll = scrollDelta - overScrollPreConsumed
        val nestedScrollDispatcher = nestedScrollDispatcher.value
        val preConsumedByParent = nestedScrollDispatcher
            .dispatchPreScroll(afterPreOverscroll.toOffset(), source)

        val scrollAvailable = afterPreOverscroll - preConsumedByParent.toFloat()
        val consumed = scrollBy(scrollAvailable.reverseIfNeeded()).reverseIfNeeded()
        val leftForParent = scrollAvailable - consumed
        val parentConsumed = nestedScrollDispatcher
            .dispatchPostScroll(consumed.toOffset(), leftForParent.toOffset(), source)
        overScrollController?.consumePostScroll(
            scrollAvailable.toOffset(),
            (leftForParent - parentConsumed.toFloat()).toOffset(),
            pointerPosition,
            source
        )
        return leftForParent
    }

    fun performRawScroll(scroll: Offset): Offset {
        return if (scrollableState.isScrollInProgress) {
            Offset.Zero
        } else {
            scrollableState.dispatchRawDelta(scroll.toFloat().reverseIfNeeded())
                .reverseIfNeeded().toOffset()
        }
    }

    suspend fun onDragStopped(axisVelocity: Float) {
        val preOverscrollConsumed = overScrollController
            ?.consumePreFling(axisVelocity.toVelocity())?.toFloat()
            ?: 0f
        val velocity = (axisVelocity - preOverscrollConsumed).toVelocity()
        val preConsumedByParent = nestedScrollDispatcher.value.dispatchPreFling(velocity)
        val available = velocity - preConsumedByParent
        val velocityLeft = doFlingAnimation(available)
        val consumedPost =
            nestedScrollDispatcher.value.dispatchPostFling(available - velocityLeft, velocityLeft)
        val totalLeft = velocityLeft - consumedPost
        overScrollController?.consumePostFling(totalLeft.toFloat().toVelocity())
        overScrollController?.release()
    }

    suspend fun doFlingAnimation(available: Velocity): Velocity {
        var result: Velocity = available
        scrollableState.scroll {
            val outerScopeScroll: (Float) -> Float = { delta ->
                val consumed = this.dispatchScroll(delta.reverseIfNeeded(), null, Fling)
                delta - consumed.reverseIfNeeded()
            }
            val scope = object : ScrollScope {
                override fun scrollBy(pixels: Float): Float {
                    return outerScopeScroll.invoke(pixels)
                }
            }
            with(scope) {
                with(flingBehavior) {
                    result = result.update(
                        performFling(available.toFloat().reverseIfNeeded()).reverseIfNeeded()
                    )
                }
            }
        }
        return result
    }

    fun shouldScrollImmediately(): Boolean {
        return scrollableState.isScrollInProgress ||
            overScrollController?.stopOverscrollAnimation() ?: false
    }
}

private class ScrollDraggableState(
    val scrollLogic: State<ScrollingLogic>
) : PointerAwareDraggableState, PointerAwareDragScope {
    var latestScrollScope: ScrollScope = NoOpScrollScope

    override fun dragBy(pixels: Float, pointerPosition: Offset) {
        with(scrollLogic.value) {
            with(latestScrollScope) {
                dispatchScroll(pixels, pointerPosition, Drag)
            }
        }
    }

    override suspend fun drag(
        dragPriority: MutatePriority,
        block: suspend PointerAwareDragScope.() -> Unit
    ) {
        scrollLogic.value.scrollableState.scroll(dragPriority) {
            latestScrollScope = this
            block()
        }
    }

    override fun dispatchRawDelta(delta: Float) {
        with(scrollLogic.value) { performRawScroll(delta.toOffset()) }
    }
}

private val NoOpScrollScope: ScrollScope = object : ScrollScope {
    override fun scrollBy(pixels: Float): Float = pixels
}

private fun scrollableNestedScrollConnection(
    scrollLogic: State<ScrollingLogic>,
    enabled: Boolean
): NestedScrollConnection = object : NestedScrollConnection {
    override fun onPostScroll(
        consumed: Offset,
        available: Offset,
        source: NestedScrollSource
    ): Offset = if (enabled) {
        scrollLogic.value.performRawScroll(available)
    } else {
        Offset.Zero
    }

    override suspend fun onPostFling(
        consumed: Velocity,
        available: Velocity
    ): Velocity {
        return if (enabled) {
            val velocityLeft = scrollLogic.value.doFlingAnimation(available)
            available - velocityLeft
        } else {
            Velocity.Zero
        }
    }
}

private class DefaultFlingBehavior(
    private val flingDecay: DecayAnimationSpec<Float>
) : FlingBehavior {
    override suspend fun ScrollScope.performFling(initialVelocity: Float): Float {
        // come up with the better threshold, but we need it since spline curve gives us NaNs
        return if (abs(initialVelocity) > 1f) {
            var velocityLeft = initialVelocity
            var lastValue = 0f
            AnimationState(
                initialValue = 0f,
                initialVelocity = initialVelocity,
            ).animateDecay(flingDecay) {
                val delta = value - lastValue
                val consumed = scrollBy(delta)
                lastValue = value
                velocityLeft = this.velocity
                // avoid rounding errors and stop if anything is unconsumed
                if (abs(delta - consumed) > 0.5f) this.cancelAnimation()
            }
            velocityLeft
        } else {
            initialVelocity
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
private fun Modifier.relocationScrollable(
    scrollableState: ScrollableState,
    orientation: Orientation,
    reverseDirection: Boolean = false
): Modifier {
    fun Float.reverseIfNeeded(): Float = if (reverseDirection) this * -1 else this
    return this.onRelocationRequest(
        onProvideDestination = { rect, layoutCoordinates ->
            val size = layoutCoordinates.size.toSize()
            when (orientation) {
                Vertical ->
                    rect.translate(0f, relocationDistance(rect.top, rect.bottom, size.height))
                Horizontal ->
                    rect.translate(relocationDistance(rect.left, rect.right, size.width), 0f)
            }
        },
        onPerformRelocation = { source, destination ->
            val offset = when (orientation) {
                Vertical -> source.top - destination.top
                Horizontal -> source.left - destination.left
            }
            scrollableState.animateScrollBy(offset.reverseIfNeeded())
        }
    )
}

// Calculate the offset needed to bring one of the edges into view. The leadingEdge is the side
// closest to the origin (For the x-axis this is 'left', for the y-axis this is 'top').
// The trailing edge is the other side (For the x-axis this is 'right', for the y-axis this is
// 'bottom').
private fun relocationDistance(leadingEdge: Float, trailingEdge: Float, parentSize: Float) = when {
    // If the item is already visible, no need to scroll.
    leadingEdge >= 0 && trailingEdge <= parentSize -> 0f

    // If the item is visible but larger than the parent, we don't scroll.
    leadingEdge < 0 && trailingEdge > parentSize -> 0f

    // Find the minimum scroll needed to make one of the edges coincide with the parent's edge.
    abs(leadingEdge) < abs(trailingEdge - parentSize) -> leadingEdge
    else -> trailingEdge - parentSize
}
