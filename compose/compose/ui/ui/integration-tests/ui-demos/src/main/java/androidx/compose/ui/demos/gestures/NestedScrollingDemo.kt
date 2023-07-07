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

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.rememberScrollableState
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.samples.NestedScrollDispatcherSample
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

/**
 * Demonstration for how multiple DragGestureDetectors interact.
 */
@Composable
fun NestedScrollingDemo() {
    Column {
        Text("Demonstrates nested scrolling.")
        Text(
            "There are 3 fake vertical scrollers inside another vertical scroller.  Try " +
                "scrolling with 1 or many fingers."
        )
        ScrollableContainer {
            RepeatingColumn(repetitions = 3) {
                Box(Modifier.height(398.dp).padding(72.dp)) {
                    // Inner composable that scrolls
                    ScrollableContainer {
                        RepeatingColumn(repetitions = 5) {
                            // Composable that indicates it is being pressed
                            Pressable(
                                height = 72.dp
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * A very simple ScrollView like implementation that allows for vertical scrolling.
 */
@Composable
private fun ScrollableContainer(content: @Composable () -> Unit) {
    val offset = remember { mutableStateOf(0f) }
    val maxOffset = remember { mutableStateOf(0f) }

    Layout(
        content = content,
        modifier = Modifier
            .scrollable(
                orientation = Orientation.Vertical,
                state = rememberScrollableState { scrollDistance ->
                    val resultingOffset = offset.value + scrollDistance
                    val dyToConsume =
                        when {
                            resultingOffset > 0f -> {
                                0f - offset.value
                            }
                            resultingOffset < maxOffset.value -> {
                                maxOffset.value - offset.value
                            }
                            else -> {
                                scrollDistance
                            }
                        }
                    offset.value += dyToConsume
                    dyToConsume
                }
            )
            .clipToBounds(),
        measurePolicy = { measurables, constraints ->
            val placeable =
                measurables.first()
                    .measure(constraints.copy(minHeight = 0, maxHeight = Constraints.Infinity))

            maxOffset.value = (constraints.maxHeight - placeable.height).toFloat()

            layout(constraints.maxWidth, constraints.maxHeight) {
                placeable.placeRelative(0, offset.value.roundToInt())
            }
        }
    )
}

/**
 * A very simple Button like implementation that visually indicates when it is being pressed.
 */
@Composable
private fun Pressable(
    height: Dp
) {

    val pressedColor = PressedColor
    val defaultColor = DefaultBackgroundColor

    val color = remember { mutableStateOf(defaultColor) }
    val showPressed = remember { mutableStateOf(false) }

    val onPress: (Offset) -> Unit = {
        showPressed.value = true
    }

    val onRelease = {
        showPressed.value = false
    }

    val onTap: (Offset) -> Unit = {
        color.value = color.value.next()
    }

    val onDoubleTap: (Offset) -> Unit = {
        color.value = color.value.prev()
    }

    val onLongPress = { _: Offset ->
        color.value = defaultColor
        showPressed.value = false
    }

    val gestureDetectors =
        Modifier
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        onPress.invoke(it)
                        val success = tryAwaitRelease()
                        if (success) onRelease.invoke() else onRelease.invoke()
                    }
                )
            }
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = onTap,
                    onDoubleTap = onDoubleTap,
                    onLongPress = onLongPress
                )
            }

    val layout = Modifier.fillMaxWidth().height(height)

    val pressOverlay =
        if (showPressed.value) Modifier.background(color = pressedColor) else Modifier
    Box(gestureDetectors.then(layout).background(color = color.value).then(pressOverlay))
}

/**
 * A simple composable that repeats its children as a vertical list of divided items [repetitions]
 * times.
 */
@Composable
private fun RepeatingColumn(repetitions: Int, content: @Composable () -> Unit) {
    Column(Modifier.border(border = BorderStroke(2.dp, BorderColor))) {
        for (i in 1..repetitions) {
            content()
            if (i != repetitions) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(Color(0f, 0f, 0f, .12f))
                )
            }
        }
    }
}

@Composable
fun NestedScrollDispatchDemo() {
    LazyColumn {
        items(5) {
            Text("I'm text $it", modifier = Modifier.padding(16.dp))
        }
        item {
            NestedScrollDispatcherSample()
        }
        items(30) {
            Text("I'm text $it", modifier = Modifier.padding(16.dp))
        }
    }
}