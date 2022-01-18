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

package androidx.compose.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.toOffset
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import com.google.common.truth.Truth.assertThat

fun Events.assertReceivedNoEvents() = assertThat(list).isEmpty()

fun Events.assertReceived(type: PointerEventType, offset: Offset) =
    received().assertHas(type, offset)

fun Events.assertReceivedLast(type: PointerEventType, offset: Offset) =
    receivedLast().assertHas(type, offset)

fun PointerEvent.assertHas(type: PointerEventType, offset: Offset) {
    assertThat(type).isEqualTo(type)
    assertThat(changes.first().position).isEqualTo(offset)
}

class Events {
    val list = mutableListOf<PointerEvent>()

    fun add(event: PointerEvent) {
        list.add(event)
    }

    fun receivedLast(): PointerEvent {
        require(list.isNotEmpty()) { "The were no events" }
        val event = list.removeFirst()
        require(list.isEmpty()) { "The event $event isn't the last" }
        return event
    }

    fun received(): PointerEvent {
        require(list.isNotEmpty()) { "The were no events" }
        val event = list.removeFirst()
        require(list.isNotEmpty()) { "The event $event is the last" }
        return event
    }
}

class FillBox {
    val events = Events()

    @Composable
    fun Content() {
        Box(
            Modifier
                .fillMaxSize()
                .collectEvents(events)
        )
    }
}

class PopupState(
    val bounds: IntRect,
    private val focusable: Boolean = false,
    private val onDismissRequest: () -> Unit = {}
) {
    val origin get() = bounds.topLeft.toOffset()
    val events = Events()

    @Composable
    fun Content() {
        Popup(
            popupPositionProvider = object : PopupPositionProvider {
                override fun calculatePosition(
                    anchorBounds: IntRect,
                    windowSize: IntSize,
                    layoutDirection: LayoutDirection,
                    popupContentSize: IntSize
                ) = bounds.topLeft
            },
            focusable = focusable,
            onDismissRequest = onDismissRequest
        ) {
            with(LocalDensity.current) {
                Box(
                    Modifier
                        .requiredSize(bounds.width.toDp(), bounds.height.toDp())
                        .collectEvents(events)
                )
            }
        }
    }
}

fun Modifier.collectEvents(events: Events) = pointerInput(Unit) {
    awaitPointerEventScope {
        while (true) {
            events.add(awaitPointerEvent())
        }
    }
}