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
 * Controller to control the overscroll logic for a particular scrollable container.
 *
 * This entity defines the "how" of the overscroll effect. If the default platform effect is needed,
 * consider using [ScrollableDefaults.overscrollEffect]. In order for overscroll to work,
 * the controller is supposed to be passed to [scrollable] modifier to receive the data of
 * overscroll, and then applied where appropriate using [overscroll] modifier.
 *
 * @sample androidx.compose.foundation.samples.OverscrollSample
 */
@ExperimentalFoundationApi
@Stable
interface OverscrollEffect {

    /**
     * Consume any overscroll before the scroll happens if needed.
     *
     * This is usually relevant when the overscroll expresses the effect that is needed to be
     * negated first before actually scrolling. The example might be a spring-based overscroll,
     * where you want to release the tension of a spring before starting to scroll to provide a
     * good user-experience.
     *
     * @param scrollDelta the original delta to scroll
     * @param pointerPosition position of the pointer causing the scroll, if known
     * @param source source of the scroll event
     *
     * @return the amount of scroll consumed that won't be available for scrollable container
     * anymore
     */
    fun consumePreScroll(
        scrollDelta: Offset,
        pointerPosition: Offset?,
        source: NestedScrollSource
    ): Offset

    /**
     * Process scroll delta that is available after the scroll iteration is over.
     *
     * This is the main method to show an overscroll, as [overscrollDelta] will be a
     * non-[zero][Offset.Zero] only if the scroll is happening at the bound of a scrollable
     * container.
     *
     * @param initialDragDelta initial drag delta before any consumption was made
     * @param overscrollDelta the amount of overscroll left after the scroll process
     * @param pointerPosition the pointer location in the bounds of the container
     * @param source source of the scroll event
     */
    fun consumePostScroll(
        initialDragDelta: Offset,
        overscrollDelta: Offset,
        pointerPosition: Offset?,
        source: NestedScrollSource
    )

    /**
     * Consume any velocity for the over scroll effect before the fling happens.
     *
     * relevant when the overscroll expresses the effect that is needed to be
     * negated (or increased) first before actually scrolling. The example might be a spring-based
     * overscroll, where you want to release the tension of a spring before starting to scroll to
     * provide a good user-experience.
     *
     * @param velocity velocity available to a scrolling container before flinging
     *
     * @return the amount of velocity that overscroll effect consumed that won't be available for
     * fling operation
     */
    suspend fun consumePreFling(velocity: Velocity): Velocity

    /**
     * Feed and process velocity overscroll to show an effect.
     *
     * @param velocity the amount of velocity that is left for overscroll after the fling happened.
     */
    suspend fun consumePostFling(velocity: Velocity)

    /**
     * Whether the overscroll effect is enabled or not. If it is not enabled, [scrollable] won't
     * send the events to this effect.
     */
    var isEnabled: Boolean

    /**
     * Whether over scroll within this controller is currently on progress or not, e.g. if the
     * overscroll effect is playing animation or shown/interactable in any other way.
     *
     * @return true if there is over scroll happening at the time of the call, false otherwise
     */
    val isInProgress: Boolean

    /**
     * A modifier that will apply the overscroll effect as desired
     */
    val effectModifier: Modifier
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal expect fun rememberOverscrollEffect(): OverscrollEffect

/**
 * Modifier to apply the overscroll as specified by [OverscrollEffect]
 *
 * This modifier is a convenience method to call [OverscrollEffect.effectModifier], which
 * performs the actual overscroll logic. Note that this modifier is the representation of the
 * overscroll on the UI, to make overscroll events to be propagated to the [OverscrollEffect],
 * you have to pass it to [scrollable].
 *
 * @sample androidx.compose.foundation.samples.OverscrollSample
 *
 * @param overscrollEffect controller that defines the behavior and the overscroll state.
 */
@ExperimentalFoundationApi
fun Modifier.overscroll(overscrollEffect: OverscrollEffect): Modifier =
    this.then(overscrollEffect.effectModifier)