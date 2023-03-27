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

package androidx.compose.foundation.pager

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.height
import androidx.compose.ui.unit.width
import androidx.test.filters.LargeTest
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@OptIn(ExperimentalFoundationApi::class)
@LargeTest
@RunWith(Parameterized::class)
internal class PagerCrossAxisTest(val config: ParamConfig) : BasePagerTest(config) {

    @Test
    fun pagerOnInfiniteCrossAxisLayout_shouldWrapContentSize() {
        // Arrange
        rule.setContent {
            InfiniteAxisRootComposable {
                HorizontalOrVerticalPager(
                    pageCount = DefaultPageCount,
                    state = rememberPagerState(),
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth()
                        .testTag(PagerTestTag),
                ) {
                    val fillModifier = if (isVertical) {
                        Modifier
                            .fillMaxHeight()
                            .width(200.dp)
                    } else {
                        Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                    }
                    Box(fillModifier)
                }
            }
        }

        // Act
        val rootBounds = rule.onRoot().getUnclippedBoundsInRoot()

        // Assert: Max Cross Axis size is handled well by wrapping content
        if (isVertical) {
            rule.onNodeWithTag(PagerTestTag)
                .assertHeightIsEqualTo(rootBounds.height)
                .assertWidthIsEqualTo(200.dp)
        } else {
            rule.onNodeWithTag(PagerTestTag)
                .assertWidthIsEqualTo(rootBounds.width)
                .assertHeightIsEqualTo(200.dp)
        }
    }

    @Composable
    private fun InfiniteAxisRootComposable(content: @Composable () -> Unit) {
        if (isVertical) {
            Row(Modifier.horizontalScroll(rememberScrollState())) {
                content()
            }
        } else {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                content()
            }
        }
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun params() = mutableListOf<ParamConfig>().apply {
            for (orientation in TestOrientation) {
                add(ParamConfig(orientation = orientation))
            }
        }
    }
}