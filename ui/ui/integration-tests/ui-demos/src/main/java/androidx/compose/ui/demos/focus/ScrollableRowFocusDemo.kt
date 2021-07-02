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

package androidx.compose.ui.demos.focus

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.FlingBehavior
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color.Companion.Black
import androidx.compose.ui.graphics.Color.Companion.Red
import androidx.compose.ui.graphics.Color.Companion.White
import androidx.compose.ui.layout.RelocationRequester
import androidx.compose.ui.layout.onRelocationRequest
import androidx.compose.ui.layout.relocationRequester
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.toSize
import kotlinx.coroutines.launch
import kotlin.math.abs

@Composable
fun ScrollableRowFocusDemo() {
    Column {
        Text("Use the dpad or arrow keys to move focus")
        Row(Modifier.horizontalScrollWithRelocation(rememberScrollState())) {
            repeat(20) {
                FocusableBox(it.toString())
            }
        }
    }
}

@Composable
private fun FocusableBox(text: String, modifier: Modifier = Modifier) {
    var color by remember { mutableStateOf(White) }
    Text(
        text = text,
        fontSize = 50.sp,
        textAlign = TextAlign.Center,
        modifier = modifier
            .size(100.dp)
            .border(2.dp, Black)
            .onFocusChanged { color = if (it.isFocused) Red else White }
            .background(color)
            .focusableWithRelocation()
    )
}

// This is a hel function that users will have to use until bringIntoView is added to
// Modifier.focusable()
@OptIn(ExperimentalComposeUiApi::class)
private fun Modifier.focusableWithRelocation() = composed {
    val relocationRequester = remember { RelocationRequester() }
    val coroutineScope = rememberCoroutineScope()
    Modifier
        .relocationRequester(relocationRequester)
        .onFocusChanged {
            if (it.isFocused) {
                coroutineScope.launch { relocationRequester.bringIntoView() }
            }
        }
        .focusable()
}

// This is a helper function that users will have to use since experimental "ui" API cannot be used
// inside Scrollable, which is ihe "foundation" package. After onRelocationRequest is added
// to Scrollable, users can use Modifier.horizontalScroll directly.
@OptIn(ExperimentalComposeUiApi::class)
private fun Modifier.horizontalScrollWithRelocation(
    state: ScrollState,
    enabled: Boolean = true,
    flingBehavior: FlingBehavior? = null,
    reverseScrolling: Boolean = false
): Modifier {
    return this
        .onRelocationRequest(
            onProvideDestination = { rect, layoutCoordinates ->
                val size = layoutCoordinates.size.toSize()
                rect.translate(relocationDistance(rect.left, rect.right, size.width), 0f)
            },
            onPerformRelocation = { source, destination ->
                val offset = destination.left - source.left
                state.animateScrollBy(if (reverseScrolling) -offset else offset)
            }
        )
        .horizontalScroll(state, enabled, flingBehavior, reverseScrolling)
}

// Calculate the offset needed to bring one of the edges into view. The leadingEdge is the side
// closest to the origin (For the x-axis this is 'left', for the y-axis this is 'top').
// The trailing edge is the other side (For the x-axis this is 'right', for the y-axis this is
// 'bottom').
private fun relocationDistance(leadingEdge: Float, trailingEdge: Float, parentSize: Float) = when {
    // If the item is already visible, no need to scroll.
    leadingEdge >= 0 && trailingEdge <= parentSize -> 0f

    // If the item is visible but larger than the parent, we don't scroll.
    leadingEdge < 0 && trailingEdge > parentSize -> 0f

    // Find the minimum scroll needed to make one of the edges coincide with the parent's edge.
    abs(leadingEdge) < abs(trailingEdge - parentSize) -> leadingEdge
    else -> trailingEdge - parentSize
}