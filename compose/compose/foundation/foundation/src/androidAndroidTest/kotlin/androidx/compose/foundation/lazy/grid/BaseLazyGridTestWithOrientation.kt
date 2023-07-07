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

package androidx.compose.foundation.lazy.grid

import androidx.compose.animation.core.snap
import androidx.compose.foundation.AutoTestFrameClock
import androidx.compose.foundation.BaseLazyLayoutTestWithOrientation
import androidx.compose.foundation.gestures.FlingBehavior
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.ScrollableDefaults
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking

open class BaseLazyGridTestWithOrientation(
    orientation: Orientation
) : BaseLazyLayoutTestWithOrientation(orientation) {

    fun LazyGridState.scrollBy(offset: Dp) {
        runBlocking(Dispatchers.Main + AutoTestFrameClock()) {
            animateScrollBy(with(rule.density) { offset.roundToPx().toFloat() }, snap())
        }
    }

    fun LazyGridState.scrollTo(index: Int) {
        runBlocking(Dispatchers.Main + AutoTestFrameClock()) {
            scrollToItem(index)
        }
    }

    fun SemanticsNodeInteraction.scrollBy(offset: Dp) = scrollMainAxisBy(offset)

    @Composable
    fun LazyGrid(
        cells: Int,
        modifier: Modifier = Modifier,
        state: LazyGridState = rememberLazyGridState(),
        contentPadding: PaddingValues = PaddingValues(0.dp),
        reverseLayout: Boolean = false,
        reverseArrangement: Boolean = false,
        flingBehavior: FlingBehavior = ScrollableDefaults.flingBehavior(),
        userScrollEnabled: Boolean = true,
        crossAxisSpacedBy: Dp = 0.dp,
        mainAxisSpacedBy: Dp = 0.dp,
        content: LazyGridScope.() -> Unit
    ) = LazyGrid(
        GridCells.Fixed(cells),
        modifier,
        state,
        contentPadding,
        reverseLayout,
        reverseArrangement,
        flingBehavior,
        userScrollEnabled,
        crossAxisSpacedBy,
        mainAxisSpacedBy,
        content
    )

    @Composable
    fun LazyGrid(
        cells: GridCells,
        modifier: Modifier = Modifier,
        state: LazyGridState = rememberLazyGridState(),
        contentPadding: PaddingValues = PaddingValues(0.dp),
        reverseLayout: Boolean = false,
        reverseArrangement: Boolean = false,
        flingBehavior: FlingBehavior = ScrollableDefaults.flingBehavior(),
        userScrollEnabled: Boolean = true,
        crossAxisSpacedBy: Dp = 0.dp,
        mainAxisSpacedBy: Dp = 0.dp,
        content: LazyGridScope.() -> Unit
    ) {
        if (vertical) {
            val verticalArrangement = when {
                mainAxisSpacedBy != 0.dp -> Arrangement.spacedBy(mainAxisSpacedBy)
                reverseLayout xor reverseArrangement -> Arrangement.Bottom
                else -> Arrangement.Top
            }
            val horizontalArrangement = when {
                crossAxisSpacedBy != 0.dp -> Arrangement.spacedBy(crossAxisSpacedBy)
                else -> Arrangement.Start
            }
            LazyVerticalGrid(
                columns = cells,
                modifier = modifier,
                state = state,
                contentPadding = contentPadding,
                reverseLayout = reverseLayout,
                flingBehavior = flingBehavior,
                userScrollEnabled = userScrollEnabled,
                verticalArrangement = verticalArrangement,
                horizontalArrangement = horizontalArrangement,
                content = content
            )
        } else {
            val horizontalArrangement = when {
                mainAxisSpacedBy != 0.dp -> Arrangement.spacedBy(mainAxisSpacedBy)
                reverseLayout xor reverseArrangement -> Arrangement.End
                else -> Arrangement.Start
            }
            val verticalArrangement = when {
                crossAxisSpacedBy != 0.dp -> Arrangement.spacedBy(crossAxisSpacedBy)
                else -> Arrangement.Top
            }
            LazyHorizontalGrid(
                rows = cells,
                modifier = modifier,
                state = state,
                contentPadding = contentPadding,
                reverseLayout = reverseLayout,
                flingBehavior = flingBehavior,
                userScrollEnabled = userScrollEnabled,
                horizontalArrangement = horizontalArrangement,
                verticalArrangement = verticalArrangement,
                content = content
            )
        }
    }
}