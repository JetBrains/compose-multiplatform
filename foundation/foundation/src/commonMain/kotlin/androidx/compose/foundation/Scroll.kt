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
@file:Suppress("DEPRECATION")

package androidx.compose.foundation

import androidx.compose.animation.asDisposableClock
import androidx.compose.animation.core.AnimationClockObservable
import androidx.compose.animation.core.AnimationEndReason
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.SpringSpec
import androidx.compose.foundation.animation.FlingConfig
import androidx.compose.foundation.animation.defaultFlingConfig
import androidx.compose.foundation.gestures.ScrollableController
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.InnerPadding
import androidx.compose.foundation.layout.InternalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.savedinstancestate.Saver
import androidx.compose.runtime.savedinstancestate.rememberSavedInstanceState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.structuralEqualityPolicy
import androidx.compose.ui.Alignment
import androidx.compose.ui.LayoutModifier
import androidx.compose.ui.Measurable
import androidx.compose.ui.MeasureScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.gesture.scrollorientationlocking.Orientation
import androidx.compose.ui.platform.AnimationClockAmbient
import androidx.compose.ui.platform.LayoutDirectionAmbient
import androidx.compose.ui.semantics.scrollBy
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

/**
 * Create and [remember] the [ScrollState] based on the currently appropriate scroll
 * configuration to allow changing scroll position or observing scroll behavior.
 *
 * Learn how to control [ScrollableColumn] or [ScrollableRow]:
 * @sample androidx.compose.foundation.samples.ControlledScrollableRowSample
 *
 * @param initial initial scroller position to start with
 */
@Composable
fun rememberScrollState(initial: Float = 0f): ScrollState {
    val clock = AnimationClockAmbient.current.asDisposableClock()
    val config = defaultFlingConfig()
    return rememberSavedInstanceState(
        clock, config,
        saver = ScrollState.Saver(config, clock)
    ) {
        ScrollState(
            flingConfig = config,
            initial = initial,
            animationClock = clock
        )
    }
}

/**
 * State of the scroll. Allows the developer to change the scroll position or get current state by
 * calling methods on this object. To be hosted and passed to [ScrollableRow], [ScrollableColumn],
 * [Modifier.verticalScroll] or [Modifier.horizontalScroll]
 *
 * To create and automatically remember [ScrollState] with default parameters use
 * [rememberScrollState].
 *
 * Learn how to control [ScrollableColumn] or [ScrollableRow]:
 * @sample androidx.compose.foundation.samples.ControlledScrollableRowSample
 *
 * @param initial value of the scroll
 * @param flingConfig fling configuration to use for flinging
 * @param animationClock animation clock to run flinging and smooth scrolling on
 */
@Stable
class ScrollState(
    initial: Float,
    internal val flingConfig: FlingConfig,
    animationClock: AnimationClockObservable
) {

    /**
     * current scroll position value in pixels
     */
    var value by mutableStateOf(initial, structuralEqualityPolicy())
        private set

    /**
     * maximum bound for [value], or [Float.POSITIVE_INFINITY] if still unknown
     */
    var maxValue: Float
        get() = _maxValueState.value
        internal set(newMax) {
            _maxValueState.value = newMax
            if (value > newMax) {
                value = newMax
            }
        }

    private var _maxValueState = mutableStateOf(Float.POSITIVE_INFINITY, structuralEqualityPolicy())

    internal val scrollableController =
        ScrollableController(
            flingConfig = flingConfig,
            animationClock = animationClock,
            consumeScrollDelta = {
                val absolute = (value + it)
                val newValue = absolute.coerceIn(0f, maxValue)
                if (absolute != newValue) stopAnimation()
                val consumed = newValue - value
                value += consumed
                consumed
            })

    /**
     * Stop any ongoing animation, smooth scrolling or fling occurring on this [ScrollState]
     */
    fun stopAnimation() {
        scrollableController.stopAnimation()
    }

    /**
     * whether this [ScrollState] is currently animating/flinging
     */
    val isAnimationRunning
        get() = scrollableController.isAnimationRunning

    /**
     * Smooth scroll to position in pixels
     *
     * @param value target value in pixels to smooth scroll to, value will be coerced to
     * 0..maxPosition
     * @param spec animation curve for smooth scroll animation
     * @param onEnd callback to be invoked when smooth scroll has finished
     */
    fun smoothScrollTo(
        value: Float,
        spec: AnimationSpec<Float> = SpringSpec(),
        onEnd: (endReason: AnimationEndReason, finishValue: Float) -> Unit = { _, _ -> }
    ) {
        smoothScrollBy(value - this.value, spec, onEnd)
    }

    /**
     * Smooth scroll by some amount of pixels
     *
     * @param value delta in pixels to scroll by, total value will be coerced to 0..maxPosition
     * @param spec animation curve for smooth scroll animation
     * @param onEnd callback to be invoked when smooth scroll has finished
     */
    fun smoothScrollBy(
        value: Float,
        spec: AnimationSpec<Float> = SpringSpec(),
        onEnd: (endReason: AnimationEndReason, finishValue: Float) -> Unit = { _, _ -> }
    ) {
        scrollableController.smoothScrollBy(value, spec, onEnd)
    }

    /**
     * Instantly jump to position in pixels
     *
     * @param value target value in pixels to jump to, value will be coerced to 0..maxPosition
     */
    fun scrollTo(value: Float) {
        this.value = value.coerceIn(0f, maxValue)
    }

    /**
     * Instantly jump by some amount of pixels
     *
     * @param value delta in pixels to jump by, total value will be coerced to 0..maxPosition
     */
    fun scrollBy(value: Float) {
        scrollTo(this.value + value)
    }

    companion object {
        /**
         * The default [Saver] implementation for [ScrollState].
         */
        fun Saver(
            flingConfig: FlingConfig,
            animationClock: AnimationClockObservable
        ): Saver<ScrollState, *> = Saver<ScrollState, Float>(
            save = { it.value },
            restore = { ScrollState(it, flingConfig, animationClock) }
        )
    }
}

/**
 * Variation of [Column] that scrolls when content is bigger than its height.
 *
 * The content of the [ScrollableColumn] is clipped to its bounds.
 *
 * @sample androidx.compose.foundation.samples.ScrollableColumnSample
 *
 * @param modifier modifier for this [ScrollableColumn]
 * @param scrollState state of the scroll, such as current offset and max offset
 * @param verticalArrangement The vertical arrangement of the layout's children
 * @param horizontalGravity The horizontal gravity of the layout's children
 * @param reverseScrollDirection reverse the direction of scrolling, when `true`, [ScrollState
 * .value] = 0 will mean bottom, when `false`, [ScrollState.value] = 0 will mean top
 * @param isScrollEnabled param to enable or disable touch input scrolling. If you own
 * [ScrollState], you still can call [ScrollState.smoothScrollTo] and other methods on it.
 * @param contentPadding convenience param to specify padding around content. This will add
 * padding for the content after it has been clipped, which is not possible via [modifier] param
 */
@Composable
@OptIn(InternalLayoutApi::class)
fun ScrollableColumn(
    modifier: Modifier = Modifier,
    scrollState: ScrollState = rememberScrollState(0f),
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    horizontalGravity: Alignment.Horizontal = Alignment.Start,
    reverseScrollDirection: Boolean = false,
    isScrollEnabled: Boolean = true,
    contentPadding: InnerPadding = InnerPadding(0.dp),
    children: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .verticalScroll(
                scrollState,
                isScrollEnabled,
                reverseScrolling = reverseScrollDirection
            )
            .clipToBounds()
            .padding(contentPadding),
        verticalArrangement = verticalArrangement,
        horizontalGravity = horizontalGravity,
        children = children
    )
}

/**
 * Variation of [Row] that scrolls when content is bigger than its width.
 *
 * The content of the [ScrollableRow] is clipped to its bounds.
 *
 * @sample androidx.compose.foundation.samples.ScrollableRowSample
 *
 * @param modifier modifier for this [ScrollableRow]
 * @param scrollState state of the scroll, such as current offset and max offset
 * @param horizontalArrangement The horizontal arrangement of the layout's children
 * @param verticalGravity The vertical gravity of the layout's children
 * @param reverseScrollDirection reverse the direction of scrolling, when `true`, [ScrollState
 * .value] = 0 will mean right, when `false`, [ScrollState.value] = 0 will mean left
 * @param isScrollEnabled param to enable or disable touch input scrolling. If you own
 * [ScrollState], you still can call [ScrollState.smoothScrollTo] and other methods on it.
 * @param contentPadding convenience param to specify padding around content. This will add
 * padding for the content after it has been clipped, which is not possible via [modifier] param.
 */
@Composable
@OptIn(InternalLayoutApi::class)
fun ScrollableRow(
    modifier: Modifier = Modifier,
    scrollState: ScrollState = rememberScrollState(0f),
    horizontalArrangement: Arrangement.Horizontal = Arrangement.Start,
    verticalGravity: Alignment.Vertical = Alignment.Top,
    reverseScrollDirection: Boolean = false,
    isScrollEnabled: Boolean = true,
    contentPadding: InnerPadding = InnerPadding(0.dp),
    children: @Composable RowScope.() -> Unit
) {
    Row(
        modifier = modifier
            .horizontalScroll(
                scrollState,
                isScrollEnabled,
                reverseScrolling = reverseScrollDirection
            )
            .clipToBounds()
            .padding(contentPadding),
        horizontalArrangement = horizontalArrangement,
        verticalGravity = verticalGravity,
        children = children
    )
}

/**
 * Modify element to allow to scroll vertically when height of the content is bigger than max
 * constraints allow.
 *
 * @sample androidx.compose.foundation.samples.VerticalScrollExample
 *
 * In order to use this modifier, you need to create and own [ScrollState]
 * @see [rememberScrollState]
 *
 * @param state state of the scroll
 * @param enabled whether or not scrolling via touch input is enabled
 * @param reverseScrolling reverse the direction of scrolling, when `true`, 0 [ScrollState.value]
 * will mean bottom, when `false`, 0 [ScrollState.value] will mean top
 */
fun Modifier.verticalScroll(
    state: ScrollState,
    enabled: Boolean = true,
    reverseScrolling: Boolean = false
) = scroll(
    state = state,
    isScrollable = enabled,
    reverseScrolling = reverseScrolling,
    isVertical = true
)

/**
 * Modify element to allow to scroll horizontally when width of the content is bigger than max
 * constraints allow.
 *
 * @sample androidx.compose.foundation.samples.HorizontalScrollSample
 *
 * In order to use this modifier, you need to create and own [ScrollState]
 * @see [rememberScrollState]
 *
 * @param state state of the scroll
 * @param enabled whether or not scrolling via touch input is enabled
 * @param reverseScrolling reverse the direction of scrolling, when `true`, 0 [ScrollState.value]
 * will mean right, when `false`, 0 [ScrollState.value] will mean left
 */
fun Modifier.horizontalScroll(
    state: ScrollState,
    enabled: Boolean = true,
    reverseScrolling: Boolean = false
) = scroll(
    state = state,
    isScrollable = enabled,
    reverseScrolling = reverseScrolling,
    isVertical = false
)

private fun Modifier.scroll(
    state: ScrollState,
    reverseScrolling: Boolean,
    isScrollable: Boolean,
    isVertical: Boolean
) = composed {
    val semantics = Modifier.semantics {
        if (isScrollable) {
            // when b/156389287 is fixed, this should be proper scrollTo with reverse handling
            scrollBy(action = { x: Float, y: Float ->
                if (isVertical) {
                    state.scrollBy(y)
                } else {
                    state.scrollBy(x)
                }
                return@scrollBy true
            })
        }
    }
    val isRtl = LayoutDirectionAmbient.current == LayoutDirection.Rtl
    val scrolling = Modifier.scrollable(
        orientation = if (isVertical) Orientation.Vertical else Orientation.Horizontal,
        // reverse scroll by default, to have "natural" gesture that goes reversed to layout
        // if rtl and horizontal, do not reverse to make it right-to-left
        reverseDirection = if (!isVertical && isRtl) reverseScrolling else !reverseScrolling,
        enabled = isScrollable,
        controller = state.scrollableController
    )
    val layout = ScrollingLayoutModifier(state, reverseScrolling, isVertical)
    semantics.then(scrolling).clipToBounds().then(layout)
}

private data class ScrollingLayoutModifier(
    val scrollerState: ScrollState,
    val isReversed: Boolean,
    val isVertical: Boolean
) : LayoutModifier {
    override fun MeasureScope.measure(
        measurable: Measurable,
        constraints: Constraints
    ): MeasureScope.MeasureResult {
        val childConstraints = constraints.copy(
            maxHeight = if (isVertical) Constraints.Infinity else constraints.maxHeight,
            maxWidth = if (isVertical) constraints.maxWidth else Constraints.Infinity
        )
        val placeable = measurable.measure(childConstraints)
        val width = placeable.width.coerceAtMost(constraints.maxWidth)
        val height = placeable.height.coerceAtMost(constraints.maxHeight)
        val scrollHeight = placeable.height.toFloat() - height.toFloat()
        val scrollWidth = placeable.width.toFloat() - width.toFloat()
        val side = if (isVertical) scrollHeight else scrollWidth
        return layout(width, height) {
            scrollerState.maxValue = side
            val scroll = scrollerState.value.coerceIn(0f, side)
            val absScroll = if (isReversed) scroll - side else -scroll
            val xOffset = if (isVertical) 0 else absScroll.roundToInt()
            val yOffset = if (isVertical) absScroll.roundToInt() else 0
            placeable.placeRelative(xOffset, yOffset)
        }
    }
}
