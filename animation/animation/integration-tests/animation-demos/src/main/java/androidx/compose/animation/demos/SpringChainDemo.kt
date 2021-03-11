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

package androidx.compose.animation.demos

import androidx.compose.animation.core.animateOffsetAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.consumeAllChanges
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.round

@Composable
fun SpringChainDemo() {
    var leader by remember { mutableStateOf(Offset(200f, 200f)) }
    Box(
        Modifier.fillMaxSize().pointerInput(Unit) {
            detectDragGestures { change, dragAmount ->
                change.consumeAllChanges()
                leader += dragAmount
            }
        }
    ) {
        Text(
            modifier = Modifier.align(Alignment.Center),
            text = "Since we are here, why not drag me around?"
        )
        val size = pastelAwakening.size
        val followers = remember { Array<State<Offset>>(size) { mutableStateOf(Offset.Zero) } }
        for (i in 0 until size) {
            // Each follower on the spring chain uses the previous follower's position as target
            followers[i] = animateOffsetAsState(if (i == 0) leader else followers[i - 1].value)
        }

        // Followers stacked in reverse orders
        for (i in followers.size - 1 downTo 0) {
            Box(
                Modifier
                    .offset { followers[i].value.round() }
                    .size(80.dp)
                    .background(pastelAwakening[i], CircleShape)
            )
        }
        // Leader
        Box(
            Modifier.offset { leader.round() }.size(80.dp)
                .background(Color(0xFFfffbd0), CircleShape)
        )
    }
}

private val pastelAwakening = listOf(
    Color(0xffdfdeff),
    Color(0xffffe0f5),
    Color(0xffffefd8),
    Color(0xffe6ffd0),
    Color(0xffd9f6ff)
)