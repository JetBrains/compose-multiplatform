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
import androidx.compose.foundation.gestures.ScrollableState
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.CollectionInfo
import androidx.compose.ui.semantics.ScrollAxisRange
import androidx.compose.ui.semantics.collectionInfo
import androidx.compose.ui.semantics.horizontalScrollAxisRange
import androidx.compose.ui.semantics.indexForKey
import androidx.compose.ui.semantics.scrollBy
import androidx.compose.ui.semantics.scrollToIndex
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.verticalScrollAxisRange
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@ExperimentalFoundationApi
@Suppress("ComposableModifierFactory", "ModifierInspectorInfo")
@Composable
internal fun Modifier.lazyListSemantics(
    itemProvider: LazyListItemProvider,
    state: LazyListState,
    coroutineScope: CoroutineScope,
    isVertical: Boolean,
    reverseScrolling: Boolean,
    userScrollEnabled: Boolean
) = this.then(
    remember(
        itemProvider,
        state,
        isVertical,
        reverseScrolling,
        userScrollEnabled
    ) {
        val indexForKeyMapping: (Any) -> Int = { needle ->
            val key = itemProvider::getKey
            var result = -1
            for (index in 0 until itemProvider.itemCount) {
                if (key(index) == needle) {
                    result = index
                    break
                }
            }
            result
        }

        val accessibilityScrollState = ScrollAxisRange(
            value = {
                // This is a simple way of representing the current position without
                // needing any lazy items to be measured. It's good enough so far, because
                // screen-readers care mostly about whether scroll position changed or not
                // rather than the actual offset in pixels.
                state.firstVisibleItemIndex + state.firstVisibleItemScrollOffset / 100_000f
            },
            maxValue = {
                if (state.canScrollForward) {
                    // If we can scroll further, we don't know the end yet,
                    // but it's upper bounded by #items + 1
                    itemProvider.itemCount + 1f
                } else {
                    // If we can't scroll further, the current value is the max
                    state.firstVisibleItemIndex + state.firstVisibleItemScrollOffset / 100_000f
                }
            },
            reverseScrolling = reverseScrolling
        )
        val scrollByAction: ((x: Float, y: Float) -> Boolean)? = if (userScrollEnabled) {
            { x, y ->
                val delta = if (isVertical) {
                    y
                } else {
                    x
                }
                coroutineScope.launch {
                    (state as ScrollableState).animateScrollBy(delta)
                }
                // TODO(aelias): is it important to return false if we know in advance we cannot scroll?
                true
            }
        } else {
            null
        }

        val scrollToIndexAction: ((Int) -> Boolean)? = if (userScrollEnabled) {
            { index ->
                require(index >= 0 && index < state.layoutInfo.totalItemsCount) {
                    "Can't scroll to index $index, it is out of " +
                        "bounds [0, ${state.layoutInfo.totalItemsCount})"
                }
                coroutineScope.launch {
                    state.scrollToItem(index)
                }
                true
            }
        } else {
            null
        }

        val collectionInfo = CollectionInfo(
            rowCount = if (isVertical) -1 else 1,
            columnCount = if (isVertical) 1 else -1
        )

        Modifier.semantics {
            indexForKey(indexForKeyMapping)

            if (isVertical) {
                verticalScrollAxisRange = accessibilityScrollState
            } else {
                horizontalScrollAxisRange = accessibilityScrollState
            }

            if (scrollByAction != null) {
                scrollBy(action = scrollByAction)
            }

            if (scrollToIndexAction != null) {
                scrollToIndex(action = scrollToIndexAction)
            }

            this.collectionInfo = collectionInfo
        }
    }
)
