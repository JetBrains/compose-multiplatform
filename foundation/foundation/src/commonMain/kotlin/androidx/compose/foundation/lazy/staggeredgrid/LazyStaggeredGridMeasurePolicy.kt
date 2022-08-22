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

package androidx.compose.foundation.lazy.staggeredgrid

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.OverscrollEffect
import androidx.compose.foundation.checkScrollableContainerConstraints
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.layout.LazyLayoutItemProvider
import androidx.compose.foundation.lazy.layout.LazyLayoutMeasureScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density

@Composable
@ExperimentalFoundationApi
internal fun rememberStaggeredGridMeasurePolicy(
    state: LazyStaggeredGridState,
    itemProvider: LazyLayoutItemProvider,
    contentPadding: PaddingValues,
    reverseLayout: Boolean,
    orientation: Orientation,
    verticalArrangement: Arrangement.Vertical,
    horizontalArrangement: Arrangement.Horizontal,
    slotSizesSums: Density.(Constraints) -> IntArray,
    overscrollEffect: OverscrollEffect
): LazyLayoutMeasureScope.(Constraints) -> LazyStaggeredGridMeasureResult = remember(
    state,
    itemProvider,
    contentPadding,
    reverseLayout,
    orientation,
    verticalArrangement,
    horizontalArrangement,
    slotSizesSums,
    overscrollEffect,
) {
    { constraints ->
        checkScrollableContainerConstraints(
            constraints,
            orientation
        )
        val resolvedSlotSums = slotSizesSums(this, constraints)

        // setup information for prefetch
        state.prefetchLaneWidths = resolvedSlotSums
        state.isVertical = orientation == Orientation.Vertical

        measure(
            state,
            itemProvider,
            resolvedSlotSums,
            constraints,
            isVertical = orientation == Orientation.Vertical,
            beforeContentPadding = 0,
            afterContentPadding = 0,
        ).also {
            state.applyMeasureResult(it)
            overscrollEffect.isEnabled = it.canScrollForward || it.canScrollBackward
        }
    }
}
