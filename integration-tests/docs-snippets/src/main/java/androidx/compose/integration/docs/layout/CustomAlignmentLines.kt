// ktlint-disable indent https://github.com/pinterest/ktlint/issues/967
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

// Ignore lint warnings in documentation snippets
@file:Suppress("unused", "UNUSED_PARAMETER", "UNUSED_VARIABLE")

package androidx.compose.integration.docs.layout

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.HorizontalAlignmentLine
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * This file lets DevRel track changes to the custom AlignmentLines code snippets present in
 * https://developer.android.com/jetpack/compose/layouts/alignment-lines
 *
 * No action required if it's modified.
 */

/**
 * AlignmentLine defined by the maximum data value in a [BarChart]
 */
private val MaxChartValue = HorizontalAlignmentLine(merger = { old, new -> min(old, new) })

/**
 * AlignmentLine defined by the minimum data value in a [BarChart]
 */
private val MinChartValue = HorizontalAlignmentLine(merger = { old, new -> max(old, new) })

@Composable
private fun BarChart(
    dataPoints: List<Int>,
    modifier: Modifier = Modifier
) {
    val maxValue: Float = remember(dataPoints) { dataPoints.maxOrNull()!! * 1.2f }
    val maxDataPoint: Int = remember(dataPoints) { dataPoints.maxOrNull()!! }
    val minDataPoint: Int = remember(dataPoints) { dataPoints.minOrNull()!! }

    var maxYBaseline by remember { mutableStateOf(Float.MAX_VALUE) }
    var minYBaseline by remember { mutableStateOf(Float.MIN_VALUE) }

    Layout(
        modifier = modifier,
        content = {
            // TODO: Omit the content block for the code snippets in DAC
            BoxWithConstraints(propagateMinConstraints = true) {
                val density = LocalDensity.current
                with(density) {
                    val yPositionRatio = remember(density, maxHeight, maxValue) {
                        maxHeight.toPx() / maxValue
                    }
                    val xPositionRatio = remember(density, maxWidth, dataPoints) {
                        maxWidth.toPx() / (dataPoints.size + 1)
                    }
                    val xOffset = remember(density) { // center points in the graph
                        xPositionRatio / dataPoints.size
                    }

                    Canvas(Modifier) {
                        dataPoints.forEachIndexed { index, dataPoint ->
                            val rectSize = Size(60f, dataPoint * yPositionRatio)
                            val topLeftOffset = Offset(
                                x = xPositionRatio * (index + 1) - xOffset,
                                y = (maxValue - dataPoint) * yPositionRatio
                            )
                            drawRect(Color(0xFF3DDC84), topLeftOffset, rectSize)

                            if (maxYBaseline == Float.MAX_VALUE && dataPoint == maxDataPoint) {
                                maxYBaseline = topLeftOffset.y
                            }
                            if (minYBaseline == Float.MIN_VALUE && dataPoint == minDataPoint) {
                                minYBaseline = topLeftOffset.y
                            }
                        }
                        drawLine(
                            Color(0xFF073042),
                            start = Offset(0f, 0f),
                            end = Offset(0f, maxHeight.toPx()),
                            strokeWidth = 6f
                        )
                        drawLine(
                            Color(0xFF073042),
                            start = Offset(0f, maxHeight.toPx()),
                            end = Offset(maxWidth.toPx(), maxHeight.toPx()),
                            strokeWidth = 6f
                        )
                    }
                }
            }
        }
    ) { measurables, constraints ->
        // Don't constrain child views further, measure them with given constraints
        // List of measured children
        check(measurables.size == 1)
        val placeable = measurables[0].measure(constraints)

        // Set the size of the layout as big as it can
        layout(
            width = constraints.maxWidth,
            height = constraints.maxHeight,
            alignmentLines = mapOf(
                MinChartValue to minYBaseline.roundToInt(),
                MaxChartValue to maxYBaseline.roundToInt()
            )
        ) {
            placeable.placeRelative(0, 0)
        }
    }
}

@Composable
private fun BarChartMinMax(
    dataPoints: List<Int>,
    maxText: @Composable () -> Unit,
    minText: @Composable () -> Unit,
    modifier: Modifier = Modifier
) {
    Layout(
        content = {
            maxText()
            minText()
            // Set a fixed size to make the example easier to follow
            BarChart(dataPoints, Modifier.size(200.dp))
        },
        modifier = modifier
    ) { measurables, constraints ->
        check(measurables.size == 3)
        val placeables = measurables.map {
            it.measure(constraints.copy(minWidth = 0, minHeight = 0))
        }

        val maxTextPlaceable = placeables[0]
        val minTextPlaceable = placeables[1]
        val barChartPlaceable = placeables[2]

        // Obtain the alignment lines from BarChart to position the Text
        val minValueBaseline = barChartPlaceable[MinChartValue]
        val maxValueBaseline = barChartPlaceable[MaxChartValue]
        layout(constraints.maxWidth, constraints.maxHeight) {
            maxTextPlaceable.placeRelative(
                x = 0,
                y = maxValueBaseline - (maxTextPlaceable.height / 2)
            )
            minTextPlaceable.placeRelative(
                x = 0,
                y = minValueBaseline - (minTextPlaceable.height / 2)
            )
            barChartPlaceable.placeRelative(
                x = max(maxTextPlaceable.width, minTextPlaceable.width) + 20,
                y = 0
            )
        }
    }
}

@Preview
@Composable
private fun ChartDataPreview() {
    MaterialTheme {
        BarChartMinMax(
            dataPoints = listOf(4, 24, 15),
            maxText = { Text("Max") },
            minText = { Text("Min") },
            modifier = Modifier.padding(24.dp)
        )
    }
}
