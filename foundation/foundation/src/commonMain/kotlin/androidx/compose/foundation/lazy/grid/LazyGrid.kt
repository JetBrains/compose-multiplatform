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

package androidx.compose.foundation.lazy.grid

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.assertNotNestingScrollableContainers
import androidx.compose.foundation.clipScrollableContainer
import androidx.compose.foundation.gestures.FlingBehavior
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.OverScrollController
import androidx.compose.foundation.gestures.ScrollableDefaults
import androidx.compose.foundation.gestures.rememberOverScrollController
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyGridScope
import androidx.compose.foundation.lazy.LazyGridState
import androidx.compose.foundation.lazy.layout.LazyLayout
import androidx.compose.foundation.lazy.layout.LazyMeasurePolicy
import androidx.compose.foundation.lazy.layout.rememberLazyLayoutPrefetchPolicy
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.layout.IntrinsicMeasureScope
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEach

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun LazyGrid(
    /** Modifier to be applied for the inner layout */
    modifier: Modifier = Modifier,
    /** State controlling the scroll position */
    state: LazyGridState,
    /** The number of items per line in the grid e.g. the columns for vertical grid. */
    slotsPerLine: Density.(Constraints) -> Int,
    /** The inner padding to be added for the whole content (not for each individual item) */
    contentPadding: PaddingValues = PaddingValues(0.dp),
    /** reverse the direction of scrolling and layout */
    reverseLayout: Boolean = false,
    /** The layout orientation of the grid */
    isVertical: Boolean = true,
    /** fling behavior to be used for flinging */
    flingBehavior: FlingBehavior = ScrollableDefaults.flingBehavior(),
    /** Whether scrolling via the user gestures is allowed. */
    userScrollEnabled: Boolean,
    /** The vertical arrangement for items/lines. */
    verticalArrangement: Arrangement.Vertical,
    /** The horizontal arrangement for items/lines. */
    horizontalArrangement: Arrangement.Horizontal,
    /** The content of the grid */
    content: LazyGridScope.() -> Unit
) {
    val overScrollController = rememberOverScrollController()

    val stateOfItemsProvider = rememberStateOfItemsProvider(state, content)

    val spanLayoutProvider = remember(stateOfItemsProvider) {
        derivedStateOf { LazyGridSpanLayoutProvider(stateOfItemsProvider.value) }
    }

    val scope = rememberCoroutineScope()
    // TODO(popam): enable placement animations
    // val placementAnimator = remember(state, isVertical) {
    //     LazyListItemPlacementAnimator(scope, isVertical)
    // }
    // state.placementAnimator = placementAnimator

    val measurePolicy = rememberLazyGridMeasurePolicy(
        stateOfItemsProvider,
        state,
        overScrollController,
        spanLayoutProvider,
        slotsPerLine,
        contentPadding,
        reverseLayout,
        isVertical,
        horizontalArrangement,
        verticalArrangement,
        // placementAnimator
    )

    state.prefetchPolicy = rememberLazyLayoutPrefetchPolicy()

    ScrollPositionUpdater(stateOfItemsProvider, state)

    LazyLayout(
        modifier = modifier
            .then(state.remeasurementModifier)
            .lazyGridSemantics(
                stateOfItemsProvider = stateOfItemsProvider,
                state = state,
                coroutineScope = scope,
                isVertical = isVertical,
                reverseScrolling = reverseLayout,
                userScrollEnabled = userScrollEnabled
            )
            .clipScrollableContainer(isVertical)
            .scrollable(
                orientation = if (isVertical) Orientation.Vertical else Orientation.Horizontal,
                reverseDirection = run {
                    // A finger moves with the content, not with the viewport. Therefore,
                    // always reverse once to have "natural" gesture that goes reversed to layout
                    var reverseDirection = !reverseLayout
                    // But if rtl and horizontal, things move the other way around
                    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl
                    if (isRtl && !isVertical) {
                        reverseDirection = !reverseDirection
                    }
                    reverseDirection
                },
                interactionSource = state.internalInteractionSource,
                flingBehavior = flingBehavior,
                state = state,
                overScrollController = overScrollController,
                enabled = userScrollEnabled
            )
            .padding(contentPadding),
        prefetchPolicy = state.prefetchPolicy,
        measurePolicy = measurePolicy,
        itemsProvider = { stateOfItemsProvider.value }
    )
}

/** Extracted to minimize the recomposition scope */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ScrollPositionUpdater(
    stateOfItemsProvider: State<LazyGridItemsProvider>,
    state: LazyGridState
) {
    val itemsProvider = stateOfItemsProvider.value
    if (itemsProvider.itemsCount > 0) {
        state.updateScrollPositionIfTheFirstItemWasMoved(itemsProvider)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun rememberLazyGridMeasurePolicy(
    /** State containing the items provider of the list. */
    stateOfItemsProvider: State<LazyGridItemsProvider>,
    /** The state of the list. */
    state: LazyGridState,
    /** The overscroll controller. */
    overScrollController: OverScrollController,
    /** Cache based provider for spans. */
    stateOfSpanLayoutProvider: State<LazyGridSpanLayoutProvider>,
    /** The number of columns of the grid. */
    slotsPerLine: Density.(Constraints) -> Int,
    /** The inner padding to be added for the whole content(nor for each individual item) */
    contentPadding: PaddingValues,
    /** reverse the direction of scrolling and layout */
    reverseLayout: Boolean,
    /** The layout orientation of the list */
    isVertical: Boolean,
    /** The horizontal arrangement for items. Required when isVertical is false */
    horizontalArrangement: Arrangement.Horizontal? = null,
    /** The vertical arrangement for items. Required when isVertical is true */
    verticalArrangement: Arrangement.Vertical? = null,
    /** Item placement animator. Should be notified with the measuring result */
    // placementAnimator: LazyListItemPlacementAnimator
) = remember(
    state,
    overScrollController,
    slotsPerLine,
    contentPadding,
    reverseLayout,
    isVertical,
    horizontalArrangement,
    verticalArrangement,
    // placementAnimator
) {
    LazyMeasurePolicy { placeablesProvider, constraints ->
        constraints.assertNotNestingScrollableContainers(isVertical)

        val itemsProvider = stateOfItemsProvider.value
        state.updateScrollPositionIfTheFirstItemWasMoved(itemsProvider)

        // Update the state's cached Density
        state.density = this

        val spanLayoutProvider = stateOfSpanLayoutProvider.value
        // Resolve slotsPerLine.
        val resolvedSlotsPerLine = slotsPerLine(constraints)
        spanLayoutProvider.slotsPerLine = resolvedSlotsPerLine

        val rawBeforeContentPadding = if (isVertical) {
            contentPadding.calculateTopPadding()
        } else {
            contentPadding.calculateStartPadding(layoutDirection)
        }.roundToPx()
        val rawAfterContentPadding = if (isVertical) {
            contentPadding.calculateBottomPadding()
        } else {
            contentPadding.calculateEndPadding(layoutDirection)
        }.roundToPx()
        val beforeContentPadding =
            if (reverseLayout) rawAfterContentPadding else rawBeforeContentPadding
        val afterContentPadding =
            if (reverseLayout) rawBeforeContentPadding else rawAfterContentPadding
        val mainAxisMaxSize = (if (isVertical) constraints.maxHeight else constraints.maxWidth)
        val spaceBetweenLinesDp = if (isVertical) {
            requireNotNull(verticalArrangement).spacing
        } else {
            requireNotNull(horizontalArrangement).spacing
        }
        val spaceBetweenLines = spaceBetweenLinesDp.roundToPx()
        val spaceBetweenSlotsDp = if (isVertical) {
            horizontalArrangement?.spacing ?: 0.dp
        } else {
            verticalArrangement?.spacing ?: 0.dp
        }
        val spaceBetweenSlots = spaceBetweenSlotsDp.roundToPx()

        val itemsCount = itemsProvider.itemsCount

        val lineProvider = LazyMeasuredLineProvider(
            constraints,
            isVertical,
            resolvedSlotsPerLine,
            spaceBetweenSlots,
            itemsProvider,
            spanLayoutProvider,
            placeablesProvider
        ) { index, firstItemIndex, spans, keys, crossAxisSizes, placeables ->
            // we add space between lines as an extra spacing for all lines apart from the last one
            // so the lazy grid measuring logic will take it into account.
            val spacing =
                if (firstItemIndex.value + keys.size == itemsCount) 0 else spaceBetweenLines
            LazyMeasuredLine(
                index = index,
                firstItemIndex = firstItemIndex,
                crossAxisSizes = crossAxisSizes,
                placeables = placeables,
                spans = spans,
                keys = keys,
                isVertical = isVertical,
                layoutDirection = layoutDirection,
                reverseLayout = reverseLayout,
                beforeContentPadding = beforeContentPadding,
                afterContentPadding = afterContentPadding,
                mainAxisSpacing = spacing,
                crossAxisSpacing = spaceBetweenSlots
                // placementAnimator = placementAnimator
            )
        }
        state.prefetchInfoRetriever = { line ->
            val lineConfiguration = spanLayoutProvider.getLineConfiguration(line.value)
            var index = ItemIndex(lineConfiguration.firstItemIndex)
            var slot = 0
            val result = ArrayList<Pair<Int, Constraints>>(lineConfiguration.spans.size)
            lineConfiguration.spans.fastForEach {
                val span = it.currentLineSpan
                result.add(index.value to lineProvider.childConstraints(slot, span))
                ++index
                slot += span
            }
            result
        }

        val firstVisibleLineIndex: LineIndex
        val firstVisibleLineScrollOffset: Int
        if (state.firstVisibleItemIndexNonObservable.value < itemsCount || itemsCount <= 0) {
            firstVisibleLineIndex = spanLayoutProvider.getLineIndexOfItem(
                state.firstVisibleItemIndexNonObservable.value
            )
            firstVisibleLineScrollOffset = state.firstVisibleItemScrollOffsetNonObservable
        } else {
            // the data set has been updated and now we have less items that we were
            // scrolled to before
            firstVisibleLineIndex = spanLayoutProvider.getLineIndexOfItem(itemsCount - 1)
            firstVisibleLineScrollOffset = 0
        }
        measureLazyGrid(
            itemsCount = itemsCount,
            lineProvider = lineProvider,
            mainAxisMaxSize = mainAxisMaxSize,
            beforeContentPadding = beforeContentPadding,
            afterContentPadding = afterContentPadding,
            firstVisibleLineIndex = firstVisibleLineIndex,
            firstVisibleLineScrollOffset = firstVisibleLineScrollOffset,
            scrollToBeConsumed = state.scrollToBeConsumed,
            constraints = constraints,
            isVertical = isVertical,
            verticalArrangement = verticalArrangement,
            horizontalArrangement = horizontalArrangement,
            reverseLayout = reverseLayout,
            density = this,
            layoutDirection = layoutDirection,
            // placementAnimator = placementAnimator,
            layout = { width, height, placement -> layout(width, height, emptyMap(), placement) }
        ).also {
            state.applyMeasureResult(it)
            refreshOverScrollInfo(overScrollController, it, contentPadding)
        }.lazyLayoutMeasureResult
    }
}

private fun IntrinsicMeasureScope.refreshOverScrollInfo(
    overScrollController: OverScrollController,
    result: LazyGridMeasureResult,
    contentPadding: PaddingValues
) {
    val verticalPadding =
        contentPadding.calculateTopPadding() +
            contentPadding.calculateBottomPadding()

    val horizontalPadding =
        contentPadding.calculateLeftPadding(layoutDirection) +
            contentPadding.calculateRightPadding(layoutDirection)

    val canScrollForward = result.canScrollForward
    val canScrollBackward = (result.firstVisibleLine?.firstItemIndex ?: 0) != 0 ||
        result.firstVisibleLineScrollOffset != 0

    overScrollController.refreshContainerInfo(
        Size(
            result.width.toFloat() + horizontalPadding.roundToPx(),
            result.height.toFloat() + verticalPadding.roundToPx()
        ),
        canScrollForward || canScrollBackward
    )
}