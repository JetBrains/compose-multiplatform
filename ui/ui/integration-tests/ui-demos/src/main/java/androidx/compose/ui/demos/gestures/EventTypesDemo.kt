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
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.consumeAllChanges
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
private fun DrawEvents(events: List<PointerEventType>, counts: List<Int>) {
    for (i in events.lastIndex downTo 0) {
        val type = events[i]
        val count = counts[i]

        val color = when (type) {
            PointerEventType.Press -> Color.Red
            PointerEventType.Move -> Color(0xFFFFA500) // Orange
            PointerEventType.Release -> Color.Yellow
            PointerEventType.Enter -> Color.Green
            PointerEventType.Exit -> Color.Blue
            else -> Color(0xFF800080) // Purple
        }
        TextItem("$type $count", color)
    }
}

/**
 * Demo to show the event types that are sent
 */
@Composable
fun EventTypesDemo() {
    val innerPointerEventTypes = remember { mutableStateListOf<PointerEventType>() }
    val innerPointerEventCounts = remember { mutableStateListOf<Int>() }
    val outerPointerEventTypes = remember { mutableStateListOf<PointerEventType>() }
    val outerPointerEventCounts = remember { mutableStateListOf<Int>() }
    Box(
        Modifier.pointerInput(Unit) {
            awaitPointerEventScope {
                while (true) {
                    val event = awaitPointerEvent()
                    event.changes.forEach { it.consumeAllChanges() }
                    addEvent(event, outerPointerEventTypes, outerPointerEventCounts)
                }
            }
        }
    ) {
        Column {
            DrawEvents(outerPointerEventTypes, outerPointerEventCounts)
        }
        Column(
            Modifier
                .align(Alignment.CenterEnd)
                .requiredSize(200.dp)
                .border(2.dp, Color.Black)
                .clipToBounds()
                .pointerInput(Unit) {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent()
                            addEvent(event, innerPointerEventTypes, innerPointerEventCounts)
                        }
                    }
                }
        ) {
            DrawEvents(innerPointerEventTypes, innerPointerEventCounts)
        }
    }
}

private fun addEvent(
    event: PointerEvent,
    events: MutableList<PointerEventType>,
    counts: MutableList<Int>
) {
    event.changes.forEach { it.consumeAllChanges() }
    if (events.lastOrNull() == event.type) {
        counts[counts.lastIndex]++
    } else {
        events += event.type
        counts += 1
    }

    while (events.size > 100) {
        events.removeAt(0)
        counts.removeAt(0)
    }
}