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
import androidx.test.ext.junit.runners.AndroidJUnit4

@MediumTest
@RunWith(AndroidJUnit4::class)
class LazyGridTest {
    private val LazyGridTag = "LazyGridTag"

    @get:Rule
    val rule = createComposeRule()

    @Test
    fun lazyGridShowsOneItem() {
        val itemTestTag = "itemTestTag"

        rule.setContent {
            LazyGrid(
                columns = 3
            ) {
                item {
                    Spacer(
                        Modifier.preferredSize(10.dp).testTag(itemTestTag)
                    )
                }
            }
        }

        rule.onNodeWithTag(itemTestTag)
            .assertIsDisplayed()
    }

    @Test
    fun lazyGridShowsOneRow() {
        val items = (1..5).map { it.toString() }

        rule.setContent {
            LazyGrid(
                columns = 3,
                modifier = Modifier.preferredHeight(100.dp).preferredWidth(300.dp)
            ) {
                items(items) {
                    Spacer(Modifier.preferredHeight(101.dp).testTag(it))
                }
            }
        }

        rule.onNodeWithTag("1")
            .assertIsDisplayed()

        rule.onNodeWithTag("2")
            .assertIsDisplayed()

        rule.onNodeWithTag("3")
            .assertIsDisplayed()

        rule.onNodeWithTag("4")
            .assertDoesNotExist()

        rule.onNodeWithTag("5")
            .assertDoesNotExist()
    }

    @Test
    fun lazyGridShowsSecondRowOnScroll() {
        val items = (1..9).map { it.toString() }

        rule.setContent {
            LazyGrid(
                columns = 3,
                modifier = Modifier.preferredHeight(100.dp).testTag(LazyGridTag)
            ) {
                items(items) {
                    Spacer(Modifier.preferredHeight(101.dp).testTag(it))
                }
            }
        }

        rule.onNodeWithTag(LazyGridTag)
            .scrollBy(y = 50.dp, density = rule.density)

        rule.onNodeWithTag("4")
            .assertIsDisplayed()

        rule.onNodeWithTag("5")
            .assertIsDisplayed()

        rule.onNodeWithTag("6")
            .assertIsDisplayed()

        rule.onNodeWithTag("7")
            .assertDoesNotExist()

        rule.onNodeWithTag("8")
            .assertDoesNotExist()

        rule.onNodeWithTag("9")
            .assertDoesNotExist()
    }

    @Test
    fun lazyGridScrollHidesFirstRow() {
        val items = (1..9).map { it.toString() }

        rule.setContent {
            LazyGrid(
                columns = 3,
                modifier = Modifier.preferredHeight(200.dp).testTag(LazyGridTag)
            ) {
                items(items) {
                    Spacer(Modifier.preferredHeight(101.dp).testTag(it))
                }
            }
        }

        rule.onNodeWithTag(LazyGridTag)
            .scrollBy(y = 102.dp, density = rule.density)

        rule.onNodeWithTag("1")
            .assertDoesNotExist()

        rule.onNodeWithTag("2")
            .assertDoesNotExist()

        rule.onNodeWithTag("3")
            .assertDoesNotExist()

        rule.onNodeWithTag("4")
            .assertIsDisplayed()

        rule.onNodeWithTag("5")
            .assertIsDisplayed()

        rule.onNodeWithTag("6")
            .assertIsDisplayed()

        rule.onNodeWithTag("7")
            .assertIsDisplayed()

        rule.onNodeWithTag("8")
            .assertIsDisplayed()

        rule.onNodeWithTag("9")
            .assertIsDisplayed()
    }
}
