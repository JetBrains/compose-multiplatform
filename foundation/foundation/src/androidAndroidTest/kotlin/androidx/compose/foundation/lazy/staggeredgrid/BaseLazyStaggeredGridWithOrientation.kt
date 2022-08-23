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
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import kotlin.math.roundToInt
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

    @Composable
    internal fun LazyStaggeredGrid(
        lanes: Int,
        modifier: Modifier = Modifier,
        state: LazyStaggeredGridState = rememberLazyStaggeredGridState(),
        content: LazyStaggeredGridScope.() -> Unit,
    ) {
        LazyStaggeredGrid(
            state = state,
            modifier = modifier,
            orientation = orientation,
            userScrollEnabled = true,
            verticalArrangement = Arrangement.Top,
            horizontalArrangement = Arrangement.Start,
            slotSizesSums = { constraints ->
                val crossAxisSize = if (orientation == Orientation.Vertical) {
                    constraints.maxWidth
                } else {
                    constraints.maxHeight
                }
                IntArray(lanes) {
                    (crossAxisSize / lanes.toDouble() * (it + 1)).roundToInt()
                }
            },
            content = content
        )
    }
}