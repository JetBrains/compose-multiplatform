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

package androidx.compose.foundation.lazy.grid

import androidx.compose.foundation.lazy.layout.LazyLayoutPlaceable
import androidx.compose.foundation.lazy.layout.LazyLayoutPlaceablesProvider
import androidx.compose.ui.unit.Constraints

/**
 * Abstracts away the subcomposition from the measuring logic.
 */
internal class LazyMeasuredItemProvider(
    private val itemsProvider: LazyGridItemsProvider,
    private val placeablesProvider: LazyLayoutPlaceablesProvider,
    private val defaultMainAxisSpacing: Int,
    private val measuredItemFactory: MeasuredItemFactory
) {
    /**
     * Used to subcompose individual items of lazy grids. Composed placeables will be measured
     * with the provided [constraints] and wrapped into [LazyMeasuredItem].
     */
    fun getAndMeasure(
        index: ItemIndex,
        mainAxisSpacing: Int = defaultMainAxisSpacing,
        constraints: Constraints
    ): LazyMeasuredItem {
        val key = itemsProvider.getKey(index.value)
        val placeables = placeablesProvider.getAndMeasure(index.value, constraints)
        val crossAxisSize = if (constraints.hasFixedWidth) {
            constraints.minWidth
        } else {
            require(constraints.hasFixedHeight)
            constraints.minHeight
        }
        return measuredItemFactory.createItem(
            index,
            key,
            crossAxisSize,
            mainAxisSpacing,
            placeables
        )
    }

    /**
     * Contains the mapping between the key and the index. It could contain not all the items of
     * the list as an optimization.
     **/
    val keyToIndexMap: Map<Any, Int> get() = itemsProvider.keyToIndexMap
}

// This interface allows to avoid autoboxing on index param
internal fun interface MeasuredItemFactory {
    fun createItem(
        index: ItemIndex,
        key: Any,
        crossAxisSize: Int,
        mainAxisSpacing: Int,
        placeables: Array<LazyLayoutPlaceable>
    ): LazyMeasuredItem
}
