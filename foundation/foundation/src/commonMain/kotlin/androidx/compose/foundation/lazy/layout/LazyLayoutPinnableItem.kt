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

import androidx.compose.foundation.ExperimentalFoundationApi
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

/**
 * Wrapper supporting [PinnableContainer] in lazy layout items. Each pinned item
 * is considered important to keep alive even if it would be discarded otherwise.
 *
 * @param key key of the item inside the lazy layout
 * @param index index of the item inside the lazy layout
 * @param pinnedItemList container of currently pinned items
 * @param content inner content of this item
 */
@ExperimentalFoundationApi
@Composable
fun LazyLayoutPinnableItem(
    key: Any?,
    index: Int,
    pinnedItemList: LazyLayoutPinnedItemList,
    content: @Composable () -> Unit
) {
    val pinnableItem = remember(key, pinnedItemList) { LazyLayoutPinnableItem(key, pinnedItemList) }
    pinnableItem.index = index
    pinnableItem.parentPinnableContainer = LocalPinnableContainer.current
    DisposableEffect(pinnableItem) { onDispose { pinnableItem.onDisposed() } }
    CompositionLocalProvider(
        LocalPinnableContainer provides pinnableItem, content = content
    )
}

/**
 * Read-only list of pinned items in a lazy layout.
 * The items are modified internally by the [PinnableContainer] consumers, for example if something
 * inside item content is focused.
 */
@ExperimentalFoundationApi
class LazyLayoutPinnedItemList private constructor(
    private val items: MutableList<PinnedItem>
) : List<LazyLayoutPinnedItemList.PinnedItem> by items {
    constructor() : this(SnapshotStateList())

    internal fun pin(item: PinnedItem) {
        items.add(item)
    }

    internal fun release(item: PinnedItem) {
        items.remove(item)
    }

    /**
     * Item pinned in a lazy layout. Pinned item should be always measured and laid out,
     * even if the item is beyond the boundaries of the layout.
     */
    @ExperimentalFoundationApi
    sealed interface PinnedItem {
        /**
         * Key of the pinned item.
         */
        val key: Any?

        /**
         * Last known index of the pinned item.
         * Note: it is possible for index to change during lifetime of the object.
         */
        val index: Int
    }
}

@OptIn(ExperimentalFoundationApi::class)
private class LazyLayoutPinnableItem(
    override val key: Any?,
    private val pinnedItemList: LazyLayoutPinnedItemList,
) : PinnableContainer, PinnableContainer.PinnedHandle, LazyLayoutPinnedItemList.PinnedItem {
    /**
     * Current index associated with this item.
     */
    override var index by mutableStateOf(-1)

    /**
     * It is a valid use case when users of this class call [pin] multiple times individually,
     * so we want to do the unpinning only when all of the users called [release].
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
                        parentHandle?.release()
                        parentHandle = value?.pin()
                    }
                }
            }
        }

    override fun pin(): PinnableContainer.PinnedHandle {
        if (pinsCount == 0) {
            pinnedItemList.pin(this)
            parentHandle = parentPinnableContainer?.pin()
        }
        pinsCount++
        return this
    }

    override fun release() {
        check(pinsCount > 0) { "Release should only be called once" }
        pinsCount--
        if (pinsCount == 0) {
            pinnedItemList.release(this)
            parentHandle?.release()
            parentHandle = null
        }
    }

    fun onDisposed() {
        repeat(pinsCount) {
            release()
        }
    }
}
