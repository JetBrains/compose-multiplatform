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

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Composable
fun EasingInfoDemo() {
    val coroutineScope = rememberCoroutineScope()
    LazyColumn {
        bezierCurveList(coroutineScope)
    }
}

fun LazyListScope.bezierCurveList(coroutineScope: CoroutineScope) {
    val graphModifier = Modifier
        .height(200.dp)
        .fillMaxWidth()

    items(EasingItemDemo.values()) { item: EasingItemDemo ->
        EasingInfo(easing = item, modifier = graphModifier, coroutineScope = coroutineScope)
        Spacer(modifier = Modifier.background(androidDark).fillMaxWidth().height(1.dp))
    }
}

@Composable
fun EasingInfo(
    easing: EasingItemDemo,
    coroutineScope: CoroutineScope,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.padding(16.dp)) {
        Text(easing.description)
        Spacer(modifier = Modifier.height(16.dp))
        EasingGraph(easing = easing.function, coroutineScope = coroutineScope)
    }
}

@Composable
fun EasingGraph(
    easing: Easing,
    modifier: Modifier = Modifier,
    coroutineScope: CoroutineScope = rememberCoroutineScope(),
) {
    val path = remember(easing) {
        val path = Path()
        path
    }

    val time = remember(easing) {
        Animatable(0f)
    }
    val easedValue = remember(easing) {
        Animatable(0f)
    }
    val listPoints = remember {
        val list = mutableStateListOf<Animatable<Float, AnimationVector1D>>()
        for (i in 0..NUMBER_SAMPLES) {
            list.add(Animatable(0f))
        }
        list
    }
    LaunchedEffect(key1 = easing) {
        for (i in 0..NUMBER_SAMPLES) {
            val point = easing.transform(i / NUMBER_SAMPLES.toFloat())
            launch {
                listPoints[i].animateTo(
                    point,
                    animationSpec = tween(durationMillis = 300, easing = LinearEasing)
                )
            }
        }
    }
    Row(modifier = modifier.fillMaxWidth()) {
        Box {
            Text("y", modifier = Modifier.align(Alignment.TopStart), fontSize = 12.sp)
            Canvas(modifier = modifier
                .aspectRatio(1f)
                .padding(16.dp)
                .pointerInput(Any()) {
                    detectTapGestures {
                        runAnimation(coroutineScope, time, easedValue, easing)
                    }
                },
                onDraw = {
                    drawGraphAxis()
                    updateGraphPath(easing, path, listPoints)
                    drawEasingPath(path, easing, time)
                })
            Text("time",
                modifier = Modifier.align(Alignment.BottomEnd),
                fontSize = 12.sp)
        }
        ExtraEasingSamples(easedValue)
    }
}

private fun DrawScope.drawEasingPath(
    path: Path,
    easing: Easing,
    time: Animatable<Float, AnimationVector1D>
) {
    translate(0f, this.size.height) {
        // draw animation path
        drawPath(path, color = AndroidBlue, style = Stroke(2.dp.toPx()))

        val transformedYValue = easing.transform(time.value)
        // animated circle on graph
        drawCircle(
            androidGreen, 8.dp.toPx(), center = Offset(
                time.value * this.size.width, -transformedYValue * this.size.height
            )
        )
    }
}

private fun DrawScope.updateGraphPath(
    easing: Easing,
    path: Path,
    listPoints: SnapshotStateList<Animatable<Float, AnimationVector1D>>
) {
    val initialPoint = easing.transform(0f)
    path.reset()
    path.moveTo(0f, -initialPoint)
    for (i in 0..NUMBER_SAMPLES) {
        val point = listPoints[i]
        path.lineTo(
            i / NUMBER_SAMPLES.toFloat() * this.size.width, -point.value * this
                .size
                .height
        )
    }
}

@Composable
private fun ExtraEasingSamples(easedValue: Animatable<Float, AnimationVector1D>) {
    Spacer(Modifier.width(16.dp))
    TranslationBoxDemo(easedValue.value)
    Column {
        Row {
            ScalingBoxDemo(easedValue.value)
            ColorBoxDemo(easedValue.value)
        }
        Row {
            RotatingBoxDemo(easedValue.value)
            AlphaBoxDemo(easedValue.value)
        }
    }
}

private const val NUMBER_SAMPLES = 100

private fun runAnimation(
    coroutineScope: CoroutineScope,
    time: Animatable<Float, AnimationVector1D>,
    easedValue: Animatable<Float, AnimationVector1D>,
    easing: Easing
) {
    coroutineScope.launch {
        time.snapTo(0f)
        time.animateTo(
            1f, animationSpec = tween(
                easing = LinearEasing,
                durationMillis = EASING_DURATION_MILLIS
            )
        )
    }
    coroutineScope.launch {
        easedValue.snapTo(0f)
        easedValue.animateTo(
            1f, animationSpec = tween(
                easing = easing,
                durationMillis = EASING_DURATION_MILLIS
            )
        )
    }
}
private const val EASING_DURATION_MILLIS = 3000

private fun DrawScope.drawGraphAxis() {
    val lineThickness = 2.dp.toPx()
    val lineColor = AndroidNavy.copy(alpha = 0.6f)
    // y axis
    drawLine(
        lineColor,
        start = Offset.Zero,
        end = Offset(0f, this.size.height),
        strokeWidth = lineThickness
    )

    // x axis
    drawLine(
        lineColor,
        start = Offset(0f, this.size.height),
        end = Offset(this.size.width, this.size.height),
        strokeWidth = lineThickness
    )
}

internal val AndroidBlue = Color(0xFF4285F4)
internal val AndroidNavy = Color(0xFF073042)