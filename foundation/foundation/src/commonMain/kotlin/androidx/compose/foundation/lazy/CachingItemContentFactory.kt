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

import androidx.compose.foundation.layout.preferredHeight
import androidx.compose.foundation.layout.preferredSize
import androidx.compose.foundation.layout.preferredWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Caches the lambdas being produced by [itemContentFactory]. This allows us to perform less
 * recompositions as the compose runtime can skip the whole composition if we subcompose with the
 * same instance of the content lambda.
 */
internal class CachingItemContentFactory(
    itemContentFactory: LazyItemScope.(Int) -> @Composable () -> Unit
) : (Int) -> @Composable () -> Unit {

    /**
     * The cached instance of the scope to be used for composing items.
     */
    private var itemScope = InitialLazyItemsScopeImpl

    /**
     * Contains the cached lambdas produced by the [itemContentFactory].
     */
    private val lambdasCache = mutableMapOf<Int, @Composable () -> Unit>()

    /**
     * Current factory for creating an item content lambdas.
     */
    var itemContentFactory: LazyItemScope.(Int) -> @Composable () -> Unit = itemContentFactory
        set(value) {
            if (field !== value) {
                lambdasCache.clear()
                field = value
            }
        }

    /**
     * Updates the [itemScope] with the last [constraints] we got from the parent.
     */
    internal fun updateItemScope(density: Density, constraints: Constraints) = with(density) {
        val width = constraints.maxWidth.toDp()
        val height = constraints.maxHeight.toDp()
        if (width != itemScope.maxWidth || height != itemScope.maxHeight) {
            itemScope = LazyItemScopeImpl(width, height)
            lambdasCache.clear()
        }
    }

    /**
     * Return cached item content lambda or creates a new lambda and puts it in the cache.
     */
    override fun invoke(index: Int) = lambdasCache.getOrPut(index) {
        itemScope.itemContentFactory(index)
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
    override fun Modifier.fillParentMaxSize(fraction: Float) = preferredSize(
        maxWidth * fraction,
        maxHeight * fraction
    )

    override fun Modifier.fillParentMaxWidth(fraction: Float) =
        preferredWidth(maxWidth * fraction)

    override fun Modifier.fillParentMaxHeight(fraction: Float) =
        preferredHeight(maxHeight * fraction)
}
