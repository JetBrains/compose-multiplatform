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

import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Caches the lambdas being produced by ItemContent factory. This allows us to perform less
 * recompositions as the compose runtime can skip the whole composition if we subcompose with the
 * same instance of the content lambda.
 */
internal class CachingItemContentFactory(
    factory: LazyItemScope.(Int) -> ItemContent,
    itemsCount: Int
) : (Int) -> ItemContent {

    /**
     * The cached instance of the scope to be used for composing items.
     */
    private var itemScope by mutableStateOf(InitialLazyItemsScopeImpl)

    /**
     * Contains the cached lambdas produced by the [factory].
     */
    private val lambdasCache = mutableMapOf<Any, CachedItemContent>()

    /**
     * Current factory for creating an item content lambdas.
     */
    private var factory: LazyItemScope.(Int) -> ItemContent by mutableStateOf(factory)

    /**
     * Current items count.
     */
    private var itemsCount: Int by mutableStateOf(itemsCount)

    internal fun update(
        factory: LazyItemScope.(Int) -> ItemContent,
        itemsCount: Int,
        state: LazyListState
    ) {
        this.factory = factory
        this.itemsCount = itemsCount
        if (itemsCount > 0) {
            val firstVisible = state.firstVisibleItemIndexNonObservable.value
            val lastVisible = state.lastVisibleItemIndexNonObservable.value
            for (i in firstVisible..minOf(itemsCount - 1, lastVisible)) {
                lambdasCache[itemScope.factory(i).key]?.index = i
            }
        }
    }

    /**
     * Updates the [itemScope] with the last [constraints] we got from the parent.
     */
    internal fun updateItemScope(density: Density, constraints: Constraints) = with(density) {
        val width = constraints.maxWidth.toDp()
        val height = constraints.maxHeight.toDp()
        itemScope = LazyItemScopeImpl(width, height)
    }

    /**
     * Return cached item content lambda or creates a new lambda and puts it in the cache.
     */
    override fun invoke(index: Int): ItemContent {
        val content = itemScope.factory(index)
        val cachedContent = lambdasCache.getOrPut(content.key) {
            CachedItemContent(index, content.key)
        }
        cachedContent.index = index
        return cachedContent
    }

    private inner class CachedItemContent(
        initialIndex: Int,
        override val key: Any
    ) : ItemContent {
        var index by mutableStateOf(initialIndex)

        override val content = @Composable {
            if (index < itemsCount) {
                val itemContent = itemScope.factory(index)
                if (itemContent.key == key) {
                    itemContent.content()
                }
            }
        }
    }
}

/**
 * Pre-allocated initial value for [LazyItemScopeImpl] to not have it nullable and avoid using
 * late init.
 */
private val InitialLazyItemsScopeImpl = LazyItemScopeImpl(0.dp, 0.dp)

private data class LazyItemScopeImpl(
    val maxWidth: Dp,
    val maxHeight: Dp
) : LazyItemScope {
    override fun Modifier.fillParentMaxSize(fraction: Float) = size(
        maxWidth * fraction,
        maxHeight * fraction
    )

    override fun Modifier.fillParentMaxWidth(fraction: Float) =
        width(maxWidth * fraction)

    override fun Modifier.fillParentMaxHeight(fraction: Float) =
        height(maxHeight * fraction)
}
