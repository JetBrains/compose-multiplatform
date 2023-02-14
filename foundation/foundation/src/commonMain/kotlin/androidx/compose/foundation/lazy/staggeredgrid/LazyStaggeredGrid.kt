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
import androidx.compose.foundation.clipScrollableContainer
import androidx.compose.foundation.gestures.FlingBehavior
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.ScrollableDefaults
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.layout.LazyLayout
import androidx.compose.foundation.lazy.layout.LazyLayoutItemProvider
import androidx.compose.foundation.lazy.layout.lazyLayoutSemantics
import androidx.compose.foundation.overscroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp

@ExperimentalFoundationApi
@Composable
internal fun LazyStaggeredGrid(
    /** State controlling the scroll position */
    state: LazyStaggeredGridState,
    /** The layout orientation of the grid */
    orientation: Orientation,
    /** Prefix sums of cross axis sizes of slots per line, e.g. the columns for vertical grid. */
    slotSizesSums: Density.(Constraints) -> IntArray,
    /** Modifier to be applied for the inner layout */
    modifier: Modifier = Modifier,
    /** The inner padding to be added for the whole content (not for each individual item) */
    contentPadding: PaddingValues = PaddingValues(0.dp),
    /** reverse the direction of scrolling and layout */
    reverseLayout: Boolean = false,
    /** fling behavior to be used for flinging */
    flingBehavior: FlingBehavior = ScrollableDefaults.flingBehavior(),
    /** Whether scrolling via the user gestures is allowed. */
    userScrollEnabled: Boolean = true,
    /** The vertical arrangement for items/lines. */
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    /** The horizontal arrangement for items/lines. */
    horizontalArrangement: Arrangement.Horizontal = Arrangement.Start,
    /** The content of the grid */
    content: LazyStaggeredGridScope.() -> Unit
) {
    val overscrollEffect = ScrollableDefaults.overscrollEffect()

    val itemProvider = rememberStaggeredGridItemProvider(state, content)
    val measurePolicy = rememberStaggeredGridMeasurePolicy(
        state,
        itemProvider,
        contentPadding,
        reverseLayout,
        orientation,
        verticalArrangement,
        horizontalArrangement,
        slotSizesSums
    )
    val semanticState = rememberLazyStaggeredGridSemanticState(state, reverseLayout)

    ScrollPositionUpdater(itemProvider, state)

    LazyLayout(
        modifier = modifier
            .then(state.remeasurementModifier)
            .clipScrollableContainer(orientation)
            .overscroll(overscrollEffect)
            .scrollable(
                orientation = orientation,
                reverseDirection = ScrollableDefaults.reverseDirection(
                    LocalLayoutDirection.current,
                    orientation,
                    reverseLayout
                ),
                interactionSource = state.mutableInteractionSource,
                flingBehavior = flingBehavior,
                state = state,
                overscrollEffect = overscrollEffect,
                enabled = userScrollEnabled
            )
            .lazyLayoutSemantics(
                itemProvider = itemProvider,
                state = semanticState,
                orientation = orientation,
                userScrollEnabled = userScrollEnabled,
                reverseScrolling = reverseLayout
            ),
        prefetchState = state.prefetchState,
        itemProvider = itemProvider,
        measurePolicy = measurePolicy
    )
}

/** Extracted to minimize the recomposition scope */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ScrollPositionUpdater(
    itemProvider: LazyLayoutItemProvider,
    state: LazyStaggeredGridState
) {
    if (itemProvider.itemCount > 0) {
        state.updateScrollPositionIfTheFirstItemWasMoved(itemProvider)
    }
}