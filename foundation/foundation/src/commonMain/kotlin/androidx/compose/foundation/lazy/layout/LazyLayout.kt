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
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.layout.SubcomposeLayoutState
import androidx.compose.ui.layout.SubcomposeSlotReusePolicy
import androidx.compose.ui.unit.Constraints

/**
 * A layout that only composes and lays out currently needed items. Can be used to build
 * efficient scrollable layouts.
 *
 * @param itemProvider provides all the needed info about the items which could be used to
 * compose and measure items as part of [measurePolicy].
 * @param modifier to apply on the layout
 * @param prefetchState allows to schedule items for prefetching
 * @param measurePolicy Measure policy which allows to only compose and measure needed items.
 */
@ExperimentalFoundationApi
@Composable
fun LazyLayout(
    itemProvider: LazyLayoutItemProvider,
    modifier: Modifier = Modifier,
    prefetchState: LazyLayoutPrefetchState? = null,
    measurePolicy: LazyLayoutMeasureScope.(Constraints) -> MeasureResult
) {
    val currentItemProvider = rememberUpdatedState(itemProvider)

    val saveableStateHolder = rememberSaveableStateHolder()
    val itemContentFactory = remember {
        LazyLayoutItemContentFactory(saveableStateHolder) { currentItemProvider.value }
    }
    val subcomposeLayoutState = remember {
        SubcomposeLayoutState(LazyLayoutItemReusePolicy(itemContentFactory))
    }
    prefetchState?.let {
        LazyLayoutPrefetcher(
            prefetchState,
            itemContentFactory,
            subcomposeLayoutState
        )
    }

    SubcomposeLayout(
        subcomposeLayoutState,
        modifier,
        remember(itemContentFactory, measurePolicy) {
            { constraints ->
                itemContentFactory.onBeforeMeasure(this, constraints)

                with(LazyLayoutMeasureScopeImpl(itemContentFactory, this)) {
                    measurePolicy(constraints)
                }
            }
        }
    )
}

@ExperimentalFoundationApi
private class LazyLayoutItemReusePolicy(
    private val factory: LazyLayoutItemContentFactory
) : SubcomposeSlotReusePolicy {
    private val countPerType = mutableMapOf<Any?, Int>()

    override fun getSlotsToRetain(slotIds: SubcomposeSlotReusePolicy.SlotIdsSet) {
        countPerType.clear()
        with(slotIds.iterator()) {
            while (hasNext()) {
                val slotId = next()
                val type = factory.getContentType(slotId)
                val currentCount = countPerType[type] ?: 0
                if (currentCount == MaxItemsToRetainForReuse) {
                    remove()
                } else {
                    countPerType[type] = currentCount + 1
                }
            }
        }
    }

    override fun areCompatible(slotId: Any?, reusableSlotId: Any?): Boolean =
        factory.getContentType(slotId) == factory.getContentType(reusableSlotId)
}

/**
 * We currently use the same number of items to reuse (recycle) items as RecyclerView does:
 * 5 (RecycledViewPool.DEFAULT_MAX_SCRAP) + 2 (Recycler.DEFAULT_CACHE_SIZE)
 */
private const val MaxItemsToRetainForReuse = 7

/**
 * Platform specific implementation of lazy layout items prefetching - precomposing next items in
 * advance during the scrolling.
 */
@ExperimentalFoundationApi
@Composable
internal expect fun LazyLayoutPrefetcher(
    prefetchState: LazyLayoutPrefetchState,
    itemContentFactory: LazyLayoutItemContentFactory,
    subcomposeLayoutState: SubcomposeLayoutState
)
