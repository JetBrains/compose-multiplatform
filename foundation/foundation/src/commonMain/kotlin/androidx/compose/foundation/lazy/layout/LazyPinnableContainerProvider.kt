/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.compose.foundation.lazy.layout

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.layout.LocalPinnableContainer
import androidx.compose.ui.layout.PinnableContainer

internal interface LazyPinnedItem {
    val index: Int
}

internal class LazyPinnedItemContainer(
    private val pinnedItems: MutableList<LazyPinnedItem> = SnapshotStateList()
) : List<LazyPinnedItem> by pinnedItems {
    fun pin(item: LazyPinnedItem) {
        pinnedItems.add(item)
    }

    fun unpin(item: LazyPinnedItem) {
        pinnedItems.remove(item)
    }
}

@Composable
internal fun LazyPinnableContainerProvider(
    owner: LazyPinnedItemContainer,
    index: Int,
    content: @Composable () -> Unit
) {
    val pinnableItem = remember(owner) { LazyPinnableItem(owner) }
    pinnableItem.index = index
    pinnableItem.parentPinnableContainer = LocalPinnableContainer.current
    DisposableEffect(pinnableItem) { onDispose { pinnableItem.onDisposed() } }
    CompositionLocalProvider(
        LocalPinnableContainer provides pinnableItem, content = content
    )
}

private class LazyPinnableItem(
    private val owner: LazyPinnedItemContainer,
) : PinnableContainer, PinnableContainer.PinnedHandle, LazyPinnedItem {
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
            owner.pin(this)
            parentHandle = parentPinnableContainer?.pin()
        }
        pinsCount++
        return this
    }

    override fun unpin() {
        check(pinsCount > 0) { "Unpin should only be called once" }
        pinsCount--
        if (pinsCount == 0) {
            owner.unpin(this)
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
