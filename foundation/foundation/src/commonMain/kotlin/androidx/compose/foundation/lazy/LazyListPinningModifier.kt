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

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.lazy.layout.ModifierLocalPinnableParent
import androidx.compose.foundation.lazy.layout.PinnableParent
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.modifier.ModifierLocalProvider
import androidx.compose.ui.modifier.ProvidableModifierLocal
import androidx.compose.ui.platform.debugInspectorInfo

/**
 * This is a temporary placeholder implementation of pinning until we implement b/195049010.
 */
internal fun Modifier.lazyListPinningModifier(
    state: LazyListState,
    beyondBoundsInfo: LazyListBeyondBoundsInfo
) = composed(
    "androidx.compose.foundation.lazy",
    state,
    beyondBoundsInfo,
    debugInspectorInfo {
        name = "lazyListPinningModifier"
        properties["state"] = state
        properties["beyondBoundsInfo"] = beyondBoundsInfo
    }
) {
    remember(state, beyondBoundsInfo) { LazyListPinningModifier(state, beyondBoundsInfo) }
}

@OptIn(ExperimentalFoundationApi::class)
private class LazyListPinningModifier(
    private val state: LazyListState,
    private val beyondBoundsInfo: LazyListBeyondBoundsInfo,
) : ModifierLocalProvider<PinnableParent?>, PinnableParent {
    override val key: ProvidableModifierLocal<PinnableParent?>
        get() = ModifierLocalPinnableParent

    override val value: PinnableParent
        get() = this

    override fun pinItems(): PinnableParent.PinnedItemsHandle {
        if (!beyondBoundsInfo.hasIntervals()) return EmptyPinnedItemsHandle
        return with(beyondBoundsInfo) {
            object : PinnableParent.PinnedItemsHandle {
                val interval = addInterval(start, end)
                override fun unpin() {
                    removeInterval(interval)
                    state.remeasurement?.forceRemeasure()
                }
            }
        }
    }

    companion object {
        private val EmptyPinnedItemsHandle = object : PinnableParent.PinnedItemsHandle {
            override fun unpin() {}
        }
    }
}
