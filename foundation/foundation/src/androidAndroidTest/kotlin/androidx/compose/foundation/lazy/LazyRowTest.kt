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

package androidx.compose.foundation.lazy

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.preferredWidth
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
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
class LazyRowTest {
    private val LazyRowTag = "LazyRowTag"

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun lazyRowShowsItem() {
        val itemTestTag = "itemTestTag"

        composeTestRule.setContent {
            LazyRow {
                item {
                    Spacer(
                        Modifier.preferredWidth(10.dp).fillParentMaxHeight().testTag(itemTestTag)
                    )
                }
            }
        }

        onNodeWithTag(itemTestTag)
            .assertIsDisplayed()
    }

    @Test
    fun lazyRowShowsItems() {
        val items = (1..4).map { it.toString() }

        composeTestRule.setContent {
            LazyRow(Modifier.preferredWidth(200.dp)) {
                items(items) {
                    Spacer(Modifier.preferredWidth(101.dp).fillParentMaxHeight().testTag(it))
                }
            }
        }

        onNodeWithTag("1")
            .assertIsDisplayed()

        onNodeWithTag("2")
            .assertIsDisplayed()

        onNodeWithTag("3")
            .assertDoesNotExist()

        onNodeWithTag("4")
            .assertDoesNotExist()
    }

    @Test
    fun lazyRowShowsIndexedItems() {
        val items = (1..4).map { it.toString() }

        composeTestRule.setContent {
            LazyRow(Modifier.preferredWidth(200.dp)) {
                itemsIndexed(items) { index, item ->
                    Spacer(
                        Modifier.preferredWidth(101.dp).fillParentMaxHeight()
                            .testTag("$index-$item")
                    )
                }
            }
        }

        onNodeWithTag("0-1")
            .assertIsDisplayed()

        onNodeWithTag("1-2")
            .assertIsDisplayed()

        onNodeWithTag("2-3")
            .assertDoesNotExist()

        onNodeWithTag("3-4")
            .assertDoesNotExist()
    }

    @Test
    fun lazyRowShowsCombinedItems() {
        val itemTestTag = "itemTestTag"
        val items = listOf(1, 2).map { it.toString() }
        val indexedItems = listOf(3, 4, 5)

        composeTestRule.setContent {
            LazyRow(Modifier.preferredWidth(200.dp)) {
                item {
                    Spacer(
                        Modifier.preferredWidth(40.dp).fillParentMaxHeight().testTag(itemTestTag)
                    )
                }
                items(items) {
                    Spacer(Modifier.preferredWidth(40.dp).fillParentMaxHeight().testTag(it))
                }
                itemsIndexed(indexedItems) { index, item ->
                    Spacer(Modifier.preferredWidth(41.dp).fillParentMaxHeight()
                        .testTag("$index-$item"))
                }
            }
        }

        onNodeWithTag(itemTestTag)
            .assertIsDisplayed()

        onNodeWithTag("1")
            .assertIsDisplayed()

        onNodeWithTag("2")
            .assertIsDisplayed()

        onNodeWithTag("0-3")
            .assertIsDisplayed()

        onNodeWithTag("1-4")
            .assertIsDisplayed()

        onNodeWithTag("2-5")
            .assertDoesNotExist()
    }

    @Test
    fun lazyRowShowsItemsOnScroll() {
        val items = (1..4).map { it.toString() }

        composeTestRule.setContent {
            LazyRow(Modifier.preferredWidth(200.dp).testTag(LazyRowTag)) {
                items(items) {
                    Spacer(Modifier.preferredWidth(101.dp).fillParentMaxHeight().testTag(it))
                }
            }
        }

        onNodeWithTag(LazyRowTag)
            .scrollBy(x = 50.dp, density = composeTestRule.density)

        onNodeWithTag("1")
            .assertIsDisplayed()

        onNodeWithTag("2")
            .assertIsDisplayed()

        onNodeWithTag("3")
            .assertIsDisplayed()

        onNodeWithTag("4")
            .assertDoesNotExist()
    }

    @Test
    fun lazyRowScrollHidesItem() {
        val items = (1..4).map { it.toString() }

        composeTestRule.setContent {
            LazyRow(Modifier.preferredWidth(200.dp).testTag(LazyRowTag)) {
                items(items) {
                    Spacer(Modifier.preferredWidth(101.dp).fillParentMaxHeight().testTag(it))
                }
            }
        }

        onNodeWithTag(LazyRowTag)
            .scrollBy(x = 102.dp, density = composeTestRule.density)

        onNodeWithTag("1")
            .assertDoesNotExist()

        onNodeWithTag("2")
            .assertIsDisplayed()

        onNodeWithTag("3")
            .assertIsDisplayed()
    }
}