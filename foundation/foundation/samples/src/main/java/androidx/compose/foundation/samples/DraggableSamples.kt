/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.compose.foundation.samples

import androidx.annotation.Sampled
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.preferredSize
import androidx.compose.foundation.layout.preferredWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.gesture.scrollorientationlocking.Orientation
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

@Sampled
@Composable
fun DraggableSample() {
    // Draw a seekbar-like composable that has a black background
    // with a red square that moves along the 300.dp drag distance
    val max = 300.dp
    val min = 0.dp
    // this is the  state we will update while dragging
    val offsetPosition = remember { mutableStateOf(0f) }

    // seekbar itself
    Box(
        modifier = Modifier
            .preferredWidth(max)
            .draggable(orientation = Orientation.Horizontal) { delta ->
                val newValue = offsetPosition.value + delta
                offsetPosition.value = newValue.coerceIn(min.toPx(), max.toPx())
            }
            .background(Color.Black)
    ) {
        Box(
            Modifier.offset { IntOffset(offsetPosition.value.roundToInt(), 0) }
                .preferredSize(50.dp)
                .background(Color.Red)
        )
    }
}