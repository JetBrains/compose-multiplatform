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

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.Constraints

/** Creates a [LazyLayoutPrefetchPolicy]. */
@Composable
internal fun rememberLazyLayoutPrefetchPolicy(): LazyLayoutPrefetchPolicy = remember {
    LazyLayoutPrefetchPolicy()
}

/**
 * Controller for lazy items prefetching, used by lazy layouts to instruct the prefetcher.
 * TODO: This is currently supporting just one item, but it should rather be a queue.
 */
@Stable
internal class LazyLayoutPrefetchPolicy {
    internal var prefetcher: Subscriber? = null

    /** Schedules a new item to prefetch, specified by [index]. */
    fun scheduleForPrefetch(index: Int) = prefetcher?.scheduleForPrefetch(index)

    /** Notifies the prefetcher that item with [index] is no longer likely to be needed. */
    fun removeFromPrefetch(index: Int) = prefetcher?.removeFromPrefetch(index)

    /** The constraints to be used for premeasuring the precomposed items. */
    var constraints = Constraints()

    interface Subscriber {
        fun scheduleForPrefetch(index: Int)
        fun removeFromPrefetch(index: Int)
    }
}
