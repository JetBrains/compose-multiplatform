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

package androidx.compose.foundation.lazy

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.runtime.Composable

internal class LazyListScopeImpl : LazyListScope {

    private val _intervals = MutableIntervalList<LazyListIntervalContent>()
    val intervals: IntervalList<LazyListIntervalContent> = _intervals

    private var _headerIndexes: MutableList<Int>? = null
    val headerIndexes: List<Int> get() = _headerIndexes ?: emptyList()

    override fun items(
        count: Int,
        key: ((index: Int) -> Any)?,
        itemContent: @Composable LazyItemScope.(index: Int) -> Unit
    ) {
        _intervals.add(
            count,
            LazyListIntervalContent(
                key = key,
                content = { index -> @Composable { itemContent(index) } }
            )
        )
    }

    override fun item(key: Any?, content: @Composable LazyItemScope.() -> Unit) {
        _intervals.add(
            1,
            LazyListIntervalContent(
                key = if (key != null) { _: Int -> key } else null,
                content = { @Composable { content() } }
            )
        )
    }

    @ExperimentalFoundationApi
    override fun stickyHeader(key: Any?, content: @Composable LazyItemScope.() -> Unit) {
        val headersIndexes = _headerIndexes ?: mutableListOf<Int>().also {
            _headerIndexes = it
        }
        headersIndexes.add(_intervals.totalSize)

        item(key, content)
    }
}

internal class LazyListIntervalContent(
    val key: ((index: Int) -> Any)?,
    val content: LazyItemScope.(index: Int) -> @Composable () -> Unit
)
