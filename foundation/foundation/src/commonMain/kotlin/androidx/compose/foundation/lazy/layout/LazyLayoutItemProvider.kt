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

package androidx.compose.foundation.lazy.layout

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable

/**
 * Provides all the needed info about the items which could be later composed and displayed as
 * children or [LazyLayout].
 */
@Stable
@ExperimentalFoundationApi
interface LazyLayoutItemProvider {

    /**
     * The total number of items in the lazy layout (visible or not).
     */
    val itemCount: Int

    /**
     * The item for the given [index].
     */
    @Composable
    fun Item(index: Int)

    /**
     * Returns the content type for the item on this index. It is used to improve the item
     * compositions reusing efficiency. Note that null is a valid type and items of such
     * type will be considered compatible.
     */
    fun getContentType(index: Int): Any? = null

    /**
     * Returns the key for the item on this index.
     *
     * @see getDefaultLazyLayoutKey which you can use if the user didn't provide a key.
     */
    fun getKey(index: Int): Any = getDefaultLazyLayoutKey(index)

    /**
     * Contains the mapping between the key and the index. It could contain not all the items of
     * the list as an optimization or be empty if user didn't provide a custom key-index mapping.
     */
    val keyToIndexMap: Map<Any, Int> get() = emptyMap()
}

/**
 * This creates an object meeting following requirements:
 * 1) Objects created for the same index are equals and never equals for different indexes.
 * 2) This class is saveable via a default SaveableStateRegistry on the platform.
 * 3) This objects can't be equals to any object which could be provided by a user as a custom key.
 */
@ExperimentalFoundationApi
expect fun getDefaultLazyLayoutKey(index: Int): Any
