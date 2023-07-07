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

package androidx.compose.foundation.demos

import androidx.compose.animation.animateColor
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.MagnifierStyle
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.magnifier
import androidx.compose.foundation.samples.MagnifierSample
import androidx.compose.integration.demos.common.ComposableDemo
import androidx.compose.material.Divider
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp

val MagnifierDemos = listOf(
    ComposableDemo("Simple Magnifier") { MagnifierSample() },
    ComposableDemo("Multitouch Custom Magnifier") { MultitouchCustomMagnifierDemo() },
)

@OptIn(ExperimentalFoundationApi::class)
private val DemoMagnifierStyle = MagnifierStyle(
    size = DpSize(100.dp, 100.dp),
    cornerRadius = 50.dp,
)

@Preview
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MultitouchCustomMagnifierDemo() {
    // Track the offset for every pointer ID that is currently "down".
    val magnifierOffsets = remember { mutableStateMapOf<PointerId, MutableState<Offset>>() }

    // Animate the background to demonstrate the magnifier updating its content when the
    // layer is redrawn.
    val colorAnimationSpec = remember {
        infiniteRepeatable(tween<Color>(1000))
    }
    val color by rememberInfiniteTransition()
        .animateColor(Color.Red, Color.Green, colorAnimationSpec)

    Column {
        Text(
            "Tap and drag below to activate magnifier. Try multiple fingers!",
            style = TextStyle(textAlign = TextAlign.Center),
            modifier = Modifier.fillMaxWidth()
        )
        if (!DemoMagnifierStyle.isSupported) {
            Text(
                "Magnifier not supported on this platform.",
                color = Color.Red,
                style = TextStyle(textAlign = TextAlign.Center),
                modifier = Modifier.fillMaxWidth()
            )
        }
        Divider()
        // Include some padding to ensure the magnifier is using the right offset.
        Box(
            Modifier
                .padding(48.dp)
                .fillMaxSize()
                .clipToBounds()
                .drawBehind {
                    // Don't use Modifier.background to ensure that Magnifier updates even if
                    // the layer is drawn without a recomposition.
                    drawRect(color)

                    // Draw something interesting to zoom in on.
                    for (diameter in 2 until size.maxDimension.toInt() step 10) {
                    drawCircle(
                        color = Color.Black,
                        radius = diameter / 2f,
                        style = Stroke()
                    )
                }
            }
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        // Track a magnifier for each finger on the screen.
                        awaitPointerEvent().changes.forEach {
                            if (it.pressed) {
                                magnifierOffsets.getOrPut(it.id) {
                                    mutableStateOf(it.position)
                                }.value = it.position
                            } else {
                                magnifierOffsets -= it.id
                            }
                            it.consume()
                        }
                    }
                }
            }
        ) {
            magnifierOffsets.keys.forEach { id ->
                key(id) {
                    val magnifierCenter by remember {
                        derivedStateOf { magnifierOffsets[id]?.value }
                    }
                    Box(
                        // This modifier would normally just be on the outer box, they're on a
                        // separate composable for this demo so that the key function can be used
                        // to preserve individual magnifier state as pointers are added and removed.
                        Modifier.magnifier(
                            sourceCenter = { magnifierCenter ?: Offset.Unspecified },
                            magnifierCenter = {
                                magnifierCenter?.let { it + Offset(0f, -100.dp.toPx()) }
                                    ?: Offset.Zero
                            },
                            zoom = 3f,
                            style = DemoMagnifierStyle
                        )
                    )
                }
            }
        }
    }
}
