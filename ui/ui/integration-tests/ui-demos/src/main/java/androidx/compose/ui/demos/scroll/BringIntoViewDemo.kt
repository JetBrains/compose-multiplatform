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

package androidx.compose.ui.demos.scroll

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.FlingBehavior
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color.Companion.Black
import androidx.compose.ui.graphics.Color.Companion.Blue
import androidx.compose.ui.graphics.Color.Companion.Cyan
import androidx.compose.ui.graphics.Color.Companion.DarkGray
import androidx.compose.ui.graphics.Color.Companion.Gray
import androidx.compose.ui.graphics.Color.Companion.Green
import androidx.compose.ui.graphics.Color.Companion.LightGray
import androidx.compose.ui.graphics.Color.Companion.Magenta
import androidx.compose.ui.graphics.Color.Companion.Red
import androidx.compose.ui.graphics.Color.Companion.White
import androidx.compose.ui.graphics.Color.Companion.Yellow
import androidx.compose.ui.layout.RelocationRequester
import androidx.compose.ui.layout.onRelocationRequest
import androidx.compose.ui.layout.relocationRequester
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import kotlinx.coroutines.launch
import kotlin.math.abs

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun BringIntoViewDemo() {
    val greenRequester = remember { RelocationRequester() }
    val redRequester = remember { RelocationRequester() }
    val coroutineScope = rememberCoroutineScope()
    Column {
        Column(
            Modifier.requiredHeight(100.dp).verticalScrollWithRelocation(rememberScrollState())
        ) {
            Row(Modifier.width(300.dp).horizontalScrollWithRelocation(rememberScrollState())) {
                Box(Modifier.background(Blue).size(100.dp))
                Box(Modifier.background(Green).size(100.dp).relocationRequester(greenRequester))
                Box(Modifier.background(Yellow).size(100.dp))
                Box(Modifier.background(Magenta).size(100.dp))
                Box(Modifier.background(Gray).size(100.dp))
                Box(Modifier.background(Black).size(100.dp))
            }
            Row(Modifier.width(300.dp).horizontalScrollWithRelocation(rememberScrollState())) {
                Box(Modifier.background(Black).size(100.dp))
                Box(Modifier.background(Cyan).size(100.dp))
                Box(Modifier.background(DarkGray).size(100.dp))
                Box(Modifier.background(White).size(100.dp))
                Box(Modifier.background(Red).size(100.dp).relocationRequester(redRequester))
                Box(Modifier.background(LightGray).size(100.dp))
            }
        }
        Button(onClick = { coroutineScope.launch { greenRequester.bringIntoView() } }) {
            Text("Bring Green box into view")
        }
        Button(onClick = { coroutineScope.launch { redRequester.bringIntoView() } }) {
            Text("Bring Red box into view")
        }
    }
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

// This is a helper function that users will have to use since experimental "ui" API cannot be used
// inside Scrollable, which is ihe "foundation" package. After onRelocationRequest is added
// to Scrollable, users can use Modifier.verticalScroll directly.
@OptIn(ExperimentalComposeUiApi::class)
private fun Modifier.verticalScrollWithRelocation(
    state: ScrollState,
    enabled: Boolean = true,
    flingBehavior: FlingBehavior? = null,
    reverseScrolling: Boolean = false
): Modifier {
    return this
        .onRelocationRequest(
            onProvideDestination = { rect, layoutCoordinates ->
                val size = layoutCoordinates.size.toSize()
                rect.translate(0f, relocationDistance(rect.top, rect.bottom, size.height))
            },
            onPerformRelocation = { source, destination ->
                val offset = destination.top - source.top
                state.animateScrollBy(if (reverseScrolling) -offset else offset)
            }
        )
        .verticalScroll(state, enabled, flingBehavior, reverseScrolling)
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
