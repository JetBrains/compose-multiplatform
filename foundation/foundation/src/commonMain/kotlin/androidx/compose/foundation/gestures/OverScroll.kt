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

package androidx.compose.foundation.gestures

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.unit.Velocity

/**
 * Controller to control the overscroll logic for a particular scrollable container.
 */
internal interface OverScrollController {
    /**
     * Release overscroll effects from being bounded by the current scroll cycle.
     *
     * This is usually called when effects are no longer matter for a particular pointer
     * interaction and is called on pointer UP or any radical change that requires reinitialisation.
     */
    fun release()

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
     * This is the main method to show an overscroll, as [overScrollDelta] will be a
     * non-[zero][Offset.Zero] only if the scroll is happening at the bound of a scrollable
     * container.
     *
     * @param initialDragDelta initial drag delta before any consumption was made
     * @param overScrollDelta the amount of overscroll left after the scroll process
     * @param pointerPosition the pointer location in the bounds of the container
     * @param source source of the scroll event
     */
    fun consumePostScroll(
        initialDragDelta: Offset,
        overScrollDelta: Offset,
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
    fun consumePreFling(
        velocity: Velocity
    ): Velocity

    /**
     * Feed and process velocity overscroll to show an effect.
     *
     * @param velocity the amount of velocity that is left for overscroll after the fling happened.
     */
    fun consumePostFling(velocity: Velocity)

    /**
     * Set information regarding scrollable container for overscroll.
     *
     * @param size the size of the container that scrolls
     * @param isContentScrolls whether content can scroll within the scrollable container or not
     */
    fun refreshContainerInfo(size: Size, isContentScrolls: Boolean)

    /**
     * Stop overscroll animation (if happening)
     *
     * @return true if there was an animation that has been stopped, false if there was no
     * animation.
     */
    fun stopOverscrollAnimation(): Boolean

    /**
     * Draw the overscroll effect.
     */
    fun DrawScope.drawOverScroll()
}

@Composable
internal expect fun rememberOverScrollController(): OverScrollController

internal expect fun Modifier.overScroll(overScrollController: OverScrollController): Modifier