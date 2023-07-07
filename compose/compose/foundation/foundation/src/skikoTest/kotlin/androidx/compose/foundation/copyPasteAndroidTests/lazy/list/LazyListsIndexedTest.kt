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

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.BasicText
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertLeftPositionInRootIsEqualTo
import androidx.compose.ui.test.assertTopPositionInRootIsEqualTo
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.runSkikoComposeUiTest
import androidx.compose.ui.unit.dp
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class)
class LazyListsIndexedTest {

    @Test
    fun lazyColumnShowsIndexedItems() = runSkikoComposeUiTest {
        val items = (1..4).map { it.toString() }

        setContent {
            LazyColumn(Modifier.height(200.dp)) {
                itemsIndexed(items) { index, item ->
                    Spacer(
                        Modifier.height(101.dp).fillParentMaxWidth()
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
    fun columnWithIndexesComposedWithCorrectIndexAndItem() = runSkikoComposeUiTest {
        val items = (0..1).map { it.toString() }

        setContent {
            LazyColumn(Modifier.height(200.dp)) {
                itemsIndexed(items) { index, item ->
                    BasicText(
                        "${index}x$item", Modifier.fillParentMaxWidth().requiredHeight(100.dp)
                    )
                }
            }
        }

        onNodeWithText("0x0")
            .assertTopPositionInRootIsEqualTo(0.dp)

        onNodeWithText("1x1")
            .assertTopPositionInRootIsEqualTo(100.dp)
    }

    @Test
    fun lazyRowShowsIndexedItems() = runSkikoComposeUiTest {
        val items = (1..4).map { it.toString() }

        setContent {
            LazyRow(Modifier.width(200.dp)) {
                itemsIndexed(items) { index, item ->
                    Spacer(
                        Modifier.width(101.dp).fillParentMaxHeight()
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
    fun rowWithIndexesComposedWithCorrectIndexAndItem() = runSkikoComposeUiTest {
        val items = (0..1).map { it.toString() }

        setContent {
            LazyRow(Modifier.width(200.dp)) {
                itemsIndexed(items) { index, item ->
                    BasicText(
                        "${index}x$item", Modifier.fillParentMaxHeight().requiredWidth(100.dp)
                    )
                }
            }
        }

        onNodeWithText("0x0")
            .assertLeftPositionInRootIsEqualTo(0.dp)

        onNodeWithText("1x1")
            .assertLeftPositionInRootIsEqualTo(100.dp)
    }
}
