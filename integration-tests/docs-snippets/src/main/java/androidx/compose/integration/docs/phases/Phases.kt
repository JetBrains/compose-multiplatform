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

@file:Suppress("CanBeVal", "ClassName", "UNUSED_PARAMETER", "unused")

package androidx.compose.integration.docs.phases

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp

/*
 * This file lets DevRel track changes to snippets present in
 * https://developer.android.com/jetpack/compose/phases
 *
 * No action required if it's modified.
 */

@Composable
private fun StateWithoutPropertyDelegate() {
    // State read without property delegate.
    val paddingState: MutableState<Dp> = remember { mutableStateOf(8.dp) }
    Text(
        text = "Hello",
        modifier = Modifier.padding(paddingState.value)
    )
}

@Composable
private fun StateWithPropertyDelegate() {
    // State read with property delegate.
    var padding: Dp by remember { mutableStateOf(8.dp) }
    Text(
        text = "Hello",
        modifier = Modifier.padding(padding)
    )
}

@Composable
private fun StateReadInComposition() {
    var padding by remember { mutableStateOf(8.dp) }
    Text(
        text = "Hello",
        // The `padding` state is read in the composition phase
        // when the modifier is constructed.
        // Changes in `padding` will invoke recomposition.
        modifier = Modifier.padding(padding)
    )
}

@Composable
private fun StateReadInLayout() {
    var offsetX by remember { mutableStateOf(8.dp) }
    Text(
        text = "Hello",
        modifier = Modifier.offset {
            // The `offsetX` state is read in the placement step
            // of the layout phase when the offset is calculated.
            // Changes in `offsetX` restart the layout.
            IntOffset(offsetX.roundToPx(), 0)
        }
    )
}

@Composable
private fun StateReadInDrawing(
    modifier: Modifier = Modifier
) {
    var color by remember { mutableStateOf(Color.Red) }
    Canvas(modifier = modifier) {
        // The `color` state is read in the drawing phase
        // when the canvas is rendered.
        // Changes in `color` restart the drawing.
        drawRect(color)
    }
}

@Composable
private fun OptimizingStateReadsComposition() {
    Box {
        val listState = rememberLazyListState()

        Image(
            // Non-optimal implementation!
            Modifier.offset(
                with(LocalDensity.current) {
                    // State read of firstVisibleItemScrollOffset in composition
                    (listState.firstVisibleItemScrollOffset / 2).toDp()
                }
            )
        )

        LazyColumn(state = listState)
    }
}

@Composable
private fun OptimizingStateReadsLayout() {
    Box {
        val listState = rememberLazyListState()

        Image(
            Modifier.offset {
                // State read of firstVisibleItemScrollOffset in Layout
                IntOffset(x = 0, y = listState.firstVisibleItemScrollOffset / 2)
            }
        )

        LazyColumn(state = listState)
    }
}

@Composable
private fun ImmediateRecomposition() {
    Box {
        var imageHeightPx by remember { mutableStateOf(0) }

        Image(
            painter = painterResource(R.drawable.rectangle),
            contentDescription = "I'm above the text",
            modifier = Modifier
                .fillMaxWidth()
                .onSizeChanged { size ->
                    // Don't do this
                    imageHeightPx = size.height
                }
        )

        Text(
            text = "I'm below the image",
            modifier = Modifier.padding(
                top = with(LocalDensity.current) { imageHeightPx.toDp() }
            )
        )
    }
}

/*
 * Fakes needed for snippets to build:
 */

@Composable
private fun Image(modifier: Modifier = Modifier) {
}

@Composable
private fun LazyColumn(state: LazyListState) {
}

private object R {
    object drawable {
        const val rectangle = 0
    }
}
