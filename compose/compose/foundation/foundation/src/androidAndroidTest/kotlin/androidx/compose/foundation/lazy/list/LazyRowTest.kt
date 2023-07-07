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

package androidx.compose.foundation.lazy.list

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
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertPositionInRootIsEqualTo
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class LazyRowTest {
    private val LazyListTag = "LazyListTag"

    @get:Rule
    val rule = createComposeRule()

    private val firstItemTag = "firstItemTag"
    private val secondItemTag = "secondItemTag"

    private fun prepareLazyRowForAlignment(verticalGravity: Alignment.Vertical) {
        rule.setContentWithTestViewConfiguration {
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

        rule.onNodeWithTag(firstItemTag)
            .assertIsDisplayed()

        rule.onNodeWithTag(secondItemTag)
            .assertIsDisplayed()

        val lazyRowBounds = rule.onNodeWithTag(LazyListTag)
            .getUnclippedBoundsInRoot()

        with(rule.density) {
            // Verify the height of the row
            assertThat(lazyRowBounds.top.roundToPx()).isWithin1PixelFrom(0.dp.roundToPx())
            assertThat(lazyRowBounds.bottom.roundToPx()).isWithin1PixelFrom(100.dp.roundToPx())
        }
    }

    @Test
    fun lazyRowAlignmentCenterVertically() {
        prepareLazyRowForAlignment(Alignment.CenterVertically)

        rule.onNodeWithTag(firstItemTag)
            .assertPositionInRootIsEqualTo(0.dp, 25.dp)

        rule.onNodeWithTag(secondItemTag)
            .assertPositionInRootIsEqualTo(50.dp, 15.dp)
    }

    @Test
    fun lazyRowAlignmentTop() {
        prepareLazyRowForAlignment(Alignment.Top)

        rule.onNodeWithTag(firstItemTag)
            .assertPositionInRootIsEqualTo(0.dp, 0.dp)

        rule.onNodeWithTag(secondItemTag)
            .assertPositionInRootIsEqualTo(50.dp, 0.dp)
    }

    @Test
    fun lazyRowAlignmentBottom() {
        prepareLazyRowForAlignment(Alignment.Bottom)

        rule.onNodeWithTag(firstItemTag)
            .assertPositionInRootIsEqualTo(0.dp, 50.dp)

        rule.onNodeWithTag(secondItemTag)
            .assertPositionInRootIsEqualTo(50.dp, 30.dp)
    }

    @Test
    fun scrollsLeftInRtl() {
        lateinit var state: LazyListState
        rule.setContentWithTestViewConfiguration {
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

        rule.onNodeWithTag(LazyListTag)
            .scrollBy(x = (-150).dp, density = rule.density)

        rule.runOnIdle {
            assertThat(state.firstVisibleItemIndex).isEqualTo(1)
            assertThat(state.firstVisibleItemScrollOffset).isGreaterThan(0)
        }
    }

   @Test
   fun laysOutRtlCorrectlyWithLargerRow() {
       val rowWidth = with(rule.density) { 300.toDp() }
       val rowHeight = with(rule.density) { 100.toDp() }
       val itemSize = with(rule.density) { 50.toDp() }
       rule.setContent {
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

       rule.onNodeWithTag("0")
           .assertPositionInRootIsEqualTo(rowWidth - itemSize, 0.dp)
    }
}
