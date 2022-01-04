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

package androidx.compose.animation.demos

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.movableContentWithReceiverOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.FirstBaseline
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun LookaheadLayoutWithAlignmentLinesDemo() {
    val helloWorld = createHelloWorld()
    Column(
        Modifier.fillMaxSize().padding(top = 100.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        var horizontal by remember { mutableStateOf(true) }
        Button({ horizontal = !horizontal }) {
            Text("Toggle Layout")
        }
        Spacer(Modifier.size(80.dp))
        SceneHost(Modifier.padding(2.dp)
            .drawBehind {
                drawRect(
                    color = Color.Red,
                    style = Stroke(5f)
                )
            }
        ) {
            if (horizontal) {
                Layout({
                    Row(
                        Modifier.fillMaxWidth().wrapContentHeight().background(Color(0xffb4c8ea)),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        helloWorld(Modifier.alignByBaseline())
                        Spacer(Modifier.size(10.dp))
                        Text(
                            text = "!!",
                            fontSize = 50.sp,
                            color = Color.White,
                            modifier = Modifier.alignByBaseline()
                                .sharedElement()
                                .background(color = Color(0xffffb900), RoundedCornerShape(10))
                        )
                    }
                    Spacer(Modifier.fillMaxWidth().requiredHeight(1.dp).background(Color.Black))
                }) { measurables, constraints ->
                    val placeables = measurables.map {
                        it.measure(constraints)
                    }
                    val row = placeables.first()
                    val position = row[FirstBaseline]
                    layout(row.width, row.height) {
                        row.place(0, 0)
                        placeables[1].place(0, position)
                    }
                }
            } else {
                Column(
                    Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    helloWorld(Modifier)
                }
            }
        }
    }
}

@Composable
private fun HelloText(modifier: Modifier = Modifier) {
    Text(
        text = "Hello",
        fontSize = 80.sp,
        color = Color.White,
        modifier = modifier
            .background(color = Color(0xfff3722c), RoundedCornerShape(10))
    )
}

@Composable
private fun WorldText(modifier: Modifier = Modifier) {
    Text(
        text = "World",
        color = Color.White,
        fontSize = 30.sp,
        modifier = modifier
            .background(color = Color(0xff90be6d), RoundedCornerShape(10))
    )
}

@Composable
private fun createHelloWorld(): SharedElement {
    return remember {
        movableContentWithReceiverOf<SceneScope, Modifier> { modifier ->
            Text(
                text = "Hello",
                fontSize = 80.sp,
                color = Color.White,
                modifier = modifier
                    .sharedElement()
                    .background(color = Color(0xfff3722c), RoundedCornerShape(10))
            )
            Spacer(Modifier.size(10.dp))
            Text(
                text = "World",
                color = Color.White,
                fontSize = 30.sp,
                modifier = modifier
                    .sharedElement()
                    .background(color = Color(0xff90be6d), RoundedCornerShape(10))
            )
        }
    }
}

typealias SharedElement = @Composable SceneScope.(Modifier) -> Unit