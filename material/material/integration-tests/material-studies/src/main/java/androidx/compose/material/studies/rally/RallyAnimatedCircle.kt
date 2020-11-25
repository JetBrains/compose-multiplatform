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

package androidx.compose.material.studies.rally

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FloatPropKey
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.transitionDefinition
import androidx.compose.animation.core.tween
import androidx.compose.animation.transition
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.AmbientDensity
import androidx.compose.ui.unit.dp

private const val DividerLengthInDegrees = 1.8f
private val AngleOffset = FloatPropKey()
private val Shift = FloatPropKey()

private val CircularTransition = transitionDefinition<Int> {
    state(0) {
        this[AngleOffset] = 0f
        this[Shift] = 0f
    }
    state(1) {
        this[AngleOffset] = 360f
        this[Shift] = 30f
    }
    transition(fromState = 0, toState = 1) {
        AngleOffset using tween(
            delayMillis = 500,
            durationMillis = 900,
            easing = CubicBezierEasing(0f, 0.75f, 0.35f, 0.85f)
        )
        Shift using tween(
            delayMillis = 500,
            durationMillis = 900,
            easing = LinearOutSlowInEasing
        )
    }
}

/** when calculating a proportion of N elements, the sum of elements has to be (1 - N * 0.005)
 * because there will be N dividers of size 1.8 degrees */
@Composable
fun AnimatedCircle(
    modifier: Modifier = Modifier,
    proportions: List<Float>,
    colors: List<Color>
) {
    val stroke = Stroke(5.dp.value * AmbientDensity.current.density)
    val state = transition(definition = CircularTransition, initState = 0, toState = 1)
    Canvas(modifier) {
        val innerRadius = (size.minDimension - stroke.width) / 2
        val halfSize = size / 2.0f
        val topLeft = Offset(
            halfSize.width - innerRadius,
            halfSize.height - innerRadius
        )
        val size = Size(innerRadius * 2, innerRadius * 2)
        var startAngle = state[Shift] - 90f
        proportions.forEachIndexed { index, proportion ->
            val sweep = proportion * state[AngleOffset]
            drawArc(
                color = colors[index],
                startAngle = startAngle + DividerLengthInDegrees / 2,
                sweepAngle = sweep - DividerLengthInDegrees,
                topLeft = topLeft,
                size = size,
                useCenter = false,
                style = stroke
            )
            startAngle += sweep
        }
    }
}