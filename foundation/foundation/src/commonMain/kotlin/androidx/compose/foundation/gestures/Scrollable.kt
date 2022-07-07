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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
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
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
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

    fun Offset.singleAxisOffset(): Offset =
        if (orientation == Horizontal) copy(y = 0f) else copy(x = 0f)

    fun Offset.toFloat(): Float =
        if (orientation == Horizontal) this.x else this.y

    fun Velocity.toFloat(): Float =
        if (orientation == Horizontal) this.x else this.y

    fun Velocity.singleAxisVelocity(): Velocity =
        if (orientation == Horizontal) copy(y = 0f) else copy(x = 0f)

    fun Velocity.update(newValue: Float): Velocity =
        if (orientation == Horizontal) copy(x = newValue) else copy(y = newValue)

    fun Float.reverseIfNeeded(): Float = if (reverseDirection) this * -1 else this

    fun Offset.reverseIfNeeded(): Offset = if (reverseDirection) this * -1f else this

    fun ScrollScope.dispatchScroll(
        availableDelta: Offset,
        pointerPosition: Offset?,
        source: NestedScrollSource
    ): Offset {
        val scrollDelta = availableDelta.singleAxisOffset()
        val overscrollPreConsumed = overscrollPreConsumeDelta(scrollDelta, pointerPosition, source)

        val afterPreOverscroll = scrollDelta - overscrollPreConsumed
        val nestedScrollDispatcher = nestedScrollDispatcher.value
        val preConsumedByParent = nestedScrollDispatcher
            .dispatchPreScroll(afterPreOverscroll, source)

        val scrollAvailable = afterPreOverscroll - preConsumedByParent
        // Consume on a single axis
        val axisConsumed =
            scrollBy(scrollAvailable.reverseIfNeeded().toFloat()).toOffset().reverseIfNeeded()

        val leftForParent = scrollAvailable - axisConsumed
        val parentConsumed = nestedScrollDispatcher.dispatchPostScroll(
            axisConsumed,
            leftForParent,
            source
        )
        overscrollPostConsumeDelta(
            scrollAvailable,
            leftForParent - parentConsumed,
            pointerPosition,
            source
        )
        return leftForParent
    }

    fun overscrollPreConsumeDelta(
        scrollDelta: Offset,
        pointerPosition: Offset?,
        source: NestedScrollSource
    ): Offset {
        return if (overscrollEffect != null && overscrollEffect.isEnabled) {
            overscrollEffect.consumePreScroll(scrollDelta, pointerPosition, source)
        } else {
            Offset.Zero
        }
    }

    private fun overscrollPostConsumeDelta(
        consumedByChain: Offset,
        availableForOverscroll: Offset,
        pointerPosition: Offset?,
        source: NestedScrollSource
    ) {
        if (overscrollEffect != null && overscrollEffect.isEnabled) {
            overscrollEffect.consumePostScroll(
                consumedByChain,
                availableForOverscroll,
                pointerPosition,
                source
            )
        }
    }

    fun performRawScroll(scroll: Offset): Offset {
        return if (scrollableState.isScrollInProgress) {
            Offset.Zero
        } else {
            scrollableState.dispatchRawDelta(scroll.toFloat().reverseIfNeeded())
                .reverseIfNeeded().toOffset()
        }
    }

    suspend fun onDragStopped(initialVelocity: Velocity) {
        val availableVelocity = initialVelocity.singleAxisVelocity()
        val preOverscrollConsumed =
            if (overscrollEffect != null && overscrollEffect.isEnabled) {
                overscrollEffect.consumePreFling(availableVelocity)
            } else {
                Velocity.Zero
            }
        val velocity = (availableVelocity - preOverscrollConsumed)
        val preConsumedByParent = nestedScrollDispatcher
            .value.dispatchPreFling(velocity)
        val available = velocity - preConsumedByParent
        val velocityLeft = doFlingAnimation(available)
        val consumedPost =
            nestedScrollDispatcher.value.dispatchPostFling(
                (available - velocityLeft),
                velocityLeft
            )
        val totalLeft = velocityLeft - consumedPost
        if (overscrollEffect != null && overscrollEffect.isEnabled) {
            overscrollEffect.consumePostFling(totalLeft)
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

    override fun dragBy(pixels: Offset, pointerPosition: Offset) {
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

    override fun dispatchRawDelta(delta: Offset) {
        with(scrollLogic.value) { performRawScroll(delta) }
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

    // These properties are used to detect the case where the viewport size is animated shrinking
    // while the scroll animation used to keep the focused child in view is still running.
    private var focusedChildBeingAnimated: LayoutCoordinates? = null
    private var focusTargetBounds: Rect? by mutableStateOf(null)
    private var focusAnimationJob: Job? = null

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
        performBringIntoView(
            source = localRect,
            destination = calculateRectForParent(localRect)
        )
    }

    /**
     * Handles when the size of the scroll viewport changes by making sure any focused child is kept
     * appropriately visible when the viewport shrinks and would otherwise hide it.
     *
     * One common instance of this is when a text field in a scrollable near the bottom is focused
     * while the soft keyboard is hidden, causing the keyboard to show, and cover the field.
     * See b/192043120 and related bugs.
     *
     * To future debuggers of this method, it might be helpful to add a draw modifier to the chain
     * above to draw the focus target bounds:
     * ```
     * .drawWithContent {
     *   drawContent()
     *   focusTargetBounds?.let {
     *     drawRect(
     *       Color.Red,
     *       topLeft = it.topLeft,
     *       size = it.size,
     *       style = Stroke(1.dp.toPx())
     *     )
     *   }
     * }
     * ```
     */
    private fun onSizeChanged(coordinates: LayoutCoordinates, oldSize: IntSize) {
        val containerShrunk = if (orientation == Horizontal) {
            coordinates.size.width < oldSize.width
        } else {
            coordinates.size.height < oldSize.height
        }
        // If the container is growing, then if the focused child is only partially visible it will
        // soon be _more_ visible, so don't scroll.
        if (!containerShrunk) return

        val focusedChild = focusedChild ?: return
        val focusedBounds = coordinates.localBoundingBoxOf(focusedChild, clipBounds = false)

        // In order to check if we need to scroll to bring the focused child into view, it's not
        // enough to consider where the child actually is right now. If the viewport was recently
        // shrunk, we may have already started a scroll animation to bring it into view. In that
        // case, we need to compare with the target of the animation, not the current position. If
        // we don't do that, then in some cases when the viewport size is being animated (e.g. when
        // the keyboard insets are being animated on API 30+) we might stop trying to keep the
        // focused child in view before the viewport animation is finished, and the scroll animation
        // will stop short and leave the focused child out of the viewport. See b/230756508.
        val eventualFocusedBounds = if (focusedChild === focusedChildBeingAnimated) {
            // A previous call to this method started an animation that is still running, so compare
            // with the target of that animation.
            checkNotNull(focusTargetBounds)
        } else {
            focusedBounds
        }

        val myOldBounds = Rect(Offset.Zero, oldSize.toSize())
        if (!myOldBounds.overlaps(eventualFocusedBounds)) {
            // The focused child was not visible before the resize, so we don't need to keep
            // it visible.
            return
        }

        val targetBounds = computeDestination(eventualFocusedBounds, coordinates.size)
        if (targetBounds == eventualFocusedBounds) {
            // The focused child is already fully visible (not clipped or hidden) after the resize,
            // or will be after it finishes animating, so we don't need to do anything.
            return
        }

        // If execution has gotten to this point, it means the focused child was at least partially
        // visible before the resize, and it is either partially clipped or completely hidden after
        // the resize, so we need to adjust scroll to keep it in view.
        focusedChildBeingAnimated = focusedChild
        focusTargetBounds = targetBounds
        scope.launch(NonCancellable) {
            val job = launch {
                // Animate the scroll offset to keep the focused child in view. This is a suspending
                // call that will suspend until the animation is finished, and only return if it
                // completes. If any other scroll operations are performed after the animation starts,
                // e.g. the viewport shrinks again or the user manually scrolls, this animation will
                // be cancelled and this function will throw a CancellationException.
                performBringIntoView(source = focusedBounds, destination = targetBounds)
            }
            focusAnimationJob = job

            // If the scroll was interrupted by another viewport shrink that happens while the
            // animation is running, we don't want to clear these fields since the later call to
            // this onSizeChanged method will have updated the fields with its own values.
            // If the animation completed, or was cancelled for any other reason, we need to clear
            // them so the next viewport shrink doesn't think there's already a scroll animation in
            // progress.
            // Doing this wrong has a few implications:
            // 1. If the fields are nulled out when another onSizeChange call happens, it will not
            //    use the current animation target and viewport animations will lose track of the
            //    focusable.
            // 2. If the fields are not nulled out in other cases, the next viewport animation will
            //    not keep the focusable in view if the focus hasn't changed.
            try {
                job.join()
            } finally {
                if (focusAnimationJob === job) {
                    focusedChildBeingAnimated = null
                    focusTargetBounds = null
                    focusAnimationJob = null
                }
            }
        }
    }

    /**
     * Compute the destination given the source rectangle and current bounds.
     *
     * @param childBounds The bounding box of the item that sent the request to be brought into view.
     * @return the destination rectangle.
     */
    private fun computeDestination(childBounds: Rect, containerSize: IntSize): Rect {
        val size = containerSize.toSize()
        return when (orientation) {
            Vertical ->
                childBounds.translate(
                    translateX = 0f,
                    translateY = -relocationDistance(
                        childBounds.top,
                        childBounds.bottom,
                        size.height
                    )
                )
            Horizontal ->
                childBounds.translate(
                    translateX = -relocationDistance(
                        childBounds.left,
                        childBounds.right,
                        size.width
                    ),
                    translateY = 0f
                )
        }
    }

    /**
     * Using the source and destination bounds, perform an animated scroll.
     */
    private suspend fun performBringIntoView(source: Rect, destination: Rect) {
        val offset = when (orientation) {
            Vertical -> destination.top - source.top
            Horizontal -> destination.left - source.left
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

            // Find the minimum scroll needed to make one of the edges coincide with the parent's
            // edge.
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
