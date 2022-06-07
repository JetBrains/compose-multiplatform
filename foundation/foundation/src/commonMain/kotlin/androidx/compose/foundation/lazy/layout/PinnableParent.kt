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

package androidx.compose.foundation.lazy.layout

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.ui.modifier.modifierLocalOf

// This is a temporary placeholder implementation that prevents the newly focused item from being
// disposed while it is brought into view. We ultimately need a generic solution for this use case
// and to support b/195049010.
/**
 * This is a modifier local that is used to implement pinning (Keeping items around even when they
 * are beyond visible bounds).
 *
 * This API is experimental and will change in the future.
 */
@Suppress("OPT_IN_MARKER_ON_WRONG_TARGET")
@get: ExperimentalFoundationApi
@ExperimentalFoundationApi
val ModifierLocalPinnableParent = modifierLocalOf<PinnableParent?> { null }

/**
 * Parent modifiers that implement this interface should retain its current children when it
 * receives a call to [pinItems] and return a [PinnedItemsHandle]. It should hold on to
 * thesechildren until it receives a call to [PinnedItemsHandle.unpin].
 */
@ExperimentalFoundationApi
interface PinnableParent {
    /**
     * Pin the currently composed items.
     */
    fun pinItems(): PinnedItemsHandle

    /**
     * This is an object returned by [PinnableParent.pinItems] when it pins its
     * currently composed items. It provides an [unpin] function to release the pinned children.
     */
    @ExperimentalFoundationApi
    interface PinnedItemsHandle {
        /**
         *  Un-pin the items associated with this [PinnedItemsHandle].
         */
        fun unpin()
    }
}
