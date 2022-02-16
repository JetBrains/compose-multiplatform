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

package androidx.compose.foundation.samples

import androidx.annotation.Sampled
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.BringIntoViewResponder
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewResponder
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusTarget
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.round
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Sampled
@Composable
fun BringIntoViewSample() {
    Row(Modifier.horizontalScroll(rememberScrollState())) {
        repeat(100) {
            val bringIntoViewRequester = remember { BringIntoViewRequester() }
            val coroutineScope = rememberCoroutineScope()
            Box(
                Modifier
                    // This associates the RelocationRequester with a Composable that wants to be
                    // brought into view.
                    .bringIntoViewRequester(bringIntoViewRequester)
                    .onFocusChanged {
                        if (it.isFocused) {
                            coroutineScope.launch {
                                // This sends a request to all parents that asks them to scroll so
                                // that this item is brought into view.
                                bringIntoViewRequester.bringIntoView()
                            }
                        }
                    }
                    .focusTarget()
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Sampled
@Composable
fun BringPartOfComposableIntoViewSample() {
    with(LocalDensity.current) {
        val bringIntoViewRequester = remember { BringIntoViewRequester() }
        val coroutineScope = rememberCoroutineScope()
        Column {
            Box(
                Modifier
                    .border(2.dp, Color.Black)
                    .size(500f.toDp())
                    .horizontalScroll(rememberScrollState())
            ) {
                Canvas(
                    Modifier
                        .size(1500f.toDp(), 500f.toDp())
                        // This associates the RelocationRequester with a Composable that wants
                        // to be brought into view.
                        .bringIntoViewRequester(bringIntoViewRequester)
                ) {
                    drawCircle(color = Color.Red, radius = 250f, center = Offset(750f, 250f))
                }
            }
            Button(
                onClick = {
                    val circleCoordinates = Rect(500f, 0f, 1000f, 500f)
                    coroutineScope.launch {
                        // This sends a request to all parents that asks them to scroll so that
                        // the circle is brought into view.
                        bringIntoViewRequester.bringIntoView(circleCoordinates)
                    }
                }
            ) {
                Text("Bring circle into View")
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalComposeUiApi::class)
@Sampled
@Composable
fun BringIntoViewResponderSample() {
    var offset: IntOffset by remember { mutableStateOf(IntOffset.Zero) }
    Box(
        modifier = Modifier
            .size(100.dp)
            .layout { measurable, constraints ->
                // Allow the content to be as big as it wants.
                val placeable = measurable.measure(
                    constraints.copy(
                        maxWidth = Constraints.Infinity,
                        maxHeight = Constraints.Infinity
                    )
                )

                layout(constraints.maxWidth, constraints.maxHeight) {
                    // Place the last-requested rectangle at the top-left of the box.
                    placeable.place(offset)
                }
            }
            .bringIntoViewResponder(remember {
                object : BringIntoViewResponder {
                    override fun calculateRectForParent(localRect: Rect): Rect {
                        // Ask our parent to bring our top-left corner into view, since that's where
                        // we're always going to position the requested content.
                        return Rect(Offset.Zero, localRect.size)
                    }

                    override suspend fun bringChildIntoView(localRect: Rect) {
                        // Offset the content right and down by the offset of the requested area so
                        // that it will always be aligned to the top-left of the box.
                        offset = -localRect.topLeft.round()
                    }
                }
            })
    ) {
        LargeContentWithFocusableChildren()
    }
}

@Composable
private fun LargeContentWithFocusableChildren() {
    Column {
        repeat(10) { row ->
            Row {
                repeat(10) { column ->
                    val interactionSource = remember { MutableInteractionSource() }
                    val isFocused by interactionSource.collectIsFocusedAsState()
                    Text(
                        "$row x $column",
                        Modifier
                            .focusable(interactionSource = interactionSource)
                            .then(
                                if (isFocused) Modifier.border(1.dp, Color.Blue) else Modifier
                            )
                            .padding(8.dp)
                    )
                }
            }
        }
    }
}