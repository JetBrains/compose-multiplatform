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

import androidx.compose.animation.core.animate
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.withInfiniteAnimationFrameMillis
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.isActive
import java.util.Calendar

private class Time(hours: State<Int>, minutes: State<Int>, seconds: State<Int>) {
    val hours by hours
    val minutes by minutes
    val seconds by seconds
}

@Preview
@Composable
fun AnimatedClockDemo() {
    val calendar = remember { Calendar.getInstance() }
    val seconds = remember { mutableStateOf(calendar[Calendar.SECOND]) }
    val minutes = remember { mutableStateOf(calendar[Calendar.MINUTE]) }
    val hours = remember { mutableStateOf(calendar[Calendar.HOUR_OF_DAY]) }
    LaunchedEffect(key1 = Unit) {
        // Start from 23:59:50 to give an impressive animation for all numbers
        calendar.set(2020, 10, 10, 23, 59, 50)
        val initialTime = calendar.timeInMillis
        val firstFrameTime = withInfiniteAnimationFrameMillis { it }
        while (isActive) {
            withInfiniteAnimationFrameMillis {
                calendar.timeInMillis = it - firstFrameTime + initialTime
                seconds.value = calendar[Calendar.SECOND]
                minutes.value = calendar[Calendar.MINUTE]
                hours.value = calendar[Calendar.HOUR_OF_DAY]
            }
        }
    }
    val time = remember { Time(hours, minutes, seconds) }
    FancyClock(time)
}

@Composable
private fun FancyClock(time: Time) {
    Row(
        modifier = Modifier.fillMaxHeight().fillMaxWidth(),
        horizontalArrangement = Arrangement.Center
    ) {
        NumberColumn(2, time.hours / 10)
        NumberColumn(9, time.hours % 10)
        Spacer(modifier = Modifier.size(16.dp))
        NumberColumn(5, time.minutes / 10)
        NumberColumn(9, time.minutes % 10)
        Spacer(modifier = Modifier.size(16.dp))
        NumberColumn(5, time.seconds / 10)
        NumberColumn(9, time.seconds % 10)
    }
}

private const val digitHeight = 24
private const val moveDuration = 700

@Composable
private fun NumberColumn(maxDigit: Int, digit: Int) {
    val offsetY: Dp = animateDpAsState(
        targetValue = ((9 - digit) * digitHeight).dp,
        animationSpec = tween(moveDuration),
    ).value
    var circleOffset by remember { mutableStateOf(0f) }
    LaunchedEffect(digit) {
        if (digit == 0) return@LaunchedEffect // Don't animate for 0 as direction is reversed
        animate(
            initialValue = 0f,
            targetValue = -1f,
            animationSpec = tween(moveDuration)
        ) { animationValue, _ -> circleOffset = animationValue }
        animate(
            initialValue = -1f,
            targetValue = 0f,
            animationSpec = spring(dampingRatio = 0.6f)
        ) { animationValue, _ -> circleOffset = animationValue }
    }
    var circleStretch by remember { mutableStateOf(1f) }
    LaunchedEffect(digit) {
        if (digit == 0) return@LaunchedEffect // Don't animate for 0 as direction is reversed
        animate(
            initialValue = 1f,
            targetValue = 2f,
            animationSpec = tween(moveDuration)
        ) { animationValue, _ -> circleStretch = animationValue }
        animate(
            initialValue = 2f,
            targetValue = 1f,
            animationSpec = spring(dampingRatio = 0.6f)
        ) { animationValue, _ -> circleStretch = animationValue }
    }
    Box(modifier = Modifier.padding(4.dp)) {
        // Draw an elevation shadow for the rounded column
        Surface(
            shape = RoundedCornerShape((digitHeight / 2).dp),
            modifier = Modifier
                .offset(y = offsetY)
                .size(digitHeight.dp, ((maxDigit + 1) * digitHeight).dp),
            elevation = 12.dp
        ) {}
        // Draw circle that follows focused digit
        Canvas(modifier = Modifier.size(digitHeight.dp, (10 * digitHeight).dp)) {
            drawRoundRect(
                color = Color(0xffd2e7d6),
                size = Size(24.dp.toPx(), (digitHeight * circleStretch).dp.toPx()),
                topLeft = Offset(0f, ((9f + circleOffset) * digitHeight).dp.toPx()),
                cornerRadius = CornerRadius(
                    (digitHeight / 2).dp.toPx(),
                    (digitHeight / 2).dp.toPx()
                )
            )
        }
        // Draw all the digits up to count
        Column(modifier = Modifier.offset(y = offsetY)) {
            for (i in (0..maxDigit)) {
                androidx.compose.material.Text(
                    color = Color.DarkGray,
                    modifier = Modifier.size(digitHeight.dp),
                    text = "$i",
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
