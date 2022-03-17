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

package androidx.compose.foundation.lazy.grid

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertLeftPositionInRootIsEqualTo
import androidx.compose.ui.test.assertTopPositionInRootIsEqualTo
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class LazyGridSpanTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun spans() {
        val columns = 4
        val columnWidth = with(rule.density) { 5.toDp() }
        val itemHeight = with(rule.density) { 10.toDp() }
        rule.setContent {
            LazyVerticalGrid(
                columns = GridCells.Fixed(columns),
                modifier = Modifier.requiredSize(columnWidth * columns, itemHeight * 3)
            ) {
                items(
                    count = 6,
                    span = { index ->
                        when (index) {
                            0 -> {
                                Truth.assertThat(maxLineSpan).isEqualTo(4)
                                Truth.assertThat(maxCurrentLineSpan).isEqualTo(4)
                                GridItemSpan(3)
                            }
                            1 -> {
                                Truth.assertThat(maxLineSpan).isEqualTo(4)
                                Truth.assertThat(maxCurrentLineSpan).isEqualTo(1)
                                GridItemSpan(1)
                            }
                            2 -> {
                                Truth.assertThat(maxLineSpan).isEqualTo(4)
                                Truth.assertThat(maxCurrentLineSpan).isEqualTo(4)
                                GridItemSpan(1)
                            }
                            3 -> {
                                Truth.assertThat(maxLineSpan).isEqualTo(4)
                                Truth.assertThat(maxCurrentLineSpan).isEqualTo(3)
                                GridItemSpan(3)
                            }
                            4 -> {
                                Truth.assertThat(maxLineSpan).isEqualTo(4)
                                Truth.assertThat(maxCurrentLineSpan).isEqualTo(4)
                                GridItemSpan(1)
                            }
                            5 -> {
                                Truth.assertThat(maxLineSpan).isEqualTo(4)
                                Truth.assertThat(maxCurrentLineSpan).isEqualTo(3)
                                GridItemSpan(1)
                            }
                            else -> error("Out of index span queried")
                        }
                    },
                ) {
                    Box(Modifier.height(itemHeight).testTag("$it"))
                }
            }
        }

        rule.onNodeWithTag("0")
            .assertTopPositionInRootIsEqualTo(0.dp)
            .assertLeftPositionInRootIsEqualTo(0.dp)
        rule.onNodeWithTag("1")
            .assertTopPositionInRootIsEqualTo(0.dp)
            .assertLeftPositionInRootIsEqualTo(columnWidth * 3)
        rule.onNodeWithTag("2")
            .assertTopPositionInRootIsEqualTo(itemHeight)
            .assertLeftPositionInRootIsEqualTo(0.dp)
        rule.onNodeWithTag("3")
            .assertTopPositionInRootIsEqualTo(itemHeight)
            .assertLeftPositionInRootIsEqualTo(columnWidth)
        rule.onNodeWithTag("4")
            .assertTopPositionInRootIsEqualTo(itemHeight * 2)
            .assertLeftPositionInRootIsEqualTo(0.dp)
        rule.onNodeWithTag("5")
            .assertTopPositionInRootIsEqualTo(itemHeight * 2)
            .assertLeftPositionInRootIsEqualTo(columnWidth)
    }

    @Test
    fun spansWithHorizontalSpacing() {
        val columns = 4
        val columnWidth = with(rule.density) { 5.toDp() }
        val itemHeight = with(rule.density) { 10.toDp() }
        val spacing = with(rule.density) { 4.toDp() }
        rule.setContent {
            LazyVerticalGrid(
                columns = GridCells.Fixed(columns),
                modifier = Modifier.requiredSize(
                    columnWidth * columns + spacing * (columns - 1),
                    itemHeight
                ),
                horizontalArrangement = Arrangement.spacedBy(spacing)
            ) {
                items(
                    count = 2,
                    span = { index ->
                        when (index) {
                            0 -> GridItemSpan(1)
                            1 -> GridItemSpan(3)
                            else -> error("Out of index span queried")
                        }
                    }
                ) {
                    Box(Modifier.height(itemHeight).testTag("$it"))
                }
            }
        }

        rule.onNodeWithTag("0")
            .assertTopPositionInRootIsEqualTo(0.dp)
            .assertLeftPositionInRootIsEqualTo(0.dp)
            .assertWidthIsEqualTo(columnWidth)
        rule.onNodeWithTag("1")
            .assertTopPositionInRootIsEqualTo(0.dp)
            .assertLeftPositionInRootIsEqualTo(columnWidth + spacing)
            .assertWidthIsEqualTo(columnWidth * 3 + spacing * 2)
    }

    @Test
    fun spansMultipleBlocks() {
        val columns = 4
        val columnWidth = with(rule.density) { 5.toDp() }
        val itemHeight = with(rule.density) { 10.toDp() }
        rule.setContent {
            LazyVerticalGrid(
                columns = GridCells.Fixed(columns),
                modifier = Modifier.requiredSize(columnWidth * columns, itemHeight)
            ) {
                items(
                    count = 1,
                    span = { index ->
                        when (index) {
                            0 -> GridItemSpan(1)
                            else -> error("Out of index span queried")
                        }
                    }
                ) {
                    Box(Modifier.height(itemHeight).testTag("0"))
                }
                item(span = {
                    if (maxCurrentLineSpan != 3) error("Wrong maxSpan")
                    GridItemSpan(2)
                }) {
                    Box(Modifier.height(itemHeight).testTag("1"))
                }
                items(
                    count = 1,
                    span = { index ->
                        if (maxCurrentLineSpan != 1 || index != 0) {
                            error("Wrong span calculation parameters")
                        }
                        GridItemSpan(1)
                    }
                ) {
                    if (it != 0) error("Wrong index")
                    Box(Modifier.height(itemHeight).testTag("2"))
                }
            }
        }

        rule.onNodeWithTag("0")
            .assertTopPositionInRootIsEqualTo(0.dp)
            .assertLeftPositionInRootIsEqualTo(0.dp)
            .assertWidthIsEqualTo(columnWidth)
        rule.onNodeWithTag("1")
            .assertTopPositionInRootIsEqualTo(0.dp)
            .assertLeftPositionInRootIsEqualTo(columnWidth)
            .assertWidthIsEqualTo(columnWidth * 2)
        rule.onNodeWithTag("2")
            .assertTopPositionInRootIsEqualTo(0.dp)
            .assertLeftPositionInRootIsEqualTo(columnWidth * 3)
            .assertWidthIsEqualTo(columnWidth)
    }

    @Test
    fun spansLineBreak() {
        val columns = 4
        val columnWidth = with(rule.density) { 5.toDp() }
        val itemHeight = with(rule.density) { 10.toDp() }
        rule.setContent {
            LazyVerticalGrid(
                columns = GridCells.Fixed(columns),
                modifier = Modifier.requiredSize(columnWidth * columns, itemHeight * 3)
            ) {
                item(span = {
                    if (maxCurrentLineSpan != 4) error("Wrong maxSpan")
                    GridItemSpan(3)
                }) {
                    Box(Modifier.height(itemHeight).testTag("0"))
                }
                items(
                    count = 4,
                    span = { index ->
                        if (maxCurrentLineSpan != when (index) {
                                0 -> 1
                                1 -> 2
                                2 -> 1
                                3 -> 2
                                else -> error("Wrong index")
                            }
                        ) error("Wrong maxSpan")
                        GridItemSpan(listOf(2, 1, 2, 2)[index])
                    }
                ) {
                    Box(Modifier.height(itemHeight).testTag((it + 1).toString()))
                }
            }
        }

        rule.onNodeWithTag("0")
            .assertTopPositionInRootIsEqualTo(0.dp)
            .assertLeftPositionInRootIsEqualTo(0.dp)
            .assertWidthIsEqualTo(columnWidth * 3)
        rule.onNodeWithTag("1")
            .assertTopPositionInRootIsEqualTo(itemHeight)
            .assertLeftPositionInRootIsEqualTo(0.dp)
            .assertWidthIsEqualTo(columnWidth * 2)
        rule.onNodeWithTag("2")
            .assertTopPositionInRootIsEqualTo(itemHeight)
            .assertLeftPositionInRootIsEqualTo(columnWidth * 2)
            .assertWidthIsEqualTo(columnWidth)
        rule.onNodeWithTag("3")
            .assertTopPositionInRootIsEqualTo(itemHeight * 2)
            .assertLeftPositionInRootIsEqualTo(0.dp)
            .assertWidthIsEqualTo(columnWidth * 2)
        rule.onNodeWithTag("4")
            .assertTopPositionInRootIsEqualTo(itemHeight * 2)
            .assertLeftPositionInRootIsEqualTo(columnWidth * 2)
            .assertWidthIsEqualTo(columnWidth * 2)
    }

    @Test
    fun spansCalculationDoesntCrash() {
        // regression from b/222530458
        lateinit var state: LazyGridState
        rule.setContent {
            state = rememberLazyGridState()
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                state = state,
                modifier = Modifier.size(100.dp)
            ) {
                repeat(100) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Box(Modifier.fillMaxWidth().height(1.dp))
                    }
                    items(10) {
                        Box(Modifier.fillMaxWidth().height(1.dp))
                    }
                }
            }
        }

        rule.runOnIdle {
            runBlocking {
                state.scrollToItem(state.layoutInfo.totalItemsCount)
            }
        }
    }
}