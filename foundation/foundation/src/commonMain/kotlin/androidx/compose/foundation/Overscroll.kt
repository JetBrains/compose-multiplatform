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

package androidx.compose.foundation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.unit.Velocity

/**
 * An OverscrollEffect represents a visual effect that displays when the edges of a scrolling
 * container have been reached with a scroll or fling. For the default platform effect that should
 * be used in most cases, see
 * [androidx.compose.foundation.gestures.ScrollableDefaults.overscrollEffect].
 *
 * OverscrollEffect conceptually 'decorates' scroll / fling events: consuming some of the delta or
 * velocity before and/or after the event is consumed by the scrolling container. [applyToScroll]
 * applies overscroll to a scroll event, and [applyToFling] applies overscroll to a fling.
 *
 * Higher level components such as [androidx.compose.foundation.lazy.LazyColumn] will automatically
 * configure an OverscrollEffect for you. To use a custom OverscrollEffect you first need to
 * provide it with scroll and/or fling events - usually by providing it to a
 * [androidx.compose.foundation.gestures.scrollable]. Then you can draw the effect on top of the
 * scrolling content using [Modifier.overscroll].
 *
 * @sample androidx.compose.foundation.samples.OverscrollSample
 */
@ExperimentalFoundationApi
@Stable
interface OverscrollEffect {
    /**
     * Applies overscroll to [performScroll]. [performScroll] should represent a drag / scroll, and
     * returns the amount of delta consumed, so in simple cases the amount of overscroll to show
     * should be equal to `delta - performScroll(delta)`. The OverscrollEffect can optionally
     * consume some delta before calling [performScroll], such as to release any existing tension.
     * The implementation *must* call [performScroll] exactly once. This function should return the
     * sum of all the delta that was consumed during this operation - both by the overscroll and
     * [performScroll].
     *
     * For example, assume we want to apply overscroll to a custom component that isn't using
     * [androidx.compose.foundation.gestures.scrollable]. Here is a simple example of a component
     * using [androidx.compose.foundation.gestures.draggable] instead:
     *
     * @sample androidx.compose.foundation.samples.OverscrollWithDraggable_Before
     *
     * To apply overscroll, we need to decorate the existing logic with applyToScroll, and
     * return the amount of delta we have consumed when updating the drag position. Note that we
     * also need to call applyToFling - this is used as an end signal for overscroll so that effects
     * can correctly reset after any animations, when the gesture has stopped.
     *
     * @sample androidx.compose.foundation.samples.OverscrollWithDraggable_After
     *
     * @param delta total scroll delta available
     * @param source the source of the delta
     * @param performScroll the scroll action that the overscroll is applied to. The [Offset]
     * parameter represents how much delta is available, and the return value is how much delta was
     * consumed. Any delta that was not consumed should be used to show the overscroll effect.
     * @return the delta consumed from [delta] by the operation of this function - including that
     * consumed by [performScroll].
     */
    fun applyToScroll(
        delta: Offset,
        source: NestedScrollSource,
        performScroll: (Offset) -> Offset
    ): Offset

    /**
     * Applies overscroll to [performFling]. [performFling] should represent a fling (the release
     * of a drag or scroll), and returns the amount of [Velocity] consumed, so in simple cases the
     * amount of overscroll to show should be equal to `velocity - performFling(velocity)`. The
     * OverscrollEffect can optionally consume some [Velocity] before calling [performFling], such
     * as to release any existing tension. The implementation *must* call [performFling] exactly
     * once.
     *
     * For example, assume we want to apply overscroll to a custom component that isn't using
     * [androidx.compose.foundation.gestures.scrollable]. Here is a simple example of a component
     * using [androidx.compose.foundation.gestures.draggable] instead:
     *
     * @sample androidx.compose.foundation.samples.OverscrollWithDraggable_Before
     *
     * To apply overscroll, we decorate the existing logic with applyToScroll, and return the amount
     * of delta we have consumed when updating the drag position. We then call applyToFling using
     * the velocity provided by onDragStopped.
     *
     * @sample androidx.compose.foundation.samples.OverscrollWithDraggable_After
     *
     * @param velocity total [Velocity] available
     * @param performFling the [Velocity] consuming lambda that the overscroll is applied to. The
     * [Velocity] parameter represents how much [Velocity] is available, and the return value is how
     * much [Velocity] was consumed. Any [Velocity] that was not consumed should be used to show the
     * overscroll effect.
     */
    suspend fun applyToFling(velocity: Velocity, performFling: suspend (Velocity) -> Velocity)

    /**
     * Whether this OverscrollEffect is currently displaying overscroll.
     *
     * @return true if this OverscrollEffect is currently displaying overscroll
     */
    val isInProgress: Boolean

    /**
     * A [Modifier] that will draw this OverscrollEffect
     */
    val effectModifier: Modifier
}

/**
 * Renders overscroll from the provided [overscrollEffect].
 *
 * This modifier is a convenience method to call [OverscrollEffect.effectModifier], which
 * renders the actual effect. Note that this modifier is only responsible for the visual part of
 * overscroll - on its own it will not handle input events. In addition to using this modifier you
 * also need to propagate events to the [overscrollEffect], most commonly by using a
 * [androidx.compose.foundation.gestures.scrollable].
 *
 * @sample androidx.compose.foundation.samples.OverscrollSample
 *
 * @param overscrollEffect the [OverscrollEffect] to render
 */
@ExperimentalFoundationApi
fun Modifier.overscroll(overscrollEffect: OverscrollEffect): Modifier =
    this.then(overscrollEffect.effectModifier)

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal expect fun rememberOverscrollEffect(): OverscrollEffect

@OptIn(ExperimentalFoundationApi::class)
internal object NoOpOverscrollEffect : OverscrollEffect {
    override fun applyToScroll(
        delta: Offset,
        source: NestedScrollSource,
        performScroll: (Offset) -> Offset
    ): Offset = performScroll(delta)

    override suspend fun applyToFling(
        velocity: Velocity,
        performFling: suspend (Velocity) -> Velocity
    ) { performFling(velocity) }

    override val isInProgress: Boolean
        get() = false

    override val effectModifier: Modifier
        get() = Modifier
}
