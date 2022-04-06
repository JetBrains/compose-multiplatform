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

package androidx.compose.foundation.lazy.layout

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.layout.AlignmentLine
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.Constraints
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalFoundationApi::class)
@LargeTest
@RunWith(AndroidJUnit4::class)
class LazyLayoutTest {

    @get:Rule
    val rule = createComposeRule()

    @Test
    fun lazyListShowsCombinedItems() {
        val counter = mutableStateOf(0)
        var remeasureCount = 0
        val policy: LazyLayoutMeasureScope.(Constraints) -> MeasureResult = {
            remeasureCount++
            object : MeasureResult {
                override val alignmentLines: Map<AlignmentLine, Int> = emptyMap()
                override val height: Int = 10
                override val width: Int = 10
                override fun placeChildren() {}
            }
        }
        val itemsProvider = object : LazyLayoutItemsProvider {
            override fun getContent(index: Int): @Composable () -> Unit = {}
            override val itemsCount: Int = 0
            override fun getKey(index: Int) = Unit
            override val keyToIndexMap: Map<Any, Int> = emptyMap()
            override fun getContentType(index: Int): Any? = null
        }

        rule.setContent {
            counter.value // just to trigger recomposition
            LazyLayout(
                itemsProvider = itemsProvider,
                measurePolicy = policy,
                // this will return a new object everytime causing LazyLayout recomposition
                // without causing remeasure
                modifier = Modifier.composed { Modifier }
            )
        }

        rule.runOnIdle {
            assertThat(remeasureCount).isEqualTo(1)
            counter.value++
        }

        rule.runOnIdle {
            assertThat(remeasureCount).isEqualTo(1)
        }
    }

    @Test
    fun prefetchItem() {
        val constraints = Constraints.fixed(50, 50)
        var measureCount = 0
        @Suppress("NAME_SHADOWING")
        val modifier = Modifier.layout { measurable, constraints ->
            measureCount++
            val placeable = measurable.measure(constraints)
            layout(placeable.width, placeable.height) {
                placeable.place(0, 0)
            }
        }
        val itemsProvider = itemProvider({ 1 }) { index ->
            { Box(Modifier.fillMaxSize().testTag("$index").then(modifier)) }
        }
        var needToCompose by mutableStateOf(false)
        val prefetchState = LazyLayoutPrefetchState()
        rule.setContent {
            LazyLayout(itemsProvider, prefetchState = prefetchState) {
                val item = if (needToCompose) {
                    measure(0, constraints)[0]
                } else null
                layout(100, 100) {
                    item?.place(0, 0)
                }
            }
        }

        rule.runOnIdle {
            assertThat(measureCount).isEqualTo(0)

            prefetchState.schedulePrefetch(0, constraints)
        }

        rule.waitUntil { measureCount == 1 }

        rule.onNodeWithTag("0").assertIsNotDisplayed()

        rule.runOnIdle {
            assertThat(measureCount).isEqualTo(1)
            needToCompose = true
        }

        rule.onNodeWithTag("0").assertIsDisplayed()

        rule.runOnIdle {
            assertThat(measureCount).isEqualTo(1)
        }
    }

    @Test
    fun cancelPrefetchedItem() {
        var composed = false
        val itemsProvider = itemProvider({ 1 }) {
            {
                Box(Modifier.fillMaxSize())
                DisposableEffect(Unit) {
                    composed = true
                    onDispose {
                        composed = false
                    }
                }
            }
        }
        val prefetchState = LazyLayoutPrefetchState()
        rule.setContent {
            LazyLayout(itemsProvider, prefetchState = prefetchState) {
                layout(100, 100) {}
            }
        }

        val handle = rule.runOnIdle {
            prefetchState.schedulePrefetch(0, Constraints.fixed(50, 50))
        }

        rule.waitUntil { composed }

        rule.runOnIdle {
            handle.cancel()
            // this is currently failing because the node is left for reuse, but will work after
            // we merge aosp/2056467
            // assertThat(composed).isFalse()
        }
    }

    private fun itemProvider(
        itemsCount: () -> Int,
        content: (Int) -> @Composable () -> Unit
    ): LazyLayoutItemsProvider {
        return object : LazyLayoutItemsProvider {
            override fun getContent(index: Int): @Composable () -> Unit {
                return content(index)
            }

            override val itemsCount: Int
                get() = itemsCount()

            override fun getKey(index: Int) = index
            override val keyToIndexMap: Map<Any, Int> = emptyMap()
            override fun getContentType(index: Int): Any? = null
        }
    }
}
