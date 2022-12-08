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

package androidx.compose.foundation.lazy

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.layout.LocalPinnableContainer
import androidx.compose.ui.layout.PinnableContainer

internal interface LazyListPinnedItem {
    val index: Int
}

@Composable
internal fun LazyListPinnableContainerProvider(
    state: LazyListState,
    index: Int,
    content: @Composable () -> Unit
) {
    val pinnableItem = remember(state) { LazyListPinnableItem(state) }
    pinnableItem.index = index
    pinnableItem.parentPinnableContainer = LocalPinnableContainer.current
    DisposableEffect(pinnableItem) { onDispose { pinnableItem.onDisposed() } }
    CompositionLocalProvider(
        LocalPinnableContainer provides pinnableItem, content = content
    )
}

private class LazyListPinnableItem(
    private val state: LazyListState,
) : PinnableContainer, PinnableContainer.PinnedHandle, LazyListPinnedItem {
    /**
     * Current index associated with this item.
     */
    override var index by mutableStateOf(-1)

    /**
     * It is a valid use case when users of this class call [pin] multiple times individually,
     * so we want to do the unpinning only when all of the users called [unpin].
     */
    private var pinsCount by mutableStateOf(0)

    /**
     * Handle associated with the current [parentPinnableContainer].
     */
    private var parentHandle by mutableStateOf<PinnableContainer.PinnedHandle?>(null)

    /**
     * Current parent [PinnableContainer].
     * Note that we should correctly re-pin if we pinned the previous container.
     */
    private var _parentPinnableContainer by mutableStateOf<PinnableContainer?>(null)
    var parentPinnableContainer: PinnableContainer? get() = _parentPinnableContainer
        set(value) {
            Snapshot.withoutReadObservation {
                val previous = _parentPinnableContainer
                if (value !== previous) {
                    _parentPinnableContainer = value
                    if (pinsCount > 0) {
                        parentHandle?.unpin()
                        parentHandle = value?.pin()
                    }
                }
            }
        }

    override fun pin(): PinnableContainer.PinnedHandle {
        if (pinsCount == 0) {
            state.pinnedItems.add(this)
            parentHandle = parentPinnableContainer?.pin()
        }
        pinsCount++
        return this
    }

    override fun unpin() {
        check(pinsCount > 0) { "Unpin should only be called once" }
        pinsCount--
        if (pinsCount == 0) {
            state.pinnedItems.remove(this)
            parentHandle?.unpin()
            parentHandle = null
        }
    }

    fun onDisposed() {
        repeat(pinsCount) {
            unpin()
        }
    }
}
