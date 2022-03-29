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
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.Constraints

/**
 * Controller for lazy items prefetching, used by lazy layouts to instruct the prefetcher.
 */
@ExperimentalFoundationApi
@Stable
internal class LazyLayoutPrefetchState {
    internal var prefetcher: Prefetcher? by mutableStateOf(null)

    /**
     * Schedules prefetching for the new items.
     *
     * @param items list of index to constraints pairs for the items to be prefetched.
     */
    fun schedulePrefetch(items: List<Pair<Int, Constraints>>) =
        prefetcher?.schedulePrefetch(items)

    /**
     * Notifies the prefetcher that previously scheduled items are no longer likely to be needed.
     * Already precomposed items will be disposed.
     */
    fun cancelScheduledPrefetch() = prefetcher?.cancelScheduledPrefetch()

    internal interface Prefetcher {
        fun schedulePrefetch(items: List<Pair<Int, Constraints>>)
        fun cancelScheduledPrefetch()
    }
}
