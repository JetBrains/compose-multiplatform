/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.compose.foundation.gestures.snapping

import androidx.compose.foundation.ExperimentalFoundationApi

/**
 * Provides information about the layout that is using a SnapFlingBehavior.
 * The provider should give the following information:
 * 1) Snapping bounds, the previous and the next snap position offset.
 * 2) Snap Step Size, the minimum size that the SnapFlingBehavior can animate.
 * 3) Approach offset calculation, an offset to be consumed before snapping to a defined bound.
 */
@ExperimentalFoundationApi
interface SnapLayoutInfoProvider {
    /**
     * The minimum offset that snapping will use to animate. (e.g. an item size)
     */
    val snapStepSize: Float

    /**
     * Calculate the distance to navigate before settling into the next snapping bound.
     *
     * @param initialVelocity The current fling movement velocity. You can use this tho calculate a
     * velocity based offset.
     */
    fun calculateApproachOffset(initialVelocity: Float): Float

    /**
     * Given a target placement in a layout, the snapping bounds should be the closest offset we
     * could snap to BEFORE and AFTER that placement. (e.g if the placement is the center of the
     * viewport and we're snapping in a list this could be the distance to the center of the item
     * before and after the center of the viewport)
     *
     * Bounds are *always* a negative (lower bound) and a positive (upper bound) value.
     */
    fun calculateSnappingOffsetBounds(): ClosedFloatingPointRange<Float>
}