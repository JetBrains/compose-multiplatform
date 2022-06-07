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

package androidx.compose.ui.demos.gestures

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
private fun TextItem(text: String, color: Color) {
    Row {
        Box(Modifier.size(25.dp).background(color))
        Spacer(Modifier.width(5.dp))
        Text(text, fontSize = 20.sp)
    }
}

@Composable
private fun DrawEvents(events: List<Pair<PointerEventType, Any>>) {
    for (i in events.lastIndex downTo 0) {
        val (type, value) = events[i]

        val color = when (type) {
            PointerEventType.Press -> Color.Red
            PointerEventType.Move -> Color(0xFFFFA500) // Orange
            PointerEventType.Release -> Color.Yellow
            PointerEventType.Enter -> Color.Green
            PointerEventType.Exit -> Color.Blue
            PointerEventType.Scroll -> Color(0xFF800080) // Purple
            else -> Color.Black
        }
        TextItem("$type $value", color)
    }
}

/**
 * Demo to show the event types that are sent
 */
@Composable
fun EventTypesDemo() {
    val innerPointerEvents = remember { mutableStateListOf<Pair<PointerEventType, Any>>() }
    val outerPointerEvents = remember { mutableStateListOf<Pair<PointerEventType, Any>>() }
    Box(
        Modifier.pointerInput(Unit) {
            awaitPointerEventScope {
                while (true) {
                    val event = awaitPointerEvent()
                    event.changes.forEach { it.consume() }
                    addEvent(event, outerPointerEvents)
                }
            }
        }
    ) {
        Column {
            DrawEvents(outerPointerEvents)
        }
        Column(
            Modifier.size(200.dp)
                .border(2.dp, Color.Black)
                .align(Alignment.CenterEnd)
                .clipToBounds()
                .pointerInput(Unit) {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent()
                            addEvent(event, innerPointerEvents)
                        }
                    }
                }
        ) {
            DrawEvents(innerPointerEvents)
        }
    }
}

private fun addEvent(
    event: PointerEvent,
    events: MutableList<Pair<PointerEventType, Any>>,
) {
    event.changes.forEach { it.consume() }
    val scrollTotal = event.changes.foldRight(Offset.Zero) { c, acc -> acc + c.scrollDelta }
    if (events.lastOrNull()?.first == event.type) {
        val (type, value) = events.last()
        if (type == PointerEventType.Scroll) {
            events[events.lastIndex] = type to ((value as Offset) + scrollTotal)
        } else {
            events[events.lastIndex] = type to ((value as Int) + 1)
        }
    } else if (event.type == PointerEventType.Scroll) {
        events += event.type to scrollTotal
    } else {
        events += event.type to 1
    }

    while (events.size > 100) {
        events.removeAt(0)
    }
}