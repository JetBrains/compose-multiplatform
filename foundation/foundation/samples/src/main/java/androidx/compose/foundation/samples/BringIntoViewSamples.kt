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
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusTarget
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
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
