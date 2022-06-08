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

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlin.math.min

@Preview
@Composable
fun AnimatedDotsDemo() {
    val infiniteTransition = rememberInfiniteTransition()
    val position by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = totalDotCount.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(2000),
            repeatMode = RepeatMode.Reverse
        )
    )
    Dots(position)
}

private const val totalDotCount = 4
private const val dotSpacing = 60f
private const val dotComposableHeight = 200f

@Composable
private fun Dots(position: Float) {
    Canvas(modifier = Modifier.requiredSize(400.dp, dotComposableHeight.dp)) {
        val centerY = dotComposableHeight / 2
        for (currentDotPosition in 1..totalDotCount) {
            val dotSize = getDotSizeForPosition(position, currentDotPosition)
            if (currentDotPosition < totalDotCount) {
                // Draw a bridge between the current dot and the next dot
                val nextDotPosition = currentDotPosition + 1
                val nextDotSize = getDotSizeForPosition(position, nextDotPosition)
                // Pick a direction to draw bridge from the smaller dot to the larger dot
                val shouldFlip = nextDotSize > dotSize
                val nextPositionDelta = -min(
                    1f,
                    abs(position - if (shouldFlip) nextDotPosition else currentDotPosition)
                )
                // Calculate the top-most and the bottom-most coordinates of current dot
                val leftX = (currentDotPosition * dotSpacing).dp.toPx()
                val leftYTop = (centerY - dotSize).dp.toPx()
                val leftYBottom = (centerY + dotSize).dp.toPx()
                // Calculate the top-most and the bottom-most coordinates of next dot
                val rightX = (nextDotPosition * dotSpacing).dp.toPx()
                val rightYTop = (centerY - nextDotSize).dp.toPx()
                val rightYBottom = (centerY + nextDotSize).dp.toPx()
                // Calculate the middle Y coordinate between two dots
                val midX = ((currentDotPosition + 0.5) * dotSpacing).dp.toPx()

                val path = if (shouldFlip) {
                    // Calculate control point Y coordinates a bit inside the current dot
                    val bezierYTop = (centerY - dotSize - 5f * nextPositionDelta).dp.toPx()
                    val bezierYBottom = (centerY + dotSize + 5f * nextPositionDelta).dp.toPx()
                    getBridgePath(
                        rightX, rightYTop, rightYBottom, leftX, leftYTop, leftYBottom,
                        midX, bezierYTop, bezierYBottom, centerY.dp.toPx()
                    )
                } else {
                    // Calculate control point Y coordinates a bit inside the next dot
                    val bezierYTop = (centerY - nextDotSize - 5f * nextPositionDelta).dp.toPx()
                    val bezierYBottom = (centerY + nextDotSize + 5f * nextPositionDelta).dp.toPx()
                    getBridgePath(
                        leftX, leftYTop, leftYBottom, rightX, rightYTop, rightYBottom,
                        midX, bezierYTop, bezierYBottom, centerY.dp.toPx()
                    )
                }
                drawPath(path, Color(0xff8eb4e6))
            }
            // Draw the current dot
            drawCircle(
                getDotColor(position, currentDotPosition),
                radius = dotSize.dp.toPx(),
                center = Offset((currentDotPosition * dotSpacing).dp.toPx(), 100.dp.toPx())
            )
        }
    }
}

/**
 * Returns a path for a bridge between two dots drawn using two quadratic beziers.
 *
 * First bezier is drawn between (startX, startYTop) and (endX, endYTop) coordinates using
 * (bezierX, bezierYTop) as control point.
 * Second bezier is drawn between (startX, startYBottom) and (endX, endYBottom) coordinates using
 * (bezierX, bezierYBottom) as control point.
 *
 * Then additional lines are drawn to make this a filled path.
 */
private fun getBridgePath(
    startX: Float,
    startYTop: Float,
    startYBottom: Float,
    endX: Float,
    endYTop: Float,
    endYBottom: Float,
    bezierX: Float,
    bezierYTop: Float,
    bezierYBottom: Float,
    midY: Float
): Path {
    return Path().apply {
        moveTo(startX, startYTop)
        quadraticBezierTo(bezierX, bezierYTop, endX, endYTop)
        lineTo(endX, midY)
        lineTo(startX, midY)
        moveTo(startX, startYTop)
        lineTo(startX, startYBottom)
        quadraticBezierTo(bezierX, bezierYBottom, endX, endYBottom)
        lineTo(endX, midY)
        lineTo(startX, midY)
    }
}

private fun getDotColor(position: Float, dotIndex: Int): Color {
    val fraction = min(abs(position - dotIndex), 1f)
    return lerp(Color(0xff1a73e8), Color(0xff468ce8), fraction)
}

private fun getDotSizeForPosition(position: Float, dotIndex: Int): Float {
    val positionDelta = abs(position - dotIndex)
    return if (positionDelta < 1f) {
        (10f + 20 * (1 - positionDelta))
    } else {
        10f
    }
}
