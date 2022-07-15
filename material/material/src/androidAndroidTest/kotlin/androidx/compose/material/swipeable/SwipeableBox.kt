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

package androidx.compose.material.swipeable

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.SwipeableV2State
import androidx.compose.material.swipeAnchors
import androidx.compose.material.swipeableV2
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterialApi::class)
@Composable
internal fun SwipeableBox(
    swipeableState: SwipeableV2State<TestState>,
    orientation: Orientation = Orientation.Horizontal,
    possibleStates: Set<TestState> = setOf(
        TestState.A,
        TestState.B,
        TestState.C
    ),
    enabled: Boolean = true,
    reverseDirection: Boolean = false,
    calculateAnchor: (state: TestState, layoutSize: IntSize) -> Float? = { state, layoutSize ->
        val size = (
            if (orientation == Orientation.Horizontal) layoutSize.width else layoutSize.height
            ).toFloat()
        when (state) {
            TestState.A -> 0f
            TestState.B -> size / 2
            TestState.C -> size
        }
    }
) = Box(Modifier.fillMaxSize()) {
    Box(
        Modifier
            .requiredSize(swipeableSize)
            .testTag(swipeableTestTag)
            .swipeableV2(
                state = swipeableState,
                orientation = orientation,
                enabled = enabled,
                reverseDirection = reverseDirection
            )
            .swipeAnchors(
                state = swipeableState,
                possibleStates = possibleStates,
                calculateAnchor = calculateAnchor
            )
            .offset {
                val currentOffset = swipeableState.offset.value.roundToInt()
                val x = if (orientation == Orientation.Horizontal) currentOffset else 0
                val y = if (orientation == Orientation.Vertical) currentOffset else 0
                IntOffset(x, y)
            }
            .background(Color.Red)
    )
}

internal const val swipeableTestTag = "swipebox"
internal val swipeableSize = 200.dp

internal enum class TestState { A, B, C }