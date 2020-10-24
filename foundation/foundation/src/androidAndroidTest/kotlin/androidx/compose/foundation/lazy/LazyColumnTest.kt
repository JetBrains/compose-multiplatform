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
import androidx.compose.foundation.layout.preferredSize
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
import androidx.test.ext.junit.runners.AndroidJUnit4

@MediumTest
@OptIn(ExperimentalLazyDsl::class)
@RunWith(AndroidJUnit4::class)
class LazyColumnTest {
    private val LazyColumnTag = "LazyColumnTag"

    @get:Rule
    val rule = createComposeRule()

    @Test
    fun lazyColumnShowsItem() {
        val itemTestTag = "itemTestTag"

        rule.setContent {
            LazyColumn {
                item {
                    Spacer(
                        Modifier.preferredHeight(10.dp).fillParentMaxWidth().testTag(itemTestTag)
                    )
                }
            }
        }

        rule.onNodeWithTag(itemTestTag)
            .assertIsDisplayed()
    }

    @Test
    fun lazyColumnShowsItems() {
        val items = (1..4).map { it.toString() }

        rule.setContent {
            LazyColumn(Modifier.preferredHeight(200.dp)) {
                items(items) {
                    Spacer(Modifier.preferredHeight(101.dp).fillParentMaxWidth().testTag(it))
                }
            }
        }

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
    fun lazyColumnShowsIndexedItems() {
        val items = (1..4).map { it.toString() }

        rule.setContent {
            LazyColumn(Modifier.preferredHeight(200.dp)) {
                itemsIndexed(items) { index, item ->
                    Spacer(
                        Modifier.preferredHeight(101.dp).fillParentMaxWidth()
                            .testTag("$index-$item")
                    )
                }
            }
        }

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
    fun lazyColumnShowsCombinedItems() {
        val itemTestTag = "itemTestTag"
        val items = listOf(1, 2).map { it.toString() }
        val indexedItems = listOf(3, 4, 5)

        rule.setContent {
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

        rule.onNodeWithTag(itemTestTag)
            .assertIsDisplayed()

        rule.onNodeWithTag("1")
            .assertIsDisplayed()

        rule.onNodeWithTag("2")
            .assertIsDisplayed()

        rule.onNodeWithTag("0-3")
            .assertIsDisplayed()

        rule.onNodeWithTag("1-4")
            .assertIsDisplayed()

        rule.onNodeWithTag("2-5")
            .assertDoesNotExist()
    }

    @Test
    fun lazyColumnShowsItemsOnScroll() {
        val items = (1..4).map { it.toString() }

        rule.setContent {
            LazyColumn(Modifier.preferredHeight(200.dp).testTag(LazyColumnTag)) {
                items(items) {
                    Spacer(Modifier.preferredHeight(101.dp).fillParentMaxWidth().testTag(it))
                }
            }
        }

        rule.onNodeWithTag(LazyColumnTag)
            .scrollBy(y = 50.dp, density = rule.density)

        rule.onNodeWithTag("1")
            .assertIsDisplayed()

        rule.onNodeWithTag("2")
            .assertIsDisplayed()

        rule.onNodeWithTag("3")
            .assertIsDisplayed()

        rule.onNodeWithTag("4")
            .assertDoesNotExist()
    }

    @Test
    fun lazyColumnScrollHidesItem() {
        val items = (1..3).map { it.toString() }

        rule.setContent {
            LazyColumn(Modifier.preferredHeight(200.dp).testTag(LazyColumnTag)) {
                items(items) {
                    Spacer(Modifier.preferredHeight(101.dp).fillParentMaxWidth().testTag(it))
                }
            }
        }

        rule.onNodeWithTag(LazyColumnTag)
            .scrollBy(y = 102.dp, density = rule.density)

        rule.onNodeWithTag("1")
            .assertDoesNotExist()

        rule.onNodeWithTag("2")
            .assertIsDisplayed()

        rule.onNodeWithTag("3")
            .assertIsDisplayed()
    }

    @Test
    fun lazyColumnAllowEmptyListItems() {
        val itemTag = "itemTag"

        rule.setContent {
            LazyColumn {
                items(emptyList<Any>()) { }
                item {
                    Spacer(Modifier.preferredSize(10.dp).testTag(itemTag))
                }
            }
        }

        rule.onNodeWithTag(itemTag)
            .assertIsDisplayed()
    }

    @Test
    fun lazyColumnAllowsNullableItems() {
        val items = listOf("1", null, "3")
        val nullTestTag = "nullTestTag"

        rule.setContent {
            LazyColumn(Modifier.preferredHeight(200.dp)) {
                items(items) {
                    if (it != null) {
                        Spacer(Modifier.preferredHeight(101.dp).fillParentMaxWidth().testTag(it))
                    } else {
                        Spacer(
                            Modifier.preferredHeight(101.dp).fillParentMaxWidth()
                                .testTag(nullTestTag)
                        )
                    }
                }
            }
        }

        rule.onNodeWithTag("1")
            .assertIsDisplayed()

        rule.onNodeWithTag(nullTestTag)
            .assertIsDisplayed()

        rule.onNodeWithTag("3")
            .assertDoesNotExist()
    }
}