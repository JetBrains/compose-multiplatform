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

import androidx.compose.runtime.Stable
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.layout.SubcomposeMeasureScope
import androidx.compose.ui.unit.Constraints

/**
 * Defines the measure and layout behaviour of a [LazyLayout].
 */
@Stable
internal fun interface LazyMeasurePolicy {
    fun MeasureScope.measure(
        measurables: LazyMeasurablesProvider,
        constraints: Constraints
    ): LazyLayoutMeasureResult
}

/** A lazily evaluated "list" of [Measurable]s. */
@Stable
internal class LazyMeasurablesProvider internal constructor(
    private val itemsProvider: LazyLayoutItemsProvider,
    private val itemContentFactory: LazyLayoutItemContentFactory,
    private val subcomposeMeasureScope: SubcomposeMeasureScope
) {
    operator fun get(index: Int): List<Measurable> {
        val key = itemsProvider.getKey(index)
        val itemContent = itemContentFactory.getContent(index, key)
        return subcomposeMeasureScope.subcompose(key, itemContent)
    }
}