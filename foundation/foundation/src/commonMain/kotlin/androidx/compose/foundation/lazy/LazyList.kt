/*
 * Copyright 2020 The Android Open Source Project
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

import androidx.compose.foundation.assertNotNestingScrollableContainers
import androidx.compose.foundation.gestures.FlingBehavior
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection

internal interface LazyKeyAndScopedContentFactory {
    fun getKey(index: Int): Any
    fun getContent(index: Int, scope: LazyItemScope): @Composable() () -> Unit
}

@Composable
internal fun LazyList(
    /** The total size of the list */
    itemsCount: Int,
    /** Modifier to be applied for the inner layout */
    modifier: Modifier,
    /** State controlling the scroll position */
    state: LazyListState,
    /** The inner padding to be added for the whole content(nor for each individual item) */
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
    /** The list of indexes of the sticky header items */
    headerIndexes: List<Int> = emptyList(),
    scopedFactory: LazyKeyAndScopedContentFactory
) {
    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    // reverse scroll by default, to have "natural" gesture that goes reversed to layout
    // if rtl and horizontal, do not reverse to make it right-to-left
    val reverseScrollDirection = if (!isVertical && isRtl) reverseLayout else !reverseLayout

    val saveableStateHolder = rememberSaveableStateHolder()
    val factory = remember {
        LazyListItemContentFactory(saveableStateHolder, scopedFactory, itemsCount)
    }
    factory.update(scopedFactory, itemsCount, state)

    SubcomposeLayout(
        modifier
            .scrollable(
                orientation = if (isVertical) Orientation.Vertical else Orientation.Horizontal,
                reverseDirection = reverseScrollDirection,
                interactionSource = state.internalInteractionSource,
                flingBehavior = flingBehavior,
                state = state
            )
            .clipToBounds()
            .padding(contentPadding)
            .then(state.remeasurementModifier)
    ) { constraints ->
        constraints.assertNotNestingScrollableContainers(isVertical)

        // Update the state's cached Density
        state.density = Density(density, fontScale)

        // this will update the scope object if the constrains have been changed
        factory.updateItemScope(this, constraints)

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

        val itemProvider = LazyMeasuredItemProvider(
            constraints,
            isVertical,
            this,
            factory
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
                startContentPadding = startContentPadding,
                endContentPadding = endContentPadding,
                spacing = spacing,
                key = key
            )
        }

        val measureResult = measureLazyList(
            itemsCount,
            itemProvider,
            mainAxisMaxSize,
            startContentPadding,
            endContentPadding,
            state.firstVisibleItemIndexNonObservable,
            state.firstVisibleItemScrollOffsetNonObservable,
            state.scrollToBeConsumed
        )

        state.applyMeasureResult(measureResult)

        val headers = if (headerIndexes.isNotEmpty()) {
            LazyListHeaders(itemProvider, headerIndexes, measureResult, startContentPadding)
        } else {
            null
        }

        layoutLazyList(
            constraints,
            isVertical,
            verticalArrangement,
            horizontalArrangement,
            measureResult,
            reverseLayout,
            headers
        )
    }
}
