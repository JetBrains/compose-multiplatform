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
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.MutatePriority
import androidx.compose.foundation.OverscrollEffect
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.gestures.Orientation.Horizontal
import androidx.compose.foundation.gestures.Orientation.Vertical
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.onFocusedBoundsChanged
import androidx.compose.foundation.relocation.BringIntoViewResponder
import androidx.compose.foundation.relocation.bringIntoViewResponder
import androidx.compose.foundation.rememberOverscrollEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollDispatcher
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.NestedScrollSource.Companion.Drag
import androidx.compose.ui.input.nestedscroll.NestedScrollSource.Companion.Fling
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.AwaitPointerEventScope
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.OnPlacedModifier
import androidx.compose.ui.layout.OnRemeasuredModifier
import androidx.compose.ui.modifier.ModifierLocalProvider
import androidx.compose.ui.modifier.modifierLocalOf
import androidx.compose.ui.platform.debugInspectorInfo
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.toSize
import androidx.compose.ui.util.fastAll
import androidx.compose.ui.util.fastForEach
import kotlin.math.abs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

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
@OptIn(ExperimentalFoundationApi::class)
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
    overscrollEffect = null
)

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
 * This overload provides the access to [OverscrollEffect] that defines the behaviour of the
 * over scrolling logic. Consider using [ScrollableDefaults.overscrollEffect] for the platform
 * look-and-feel.
 *
 * @sample androidx.compose.foundation.samples.ScrollableSample
 *
 * @param state [ScrollableState] state of the scrollable. Defines how scroll events will be
 * interpreted by the user land logic and contains useful information about on-going events.
 * @param orientation orientation of the scrolling
 * @param overscrollEffect effect to which the deltas will be fed when the scrollable have
 * some scrolling delta left. Pass `null` for no overscroll. If you pass an effect you should
 * also apply [androidx.compose.foundation.overscroll] modifier.
 * @param enabled whether or not scrolling in enabled
 * @param reverseDirection reverse the direction of the scroll, so top to bottom scroll will
 * behave like bottom to top and left to right will behave like right to left.
 * @param flingBehavior logic describing fling behavior when drag has finished with velocity. If
 * `null`, default from [ScrollableDefaults.flingBehavior] will be used.
 * @param interactionSource [MutableInteractionSource] that will be used to emit
 * drag events when this scrollable is being dragged.
 */
@ExperimentalFoundationApi
fun Modifier.scrollable(
    state: ScrollableState,
    orientation: Orientation,
    overscrollEffect: OverscrollEffect?,
    enabled: Boolean = true,
    reverseDirection: Boolean = false,
    flingBehavior: FlingBehavior? = null,
    interactionSource: MutableInteractionSource? = null
): Modifier = composed(
    inspectorInfo = debugInspectorInfo {
        name = "scrollable"
        properties["orientation"] = orientation
        properties["state"] = state
        properties["overscrollEffect"] = overscrollEffect
        properties["enabled"] = enabled
        properties["reverseDirection"] = reverseDirection
        properties["flingBehavior"] = flingBehavior
        properties["interactionSource"] = interactionSource
    },
    factory = {
        val coroutineScope = rememberCoroutineScope()
        val keepFocusedChildInViewModifier =
            remember(coroutineScope, orientation, state, reverseDirection) {
                ContentInViewModifier(coroutineScope, orientation, state, reverseDirection)
            }

        Modifier
            .focusGroup()
            .then(keepFocusedChildInViewModifier.modifier)
            .pointerScrollable(
                interactionSource,
                orientation,
                reverseDirection,
                state,
                flingBehavior,
                overscrollEffect,
                enabled
            )
            .then(if (enabled) ModifierLocalScrollableContainerProvider else Modifier)
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

    /**
     * Create and remember default [OverscrollEffect] that will be used for showing over scroll
     * effects.
     */
    @Composable
    @ExperimentalFoundationApi
    fun overscrollEffect(): OverscrollEffect {
        return rememberOverscrollEffect()
    }
}

internal interface ScrollConfig {
    fun Density.calculateMouseWheelScroll(event: PointerEvent, bounds: IntSize): Offset
}

@Composable
internal expect fun platformScrollConfig(): ScrollConfig

@Suppress("ComposableModifierFactory")
@Composable
@OptIn(ExperimentalFoundationApi::class)
private fun Modifier.pointerScrollable(
    interactionSource: MutableInteractionSource?,
    orientation: Orientation,
    reverseDirection: Boolean,
    controller: ScrollableState,
    flingBehavior: FlingBehavior?,
    overscrollEffect: OverscrollEffect?,
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
            overscrollEffect
        )
    )
    val nestedScrollConnection = remember(enabled) {
        scrollableNestedScrollConnection(scrollLogic, enabled)
    }
    val draggableState = remember { ScrollDraggableState(scrollLogic) }
    val scrollConfig = platformScrollConfig()

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
    )
        .mouseWheelScroll(scrollLogic, scrollConfig)
        .nestedScroll(nestedScrollConnection, nestedScrollDispatcher.value)
}

private fun Modifier.mouseWheelScroll(
    scrollingLogicState: State<ScrollingLogic>,
    mouseWheelScrollConfig: ScrollConfig,
) = pointerInput(scrollingLogicState, mouseWheelScrollConfig) {
    awaitPointerEventScope {
        while (true) {
            val event = awaitScrollEvent()
            if (event.changes.fastAll { !it.isConsumed }) {
                with(mouseWheelScrollConfig) {
                    val scrollAmount = calculateMouseWheelScroll(event, size)
                    with(scrollingLogicState.value) {
                        val delta = scrollAmount.toFloat().reverseIfNeeded()
                        val consumedDelta = scrollableState.dispatchRawDelta(delta)
                        if (consumedDelta != 0f) {
                            event.changes.fastForEach { it.consume() }
                        }
                    }
                }
            }
        }
    }
}

private suspend fun AwaitPointerEventScope.awaitScrollEvent(): PointerEvent {
    var event: PointerEvent
    do {
        event = awaitPointerEvent()
    } while (event.type != PointerEventType.Scroll)
    return event
}

@OptIn(ExperimentalFoundationApi::class)
private class ScrollingLogic(
    val orientation: Orientation,
    val reverseDirection: Boolean,
    val nestedScrollDispatcher: State<NestedScrollDispatcher>,
    val scrollableState: ScrollableState,
    val flingBehavior: FlingBehavior,
    val overscrollEffect: OverscrollEffect?
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

    fun Offset.reverseIfNeeded(): Offset = if (reverseDirection) this * -1f else this

    fun ScrollScope.dispatchScroll(
        scrollDelta: Offset,
        pointerPosition: Offset?,
        source: NestedScrollSource
    ): Offset {
        val overscrollPreConsumed =
            if (overscrollEffect != null && overscrollEffect.isEnabled) {
                overscrollEffect.consumePreScroll(scrollDelta, pointerPosition, source)
            } else {
                Offset.Zero
            }

        val afterPreOverscroll = scrollDelta - overscrollPreConsumed
        val nestedScrollDispatcher = nestedScrollDispatcher.value
        val preConsumedByParent = nestedScrollDispatcher
            .dispatchPreScroll(afterPreOverscroll, source)

        val scrollAvailable = afterPreOverscroll - preConsumedByParent

        // Consume on a single axis
        val axisConsumed =
            scrollBy(scrollAvailable.reverseIfNeeded().toFloat()).toOffset().reverseIfNeeded()

        val leftForParent = scrollAvailable - axisConsumed
        val parentConsumed = nestedScrollDispatcher
            .dispatchPostScroll(axisConsumed, leftForParent, source)
        if (overscrollEffect != null && overscrollEffect.isEnabled) {
            overscrollEffect.consumePostScroll(
                scrollAvailable,
                (leftForParent - parentConsumed),
                pointerPosition,
                source
            )
        }
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
        val preOverscrollConsumed =
            if (overscrollEffect != null && overscrollEffect.isEnabled) {
                overscrollEffect.consumePreFling(axisVelocity.toVelocity()).toFloat()
            } else {
                0f
            }
        val velocity = (axisVelocity - preOverscrollConsumed).toVelocity()
        val preConsumedByParent = nestedScrollDispatcher.value.dispatchPreFling(velocity)
        val available = velocity - preConsumedByParent
        val velocityLeft = doFlingAnimation(available)
        val consumedPost =
            nestedScrollDispatcher.value.dispatchPostFling(
                available - velocityLeft,
                velocityLeft
            )
        val totalLeft = velocityLeft - consumedPost
        if (overscrollEffect != null && overscrollEffect.isEnabled) {
            overscrollEffect.consumePostFling(totalLeft.toFloat().toVelocity())
        }
    }

    suspend fun doFlingAnimation(available: Velocity): Velocity {
        var result: Velocity = available
        scrollableState.scroll {
            val outerScopeScroll: (Offset) -> Offset = { delta ->
                val consumed = this.dispatchScroll(delta.reverseIfNeeded(), null, Fling)
                delta - consumed.reverseIfNeeded()
            }
            val scope = object : ScrollScope {
                override fun scrollBy(pixels: Float): Float {
                    return outerScopeScroll.invoke(pixels.toOffset()).toFloat()
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
            overscrollEffect?.isInProgress ?: false
    }
}

private class ScrollDraggableState(
    val scrollLogic: State<ScrollingLogic>
) : PointerAwareDraggableState, PointerAwareDragScope {
    var latestScrollScope: ScrollScope = NoOpScrollScope

    override fun dragBy(pixels: Float, pointerPosition: Offset) {
        with(scrollLogic.value) {
            with(latestScrollScope) {
                dispatchScroll(pixels.toOffset(), pointerPosition, Drag)
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

/**
 * Handles any logic related to bringing or keeping content in view, including
 * [BringIntoViewResponder] and ensuring the focused child stays in view when the scrollable area
 * is shrunk.
 */
@OptIn(ExperimentalFoundationApi::class)
private class ContentInViewModifier(
    private val scope: CoroutineScope,
    private val orientation: Orientation,
    private val scrollableState: ScrollableState,
    private val reverseDirection: Boolean
) : BringIntoViewResponder, OnRemeasuredModifier, OnPlacedModifier {
    private var focusedChild: LayoutCoordinates? = null
    private var coordinates: LayoutCoordinates? = null
    private var oldSize: IntSize? = null

    val modifier: Modifier = this
        .onFocusedBoundsChanged { focusedChild = it }
        .bringIntoViewResponder(this)

    override fun onRemeasured(size: IntSize) {
        val coordinates = coordinates
        val oldSize = oldSize
        // We only care when this node becomes smaller than it previously was, so don't care about
        // the initial measurement.
        if (oldSize != null && oldSize != size && coordinates?.isAttached == true) {
            onSizeChanged(coordinates, oldSize)
        }
        this.oldSize = size
    }

    override fun onPlaced(coordinates: LayoutCoordinates) {
        this.coordinates = coordinates
    }

    override fun calculateRectForParent(localRect: Rect): Rect {
        val oldSize = checkNotNull(oldSize) {
            "Expected BringIntoViewRequester to not be used before parents are placed."
        }
        // oldSize will only be null before the initial measurement.
        return computeDestination(localRect, oldSize)
    }

    override suspend fun bringChildIntoView(localRect: Rect) {
        performBringIntoView(localRect, calculateRectForParent(localRect))
    }

    private fun onSizeChanged(coordinates: LayoutCoordinates, oldSize: IntSize) {
        val containerShrunk = if (orientation == Horizontal) {
            coordinates.size.width < oldSize.width
        } else {
            coordinates.size.height < oldSize.height
        }
        // If the container is growing, then if the focused child is only partially visible it will
        // soon be _more_ visible, so don't scroll.
        if (!containerShrunk) return

        val focusedBounds = focusedChild
            ?.let { coordinates.localBoundingBoxOf(it, clipBounds = false) }
            ?: return
        val myOldBounds = Rect(Offset.Zero, oldSize.toSize())
        val adjustedBounds = computeDestination(focusedBounds, coordinates.size)
        val wasVisible = myOldBounds.overlaps(focusedBounds)
        val isFocusedChildClipped = adjustedBounds != focusedBounds

        if (wasVisible && isFocusedChildClipped) {
            scope.launch {
                performBringIntoView(focusedBounds, adjustedBounds)
            }
        }
    }

    /**
     * Compute the destination given the source rectangle and current bounds.
     *
     * @param source The bounding box of the item that sent the request to be brought into view.
     * @return the destination rectangle.
     */
    private fun computeDestination(source: Rect, intSize: IntSize): Rect {
        val size = intSize.toSize()
        return when (orientation) {
            Vertical ->
                source.translate(0f, relocationDistance(source.top, source.bottom, size.height))
            Horizontal ->
                source.translate(relocationDistance(source.left, source.right, size.width), 0f)
        }
    }

    /**
     * Using the source and destination bounds, perform an animated scroll.
     */
    private suspend fun performBringIntoView(source: Rect, destination: Rect) {
        val offset = when (orientation) {
            Vertical -> source.top - destination.top
            Horizontal -> source.left - destination.left
        }
        val scrollDelta = if (reverseDirection) -offset else offset

        // Note that this results in weird behavior if called before the previous
        // performBringIntoView finishes due to b/220119990.
        scrollableState.animateScrollBy(scrollDelta)
    }

    /**
     * Calculate the offset needed to bring one of the edges into view. The leadingEdge is the side
     * closest to the origin (For the x-axis this is 'left', for the y-axis this is 'top').
     * The trailing edge is the other side (For the x-axis this is 'right', for the y-axis this is
     * 'bottom').
     */
    private fun relocationDistance(leadingEdge: Float, trailingEdge: Float, parentSize: Float) =
        when {
            // If the item is already visible, no need to scroll.
            leadingEdge >= 0 && trailingEdge <= parentSize -> 0f

            // If the item is visible but larger than the parent, we don't scroll.
            leadingEdge < 0 && trailingEdge > parentSize -> 0f

            // Find the minimum scroll needed to make one of the edges coincide with the parent's edge.
            abs(leadingEdge) < abs(trailingEdge - parentSize) -> leadingEdge
            else -> trailingEdge - parentSize
        }
}

// TODO: b/203141462 - make this public and move it to ui
/**
 * Whether this modifier is inside a scrollable container, provided by [Modifier.scrollable].
 * Defaults to false.
 */
internal val ModifierLocalScrollableContainer = modifierLocalOf { false }

private object ModifierLocalScrollableContainerProvider : ModifierLocalProvider<Boolean> {
    override val key = ModifierLocalScrollableContainer
    override val value = true
}
