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
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.InternalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.gesture.scrollorientationlocking.Orientation
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.platform.AmbientLayoutDirection
import androidx.compose.ui.unit.LayoutDirection

@OptIn(InternalLayoutApi::class)
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
    /** The layout orientation of the list */
    isVertical: Boolean,
    /** The alignment to align items horizontally. Required when isVertical is true */
    horizontalAlignment: Alignment.Horizontal? = null,
    /** The vertical arrangement for items. Required when isVertical is true */
    verticalArrangement: Arrangement.Vertical? = null,
    /** The alignment to align items vertically. Required when isVertical is false */
    verticalAlignment: Alignment.Vertical? = null,
    /** The horizontal arrangement for items. Required when isVertical is false */
    horizontalArrangement: Arrangement.Horizontal? = null,
    /** The factory defining the content for an item on the given position in the list */
    itemContentFactory: LazyItemScope.(Int) -> @Composable () -> Unit
) {
    val reverseDirection = AmbientLayoutDirection.current == LayoutDirection.Rtl && !isVertical

    val cachingItemContentFactory = remember { CachingItemContentFactory(itemContentFactory) }
    cachingItemContentFactory.itemContentFactory = itemContentFactory

    val startContentPadding = if (isVertical) contentPadding.top else contentPadding.start
    val endContentPadding = if (isVertical) contentPadding.bottom else contentPadding.end
    SubcomposeLayout(
        modifier
            .scrollable(
                orientation = if (isVertical) Orientation.Vertical else Orientation.Horizontal,
                // reverse scroll by default, to have "natural" gesture that goes reversed to layout
                reverseDirection = !reverseDirection,
                controller = state.scrollableController
            )
            .clipToBounds()
            .padding(contentPadding)
            .then(state.remeasurementModifier)
    ) { constraints ->
        constraints.assertNotNestingScrollableContainers(isVertical)

        // this will update the scope object if the constrains have been changed
        cachingItemContentFactory.updateItemScope(this, constraints)

        val startContentPaddingPx = startContentPadding.toIntPx()
        val endContentPaddingPx = endContentPadding.toIntPx()
        val mainAxisMaxSize = (if (isVertical) constraints.maxHeight else constraints.maxWidth)
        val spaceBetweenItemsDp = if (isVertical) {
            requireNotNull(verticalArrangement).spacing
        } else {
            requireNotNull(horizontalArrangement).spacing
        }
        val spaceBetweenItems = spaceBetweenItemsDp.toIntPx()

        val itemProvider = LazyMeasuredItemProvider(
            constraints,
            isVertical,
            this,
            cachingItemContentFactory
        ) { index, placeables ->
            // we add spaceBetweenItems as an extra size for all items apart from the last one so
            // the lazy list measuring logic will take it into account.
            val extraMainAxisSize = if (index.value == itemsCount - 1) 0 else spaceBetweenItems
            LazyMeasuredItem(
                placeables = placeables,
                isVertical = isVertical,
                horizontalAlignment = horizontalAlignment,
                verticalAlignment = verticalAlignment,
                layoutDirection = layoutDirection,
                startContentPadding = startContentPaddingPx,
                endContentPadding = endContentPaddingPx,
                extraMainAxisSize = extraMainAxisSize
            )
        }

        val measureResult = measureLazyList(
            itemsCount,
            itemProvider,
            mainAxisMaxSize,
            startContentPaddingPx,
            endContentPaddingPx,
            state.firstVisibleItemIndexNonObservable,
            state.firstVisibleItemScrollOffsetNonObservable,
            state.scrollToBeConsumed
        )

        state.applyMeasureResult(measureResult)

        layoutLazyList(
            constraints,
            isVertical,
            verticalArrangement,
            horizontalArrangement,
            measureResult
        )
    }
}
