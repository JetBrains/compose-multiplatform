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
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.ui.modifier.ModifierLocalProvider
import androidx.compose.ui.modifier.modifierLocalOf
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.debugInspectorInfo
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.util.fastAll
import androidx.compose.ui.util.fastForEach
import kotlin.math.abs
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

    /**
     * Used to determine the value of `reverseDirection` parameter of [Modifier.scrollable]
     * in scrollable layouts.
     *
     * @param layoutDirection current layout direction (e.g. from [LocalLayoutDirection])
     * @param orientation orientation of scroll
     * @param reverseScrolling whether scrolling direction should be reversed
     *
     * @return `true` if scroll direction should be reversed, `false` otherwise.
     */
    fun reverseDirection(
        layoutDirection: LayoutDirection,
        orientation: Orientation,
        reverseScrolling: Boolean
    ): Boolean {
        // A finger moves with the content, not with the viewport. Therefore,
        // always reverse once to have "natural" gesture that goes reversed to layout
        var reverseDirection = !reverseScrolling
        // But if rtl and horizontal, things move the other way around
        val isRtl = layoutDirection == LayoutDirection.Rtl
        if (isRtl && orientation != Orientation.Vertical) {
            reverseDirection = !reverseDirection
        }
        return reverseDirection
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
        draggableState,
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
    private val isNestedFlinging = mutableStateOf(false)
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

    fun ScrollScope.dispatchScroll(availableDelta: Offset, source: NestedScrollSource): Offset {
        val scrollDelta = availableDelta.singleAxisOffset()
        val overscrollPreConsumed = overscrollPreConsumeDelta(scrollDelta, source)

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
            source
        )

        return leftForParent - parentConsumed
    }

    fun overscrollPreConsumeDelta(
        scrollDelta: Offset,
        source: NestedScrollSource
    ): Offset {
        return if (overscrollEffect != null && overscrollEffect.isEnabled) {
            overscrollEffect.consumePreScroll(scrollDelta, source)
        } else {
            Offset.Zero
        }
    }

    private fun overscrollPostConsumeDelta(
        consumedByChain: Offset,
        availableForOverscroll: Offset,
        source: NestedScrollSource
    ) {
        if (overscrollEffect != null && overscrollEffect.isEnabled) {
            overscrollEffect.consumePostScroll(
                consumedByChain,
                availableForOverscroll,
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
        // Self started flinging, set
        registerNestedFling(true)

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

        // Self stopped flinging, reset
        registerNestedFling(false)
    }

    suspend fun doFlingAnimation(available: Velocity): Velocity {
        var result: Velocity = available
        scrollableState.scroll {
            val outerScopeScroll: (Offset) -> Offset = { delta ->
                val consumed = this.dispatchScroll(delta.reverseIfNeeded(), Fling)
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
        return scrollableState.isScrollInProgress || isNestedFlinging.value ||
            overscrollEffect?.isInProgress ?: false
    }

    fun registerNestedFling(isFlinging: Boolean) {
        isNestedFlinging.value = isFlinging
    }
}

private class ScrollDraggableState(
    val scrollLogic: State<ScrollingLogic>
) : DraggableState, DragScope {
    var latestScrollScope: ScrollScope = NoOpScrollScope

    override fun dragBy(pixels: Float) {
        with(scrollLogic.value) {
            with(latestScrollScope) {
                dispatchScroll(pixels.toOffset(), Drag)
            }
        }
    }

    override suspend fun drag(dragPriority: MutatePriority, block: suspend DragScope.() -> Unit) {
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
    override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
        // child will fling, set
        if (source == Fling) {
            scrollLogic.value.registerNestedFling(true)
        }
        return Offset.Zero
    }

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
        }.also {
            // Flinging child finished flinging, reset
            scrollLogic.value.registerNestedFling(false)
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
