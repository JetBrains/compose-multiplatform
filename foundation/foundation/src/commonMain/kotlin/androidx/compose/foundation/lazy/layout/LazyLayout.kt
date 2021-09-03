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
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.layout.SubcomposeLayoutState

/**
 * A layout that only composes and lays out currently visible items. Can be used to build
 * efficient scrollable layouts.
 */
@Composable
internal fun LazyLayout(
    itemsProvider: () -> LazyLayoutItemsProvider,
    modifier: Modifier = Modifier,
    state: LazyLayoutState = rememberLazyLayoutState(),
    prefetchPolicy: LazyLayoutPrefetchPolicy? = null,
    measurePolicy: LazyMeasurePolicy
) {
    state.itemsProvider = itemsProvider
    val itemContentFactory = rememberItemContentFactory(state)
    val subcomposeLayoutState = remember { SubcomposeLayoutState(MaxItemsToRetainForReuse) }
    prefetchPolicy?.let {
        LazyLayoutPrefetcher(prefetchPolicy, state, itemContentFactory, subcomposeLayoutState)
    }

    SubcomposeLayout(
        subcomposeLayoutState,
        modifier.then(state.remeasurementModifier)
    ) { constraints ->
        itemContentFactory.onBeforeMeasure(this, constraints)

        val measurables = LazyMeasurablesProvider(
            state.itemsProvider(),
            itemContentFactory,
            this
        )
        val measureResult = with(measurePolicy) { measure(measurables, constraints) }

        state.onPostMeasureListener?.apply { onPostMeasure(measureResult) }
        state.layoutInfoState.value = measureResult
        state.layoutInfoNonObservable = measureResult

        measureResult
    }
}

private const val MaxItemsToRetainForReuse = 2

/**
 * Platform specific implementation of lazy layout items prefetching - precomposing next items in
 * advance during the scrolling.
 */
@Composable
internal expect fun LazyLayoutPrefetcher(
    prefetchPolicy: LazyLayoutPrefetchPolicy,
    state: LazyLayoutState,
    itemContentFactory: LazyLayoutItemContentFactory,
    subcomposeLayoutState: SubcomposeLayoutState
)
