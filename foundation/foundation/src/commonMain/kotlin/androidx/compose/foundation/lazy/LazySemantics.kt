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

import androidx.compose.runtime.State
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.CollectionInfo
import androidx.compose.ui.semantics.collectionInfo
import androidx.compose.ui.semantics.indexForKey
import androidx.compose.ui.semantics.scrollToIndex
import androidx.compose.ui.semantics.semantics
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

internal fun Modifier.lazyListSemantics(
    stateOfItemsProvider: State<LazyListItemsProvider>,
    state: LazyListState,
    coroutineScope: CoroutineScope,
    isVertical: Boolean
): Modifier {
    return semantics {
        indexForKey { needle ->
            val key = stateOfItemsProvider.value::getKey
            for (index in 0 until stateOfItemsProvider.value.itemsCount) {
                if (key(index) == needle) {
                    return@indexForKey index
                }
            }
            -1
        }

        scrollToIndex { index ->
            require(index >= 0 && index < stateOfItemsProvider.value.itemsCount) {
                "Can't scroll to index $index, it is out of bounds [0, ${stateOfItemsProvider
                    .value.itemsCount})"
            }
            coroutineScope.launch {
                state.scrollToItem(index)
            }
            true
        }

        collectionInfo = CollectionInfo(
            rowCount = if (isVertical) -1 else 1,
            columnCount = if (isVertical) 1 else -1
        )
    }
}
