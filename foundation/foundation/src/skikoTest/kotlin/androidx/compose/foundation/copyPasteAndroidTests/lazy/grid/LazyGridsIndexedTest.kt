/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.compose.foundation.copyPasteAndroidTests.lazy.grid

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.text.BasicText
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTopPositionInRootIsEqualTo
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.runSkikoComposeUiTest
import androidx.compose.ui.unit.dp
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class)
class LazyGridsIndexedTest {

    @Test
    fun lazyVerticalGridShowsIndexedItems() = runSkikoComposeUiTest {
        val items = (1..4).map { it.toString() }

        setContent {
            LazyVerticalGrid(GridCells.Fixed(1), Modifier.height(200.dp)) {
                itemsIndexed(items) { index, item ->
                    Spacer(
                        Modifier.height(101.dp).testTag("$index-$item")
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
    fun verticalGridWithIndexesComposedWithCorrectIndexAndItem() = runSkikoComposeUiTest {
        val items = (0..1).map { it.toString() }

        setContent {
            LazyVerticalGrid(GridCells.Fixed(1), Modifier.height(200.dp)) {
                itemsIndexed(items) { index, item ->
                    BasicText(
                        "${index}x$item", Modifier.requiredHeight(100.dp)
                    )
                }
            }
        }

        onNodeWithText("0x0")
            .assertTopPositionInRootIsEqualTo(0.dp)

        onNodeWithText("1x1")
            .assertTopPositionInRootIsEqualTo(100.dp)
    }
}
