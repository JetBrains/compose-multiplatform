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

package androidx.compose.ui.samples

import androidx.annotation.Sampled
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.gestures.FlingBehavior
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.RelocationRequester
import androidx.compose.ui.layout.onRelocationRequest
import androidx.compose.ui.layout.relocationRequester
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import kotlinx.coroutines.launch
import kotlin.math.abs

@ExperimentalComposeUiApi
@Sampled
@Composable
fun BringIntoViewSample() {
    // This is a helper function that users will have to use since experimental "ui" API cannot
    // be used inside Scrollable, which is ihe "foundation" package. After onRelocationRequest is
    // added to Scrollable, users can use Modifier.horizontalScroll directly.
    @OptIn(ExperimentalComposeUiApi::class)
    fun Modifier.horizontalScrollWithRelocation(
        state: ScrollState,
        enabled: Boolean = true,
        flingBehavior: FlingBehavior? = null,
        reverseScrolling: Boolean = false
    ): Modifier {
        // Calculate the offset needed to bring one of the edges into view. The leadingEdge is
        // the side closest to the origin (For the x-axis this is 'left', for the y-axis this is
        // 'top'). The trailing edge is the other side (For the x-axis this is 'right', for the
        // y-axis this is 'bottom').
        fun relocationDistance(leadingEdge: Float, trailingEdge: Float, parentSize: Float) = when {
            // If the item is already visible, no need to scroll.
            leadingEdge >= 0 && trailingEdge <= parentSize -> 0f

            // If the item is visible but larger than the parent, we don't scroll.
            leadingEdge < 0 && trailingEdge > parentSize -> 0f

            // Find the minimum scroll needed to make one of the edges coincide with the parent's edge.
            abs(leadingEdge) < abs(trailingEdge - parentSize) -> leadingEdge
            else -> trailingEdge - parentSize
        }

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

    val relocationRequester = remember { RelocationRequester() }
    val scrollState = rememberScrollState()
    val coroutineScope = rememberCoroutineScope()
    Column {
        Box(Modifier.width(100.dp).horizontalScrollWithRelocation(scrollState)) {
            Box(Modifier.size(100.dp))
            Box(Modifier.size(100.dp).relocationRequester(relocationRequester))
        }
        Button(onClick = { coroutineScope.launch { relocationRequester.bringIntoView() } }) {
            Text("Bring box into view")
        }
    }
}
