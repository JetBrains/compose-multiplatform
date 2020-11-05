/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.compose.foundation.demos

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.tapGestureDetector
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.preferredHeight
import androidx.compose.foundation.layout.preferredSize
import androidx.compose.foundation.layout.size
import androidx.compose.integration.demos.common.ComposableDemo
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.gesture.ExperimentalPointerInput
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import kotlin.random.Random

val CoroutineGestureDemos = listOf(
    ComposableDemo("Tap/Double-Tap/Long Press") { CoroutineTapDemo() }
)

fun hueToColor(hue: Float): Color {
    val huePrime = hue / 60
    val hueRange = huePrime.toInt()
    val hueRemainder = huePrime - hueRange
    return when (hueRange) {
        0 -> Color(1f, hueRemainder, 0f)
        1 -> Color(1f - hueRemainder, 1f, 0f)
        2 -> Color(0f, 1f, hueRemainder)
        3 -> Color(0f, 1f - hueRemainder, 1f)
        4 -> Color(hueRemainder, 0f, 1f)
        else -> Color(1f, 0f, 1f - hueRemainder)
    }
}

fun randomHue() = Random.nextFloat() * 360

fun anotherRandomHue(hue: Float): Float {
    val newHue: Float = Random.nextFloat() * 260f

    // we don't want the hue to be close, so we ensure that it isn't with 50 of the current hue
    return if (newHue > hue - 50f) {
        newHue + 100f
    } else {
        newHue
    }
}
/**
 * Gesture detector for tap, double-tap, and long-press.
 */
@OptIn(ExperimentalPointerInput::class)
@Composable
fun CoroutineTapDemo() {
    var tapHue by remember { mutableStateOf(randomHue()) }
    var longPressHue by remember { mutableStateOf(randomHue()) }
    var doubleTapHue by remember { mutableStateOf(randomHue()) }
    var pressHue by remember { mutableStateOf(randomHue()) }
    var releaseHue by remember { mutableStateOf(randomHue()) }
    var cancelHue by remember { mutableStateOf(randomHue()) }

    Column {
        Text("The boxes change color when you tap the white box.")
        Spacer(Modifier.size(5.dp))
        Box(
            Modifier
                .fillMaxWidth()
                .preferredHeight(50.dp)
                .pointerInput {
                    tapGestureDetector(
                        onTap = { tapHue = anotherRandomHue(tapHue) },
                        onDoubleTap = { doubleTapHue = anotherRandomHue(doubleTapHue) },
                        onLongPress = { longPressHue = anotherRandomHue(longPressHue) },
                        onPress = {
                            pressHue = anotherRandomHue(pressHue)
                            if (tryAwaitRelease()) {
                                releaseHue = anotherRandomHue(releaseHue)
                            } else {
                                cancelHue = anotherRandomHue(cancelHue)
                            }
                        }
                    )
                }
                .background(Color.White)
                .border(BorderStroke(2.dp, Color.Black))
        ) {
            Text("Tap, double-tap, or long-press", Modifier.align(Alignment.Center))
        }
        Spacer(Modifier.size(5.dp))
        Row {
            Box(
                Modifier
                    .preferredSize(50.dp)
                    .background(hueToColor(tapHue))
                    .border(BorderStroke(2.dp, Color.Black))
            )
            Text("Changes color on tap", Modifier.align(Alignment.CenterVertically))
        }
        Spacer(Modifier.size(5.dp))
        Row {
            Box(
                Modifier
                    .preferredSize(50.dp)
                    .clipToBounds()
                    .background(hueToColor(doubleTapHue))
                    .border(BorderStroke(2.dp, Color.Black))
            )
            Text("Changes color on double-tap", Modifier.align(Alignment.CenterVertically))
        }
        Spacer(Modifier.size(5.dp))
        Row {
            Box(
                Modifier
                    .preferredSize(50.dp)
                    .clipToBounds()
                    .background(hueToColor(longPressHue))
                    .border(BorderStroke(2.dp, Color.Black))
            )
            Text("Changes color on long press", Modifier.align(Alignment.CenterVertically))
        }
        Spacer(Modifier.size(5.dp))
        Row {
            Box(
                Modifier
                    .preferredSize(50.dp)
                    .clipToBounds()
                    .background(hueToColor(pressHue))
                    .border(BorderStroke(2.dp, Color.Black))
            )
            Text("Changes color on press", Modifier.align(Alignment.CenterVertically))
        }
        Spacer(Modifier.size(5.dp))
        Row {
            Box(
                Modifier
                    .preferredSize(50.dp)
                    .clipToBounds()
                    .background(hueToColor(releaseHue))
                    .border(BorderStroke(2.dp, Color.Black))
            )
            Text("Changes color on release", Modifier.align(Alignment.CenterVertically))
        }
        Spacer(Modifier.size(5.dp))
        Row {
            Box(
                Modifier
                    .preferredSize(50.dp)
                    .clipToBounds()
                    .background(hueToColor(cancelHue))
                    .border(BorderStroke(2.dp, Color.Black))
            )
            Text("Changes color on cancel", Modifier.align(Alignment.CenterVertically))
        }
    }
}