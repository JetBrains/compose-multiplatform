/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.compose.ui.demos.gestures

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.gesture.RawScaleObserver
import androidx.compose.ui.gesture.rawScaleGestureFilter
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.Layout
import kotlin.math.roundToInt

/**
 * Demo app created to study some complex interactions of multiple DragGestureDetectors.
 */
@Composable
fun NestedScalingDemo() {
    Column {
        Text("Demonstrates nested scaling.")
        Text(
            "As of now, this works the same way that nested scrolling does.  There is a scaling " +
                "region inside another scaling region. If you scale the inner region far " +
                "enough, it will actually stop scaling and the outer region will scale instead. " +
                "Or you can just scale the outer region (Scale out to get started)"
        )
        Layout(
            content = {
                Scalable(.66666666f, Color(0xFFffeb3b.toInt())) {
                    Scalable(.5f, Color(0xFF4caf50.toInt())) {}
                }
            }
        ) { measurables, constraints ->
            val placeable = measurables.first().measure(constraints)

            layout(constraints.maxWidth, constraints.maxHeight) {
                placeable.placeRelative(
                    (constraints.maxWidth - placeable.width) / 2,
                    (constraints.maxHeight - placeable.height) / 2
                )
            }
        }
    }
}

@Composable
private fun Scalable(
    minPercent: Float,
    color: Color,
    content: @Composable () -> Unit
) {

    val currentPercent = remember { mutableStateOf(1f) }

    val outerScaleObserver = object : RawScaleObserver {
        override fun onScale(scaleFactor: Float): Float {
            val oldSize = currentPercent.value
            currentPercent.value = oldSize * scaleFactor
            if (currentPercent.value < minPercent) {
                currentPercent.value = minPercent
            } else if (currentPercent.value > 1f) {
                currentPercent.value = 1f
            }
            return currentPercent.value / oldSize
        }
    }

    Layout(
        content = content,
        modifier = Modifier.wrapContentSize(Alignment.Center)
            .rawScaleGestureFilter(outerScaleObserver)
            .background(color = color),
        measureBlock = { measurables, constraints ->
            val newConstraints =
                constraints.copy(
                    maxWidth = (constraints.maxWidth * currentPercent.value).roundToInt(),
                    maxHeight = (constraints.maxHeight * currentPercent.value).roundToInt(),
                    minWidth = 0,
                    minHeight = 0
                )

            val placeable = if (measurables.isNotEmpty()) {
                measurables.first().measure(newConstraints)
            } else {
                null
            }

            layout(newConstraints.maxWidth, newConstraints.maxHeight) {
                placeable?.placeRelative(
                    (newConstraints.maxWidth - placeable.width) / 2,
                    (newConstraints.maxHeight - placeable.height) / 2
                )
            }
        }
    )
}