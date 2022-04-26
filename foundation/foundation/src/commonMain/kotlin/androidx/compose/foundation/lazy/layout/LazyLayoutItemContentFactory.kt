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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.SaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density

/**
 * This class:
 * 1) Caches the lambdas being produced by [itemProvider]. This allows us to perform less
 * recompositions as the compose runtime can skip the whole composition if we subcompose with the
 * same instance of the content lambda.
 * 2) Updates the mapping between keys and indexes when we have a new factory
 * 3) Adds state restoration on top of the composable returned by [itemProvider] with help of
 * [saveableStateHolder].
 */
@ExperimentalFoundationApi
internal class LazyLayoutItemContentFactory(
    private val saveableStateHolder: SaveableStateHolder,
    val itemProvider: () -> LazyLayoutItemProvider,
) {
    /** Contains the cached lambdas produced by the [itemProvider]. */
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
     * Returns the content type for the item with the given key. It is used to improve the item
     * compositions reusing efficiency.
     **/
    fun getContentType(key: Any?): Any? {
        val cachedContent = lambdasCache[key]
        return if (cachedContent != null) {
            cachedContent.type
        } else {
            val itemProvider = itemProvider()
            val index = itemProvider.keyToIndexMap[key]
            if (index != null) {
                itemProvider.getContentType(index)
            } else {
                null
            }
        }
    }

    /**
     * Return cached item content lambda or creates a new lambda and puts it in the cache.
     */
    fun getContent(index: Int, key: Any): @Composable () -> Unit {
        val cached = lambdasCache[key]
        val type = itemProvider().getContentType(index)
        return if (cached != null && cached.lastKnownIndex == index && cached.type == type) {
            cached.content
        } else {
            val newContent = CachedItemContent(index, key, type)
            lambdasCache[key] = newContent
            newContent.content
        }
    }

    private inner class CachedItemContent(
        initialIndex: Int,
        val key: Any,
        val type: Any?
    ) {
        var lastKnownIndex by mutableStateOf(initialIndex)
            private set

        private var _content: (@Composable () -> Unit)? = null
        val content: (@Composable () -> Unit)
            get() = _content ?: createContentLambda().also { _content = it }

        private fun createContentLambda() = @Composable {
            val itemProvider = itemProvider()
            val index = itemProvider.keyToIndexMap[key]?.also {
                lastKnownIndex = it
            } ?: lastKnownIndex
            if (index < itemProvider.itemCount) {
                val key = itemProvider.getKey(index)
                if (key == this.key) {
                    saveableStateHolder.SaveableStateProvider(key) {
                        itemProvider.Item(index)
                    }
                }
            }
            DisposableEffect(key) {
                onDispose {
                    // we clear the cached content lambda when disposed to not leak RecomposeScopes
                    _content = null
                }
            }
        }
    }
}
