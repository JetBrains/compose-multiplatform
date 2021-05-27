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
package androidx.compose.integration.docs.gestures

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.gestures.rememberScrollableState
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.integration.docs.gestures.GesturesSnippet1.ClickableSample
import androidx.compose.integration.docs.gestures.GesturesSnippet3.ScrollBoxes
import androidx.compose.integration.docs.gestures.GesturesSnippet6.NestedSample
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.FractionalThreshold
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.rememberSwipeableState
import androidx.compose.material.swipeable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

/**
 * This file lets DevRel track changes to snippets present in
 * https://developer.android.com/jetpack/compose/gestures
 *
 * No action required if it's modified.
 */

private object GesturesSnippet1 {
    @Composable
    fun ClickableSample() {
        val count = remember { mutableStateOf(0) }
        // content that you want to make clickable
        Text(
            text = count.value.toString(),
            modifier = Modifier
                .clickable { count.value += 1 }
                .padding(24.dp)
        )
    }
}

@Preview
@Composable
private fun Preview1() {
    MaterialTheme {
        Surface {
            Box(
                modifier = Modifier
                    .background(Color.LightGray)
                    .size(240.dp),
                contentAlignment = Alignment.Center
            ) {
                ClickableSample()
            }
        }
    }
}

@Composable fun GesturesSnippet2() {
    Modifier.pointerInput(Unit) {
        detectTapGestures(
            onPress = { /* Called when the gesture starts */ },
            onDoubleTap = { /* Called on Double Tap */ },
            onLongPress = { /* Called on Long Press */ },
            onTap = { /* Called on Tap */ }
        )
    }
}

private object GesturesSnippet3 {
    @Composable
    fun ScrollBoxes() {
        Column(
            modifier = Modifier
                .background(Color.LightGray)
                .size(100.dp)
                .verticalScroll(rememberScrollState())
        ) {
            repeat(10) {
                Text("Item $it", modifier = Modifier.padding(2.dp))
            }
        }
    }
}

@Preview
@Composable
private fun Preview3() {
    MaterialTheme {
        Surface {
            Box(
                modifier = Modifier
                    .background(Color.LightGray)
                    .size(240.dp),
                contentAlignment = Alignment.Center
            ) {
                ScrollBoxes()
            }
        }
    }
}

private object GesturesSnippet4 {
    @Composable
    private fun ScrollBoxesSmooth() {

        // Smoothly scroll 100px on first composition
        val state = rememberScrollState()
        LaunchedEffect(Unit) { state.animateScrollTo(100) }

        Column(
            modifier = Modifier
                .background(Color.LightGray)
                .size(100.dp)
                .padding(horizontal = 8.dp)
                .verticalScroll(state)
        ) {
            repeat(10) {
                Text("Item $it", modifier = Modifier.padding(2.dp))
            }
        }
    }
}

private object GesturesSnippet5 {
    @Composable
    fun ScrollableSample() {
        // actual composable state
        var offset by remember { mutableStateOf(0f) }
        Box(
            Modifier
                .size(150.dp)
                .scrollable(
                    orientation = Orientation.Vertical,
                    // Scrollable state: describes how to consume scrolling delta and update offset
                    state = rememberScrollableState { delta ->
                        offset += delta
                        delta
                    }
                )
                .background(Color.LightGray),
            contentAlignment = Alignment.Center
        ) {
            Text(offset.toString())
        }
    }
}

@Preview
@Composable
private fun Preview5() {
    MaterialTheme {
        Surface {
            Box(
                modifier = Modifier
                    .background(Color.LightGray)
                    .size(240.dp),
                contentAlignment = Alignment.Center
            ) {
                GesturesSnippet5.ScrollableSample()
            }
        }
    }
}

private object GesturesSnippet6 {
    @Composable
    fun NestedSample() {
        val gradient = Brush.verticalGradient(0f to Color.Gray, 1000f to Color.White)
        Box(
            modifier = Modifier
                .background(Color.LightGray)
                .verticalScroll(rememberScrollState())
                .padding(32.dp)
        ) {
            Column {
                repeat(6) {
                    Box(
                        modifier = Modifier
                            .height(128.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        Text(
                            "Scroll here",
                            modifier = Modifier
                                .border(12.dp, Color.DarkGray)
                                .background(brush = gradient)
                                .padding(24.dp)
                                .height(150.dp)
                        )
                    }
                }
            }
        }
    }
}

@Preview
@Composable
private fun Preview6() {
    MaterialTheme {
        Surface {
            Box(
                modifier = Modifier
                    .background(Color.DarkGray)
                    .size(240.dp),
                contentAlignment = Alignment.Center
            ) {
                NestedSample()
            }
        }
    }
}

@Composable private fun GesturesSnippet7() {
    var offsetX by remember { mutableStateOf(0f) }
    Text(
        modifier = Modifier
            .offset { IntOffset(offsetX.roundToInt(), 0) }
            .draggable(
                orientation = Orientation.Horizontal,
                state = rememberDraggableState { delta ->
                    offsetX += delta
                }
            ),
        text = "Drag me!"
    )
}
@Composable private fun GesturesSnippet8() {
    Box(modifier = Modifier.fillMaxSize()) {
        var offsetX by remember { mutableStateOf(0f) }
        var offsetY by remember { mutableStateOf(0f) }

        Box(
            Modifier
                .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
                .background(Color.Blue)
                .size(50.dp)
                .pointerInput(Unit) {
                    detectDragGestures { _, dragAmount ->
                        offsetX += dragAmount.x
                        offsetY += dragAmount.y
                    }
                }
        )
    }
}

@Preview
@Composable
private fun Preview8() {
    MaterialTheme { Surface(color = Color.LightGray) { GesturesSnippet8() } }
}

private object GesturesSnippet9 {
    @OptIn(ExperimentalMaterialApi::class)
    @Composable
    fun SwipeableSample() {
        val width = 96.dp
        val squareSize = 48.dp

        val swipeableState = rememberSwipeableState(0)
        val sizePx = with(LocalDensity.current) { squareSize.toPx() }
        val anchors = mapOf(0f to 0, sizePx to 1) // Maps anchor points (in px) to states

        Box(
            modifier = Modifier
                .width(width)
                .swipeable(
                    state = swipeableState,
                    anchors = anchors,
                    thresholds = { _, _ -> FractionalThreshold(0.3f) },
                    orientation = Orientation.Horizontal
                )
                .background(Color.LightGray)
        ) {
            Box(
                Modifier
                    .offset { IntOffset(swipeableState.offset.value.roundToInt(), 0) }
                    .size(squareSize)
                    .background(Color.DarkGray)
            )
        }
    }
}

@Preview
@Composable
private fun Preview9() {
    MaterialTheme {
        Surface(color = Color.LightGray, modifier = Modifier.padding(64.dp)) {
            GesturesSnippet9.SwipeableSample()
        }
    }
}

private object GesturesSnippet10 {
    @Composable
    fun TransformableSample() {
        // set up all transformation states
        var scale by remember { mutableStateOf(1f) }
        var rotation by remember { mutableStateOf(0f) }
        var offset by remember { mutableStateOf(Offset.Zero) }
        val state = rememberTransformableState { zoomChange, offsetChange, rotationChange ->
            scale *= zoomChange
            rotation += rotationChange
            offset += offsetChange
        }
        Box(
            Modifier
                // apply other transformations like rotation and zoom on the pizza slice emoji
                .graphicsLayer(
                    scaleX = scale,
                    scaleY = scale,
                    rotationZ = rotation,
                    translationX = offset.x,
                    translationY = offset.y
                )
                // add transformable to listen to multitouch transformation events after offset
                .transformable(state = state)
                .background(Color.Blue)
                .fillMaxSize()
        )
    }
}

@Preview
@Composable
private fun Preview10() {
    MaterialTheme {
        Surface(color = Color.LightGray, modifier = Modifier.padding(64.dp)) {
            GesturesSnippet10.TransformableSample()
        }
    }
}