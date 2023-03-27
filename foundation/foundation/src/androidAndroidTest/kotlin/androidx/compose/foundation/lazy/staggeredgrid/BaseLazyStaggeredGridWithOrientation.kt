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

package androidx.compose.foundation.lazy.staggeredgrid

import androidx.compose.animation.core.snap
import androidx.compose.foundation.AutoTestFrameClock
import androidx.compose.foundation.BaseLazyLayoutTestWithOrientation
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking

@OptIn(ExperimentalFoundationApi::class)
open class BaseLazyStaggeredGridWithOrientation(
    private val orientation: Orientation
) : BaseLazyLayoutTestWithOrientation(orientation) {

    internal fun LazyStaggeredGridState.scrollBy(offset: Dp) {
        runBlocking(Dispatchers.Main + AutoTestFrameClock()) {
            animateScrollBy(with(rule.density) { offset.roundToPx().toFloat() }, snap())
        }
    }

    internal fun LazyStaggeredGridState.scrollTo(index: Int) {
        runBlocking(Dispatchers.Main + AutoTestFrameClock()) {
            scrollToItem(index)
        }
    }

    @Composable
    internal fun LazyStaggeredGrid(
        lanes: Int,
        modifier: Modifier = Modifier,
        state: LazyStaggeredGridState = rememberLazyStaggeredGridState(),
        contentPadding: PaddingValues = PaddingValues(0.dp),
        reverseLayout: Boolean = false,
        mainAxisSpacing: Dp = 0.dp,
        crossAxisArrangement: Arrangement.HorizontalOrVertical = Arrangement.spacedBy(0.dp),
        content: LazyStaggeredGridScope.() -> Unit,
    ) {
        LazyStaggeredGrid(
            StaggeredGridCells.Fixed(lanes),
            modifier,
            state,
            contentPadding,
            mainAxisSpacing,
            crossAxisArrangement,
            reverseLayout,
            content
        )
    }

    internal fun axisSize(crossAxis: Int, mainAxis: Int): IntSize =
        IntSize(
            if (orientation == Orientation.Vertical) crossAxis else mainAxis,
            if (orientation == Orientation.Vertical) mainAxis else crossAxis,
        )

    internal fun axisOffset(crossAxis: Int, mainAxis: Int): IntOffset =
        IntOffset(
            if (orientation == Orientation.Vertical) crossAxis else mainAxis,
            if (orientation == Orientation.Vertical) mainAxis else crossAxis,
        )

    @Composable
    internal fun LazyStaggeredGrid(
        cells: StaggeredGridCells,
        modifier: Modifier = Modifier,
        state: LazyStaggeredGridState = rememberLazyStaggeredGridState(),
        contentPadding: PaddingValues = PaddingValues(0.dp),
        mainAxisSpacing: Dp = 0.dp,
        crossAxisArrangement: Arrangement.HorizontalOrVertical = Arrangement.spacedBy(0.dp),
        reverseLayout: Boolean = false,
        content: LazyStaggeredGridScope.() -> Unit,
    ) {
        if (orientation == Orientation.Vertical) {
            LazyVerticalStaggeredGrid(
                columns = cells,
                modifier = modifier,
                contentPadding = contentPadding,
                verticalItemSpacing = mainAxisSpacing,
                horizontalArrangement = crossAxisArrangement,
                state = state,
                reverseLayout = reverseLayout,
                content = content
            )
        } else {
            LazyHorizontalStaggeredGrid(
                rows = cells,
                modifier = modifier,
                contentPadding = contentPadding,
                verticalArrangement = crossAxisArrangement,
                horizontalItemSpacing = mainAxisSpacing,
                state = state,
                reverseLayout = reverseLayout,
                content = content
            )
        }
    }
}