/*
 * Copyright 2020 The Android Open Source Project
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

/**
 * Contains useful information about an individual item in lazy lists like [LazyColumn]
 *  or [LazyRow].
 *
 * @see LazyListLayoutInfo
 */
interface LazyListItemInfo {
    /**
     * The index of the item in the list.
     */
    val index: Int

    /**
     * The key of the item which was passed to the item() or items() function.
     */
    val key: Any

    /**
     * The main axis offset of the item in pixels. It is relative to the start of the lazy list container.
     */
    val offset: Int

    /**
     * The main axis size of the item in pixels. Note that if you emit multiple layouts in the composable
     * slot for the item then this size will be calculated as the sum of their sizes.
     */
    val size: Int
}
