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
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.lazy.layout.LazyLayoutItemProvider
import androidx.compose.foundation.lazy.layout.LazyLayoutSemanticState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.semantics.CollectionInfo
import androidx.compose.ui.semantics.ScrollAxisRange

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun rememberLazyListSemanticState(
    state: LazyListState,
    itemProvider: LazyLayoutItemProvider,
    reverseScrolling: Boolean,
    isVertical: Boolean
): LazyLayoutSemanticState =
    remember(state, itemProvider, reverseScrolling, isVertical) {
        object : LazyLayoutSemanticState {
            override fun scrollAxisRange(): ScrollAxisRange =
                ScrollAxisRange(
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
                            state.firstVisibleItemIndex +
                                state.firstVisibleItemScrollOffset / 100_000f
                        }
                    },
                    reverseScrolling = reverseScrolling
                )

            override suspend fun animateScrollBy(delta: Float) {
                state.animateScrollBy(delta)
            }

            override suspend fun scrollToItem(index: Int) {
                state.scrollToItem(index)
            }

            override fun collectionInfo(): CollectionInfo =
                if (isVertical) {
                    CollectionInfo(rowCount = -1, columnCount = 1)
                } else {
                    CollectionInfo(rowCount = 1, columnCount = -1)
                }
        }
    }
