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

package androidx.compose.ui.layout

import androidx.compose.runtime.Stable
import androidx.compose.runtime.compositionLocalOf

/**
 * Use this composition local to get the [PinnableContainer] handling the current subhierarchy.
 *
 * It will be not null, for example, when the current content is composed as an item of lazy list.
 */
val LocalPinnableContainer = compositionLocalOf<PinnableContainer?> { null }

/**
 * Represents a container which can be pinned when the content of this container is important.
 *
 * For example, each item of lazy list represents one [PinnableContainer], and if this
 * container is pinned, this item will not be disposed when scrolled out of the viewport.
 *
 * Pinning a currently focused item so the focus is not lost is one of the examples when this
 * functionality can be useful.
 *
 * @see LocalPinnableContainer
 */
@Stable
interface PinnableContainer {

    /**
     * Allows to pin this container when the associated content is considered important.
     *
     * For example, if this [PinnableContainer] is an item of lazy list pinning will mean
     * this item will not be disposed when scrolled out of the viewport.
     *
     * Don't forget to call [PinnedHandle.release] when this content is not important anymore.
     */
    fun pin(): PinnedHandle

    /**
     * This is an object returned by [pin] which allows to release the pinning.
     */
    @Suppress("NotCloseable")
    fun interface PinnedHandle {
        /**
         * Releases the pin.
         *
         * For example, if this [PinnableContainer] is an item of lazy list releasing the
         * pinning will allow lazy list to stop composing the item when it is not visible.
         */
        fun release()
    }
}
