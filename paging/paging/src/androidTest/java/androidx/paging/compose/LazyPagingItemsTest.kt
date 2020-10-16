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

package androidx.paging.compose

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.preferredHeight
import androidx.compose.foundation.layout.preferredWidth
import androidx.compose.foundation.lazy.ExperimentalLazyDsl
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingSource
import androidx.test.filters.MediumTest
import androidx.ui.test.assertIsDisplayed
import androidx.ui.test.createComposeRule
import androidx.ui.test.onNodeWithTag
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@MediumTest
@OptIn(ExperimentalLazyDsl::class)
@RunWith(JUnit4::class)
class LazyPagingItemsTest {
    @get:Rule
    val rule = createComposeRule()

    val items = (1..10).toList().map { "$it" }

    val pagingSource = object : PagingSource<Int, String>() {
        override suspend fun load(params: LoadParams<Int>): LoadResult<Int, String> {
            val pageNumber = params.key ?: 0
            val prevKey = if (pageNumber > 0) pageNumber - 1 else null
            val nextKey = if (pageNumber < items.size) pageNumber + 1 else null
            val subList = if (pageNumber < items.size) {
                items.subList(pageNumber, pageNumber + 1)
            } else {
                emptyList()
            }

            return LoadResult.Page(
                data = subList,
                prevKey = prevKey,
                nextKey = nextKey
            )
        }
    }

    val pager = Pager(
        PagingConfig(
            pageSize = 1,
            enablePlaceholders = true,
            maxSize = 200
        )
    ) {
        pagingSource
    }

    @Test
    fun lazyPagingColumnShowsItems() {
        rule.setContent {
            val lazyPagingItems = pager.flow.collectAsLazyPagingItems()
            LazyColumn(Modifier.preferredHeight(200.dp)) {
                items(lazyPagingItems) {
                    Spacer(
                        Modifier.preferredHeight(101.dp).fillParentMaxWidth()
                            .testTag("$it")
                    )
                }
            }
        }

        rule.waitForIdle()

        rule.onNodeWithTag("1")
            .assertIsDisplayed()

        rule.onNodeWithTag("2")
            .assertIsDisplayed()

        rule.onNodeWithTag("3")
            .assertDoesNotExist()

        rule.onNodeWithTag("4")
            .assertDoesNotExist()
    }

    @Test
    fun lazyPagingColumnShowsIndexedItems() {
        rule.setContent {
            val lazyPagingItems = pager.flow.collectAsLazyPagingItems()
            LazyColumn(Modifier.preferredHeight(200.dp)) {
                itemsIndexed(lazyPagingItems) { index, item ->
                    Spacer(
                        Modifier.preferredHeight(101.dp).fillParentMaxWidth()
                            .testTag("$index-$item")
                    )
                }
            }
        }

        rule.waitForIdle()

        rule.onNodeWithTag("0-1")
            .assertIsDisplayed()

        rule.onNodeWithTag("1-2")
            .assertIsDisplayed()

        rule.onNodeWithTag("2-3")
            .assertDoesNotExist()

        rule.onNodeWithTag("3-4")
            .assertDoesNotExist()
    }

    @Test
    fun lazyPagingRowShowsItems() {
        rule.setContent {
            val lazyPagingItems = pager.flow.collectAsLazyPagingItems()
            LazyRow(Modifier.preferredWidth(200.dp)) {
                items(lazyPagingItems) {
                    Spacer(
                        Modifier.preferredWidth(101.dp).fillParentMaxHeight()
                            .testTag("$it")
                    )
                }
            }
        }

        rule.waitForIdle()

        rule.onNodeWithTag("1")
            .assertIsDisplayed()

        rule.onNodeWithTag("2")
            .assertIsDisplayed()

        rule.onNodeWithTag("3")
            .assertDoesNotExist()

        rule.onNodeWithTag("4")
            .assertDoesNotExist()
    }

    @Test
    fun lazyPagingRowShowsIndexedItems() {
        rule.setContent {
            val lazyPagingItems = pager.flow.collectAsLazyPagingItems()
            LazyRow(Modifier.preferredWidth(200.dp)) {
                itemsIndexed(lazyPagingItems) { index, item ->
                    Spacer(
                        Modifier.preferredWidth(101.dp).fillParentMaxHeight()
                            .testTag("$index-$item")
                    )
                }
            }
        }

        rule.waitForIdle()

        rule.onNodeWithTag("0-1")
            .assertIsDisplayed()

        rule.onNodeWithTag("1-2")
            .assertIsDisplayed()

        rule.onNodeWithTag("2-3")
            .assertDoesNotExist()

        rule.onNodeWithTag("3-4")
            .assertDoesNotExist()
    }
}