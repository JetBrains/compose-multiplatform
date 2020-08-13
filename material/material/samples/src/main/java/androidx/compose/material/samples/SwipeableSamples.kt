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

package androidx.compose.material.samples

import androidx.annotation.Sampled
import androidx.compose.foundation.Box
import androidx.compose.foundation.ContentGravity
import androidx.compose.foundation.Text
import androidx.compose.foundation.layout.offsetPx
import androidx.compose.foundation.layout.preferredSize
import androidx.compose.foundation.layout.preferredWidth
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.FractionalThreshold
import androidx.compose.material.rememberSwipeableState
import androidx.compose.material.swipeable
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.gesture.scrollorientationlocking.Orientation
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.DensityAmbient
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Sampled
@Composable
@OptIn(ExperimentalMaterialApi::class)
fun SwipeableSample() {
    // Draw a slider-like composable consisting of a red square moving along a
    // black background, with three states: "A" (min), "B" (middle), and "C" (max).
    val width = 350.dp
    val squareSize = 50.dp

    val swipeableState = rememberSwipeableState("A")
    val sizePx = with(DensityAmbient.current) { (width - squareSize).toPx() }
    val anchors = mapOf(0f to "A", sizePx / 2 to "B", sizePx to "C")

    Box(
        modifier = Modifier
            .preferredWidth(width)
            .swipeable(
                state = swipeableState,
                anchors = anchors,
                thresholds = { _, _ -> FractionalThreshold(0.5f) },
                orientation = Orientation.Horizontal
            ),
        backgroundColor = Color.Black
    ) {
        Box(
            Modifier.offsetPx(x = swipeableState.offset).preferredSize(squareSize),
            backgroundColor = Color.Red,
            gravity = ContentGravity.Center
        ) {
            Text(swipeableState.value, color = Color.White, fontSize = 24.sp)
        }
    }
}