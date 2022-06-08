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

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlin.math.max

@Composable
fun LookaheadMeasurePlaceDemo() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        var isHorizontal by remember { mutableStateOf(true) }
        Button(modifier = Modifier.padding(top = 20.dp, bottom = 20.dp),
            onClick = { isHorizontal = !isHorizontal }) {
            Text("Toggle")
        }
        Column(Modifier.background(Color(0xfffdedac), RoundedCornerShape(10)).padding(10.dp)) {
            Text("Scene Host")
            SceneHost(
                Modifier.height(200.dp).fillMaxWidth().wrapContentSize(Alignment.CenterStart)
            ) {
                MyFlowRow {
                    Box(
                        Modifier.height(50.dp)
                            .fillMaxWidth(if (isHorizontal) 0.4f else 1f)
                            .sharedElement()
                            .background(colors[0], RoundedCornerShape(10))
                    )
                    Box(
                        Modifier.height(50.dp)
                            .fillMaxWidth(if (isHorizontal) 0.2f else 0.4f)
                            .sharedElement()
                            .background(colors[1], RoundedCornerShape(10))
                    )
                    Box(
                        Modifier.height(50.dp)
                            .fillMaxWidth(if (isHorizontal) 0.2f else 0.4f)
                            .sharedElement()
                            .background(colors[2], RoundedCornerShape(10))
                    )
                }
                Box(Modifier.size(if (isHorizontal) 200.dp else 100.dp))
            }
        }

        Spacer(Modifier.size(50.dp))

        Column(Modifier.background(Color(0xfffdedac), RoundedCornerShape(10)).padding(10.dp)) {
            Text("Animating Width")
            MyFlowRow(
                modifier = Modifier.height(200.dp).fillMaxWidth()
                    .wrapContentSize(Alignment.CenterStart)
            ) {
                Box(
                    Modifier.height(50.dp)
                        .fillMaxWidth(animateFloatAsState(if (isHorizontal) 0.4f else 1f).value)
                        .background(colors[0], RoundedCornerShape(10))
                )
                Box(
                    Modifier.height(50.dp)
                        .fillMaxWidth(animateFloatAsState(if (isHorizontal) 0.2f else 0.4f).value)
                        .background(colors[1], RoundedCornerShape(10))
                )
                Box(
                    Modifier.height(50.dp)
                        .fillMaxWidth(animateFloatAsState(if (isHorizontal) 0.2f else 0.4f).value)
                        .background(colors[2], RoundedCornerShape(10))
                )
            }
        }
    }
}

fun printStack(tag: String) {
    Thread.currentThread().stackTrace.forEach {
        println("$tag, $it")
    }
}

@Composable
internal fun MyFlowRow(
    modifier: Modifier = Modifier,
    mainAxisSpacing: Dp = 20.dp,
    crossAxisSpacing: Dp = 20.dp,
    content: @Composable () -> Unit
) {
    Layout(modifier = modifier, content = content) { measurables, constraints ->
        val sequences = mutableListOf<List<Placeable>>()
        val crossAxisSizes = mutableListOf<Int>()
        val crossAxisPositions = mutableListOf<Int>()

        var mainAxisSpace = 0
        var crossAxisSpace = 0

        val currentSequence = mutableListOf<Placeable>()
        var currentMainAxisSize = 0
        var currentCrossAxisSize = 0

        // Return whether the placeable can be added to the current sequence.
        fun canAddToCurrentSequence(placeable: Placeable) =
            currentSequence.isEmpty() || currentMainAxisSize + mainAxisSpacing.roundToPx() +
                placeable.width <= constraints.maxWidth

        // Store current sequence information and start a new sequence.
        fun startNewSequence() {
            if (sequences.isNotEmpty()) {
                crossAxisSpace += crossAxisSpacing.roundToPx()
            }
            sequences += currentSequence.toList()
            crossAxisSizes += currentCrossAxisSize
            crossAxisPositions += crossAxisSpace

            crossAxisSpace += currentCrossAxisSize
            mainAxisSpace = max(mainAxisSpace, currentMainAxisSize)

            currentSequence.clear()
            currentMainAxisSize = 0
            currentCrossAxisSize = 0
        }

        for (measurable in measurables) {
            // Ask the child for its preferred size.
            val placeable = measurable.measure(constraints)

            // Start a new sequence if there is not enough space.
            if (!canAddToCurrentSequence(placeable)) startNewSequence()

            // Add the child to the current sequence.
            if (currentSequence.isNotEmpty()) {
                currentMainAxisSize += mainAxisSpacing.roundToPx()
            }
            currentSequence.add(placeable)
            currentMainAxisSize += placeable.width
            currentCrossAxisSize = max(currentCrossAxisSize, placeable.height)
        }

        if (currentSequence.isNotEmpty()) startNewSequence()

        val mainAxisLayoutSize = max(mainAxisSpace, constraints.minWidth)

        val crossAxisLayoutSize = max(crossAxisSpace, constraints.minHeight)

        val layoutWidth = mainAxisLayoutSize

        val layoutHeight = crossAxisLayoutSize

        layout(layoutWidth, layoutHeight) {
            sequences.forEachIndexed { i, placeables ->
                val childrenMainAxisSizes = IntArray(placeables.size) { j ->
                    placeables[j].width +
                        if (j < placeables.lastIndex) mainAxisSpacing.roundToPx() else 0
                }
                val arrangement = Arrangement.Start
                // TODO(soboleva): rtl support
                // Handle vertical direction
                val mainAxisPositions = IntArray(childrenMainAxisSizes.size) { 0 }
                with(arrangement) {
                    arrange(
                        mainAxisLayoutSize,
                        childrenMainAxisSizes,
                        LayoutDirection.Ltr,
                        mainAxisPositions
                    )
                }
                placeables.forEachIndexed { j, placeable ->
                    placeable.place(
                        x = mainAxisPositions[j],
                        y = crossAxisPositions[i]
                    )
                }
            }
        }
    }
}

private val colors = listOf(
    Color(0xffff6f69),
    Color(0xffffcc5c),
    Color(0xff2a9d84),
    Color(0xff264653)
)