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

package androidx.compose.foundation.lazy.staggeredgrid

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.lazy.layout.DelegatingLazyLayoutItemProvider
import androidx.compose.foundation.lazy.layout.LazyLayoutItemProvider
import androidx.compose.foundation.lazy.layout.rememberLazyNearestItemsRangeState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState

@Composable
@ExperimentalFoundationApi
internal fun rememberStaggeredGridItemProvider(
    state: LazyStaggeredGridState,
    content: LazyStaggeredGridScope.() -> Unit,
): LazyLayoutItemProvider {
    val latestContent = rememberUpdatedState(content)
    val nearestItemsRangeState = rememberLazyNearestItemsRangeState(
        firstVisibleItemIndex = { state.firstVisibleItems.getOrNull(0) ?: 0 },
        slidingWindowSize = { 90 },
        extraItemCount = { 200 }
    )
    return remember(state) {
        val itemProviderState = derivedStateOf {
            val scope = LazyStaggeredGridScopeImpl().apply(latestContent.value)
            LazyLayoutItemProvider(
                scope.intervals,
                nearestItemsRangeState.value,
            ) { interval, index ->
                interval.item.invoke(LazyStaggeredGridItemScopeImpl, index)
            }
        }
        object : LazyLayoutItemProvider by DelegatingLazyLayoutItemProvider(itemProviderState) { }
    }
}