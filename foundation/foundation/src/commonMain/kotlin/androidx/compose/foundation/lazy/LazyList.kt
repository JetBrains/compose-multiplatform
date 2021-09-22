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

package androidx.compose.foundation.lazy

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.assertNotNestingScrollableContainers
import androidx.compose.foundation.clipScrollableContainer
import androidx.compose.foundation.gestures.FlingBehavior
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.OverScrollController
import androidx.compose.foundation.gestures.rememberOverScrollController
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.layout.LazyLayout
import androidx.compose.foundation.lazy.layout.LazyMeasurePolicy
import androidx.compose.foundation.lazy.layout.rememberLazyLayoutPrefetchPolicy
import androidx.compose.foundation.lazy.layout.rememberLazyLayoutState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.layout.IntrinsicMeasureScope
import androidx.compose.ui.node.Ref
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection

@Composable
internal fun LazyList(
    /** Modifier to be applied for the inner layout */
    modifier: Modifier,
    /** State controlling the scroll position */
    state: LazyListState,
    /** The inner padding to be added for the whole content(not for each individual item) */
    contentPadding: PaddingValues,
    /** reverse the direction of scrolling and layout */
    reverseLayout: Boolean,
    /** The layout orientation of the list */
    isVertical: Boolean,
    /** fling behavior to be used for flinging */
    flingBehavior: FlingBehavior,
    /** The alignment to align items horizontally. Required when isVertical is true */
    horizontalAlignment: Alignment.Horizontal? = null,
    /** The vertical arrangement for items. Required when isVertical is true */
    verticalArrangement: Arrangement.Vertical? = null,
    /** The alignment to align items vertically. Required when isVertical is false */
    verticalAlignment: Alignment.Vertical? = null,
    /** The horizontal arrangement for items. Required when isVertical is false */
    horizontalArrangement: Arrangement.Horizontal? = null,
    /** The content of the list */
    content: LazyListScope.() -> Unit
) {
    val overScrollController = rememberOverScrollController()

    val itemScope: Ref<LazyItemScopeImpl> = remember { Ref() }

    val stateOfItemsProvider = rememberStateOfItemsProvider(content, itemScope)

    val measurePolicy = rememberLazyListMeasurePolicy(
        stateOfItemsProvider,
        itemScope,
        state,
        overScrollController,
        contentPadding,
        reverseLayout,
        isVertical,
        horizontalAlignment,
        verticalAlignment,
        horizontalArrangement,
        verticalArrangement
    )

    state.prefetchPolicy = rememberLazyLayoutPrefetchPolicy()
    val innerState = rememberLazyLayoutState().also { state.innerState = it }

    val itemsProvider = stateOfItemsProvider.value
    if (itemsProvider.itemsCount > 0) {
        state.updateScrollPositionIfTheFirstItemWasMoved(itemsProvider)
    }

    LazyLayout(
        modifier = modifier
            .lazyListSemantics(
                stateOfItemsProvider = stateOfItemsProvider,
                state = state,
                coroutineScope = rememberCoroutineScope(),
                isVertical = isVertical,
                reverseScrolling = reverseLayout
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
                overScrollController = overScrollController
            )
            .padding(contentPadding),
        state = innerState,
        prefetchPolicy = state.prefetchPolicy,
        measurePolicy = measurePolicy,
        itemsProvider = { stateOfItemsProvider.value }
    )
}

@Composable
private fun rememberStateOfItemsProvider(
    content: LazyListScope.() -> Unit,
    itemScope: Ref<LazyItemScopeImpl>
): State<LazyListItemsProvider> {
    val latestContent = rememberUpdatedState(content)
    return remember {
        derivedStateOf { LazyListScopeImpl(itemScope).apply(latestContent.value) }
    }
}

internal class LazyListScopeImpl(
    private val itemScope: Ref<LazyItemScopeImpl>
) : LazyListScope, LazyListItemsProvider {
    private val intervals = IntervalList<IntervalContent>()
    override val itemsCount get() = intervals.totalSize
    private var _headerIndexes: MutableList<Int>? = null
    override val headerIndexes: List<Int> get() = _headerIndexes ?: emptyList()

    override fun getKey(index: Int): Any {
        val interval = intervals.intervalForIndex(index)
        val localIntervalIndex = index - interval.startIndex
        val key = interval.content.key?.invoke(localIntervalIndex)
        return key ?: getDefaultLazyKeyFor(index)
    }

    override fun getContent(index: Int): @Composable () -> Unit {
        val interval = intervals.intervalForIndex(index)
        val localIntervalIndex = index - interval.startIndex
        return interval.content.content.invoke(itemScope.value!!, localIntervalIndex)
    }

    override fun items(
        count: Int,
        key: ((index: Int) -> Any)?,
        itemContent: @Composable LazyItemScope.(index: Int) -> Unit
    ) {
        intervals.add(
            count,
            IntervalContent(
                key = key,
                content = { index -> @Composable { itemContent(index) } }
            )
        )
    }

    override fun item(key: Any?, content: @Composable LazyItemScope.() -> Unit) {
        intervals.add(
            1,
            IntervalContent(
                key = if (key != null) { _: Int -> key } else null,
                content = { @Composable { content() } }
            )
        )
    }

    @ExperimentalFoundationApi
    override fun stickyHeader(key: Any?, content: @Composable LazyItemScope.() -> Unit) {
        val headersIndexes = _headerIndexes ?: mutableListOf<Int>().also {
            _headerIndexes = it
        }
        headersIndexes.add(itemsCount)

        item(key, content)
    }
}

internal class IntervalContent(
    val key: ((index: Int) -> Any)?,
    val content: LazyItemScope.(index: Int) -> @Composable () -> Unit
)

@Composable
private fun rememberLazyListMeasurePolicy(
    /** State containing the items provider of the list. */
    stateOfItemsProvider: State<LazyListItemsProvider>,
    /** Value holder for the item scope used to compose items. */
    itemScope: Ref<LazyItemScopeImpl>,
    /** The state of the list. */
    state: LazyListState,
    /** The overscroll controller. */
    overScrollController: OverScrollController,
    /** The inner padding to be added for the whole content(nor for each individual item) */
    contentPadding: PaddingValues,
    /** reverse the direction of scrolling and layout */
    reverseLayout: Boolean,
    /** The layout orientation of the list */
    isVertical: Boolean,
    /** The alignment to align items horizontally. Required when isVertical is true */
    horizontalAlignment: Alignment.Horizontal? = null,
    /** The alignment to align items vertically. Required when isVertical is false */
    verticalAlignment: Alignment.Vertical? = null,
    /** The horizontal arrangement for items. Required when isVertical is false */
    horizontalArrangement: Arrangement.Horizontal? = null,
    /** The vertical arrangement for items. Required when isVertical is true */
    verticalArrangement: Arrangement.Vertical? = null
) = remember(
    state,
    overScrollController,
    contentPadding,
    reverseLayout,
    isVertical,
    horizontalAlignment,
    verticalAlignment,
    horizontalArrangement,
    verticalArrangement
) {
    LazyMeasurePolicy { placeablesProvider, constraints ->
        constraints.assertNotNestingScrollableContainers(isVertical)

        val itemsProvider = stateOfItemsProvider.value
        state.updateScrollPositionIfTheFirstItemWasMoved(itemsProvider)

        // Update the state's cached Density
        state.density = this

        // this will update the scope object if the constrains have been changed
        itemScope.update(this, constraints)

        val startContentPadding = if (isVertical) {
            contentPadding.calculateTopPadding()
        } else {
            contentPadding.calculateStartPadding(layoutDirection)
        }.roundToPx()
        val endContentPadding = if (isVertical) {
            contentPadding.calculateBottomPadding()
        } else {
            contentPadding.calculateEndPadding(layoutDirection)
        }.roundToPx()
        val mainAxisMaxSize = (if (isVertical) constraints.maxHeight else constraints.maxWidth)
        val spaceBetweenItemsDp = if (isVertical) {
            requireNotNull(verticalArrangement).spacing
        } else {
            requireNotNull(horizontalArrangement).spacing
        }
        val spaceBetweenItems = spaceBetweenItemsDp.roundToPx()

        val itemsCount = itemsProvider.itemsCount

        val itemProvider = LazyMeasuredItemProvider(
            constraints,
            isVertical,
            itemsProvider,
            placeablesProvider
        ) { index, key, placeables ->
            // we add spaceBetweenItems as an extra spacing for all items apart from the last one so
            // the lazy list measuring logic will take it into account.
            val spacing = if (index.value == itemsCount - 1) 0 else spaceBetweenItems
            LazyMeasuredItem(
                index = index.value,
                placeables = placeables,
                isVertical = isVertical,
                horizontalAlignment = horizontalAlignment,
                verticalAlignment = verticalAlignment,
                layoutDirection = layoutDirection,
                reverseLayout = reverseLayout,
                startContentPadding = startContentPadding,
                endContentPadding = endContentPadding,
                spacing = spacing,
                key = key
            )
        }
        state.prefetchPolicy?.constraints = itemProvider.childConstraints

        measureLazyList(
            itemsCount = itemsCount,
            itemProvider = itemProvider,
            mainAxisMaxSize = mainAxisMaxSize,
            startContentPadding = if (reverseLayout) endContentPadding else startContentPadding,
            endContentPadding = if (reverseLayout) startContentPadding else endContentPadding,
            firstVisibleItemIndex = state.firstVisibleItemIndexNonObservable,
            firstVisibleItemScrollOffset = state.firstVisibleItemScrollOffsetNonObservable,
            scrollToBeConsumed = state.scrollToBeConsumed,
            constraints = constraints,
            isVertical = isVertical,
            headerIndexes = itemsProvider.headerIndexes,
            verticalArrangement = verticalArrangement,
            horizontalArrangement = horizontalArrangement,
            reverseLayout = reverseLayout,
            density = this,
            layoutDirection = layoutDirection,
            layout = { width, height, placement -> layout(width, height, emptyMap(), placement) }
        ).also {
            state.applyMeasureResult(it)
            refreshOverScrollInfo(overScrollController, it, contentPadding)
        }.lazyLayoutMeasureResult
    }
}

private fun Ref<LazyItemScopeImpl>.update(density: Density, constraints: Constraints) {
    val value = value
    if (value == null || value.density != density || value.constraints != constraints) {
        this.value = LazyItemScopeImpl(density, constraints)
    }
}

private fun IntrinsicMeasureScope.refreshOverScrollInfo(
    overScrollController: OverScrollController,
    result: LazyListMeasureResult,
    contentPadding: PaddingValues
) {
    val verticalPadding =
        contentPadding.calculateTopPadding() +
            contentPadding.calculateBottomPadding()

    val horizontalPadding =
        contentPadding.calculateLeftPadding(layoutDirection) +
            contentPadding.calculateRightPadding(layoutDirection)

    val canScrollForward = result.canScrollForward
    val canScrollBackward = (result.firstVisibleItem?.index ?: 0) != 0 ||
        result.firstVisibleItemScrollOffset != 0

    overScrollController.refreshContainerInfo(
        Size(
            result.width.toFloat() + horizontalPadding.roundToPx(),
            result.height.toFloat() + verticalPadding.roundToPx()
        ),
        canScrollForward || canScrollBackward
    )
}