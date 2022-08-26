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
import androidx.compose.foundation.lazy.layout.LazyLayoutItemProvider
import androidx.compose.foundation.lazy.layout.findIndexByKey
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.Snapshot

@ExperimentalFoundationApi
internal class LazyStaggeredGridScrollPosition(
    initialIndices: IntArray,
    initialOffsets: IntArray,
    private val fillIndices: (targetIndex: Int, laneCount: Int) -> IntArray
) {
    var indices by mutableStateOf(initialIndices)
    var offsets by mutableStateOf(initialOffsets)

    private var hadFirstNotEmptyLayout = false

    /** The last know key of the item at lowest of [indices] position. */
    private var lastKnownFirstItemKey: Any? = null

    /**
     * Updates the current scroll position based on the results of the last measurement.
     */
    fun updateFromMeasureResult(measureResult: LazyStaggeredGridMeasureResult) {
        lastKnownFirstItemKey = measureResult.visibleItemsInfo.firstOrNull()?.key
        // we ignore the index and offset from measureResult until we get at least one
        // measurement with real items. otherwise the initial index and scroll passed to the
        // state would be lost and overridden with zeros.
        if (hadFirstNotEmptyLayout || measureResult.totalItemsCount > 0) {
            hadFirstNotEmptyLayout = true
            Snapshot.withoutReadObservation {
                update(
                    measureResult.firstVisibleItemIndices,
                    measureResult.firstVisibleItemScrollOffsets
                )
            }
        }
    }

    /**
     * Updates the scroll position - the passed values will be used as a start position for
     * composing the items during the next measure pass and will be updated by the real
     * position calculated during the measurement. This means that there is no guarantee that
     * exactly this index and offset will be applied as it is possible that:
     * a) there will be no item at this index in reality
     * b) item at this index will be smaller than the asked scrollOffset, which means we would
     * switch to the next item
     * c) there will be not enough items to fill the viewport after the requested index, so we
     * would have to compose few elements before the asked index, changing the first visible item.
     */
    fun requestPosition(index: Int, scrollOffset: Int) {
        val newIndices = fillIndices(index, indices.size)
        val newOffsets = IntArray(newIndices.size) { scrollOffset }
        update(newIndices, newOffsets)
        // clear the stored key as we have a direct request to scroll to [index] position and the
        // next [updateScrollPositionIfTheFirstItemWasMoved] shouldn't override this.
        lastKnownFirstItemKey = null
    }

    /**
     * In addition to keeping the first visible item index we also store the key of this item.
     * When the user provided custom keys for the items this mechanism allows us to detect when
     * there were items added or removed before our current first visible item and keep this item
     * as the first visible one even given that its index has been changed.
     */
    @ExperimentalFoundationApi
    fun updateScrollPositionIfTheFirstItemWasMoved(itemProvider: LazyLayoutItemProvider) {
        Snapshot.withoutReadObservation {
            val lastIndex = itemProvider.findIndexByKey(
                key = lastKnownFirstItemKey,
                lastKnownIndex = indices.getOrNull(0) ?: 0
            )
            if (lastIndex !in indices) {
                update(
                    fillIndices(lastIndex, indices.size),
                    offsets
                )
            }
        }
    }

    private fun update(indices: IntArray, offsets: IntArray) {
        if (!indices.contentEquals(this.indices)) {
            this.indices = indices
        }
        if (!offsets.contentEquals(this.offsets)) {
            this.offsets = offsets
        }
    }
}