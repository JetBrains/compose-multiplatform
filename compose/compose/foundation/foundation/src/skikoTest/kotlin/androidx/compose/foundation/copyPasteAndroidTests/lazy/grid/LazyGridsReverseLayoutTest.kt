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

package androidx.compose.foundation.copyPasteAndroidTests.lazy.grid

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertLeftPositionInRootIsEqualTo
import androidx.compose.ui.test.assertTopPositionInRootIsEqualTo
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.runSkikoComposeUiTest
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.test.BeforeTest
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class)
class LazyGridsReverseLayoutTest {

    private var itemSize: Dp = Dp.Infinity

    private val density = Density(1f)
    
    @BeforeTest
    fun before() {
        with(density) {
            itemSize = 50.toDp()
        }
    }

    @Test
    fun verticalGrid_reverseLayout() = runSkikoComposeUiTest {
        setContent {
            LazyVerticalGrid(
                GridCells.Fixed(2),
                Modifier.width(itemSize * 2),
                reverseLayout = true
            ) {
                items(4) {
                    Box(Modifier.height(itemSize).testTag(it.toString()))
                }
            }
        }

        onNodeWithTag("2")
            .assertTopPositionInRootIsEqualTo(0.dp)
            .assertLeftPositionInRootIsEqualTo(0.dp)
        onNodeWithTag("3")
            .assertTopPositionInRootIsEqualTo(0.dp)
            .assertLeftPositionInRootIsEqualTo(itemSize)
        onNodeWithTag("0")
            .assertTopPositionInRootIsEqualTo(itemSize)
            .assertLeftPositionInRootIsEqualTo(0.dp)
        onNodeWithTag("1")
            .assertTopPositionInRootIsEqualTo(itemSize)
            .assertLeftPositionInRootIsEqualTo(itemSize)
    }

    @Test
    fun column_emitTwoElementsAsOneItem() = runSkikoComposeUiTest {
        setContent {
            LazyVerticalGrid(
                GridCells.Fixed(2),
                Modifier.width(itemSize * 2),
                reverseLayout = true
            ) {
                items(4) {
                    Box(Modifier.height(itemSize).testTag((it * 2).toString()))
                    Box(Modifier.height(itemSize).testTag((it * 2 + 1).toString()))
                }
            }
        }

        onNodeWithTag("4")
            .assertTopPositionInRootIsEqualTo(0.dp)
            .assertLeftPositionInRootIsEqualTo(0.dp)
        onNodeWithTag("5")
            .assertTopPositionInRootIsEqualTo(0.dp)
            .assertLeftPositionInRootIsEqualTo(0.dp)
        onNodeWithTag("6")
            .assertTopPositionInRootIsEqualTo(0.dp)
            .assertLeftPositionInRootIsEqualTo(itemSize)
        onNodeWithTag("7")
            .assertTopPositionInRootIsEqualTo(0.dp)
            .assertLeftPositionInRootIsEqualTo(itemSize)
        onNodeWithTag("0")
            .assertTopPositionInRootIsEqualTo(itemSize)
            .assertLeftPositionInRootIsEqualTo(0.dp)
        onNodeWithTag("1")
            .assertTopPositionInRootIsEqualTo(itemSize)
            .assertLeftPositionInRootIsEqualTo(0.dp)
        onNodeWithTag("2")
            .assertTopPositionInRootIsEqualTo(itemSize)
            .assertLeftPositionInRootIsEqualTo(itemSize)
        onNodeWithTag("3")
            .assertTopPositionInRootIsEqualTo(itemSize)
            .assertLeftPositionInRootIsEqualTo(itemSize)
    }

    @Test
    fun verticalGrid_whenParameterChanges() = runSkikoComposeUiTest {
        var reverse by mutableStateOf(true)
        setContent {
            LazyVerticalGrid(
                GridCells.Fixed(2),
                Modifier.width(itemSize * 2),
                reverseLayout = reverse
            ) {
                items(4) {
                    Box(Modifier.size(itemSize).testTag(it.toString()))
                }
            }
        }

        onNodeWithTag("2")
            .assertTopPositionInRootIsEqualTo(0.dp)
            .assertLeftPositionInRootIsEqualTo(0.dp)
        onNodeWithTag("3")
            .assertTopPositionInRootIsEqualTo(0.dp)
            .assertLeftPositionInRootIsEqualTo(itemSize)
        onNodeWithTag("0")
            .assertTopPositionInRootIsEqualTo(itemSize)
            .assertLeftPositionInRootIsEqualTo(0.dp)
        onNodeWithTag("1")
            .assertTopPositionInRootIsEqualTo(itemSize)
            .assertLeftPositionInRootIsEqualTo(itemSize)

        runOnIdle {
            reverse = false
        }

        onNodeWithTag("0")
            .assertTopPositionInRootIsEqualTo(0.dp)
            .assertLeftPositionInRootIsEqualTo(0.dp)
        onNodeWithTag("1")
            .assertTopPositionInRootIsEqualTo(0.dp)
            .assertLeftPositionInRootIsEqualTo(itemSize)
        onNodeWithTag("2")
            .assertTopPositionInRootIsEqualTo(itemSize)
            .assertLeftPositionInRootIsEqualTo(0.dp)
        onNodeWithTag("3")
            .assertTopPositionInRootIsEqualTo(itemSize)
            .assertLeftPositionInRootIsEqualTo(itemSize)
    }
}
