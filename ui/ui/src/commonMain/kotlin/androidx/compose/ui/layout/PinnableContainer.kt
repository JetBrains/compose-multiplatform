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
 * Parent layouts with pinning support for the children content will provide the current
 * [PinnableContainer] via this composition local.
 */
val LocalPinnableContainer = compositionLocalOf<PinnableContainer?> { null }

/**
 * Represents a container which can be pinned by some of its parents.
 *
 * For example in lists which only compose visible items it means this item will be kept
 * composed even when it will be scrolled out of the view.
 */
@Stable
interface PinnableContainer {

    /**
     * Pin the current container when its content needs to be kept alive, for example when it has
     * focus or user is interacting with it in some other way.
     *
     * Don't forget to call [PinnedHandle.unpin] when this content is not needed anymore.
     */
    fun pin(): PinnedHandle

    /**
     * This is an object returned by [pin] which allows to unpin the content.
     */
    fun interface PinnedHandle {
        /**
         *  Unpin the container associated with this handle.
         */
        fun unpin()
    }
}
