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
import androidx.compose.foundation.layout.preferredHeight
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
class LazyColumnTest {
    private val LazyColumnTag = "LazyColumnTag"

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun lazyColumnShowsItem() {
        val itemTestTag = "itemTestTag"

        composeTestRule.setContent {
            LazyColumn {
                item {
                    Spacer(
                        Modifier.preferredHeight(10.dp).fillParentMaxWidth().testTag(itemTestTag)
                    )
                }
            }
        }

        onNodeWithTag(itemTestTag)
            .assertIsDisplayed()
    }

    @Test
    fun lazyColumnShowsItems() {
        val items = (1..4).map { it.toString() }

        composeTestRule.setContent {
            LazyColumn(Modifier.preferredHeight(200.dp)) {
                items(items) {
                    Spacer(Modifier.preferredHeight(101.dp).fillParentMaxWidth().testTag(it))
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
    fun lazyColumnShowsIndexedItems() {
        val items = (1..4).map { it.toString() }

        composeTestRule.setContent {
            LazyColumn(Modifier.preferredHeight(200.dp)) {
                itemsIndexed(items) { index, item ->
                    Spacer(Modifier.preferredHeight(101.dp).fillParentMaxWidth()
                            .testTag("$index-$item"))
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
    fun lazyColumnShowsCombinedItems() {
        val itemTestTag = "itemTestTag"
        val items = listOf(1, 2).map { it.toString() }
        val indexedItems = listOf(3, 4, 5)

        composeTestRule.setContent {
            LazyColumn(Modifier.preferredHeight(200.dp)) {
                item {
                    Spacer(
                        Modifier.preferredHeight(40.dp).fillParentMaxWidth().testTag(itemTestTag)
                    )
                }
                items(items) {
                    Spacer(Modifier.preferredHeight(40.dp).fillParentMaxWidth().testTag(it))
                }
                itemsIndexed(indexedItems) { index, item ->
                    Spacer(
                        Modifier.preferredHeight(41.dp).fillParentMaxWidth()
                            .testTag("$index-$item")
                    )
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
    fun lazyColumnShowsItemsOnScroll() {
        val items = (1..4).map { it.toString() }

        composeTestRule.setContent {
            LazyColumn(Modifier.preferredHeight(200.dp).testTag(LazyColumnTag)) {
                items(items) {
                    Spacer(Modifier.preferredHeight(101.dp).fillParentMaxWidth().testTag(it))
                }
            }
        }

        onNodeWithTag(LazyColumnTag)
            .scrollBy(y = 50.dp, density = composeTestRule.density)

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
    fun lazyColumnScrollHidesItem() {
        val items = (1..3).map { it.toString() }

        composeTestRule.setContent {
            LazyColumn(Modifier.preferredHeight(200.dp).testTag(LazyColumnTag)) {
                items(items) {
                    Spacer(Modifier.preferredHeight(101.dp).fillParentMaxWidth().testTag(it))
                }
            }
        }

        onNodeWithTag(LazyColumnTag)
            .scrollBy(y = 102.dp, density = composeTestRule.density)

        onNodeWithTag("1")
            .assertDoesNotExist()

        onNodeWithTag("2")
            .assertIsDisplayed()

        onNodeWithTag("3")
            .assertIsDisplayed()
    }
}