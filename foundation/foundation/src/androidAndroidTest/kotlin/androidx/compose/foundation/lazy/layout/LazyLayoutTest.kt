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

import android.os.Parcelable
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
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.DpRect
import androidx.compose.ui.unit.dp
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
    fun recompositionWithTheSameInputDoesntCauseRemeasure() {
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
        val itemProvider = itemProvider({ 0 }) { }

        rule.setContent {
            counter.value // just to trigger recomposition
            LazyLayout(
                itemProvider = itemProvider,
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
    fun measureAndPlaceTwoItems() {
        val itemProvider = itemProvider({ 2 }) { index ->
            Box(Modifier.fillMaxSize().testTag("$index"))
        }
        rule.setContent {
            LazyLayout(itemProvider) {
                val item1 = measure(0, Constraints.fixed(50, 50))[0]
                val item2 = measure(1, Constraints.fixed(20, 20))[0]
                layout(100, 100) {
                    item1.place(0, 0)
                    item2.place(80, 80)
                }
            }
        }

        with(rule.density) {
            assertThat(rule.onNodeWithTag("0").getBoundsInRoot())
                .isEqualTo(DpRect(0.dp, 0.dp, 50.toDp(), 50.toDp()))
            assertThat(rule.onNodeWithTag("1").getBoundsInRoot())
                .isEqualTo(DpRect(80.toDp(), 80.toDp(), 100.toDp(), 100.toDp()))
        }
    }

    @Test
    fun measureAndPlaceMultipleLayoutsInOneItem() {
        val itemProvider = itemProvider({ 1 }) { index ->
            Box(Modifier.fillMaxSize().testTag("${index}x0"))
            Box(Modifier.fillMaxSize().testTag("${index}x1"))
        }

        rule.setContent {
            LazyLayout(itemProvider) {
                val items = measure(0, Constraints.fixed(50, 50))
                layout(100, 100) {
                    items[0].place(0, 0)
                    items[1].place(50, 50)
                }
            }
        }

        with(rule.density) {
            assertThat(rule.onNodeWithTag("0x0").getBoundsInRoot())
                .isEqualTo(DpRect(0.dp, 0.dp, 50.toDp(), 50.toDp()))
            assertThat(rule.onNodeWithTag("0x1").getBoundsInRoot())
                .isEqualTo(DpRect(50.toDp(), 50.toDp(), 100.toDp(), 100.toDp()))
        }
    }

    @Test
    fun updatingitemProvider() {
        var itemProvider by mutableStateOf(itemProvider({ 1 }) { index ->
            Box(Modifier.fillMaxSize().testTag("$index"))
        })

        rule.setContent {
            LazyLayout(itemProvider) {
                val constraints = Constraints.fixed(100, 100)
                val items = mutableListOf<Placeable>()
                repeat(itemProvider.itemCount) { index ->
                    items.addAll(measure(index, constraints))
                }
                layout(100, 100) {
                    items.forEach {
                        it.place(0, 0)
                    }
                }
            }
        }

        rule.onNodeWithTag("0").assertIsDisplayed()
        rule.onNodeWithTag("1").assertDoesNotExist()

        rule.runOnIdle {
            itemProvider = itemProvider({ 2 }) { index ->
                Box(Modifier.fillMaxSize().testTag("$index"))
            }
        }

        rule.onNodeWithTag("0").assertIsDisplayed()
        rule.onNodeWithTag("1").assertIsDisplayed()
    }

    @Test
    fun stateBaseditemProvider() {
        var itemCount by mutableStateOf(1)
        val itemProvider = itemProvider({ itemCount }) { index ->
            Box(Modifier.fillMaxSize().testTag("$index"))
        }

        rule.setContent {
            LazyLayout(itemProvider) {
                val constraints = Constraints.fixed(100, 100)
                val items = mutableListOf<Placeable>()
                repeat(itemProvider.itemCount) { index ->
                    items.addAll(measure(index, constraints))
                }
                layout(100, 100) {
                    items.forEach {
                        it.place(0, 0)
                    }
                }
            }
        }

        rule.onNodeWithTag("0").assertIsDisplayed()
        rule.onNodeWithTag("1").assertDoesNotExist()

        rule.runOnIdle {
            itemCount = 2
        }

        rule.onNodeWithTag("0").assertIsDisplayed()
        rule.onNodeWithTag("1").assertIsDisplayed()
    }

    @Test
    fun getDefaultLazyLayoutKeyIsFollowingClaimedRequirements() {
        assertThat(getDefaultLazyLayoutKey(0)).isEqualTo(getDefaultLazyLayoutKey(0))
        assertThat(getDefaultLazyLayoutKey(0)).isNotEqualTo(getDefaultLazyLayoutKey(1))
        assertThat(getDefaultLazyLayoutKey(0)).isNotEqualTo(0)
        assertThat(getDefaultLazyLayoutKey(0)).isInstanceOf(Parcelable::class.java)
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
        val itemProvider = itemProvider({ 1 }) { index ->
            Box(Modifier.fillMaxSize().testTag("$index").then(modifier))
        }
        var needToCompose by mutableStateOf(false)
        val prefetchState = LazyLayoutPrefetchState()
        rule.setContent {
            LazyLayout(itemProvider, prefetchState = prefetchState) {
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
        val itemProvider = itemProvider({ 1 }) {
            Box(Modifier.fillMaxSize())
            DisposableEffect(Unit) {
                composed = true
                onDispose {
                    composed = false
                }
            }
        }
        val prefetchState = LazyLayoutPrefetchState()
        rule.setContent {
            LazyLayout(itemProvider, prefetchState = prefetchState) {
                layout(100, 100) {}
            }
        }

        val handle = rule.runOnIdle {
            prefetchState.schedulePrefetch(0, Constraints.fixed(50, 50))
        }

        rule.waitUntil { composed }

        rule.runOnIdle {
            handle.cancel()
        }

        rule.runOnIdle {
            assertThat(composed).isFalse()
        }
    }

    @Test
    fun keptForReuseItemIsDisposedWhenCanceled() {
        val needChild = mutableStateOf(true)
        var composed = true
        val itemProvider = itemProvider({ 1 }) {
            DisposableEffect(Unit) {
                composed = true
                onDispose {
                    composed = false
                }
            }
        }

        rule.setContent {
            LazyLayout(itemProvider) { constraints ->
                if (needChild.value) {
                    measure(0, constraints)
                }
                layout(10, 10) {}
            }
        }

        rule.runOnIdle {
            assertThat(composed).isTrue()
            needChild.value = false
        }

        rule.runOnIdle {
            assertThat(composed).isFalse()
        }
    }

    @Test
    fun nodeIsReusedWithoutExtraRemeasure() {
        var indexToCompose by mutableStateOf<Int?>(0)
        var remeasuresCount = 0
        val modifier = Modifier.layout { measurable, constraints ->
            val placeable = measurable.measure(constraints)
            remeasuresCount++
            layout(placeable.width, placeable.height) {
                placeable.place(0, 0)
            }
        }.fillMaxSize()
        val itemProvider = itemProvider({ 2 }) {
            Box(modifier)
        }

        rule.setContent {
            LazyLayout(itemProvider) { constraints ->
                val node = if (indexToCompose != null) {
                    measure(indexToCompose!!, constraints).first()
                } else {
                    null
                }
                layout(10, 10) {
                    node?.place(0, 0)
                }
            }
        }

        rule.runOnIdle {
            assertThat(remeasuresCount).isEqualTo(1)
            // node will be kept for reuse
            indexToCompose = null
        }

        rule.runOnIdle {
            // node with index 0 should be now reused for index 1
            indexToCompose = 1
        }

        rule.runOnIdle {
            assertThat(remeasuresCount).isEqualTo(1)
        }
    }

    private fun itemProvider(
        itemCount: () -> Int,
        itemContent: @Composable (Int) -> Unit
    ): LazyLayoutItemProvider {
        return object : LazyLayoutItemProvider {
            @Composable
            override fun Item(index: Int) {
                itemContent(index)
            }

            override val itemCount: Int get() = itemCount()
        }
    }
}
