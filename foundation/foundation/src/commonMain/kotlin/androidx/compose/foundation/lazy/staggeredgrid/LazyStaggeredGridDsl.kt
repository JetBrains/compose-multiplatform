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
import androidx.compose.runtime.Composable

@DslMarker
internal annotation class LazyStaggeredGridScopeMarker

@ExperimentalFoundationApi
@LazyStaggeredGridScopeMarker
internal sealed interface LazyStaggeredGridScope {
    fun items(
        count: Int,
        key: ((index: Int) -> Any)? = null,
        contentType: (index: Int) -> Any? = { null },
        itemContent: @Composable LazyStaggeredGridItemScope.(index: Int) -> Unit
    )
}

@ExperimentalFoundationApi
internal sealed interface LazyStaggeredGridItemScope

@ExperimentalFoundationApi
internal fun LazyStaggeredGridScope.item(
    key: Any?,
    contentType: Any?,
    itemContent: @Composable LazyStaggeredGridItemScope.(index: Int) -> Unit
) {
    items(
        count = 1,
        key = key?.let { { it } },
        contentType = { contentType },
        itemContent = itemContent
    )
}

// todo(b/182882362): item DSL for lists/arrays