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

import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize

internal class LazyStaggeredGridMeasureResult(
    val firstVisibleItemIndices: IntArray,
    val firstVisibleItemScrollOffsets: IntArray,
    val consumedScroll: Float,
    val measureResult: MeasureResult,
    val canScrollForward: Boolean,
    val canScrollBackward: Boolean,
    override val totalItemsCount: Int,
    override val visibleItemsInfo: List<LazyStaggeredGridItemInfo>
) : LazyStaggeredGridLayoutInfo, MeasureResult by measureResult

internal interface LazyStaggeredGridItemInfo {
    val offset: IntOffset
    val index: Int
    val lane: Int
    val key: Any
    val size: IntSize
}

internal interface LazyStaggeredGridLayoutInfo {
    val visibleItemsInfo: List<LazyStaggeredGridItemInfo>
    val totalItemsCount: Int

    companion object Empty : LazyStaggeredGridLayoutInfo {
        override val visibleItemsInfo: List<LazyStaggeredGridItemInfo> = emptyList()
        override val totalItemsCount: Int = 0
    }
}