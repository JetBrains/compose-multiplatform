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

package androidx.compose.animation.demos

import androidx.compose.animation.animatedFloat
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.FloatPropKey
import androidx.compose.animation.core.ManualAnimationClock
import androidx.compose.animation.core.TweenSpec
import androidx.compose.animation.core.transitionDefinition
import androidx.compose.animation.core.tween
import androidx.compose.animation.transition
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.preferredHeight
import androidx.compose.foundation.layout.preferredSize
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Providers
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.gesture.DragObserver
import androidx.compose.ui.gesture.pressIndicatorGestureFilter
import androidx.compose.ui.gesture.rawDragGestureFilter
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.AmbientAnimationClock
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun AnimatableSeekBarDemo() {
    val clock = remember { ManualAnimationClock(0L) }
    Providers(AmbientAnimationClock provides clock) {
        Column {
            Text(
                "Drag to update AnimationClock",
                fontSize = 20.sp,
                modifier = Modifier.padding(40.dp)
            )

            Box(Modifier.padding(start = 10.dp, end = 10.dp, bottom = 30.dp)) {
                MovingTargetExample(clock)
            }

            val state = transition(
                definition = transDef,
                initState = "start",
                toState = "end"
            )
            Canvas(Modifier.preferredSize(600.dp, 400.dp)) {
                val rectSize = size * 0.2f
                drawRect(Color(1.0f, 0f, 0f, state[alphaKey]), size = rectSize)

                drawRect(
                    Color(0f, 0f, 1f, state[alphaKey]),
                    topLeft = Offset(state[offset1] * size.width, 0.0f),
                    size = rectSize
                )

                drawRect(
                    Color(0f, 1f, 1f, state[alphaKey]),
                    topLeft = Offset(state[offset2] * size.width, 0.0f),
                    size = rectSize
                )

                drawRect(
                    Color(0f, 1f, 0f, state[alphaKey]),
                    topLeft = Offset(state[offset3] * size.width, 0.0f),
                    size = rectSize
                )
            }
        }
    }
}

@Composable
fun MovingTargetExample(clock: ManualAnimationClock) {
    val animValue = animatedFloat(0f)

    val dragObserver = object : DragObserver {
        override fun onDrag(dragDistance: Offset): Offset {
            animValue.snapTo(animValue.targetValue + dragDistance.x)
            return dragDistance
        }
    }

    val onPress: (Offset) -> Unit = { position ->
        animValue.animateTo(position.x, TweenSpec(durationMillis = 400))
    }

    DrawSeekBar(
        Modifier
            .rawDragGestureFilter(dragObserver)
            .pressIndicatorGestureFilter(onStart = onPress),
        animValue.value,
        clock
    )
}

@Composable
fun DrawSeekBar(modifier: Modifier = Modifier, x: Float, clock: ManualAnimationClock) {
    Canvas(modifier.fillMaxWidth().preferredHeight(60.dp)) {
        val xConstraint = x.coerceIn(0f, size.width)
        val clockTimeMillis = (400 * (x / size.width)).toLong().coerceIn(0, 399)
        if (clock.clockTimeMillis != clockTimeMillis) {
            clock.clockTimeMillis = clockTimeMillis
        }
        // draw bar
        val barHeight = 10.0f
        val offset = Offset(0.0f, center.y - 5)
        drawRect(
            Color.Gray,
            topLeft = offset,
            size = Size(size.width, barHeight)
        )
        drawRect(
            Color.Magenta,
            topLeft = offset,
            size = Size(xConstraint, barHeight)
        )

        // draw ticker
        drawCircle(
            Color.Magenta,
            center = Offset(xConstraint, center.y),
            radius = 40f
        )
    }
}

private val alphaKey = FloatPropKey()
private val offset1 = FloatPropKey()
private val offset2 = FloatPropKey()
private val offset3 = FloatPropKey()

private val transDef = transitionDefinition<String> {

    state("start") {
        this[alphaKey] = 1f
        this[offset1] = 0f
        this[offset2] = 0f
        this[offset3] = 0f
    }

    state("end") {
        this[alphaKey] = 0.2f
        this[offset1] = 0.26f
        this[offset2] = 0.53f
        this[offset3] = 0.8f
    }

    transition {
        alphaKey using tween(
            easing = FastOutSlowInEasing,
            durationMillis = 400
        )
        offset1 using tween(
            easing = FastOutSlowInEasing,
            durationMillis = 400
        )
        offset2 using tween(
            easing = FastOutSlowInEasing,
            durationMillis = 400
        )
        offset3 using tween(
            easing = FastOutSlowInEasing,
            durationMillis = 400
        )
    }
}
