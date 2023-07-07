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

package androidx.compose.animation.demos.fancy

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.animateOffsetAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.round

@Preview
@Composable
fun SpringChainDemo() {
    var leader by remember { mutableStateOf(Offset(200f, 200f)) }
    Box(
        Modifier.fillMaxSize().pointerInput(Unit) {
            detectDragGestures { _, dragAmount ->
                leader += dragAmount
            }
        }
    ) {
        Text(
            modifier = Modifier.align(Alignment.Center),
            text = "Since we are here, why not drag me around?"
        )
        val size = vibrantColors.size
        val followers = remember { Array<State<Offset>>(size) { mutableStateOf(Offset.Zero) } }
        for (i in 0 until size) {
            // Each follower on the spring chain uses the previous follower's position as target
            followers[i] = animateOffsetAsState(if (i == 0) leader else followers[i - 1].value)
        }

        var expanded by remember { mutableStateOf(false) }
        // Put space between followers when expanded
        val spacing by animateIntAsState(if (expanded) -300 else 0, spring(dampingRatio = 0.7f))

        // Followers stacked in reverse orders
        for (i in followers.size - 1 downTo 0) {
            Box(
                Modifier
                    .offset { followers[i].value.round() }
                    .offset { IntOffset(0, spacing * (i + 1)) }
                    .size(circleSize)
                    .background(vibrantColors[i], CircleShape)
            )
        }

        // Leader
        Box(
            Modifier.offset { leader.round() }.size(circleSize)
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) { expanded = !expanded }
                .background(Color(0xFFfff8ad), CircleShape)
        ) {
            // Rotate icon when expanded / collapsed
            val rotation by animateFloatAsState(if (expanded) 180f else 0f)
            Icon(
                Icons.Filled.KeyboardArrowDown,
                contentDescription = "Expand or Collapse",
                modifier = Modifier.size(30.dp).align(Alignment.Center)
                    .graphicsLayer { this.rotationZ = rotation },
                tint = Color.Gray
            )
        }
    }
}

val circleSize = 60.dp

private val vibrantColors = listOf(
    Color(0xffbfbdff),
    Color(0xffffc7ed),
    Color(0xffffdcab),
    Color(0xffd5ffb0),
    Color(0xffbaefff)
)