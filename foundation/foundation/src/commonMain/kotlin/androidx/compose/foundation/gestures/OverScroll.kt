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
import androidx.compose.ui.unit.Velocity

internal interface OverScrollController {
    /**
     * Release overscroll effects from being bounded by the current scroller cycle.
     *
     * This is usually called when effects are no longer matter for a particular pointer
     * interaction and is called on pointer UP or any radical change that requires reinitialisation.
     *
     * @return true if invalidation has been scheduled, false otherwise
     */
    fun release(): Boolean

    /**
     * Feed and process drag delta information into overscroll effect.
     *
     * @param initialDragDelta initial drag delta before any consumption was made
     * @param overScrollDelta the amount of overscroll left after the scroll process
     * @param pointerPosition the pointer location in the bounds of the container
     *
     * @return true if invalidation has been scheduled, false otherwise
     */
    fun processDragDelta(
        initialDragDelta: Offset,
        overScrollDelta: Offset,
        pointerPosition: Offset?
    ): Boolean

    /**
     * Feed and process velocity overscroll to show an effect.
     *
     * @param velocity the amount of velocity that is left for overscroll
     *
     * @return true if invalidation has been scheduled, false otherwise
     */
    fun processVelocity(velocity: Velocity): Boolean

    /**
     * Set information regarding scrollable container for overscroll.
     *
     * @param size the size of the container that scrolls
     * @param isContentScrolls whether content can scroll within the scrollable container or not
     */
    fun refreshContainerInfo(size: Size, isContentScrolls: Boolean)

    fun DrawScope.drawOverScroll()
}

@Composable
internal expect fun rememberOverScrollController(): OverScrollController

internal expect fun Modifier.overScroll(overScrollController: OverScrollController): Modifier