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
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
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

    internal fun Modifier.debugBorder(color: Color = Color.Black) = border(1.dp, color)

    @Composable
    internal fun LazyStaggeredGrid(
        lanes: Int,
        modifier: Modifier = Modifier,
        state: LazyStaggeredGridState = rememberLazyStaggeredGridState(),
        contentPadding: PaddingValues = PaddingValues(0.dp),
        mainAxisArrangement: Arrangement.HorizontalOrVertical = Arrangement.spacedBy(0.dp),
        crossAxisArrangement: Arrangement.HorizontalOrVertical = Arrangement.spacedBy(0.dp),
        content: LazyStaggeredGridScope.() -> Unit,
    ) {
        if (orientation == Orientation.Vertical) {
            LazyVerticalStaggeredGrid(
                columns = StaggeredGridCells.Fixed(lanes),
                modifier = modifier,
                contentPadding = contentPadding,
                verticalArrangement = mainAxisArrangement,
                horizontalArrangement = crossAxisArrangement,
                state = state,
                content = content
            )
        } else {
            LazyHorizontalStaggeredGrid(
                rows = StaggeredGridCells.Fixed(lanes),
                modifier = modifier,
                contentPadding = contentPadding,
                verticalArrangement = crossAxisArrangement,
                horizontalArrangement = mainAxisArrangement,
                state = state,
                content = content
            )
        }
    }
}