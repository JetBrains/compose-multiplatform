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

package androidx.compose.foundation.copyPasteAndroidTests.lazy.list

import androidx.compose.foundation.assertThat
import androidx.compose.foundation.isGreaterThan
import androidx.compose.foundation.isEqualTo
import androidx.compose.foundation.isWithin1PixelFrom
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.ScrollWheel
import androidx.compose.ui.test.SkikoComposeUiTest
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertPositionInRootIsEqualTo
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.runSkikoComposeUiTest
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlin.test.Ignore
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class)
class LazyRowTest {
    private val LazyListTag = "LazyListTag"

    private val firstItemTag = "firstItemTag"
    private val secondItemTag = "secondItemTag"

    private fun SkikoComposeUiTest.prepareLazyRowForAlignment(verticalGravity: Alignment.Vertical) {
        setContent {
            LazyRow(
                Modifier.testTag(LazyListTag).requiredHeight(100.dp),
                verticalAlignment = verticalGravity
            ) {
                items(listOf(1, 2)) {
                    if (it == 1) {
                        Spacer(Modifier.size(50.dp).testTag(firstItemTag))
                    } else {
                        Spacer(Modifier.size(70.dp).testTag(secondItemTag))
                    }
                }
            }
        }

        onNodeWithTag(firstItemTag)
            .assertIsDisplayed()

        onNodeWithTag(secondItemTag)
            .assertIsDisplayed()

        val lazyRowBounds = onNodeWithTag(LazyListTag)
            .getUnclippedBoundsInRoot()

        with(density) {
            // Verify the height of the row
            assertThat(lazyRowBounds.top.roundToPx()).isWithin1PixelFrom(0.dp.roundToPx())
            assertThat(lazyRowBounds.bottom.roundToPx()).isWithin1PixelFrom(100.dp.roundToPx())
        }
    }

    @Test
    fun lazyRowAlignmentCenterVertically() = runSkikoComposeUiTest {
        prepareLazyRowForAlignment(Alignment.CenterVertically)

        onNodeWithTag(firstItemTag)
            .assertPositionInRootIsEqualTo(0.dp, 25.dp)

        onNodeWithTag(secondItemTag)
            .assertPositionInRootIsEqualTo(50.dp, 15.dp)
    }

    @Test
    fun lazyRowAlignmentTop() = runSkikoComposeUiTest {
        prepareLazyRowForAlignment(Alignment.Top)

        onNodeWithTag(firstItemTag)
            .assertPositionInRootIsEqualTo(0.dp, 0.dp)

        onNodeWithTag(secondItemTag)
            .assertPositionInRootIsEqualTo(50.dp, 0.dp)
    }

    @Test
    fun lazyRowAlignmentBottom() = runSkikoComposeUiTest {
        prepareLazyRowForAlignment(Alignment.Bottom)

        onNodeWithTag(firstItemTag)
            .assertPositionInRootIsEqualTo(0.dp, 50.dp)

        onNodeWithTag(secondItemTag)
            .assertPositionInRootIsEqualTo(50.dp, 30.dp)
    }

    @Test
    @Ignore // TODO: the test is failing
    fun scrollsLeftInRtl() = runSkikoComposeUiTest {
        lateinit var state: LazyListState
        setContent {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                Box(Modifier.width(100.dp)) {
                    state = rememberLazyListState()
                    LazyRow(Modifier.testTag(LazyListTag), state) {
                        items(4) {
                            Spacer(
                                Modifier.width(101.dp).fillParentMaxHeight().testTag("$it")
                            )
                        }
                    }
                }
            }
        }

        onNodeWithTag(LazyListTag).performMouseInput {
            scroll(-150.dp.toPx(), ScrollWheel.Horizontal)
        }

        runOnIdle {
            assertThat(state.firstVisibleItemIndex).isEqualTo(1)
            assertThat(state.firstVisibleItemScrollOffset).isGreaterThan(0)
        }
    }

    @Test
    fun laysOutRtlCorrectlyWithLargerRow() = runSkikoComposeUiTest {
        val rowWidth = with(density) { 300.toDp() }
        val rowHeight = with(density) { 100.toDp() }
        val itemSize = with(density) { 50.toDp() }
        setContent {
            Column {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    LazyRow(modifier = Modifier.size(width = rowWidth, height = rowHeight)) {
                        items(3) { index ->
                            val label = index.toString()
                            BasicText(label, Modifier.size(itemSize).testTag(label))
                        }
                    }
                }
            }
        }

        onNodeWithTag("0")
            .assertPositionInRootIsEqualTo(rowWidth - itemSize, 0.dp)
    }
}
