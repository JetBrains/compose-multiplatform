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

package androidx.compose.foundation.gesture.snapping

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.snapping.SnapLayoutInfoProvider
import androidx.compose.foundation.gestures.snapping.lazyListSnapLayoutInfoProvider
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.list.BaseLazyListTestWithOrientation
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.test.filters.LargeTest
import kotlin.math.round
import kotlin.math.sign
import kotlin.test.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@OptIn(ExperimentalFoundationApi::class)
@LargeTest
@RunWith(Parameterized::class)
class LazyListSnapLayoutInfoProviderTest(orientation: Orientation) :
    BaseLazyListTestWithOrientation(orientation) {

    @Test
    fun snapStepSize_sameSizeItems_shouldBeAverageItemSize() {
        var expectedItemSize = 0f
        var actualItemSize = 0f

        rule.setContent {
            val state = rememberLazyListState()
            val layoutInfoProvider = remember(state) { createLayoutInfo(state) }.also {
                actualItemSize = it.snapStepSize
            }
            expectedItemSize = with(LocalDensity.current) { FixedItemSize.toPx() }
            MainLayout(
                state = state,
                layoutInfo = layoutInfoProvider,
                items = 200,
                itemSizeProvider = { FixedItemSize }
            )
        }

        rule.runOnIdle {
            assertEquals(round(expectedItemSize), round(actualItemSize))
        }
    }

    @Test
    fun snapStepSize_differentSizeItems_shouldBeAverageItemSize() {
        var actualItemSize = 0f
        var expectedItemSize = 0f

        rule.setContent {
            val state = rememberLazyListState()
            val layoutInfoProvider = remember(state) { createLayoutInfo(state) }.also {
                actualItemSize = it.snapStepSize
            }
            expectedItemSize = state.layoutInfo.visibleItemsInfo.map { it.size }.average().toFloat()

            MainLayout(state, layoutInfoProvider, DynamicItemSizes.size, { DynamicItemSizes[it] })
        }

        rule.runOnIdle {
            assertEquals(round(expectedItemSize), round(actualItemSize))
        }
    }

    @Test
    fun snapStepSize_withSpacers_shouldBeAverageItemSize() {
        var snapStepSize = 0f
        var actualItemSize = 0f
        rule.setContent {
            val state = rememberLazyListState()
            val layoutInfoProvider = remember(state) { createLayoutInfo(state) }.also {
                snapStepSize = it.snapStepSize
            }

            actualItemSize =
                with(LocalDensity.current) { (FixedItemSize + FixedItemSize / 2).toPx() }
            MainLayout(
                state = state,
                layoutInfo = layoutInfoProvider,
                items = 200,
                itemSizeProvider = { FixedItemSize }) {
                Box(modifier = Modifier.size(FixedItemSize))
                Spacer(modifier = Modifier.size(FixedItemSize / 2))
            }
        }

        rule.runOnIdle {
            assertEquals(round(actualItemSize), round(snapStepSize))
        }
    }

    @Test
    fun snappingOffsetBounds_shouldBeDifferentSignedBounds() {
        var upperBound = 0f
        var lowerBound = 0f
        rule.setContent {
            val state = rememberLazyListState()
            val layoutInfoProvider = remember(state) { createLayoutInfo(state) }
            val bounds = layoutInfoProvider.calculateSnappingOffsetBounds()
            lowerBound = bounds.start
            upperBound = bounds.endInclusive
            MainLayout(
                state = state,
                layoutInfo = layoutInfoProvider,
                items = 200,
                itemSizeProvider = { FixedItemSize }
            )
        }

        rule.runOnIdle {
            assertEquals(sign(lowerBound), sign(-1f))
            assertEquals(sign(upperBound), sign(1f))
        }
    }

    @Test
    fun calculateApproachOffset_approachOffsetIsAlwaysZero() {
        var snapLayoutInfoProvider: SnapLayoutInfoProvider? = null
        rule.setContent {
            val state = rememberLazyListState()
            val layoutInfoProvider = remember(state) { createLayoutInfo(state) }.also {
                snapLayoutInfoProvider = it
            }
            LazyColumnOrRow(
                state = state,
                flingBehavior = rememberSnapFlingBehavior(layoutInfoProvider)
            ) {
                items(200) {
                    Box(modifier = Modifier.size(200.dp))
                }
            }
        }

        rule.runOnIdle {
            assertEquals(snapLayoutInfoProvider?.calculateApproachOffset(1000f), 0f)
            assertEquals(snapLayoutInfoProvider?.calculateApproachOffset(-1000f), 0f)
        }
    }

    @Composable
    private fun MainLayout(
        state: LazyListState,
        layoutInfo: SnapLayoutInfoProvider,
        items: Int,
        itemSizeProvider: (Int) -> Dp,
        listItem: @Composable (Int) -> Unit = { Box(Modifier.size(itemSizeProvider(it))) }
    ) {
        LazyColumnOrRow(
            state = state,
            flingBehavior = rememberSnapFlingBehavior(snapLayoutInfoProvider = layoutInfo)
        ) {
            items(items) { listItem(it) }
        }
    }

    private fun createLayoutInfo(state: LazyListState): SnapLayoutInfoProvider {
        return lazyListSnapLayoutInfoProvider(state)
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun params() = arrayOf(Orientation.Vertical, Orientation.Horizontal)

        val FixedItemSize = 200.dp
        val DynamicItemSizes = (200..500).map { it.dp }
    }
}