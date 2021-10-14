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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.SaveableStateHolder
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density

@Composable
internal fun rememberItemContentFactory(state: LazyLayoutState): LazyLayoutItemContentFactory {
    val saveableStateHolder = rememberSaveableStateHolder()
    val itemsProvider = state.itemsProvider
    return remember(itemsProvider) {
        LazyLayoutItemContentFactory(saveableStateHolder, itemsProvider)
    }
}

/**
 * This class:
 * 1) Caches the lambdas being produced by [itemsProvider]. This allows us to perform less
 * recompositions as the compose runtime can skip the whole composition if we subcompose with the
 * same instance of the content lambda.
 * 2) Updates the mapping between keys and indexes when we have a new factory
 * 3) Adds state restoration on top of the composable returned by [itemsProvider] with help of
 * [saveableStateHolder].
 */
internal class LazyLayoutItemContentFactory(
    private val saveableStateHolder: SaveableStateHolder,
    private val itemsProvider: () -> LazyLayoutItemsProvider,
) {
    /** Contains the cached lambdas produced by the [itemsProvider]. */
    private val lambdasCache = mutableMapOf<Any, CachedItemContent>()

    /** Density used to obtain the cached lambdas. */
    private var densityOfCachedLambdas = Density(0f, 0f)

    /** Constraints used to obtain the cached lambdas. */
    private var constraintsOfCachedLambdas = Constraints()

    /**
     * Invalidate the cached lambas if the density or constraints have changed.
     * TODO(popam): probably LazyLayoutState should provide an invalidate() method instead.
     */
    fun onBeforeMeasure(density: Density, constraints: Constraints) {
        if (density != densityOfCachedLambdas || constraints != constraintsOfCachedLambdas) {
            densityOfCachedLambdas = density
            constraintsOfCachedLambdas = constraints
            lambdasCache.clear()
        }
    }

    /**
     * Return cached item content lambda or creates a new lambda and puts it in the cache.
     */
    fun getContent(index: Int, key: Any): @Composable () -> Unit {
        val cachedContent = lambdasCache[key]
        return if (cachedContent != null && cachedContent.lastKnownIndex == index) {
            cachedContent.content
        } else {
            val newContent = CachedItemContent(index, key)
            lambdasCache[key] = newContent
            newContent.content
        }
    }

    private inner class CachedItemContent(
        initialIndex: Int,
        val key: Any
    ) {
        var lastKnownIndex by mutableStateOf(initialIndex)
            private set

        val content: @Composable () -> Unit = @Composable {
            val itemsProvider = itemsProvider()
            val index = itemsProvider.keyToIndexMap[key]?.also {
                lastKnownIndex = it
            } ?: lastKnownIndex
            if (index < itemsProvider.itemsCount) {
                val key = itemsProvider.getKey(index)
                if (key == this.key) {
                    val content = itemsProvider.getContent(index)
                    saveableStateHolder.SaveableStateProvider(key, content)
                }
            }
        }
    }
}
