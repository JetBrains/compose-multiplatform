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
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.Constraints

/** Creates a [LazyLayoutPrefetchPolicy]. */
@ExperimentalFoundationApi
@Composable
internal fun rememberLazyLayoutPrefetchPolicy(): LazyLayoutPrefetchPolicy = remember {
    LazyLayoutPrefetchPolicy()
}

/**
 * Controller for lazy items prefetching, used by lazy layouts to instruct the prefetcher.
 */
@ExperimentalFoundationApi
@Stable
internal class LazyLayoutPrefetchPolicy {
    internal var prefetcher: Subscriber? = null

    /** Schedules new items to prefetch, specified by [indices]. */
    fun scheduleForPrefetch(indices: List<Pair<Int, Constraints>>) =
        prefetcher?.scheduleForPrefetch(indices)

    /** Notifies the prefetcher that previously scheduled items are no longer likely to be needed. */
    fun cancelScheduledPrefetch() = prefetcher?.cancelScheduledPrefetch()

    interface Subscriber {
        fun scheduleForPrefetch(indices: List<Pair<Int, Constraints>>)
        fun cancelScheduledPrefetch()
    }
}
