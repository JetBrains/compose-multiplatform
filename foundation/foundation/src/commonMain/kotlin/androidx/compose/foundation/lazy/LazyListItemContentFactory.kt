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
import androidx.compose.runtime.neverEqualPolicy
import androidx.compose.runtime.saveable.SaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * This class:
 * 1) Caches the lambdas being produced by [scopedFactory]. This allows us to perform less
 * recompositions as the compose runtime can skip the whole composition if we subcompose with the
 * same instance of the content lambda.
 * 2) Updates the mapping between keys and indexes when we have a new factory
 * 3) Creates an [itemScope] to be used with [scopedFactory]
 * 4) Adds state restoration on top of the composable returned by [scopedFactory] with help of
 * [saveableStateHolder].
 */
internal class LazyListItemContentFactory(
    private val saveableStateHolder: SaveableStateHolder,
    private var scopedFactory: LazyKeyAndScopedContentFactory,
    itemsCount: Int
) {

    /**
     * Contains the cached lambdas produced by the [scopedFactory].
     */
    private val lambdasCache = mutableMapOf<Any, CachedItemContent>()

    /**
     * Current factory for creating an item content lambdas.
     */
    private var observableScopedFactory by mutableStateOf(scopedFactory, neverEqualPolicy())

    /**
     * Current items count.
     */
    private var itemsCount: Int by mutableStateOf(itemsCount)

    fun update(
        scopedFactory: LazyKeyAndScopedContentFactory,
        itemsCount: Int,
        state: LazyListState
    ) {
        if (this.scopedFactory != scopedFactory) {
            this.scopedFactory = scopedFactory
            observableScopedFactory = scopedFactory
        }
        this.itemsCount = itemsCount
        if (itemsCount > 0) {
            val firstVisible = state.firstVisibleItemIndexNonObservable.value
            val lastVisible = state.lastVisibleItemIndexNonObservable.value
            for (i in firstVisible..minOf(itemsCount - 1, lastVisible)) {
                lambdasCache[scopedFactory.getKey(i)]?.index = i
            }
        }
    }

    /**
     * Return a key associated with the given [index].
     */
    fun getKey(index: Int) = scopedFactory.getKey(index)

    /**
     * Return cached item content lambda or creates a new lambda and puts it in the cache.
     */
    fun getContent(index: Int, key: Any): @Composable () -> Unit {
        val cachedContent = lambdasCache.getOrPut(key) { CachedItemContent(index, key) }
        cachedContent.index = index
        return cachedContent.content
    }

    private inner class CachedItemContent(
        initialIndex: Int,
        val key: Any
    ) {
        var index by mutableStateOf(initialIndex)

        val content: @Composable () -> Unit = @Composable {
            if (index < itemsCount) {
                val content = observableScopedFactory.getContent(index, itemScope)
                saveableStateHolder.SaveableStateProvider(key, content)
            }
        }
    }

    /**
     * The cached instance of the scope to be used for composing items.
     */
    private var itemScope by mutableStateOf(InitialLazyItemsScopeImpl)
    private var lastDensity: Density = Density(0f, 0f)
    private var lastConstraints: Constraints = Constraints()

    /**
     * Updates the [itemScope] with the last [constraints] we got from the parent.
     */
    fun updateItemScope(density: Density, constraints: Constraints) {
        if (lastDensity != density || lastConstraints != constraints) {
            lastDensity = density
            lastConstraints = constraints
            with(density) {
                val width = constraints.maxWidth.toDp()
                val height = constraints.maxHeight.toDp()
                itemScope = LazyItemScopeImpl(width, height)
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
