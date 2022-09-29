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
import androidx.compose.foundation.lazy.layout.LazyLayoutIntervalContent
import androidx.compose.foundation.lazy.layout.MutableIntervalList
import androidx.compose.runtime.Composable

@ExperimentalFoundationApi
internal class LazyStaggeredGridScopeImpl : LazyStaggeredGridScope {
    val intervals = MutableIntervalList<LazyStaggeredGridIntervalContent>()

    @ExperimentalFoundationApi
    override fun item(
        key: Any?,
        contentType: Any?,
        content: @Composable LazyStaggeredGridItemScope.() -> Unit
    ) {
        items(
            count = 1,
            key = key?.let { { key } },
            contentType = { contentType },
            itemContent = { content() }
        )
    }

    override fun items(
        count: Int,
        key: ((index: Int) -> Any)?,
        contentType: (index: Int) -> Any?,
        itemContent: @Composable LazyStaggeredGridItemScope.(index: Int) -> Unit
    ) {
        intervals.addInterval(
            count,
            LazyStaggeredGridIntervalContent(
                key,
                contentType,
                itemContent
            )
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
internal object LazyStaggeredGridItemScopeImpl : LazyStaggeredGridItemScope

@OptIn(ExperimentalFoundationApi::class)
internal class LazyStaggeredGridIntervalContent(
    override val key: ((index: Int) -> Any)?,
    override val type: ((index: Int) -> Any?),
    val item: @Composable LazyStaggeredGridItemScope.(Int) -> Unit
) : LazyLayoutIntervalContent