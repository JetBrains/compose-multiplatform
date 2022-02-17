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
import androidx.compose.runtime.Stable
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.layout.SubcomposeMeasureScope
import androidx.compose.ui.unit.Constraints

@ExperimentalFoundationApi
internal interface LazyLayoutMeasureScope : MeasureScope {
    /**
     * Subcompose and measure the item of lazy layout.
     */
    fun measure(index: Int, constraints: Constraints): Array<Placeable>
}

@ExperimentalFoundationApi
@Stable
internal class LazyLayoutMeasureScopeImpl internal constructor(
    private val itemContentFactory: LazyLayoutItemContentFactory,
    private val subcomposeMeasureScope: SubcomposeMeasureScope
) : LazyLayoutMeasureScope, MeasureScope by subcomposeMeasureScope {

    /**
     * A cache of the previously composed items. It allows us to support [get]
     * re-executions with the same index during the same measure pass.
     */
    private val placeablesCache = hashMapOf<Int, Array<Placeable>>()

    override fun measure(index: Int, constraints: Constraints): Array<Placeable> {
        val cachedPlaceable = placeablesCache[index]
        return if (cachedPlaceable != null) {
            cachedPlaceable
        } else {
            val key = itemContentFactory.itemsProvider().getKey(index)
            val itemContent = itemContentFactory.getContent(index, key)
            val measurables = subcomposeMeasureScope.subcompose(key, itemContent)
            Array(measurables.size) { i ->
                measurables[i].measure(constraints)
            }.also {
                placeablesCache[index] = it
            }
        }
    }
}