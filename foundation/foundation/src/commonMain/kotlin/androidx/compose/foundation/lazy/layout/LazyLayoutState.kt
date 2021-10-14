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
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.layout.Remeasurement
import androidx.compose.ui.layout.RemeasurementModifier

/**
 * Creates a [LazyLayoutState] that is remembered across recompositions.
 */
@Composable
internal fun rememberLazyLayoutState(): LazyLayoutState {
    return remember { LazyLayoutState() }
}

/**
 * A state object that can be hoisted to interact and observe the state of the [LazyLayout].
 *
 * In most cases, this will be created via [rememberLazyLayoutState].
*/
@Stable
internal class LazyLayoutState internal constructor() {
    /**
     * Information about the layout of the lazy layout, calculated during the latest layout pass.
     */
    val layoutInfo: LazyLayoutInfo get() = layoutInfoState.value

    /** Backing state for [layoutInfo] */
    internal val layoutInfoState = mutableStateOf<LazyLayoutInfo>(EmptyLazyLayoutInfo)

    internal var layoutInfoNonObservable: LazyLayoutInfo = EmptyLazyLayoutInfo

    /**
     * Remeasures the lazy list now. This can be used, for example, in reaction to scrolling.
     */
    fun remeasure() = remeasurement?.forceRemeasure()

    /**
     * The [Remeasurement] object associated with our layout. It allows us to remeasure
     * synchronously during scroll.
     */
    private var remeasurement: Remeasurement? = null

    /**
     * The modifier which provides [remeasurement].
     */
    internal val remeasurementModifier = object : RemeasurementModifier {
        override fun onRemeasurementAvailable(remeasurement: Remeasurement) {
            this@LazyLayoutState.remeasurement = remeasurement
        }
    }

    /**
     * The items provider of the lazy layout.
     */
    internal var itemsProvider: () -> LazyLayoutItemsProvider = { NoItemsProvider }

    /**
     * Listener to be notified after measurement - the prefetcher.
     */
    internal var onPostMeasureListener: LazyLayoutOnPostMeasureListener? = null
}

internal interface LazyLayoutInfo {
    /** The items currently participating in the layout of the lazy layout. */
    val visibleItemsInfo: List<LazyLayoutItemInfo>
}

private object EmptyLazyLayoutInfo : LazyLayoutInfo {
    override val visibleItemsInfo = emptyList<LazyLayoutItemInfo>()
}

internal interface LazyLayoutOnPostMeasureListener {
    fun onPostMeasure(
        result: LazyLayoutMeasureResult,
        placeablesProvider: LazyLayoutPlaceablesProvider
    )
}

private object NoItemsProvider : LazyLayoutItemsProvider {
    override fun getContent(index: Int): () -> Unit = error("No items")

    override val itemsCount = 0

    override fun getKey(index: Int): Any = error("No items")

    override val keyToIndexMap: Map<Any, Int>
        get() = error("No items")
}