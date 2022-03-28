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

package androidx.compose.foundation.text.selection

import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.gestures.forEachGesture
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.AwaitPointerEventScope
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.input.pointer.changedToDown
import androidx.compose.ui.input.pointer.isPrimaryPressed
import androidx.compose.ui.input.pointer.isShiftPressed
import androidx.compose.ui.platform.ViewConfiguration
import androidx.compose.ui.util.fastAll

// * Without shift it starts the new selection from the scratch.
// * With shift expand / shrink existed selection.
// * Click sets start and end of the selection, but shift click only the end of
// selection.
// * The specific case of it when selection is collapsed, but the same logic is
// applied for not collapsed selection too.
internal interface MouseSelectionObserver {
    // on start of shift click. if returns true event will be consumed
    fun onExtend(downPosition: Offset): Boolean
    // on drag after shift click. if returns true event will be consumed
    fun onExtendDrag(dragPosition: Offset): Boolean

    // if returns true event will be consumed
    fun onStart(downPosition: Offset, adjustment: SelectionAdjustment): Boolean
    fun onDrag(dragPosition: Offset, adjustment: SelectionAdjustment): Boolean
}

// Distance in pixels between consecutive click positions to be considered them as clicks sequence
internal const val ClicksSlop = 100.0

private class ClicksCounter(
    private val viewConfiguration: ViewConfiguration
) {
    var clicks = 0
    var prevClick: PointerInputChange? = null
    fun update(event: PointerEvent) {
        val currentPrevClick = prevClick
        val newClick = event.changes[0]
        if (currentPrevClick != null &&
            timeIsTolerable(currentPrevClick, newClick) &&
            positionIsTolerable(currentPrevClick, newClick)
        ) {
            clicks += 1
        } else {
            clicks = 1
        }
        prevClick = newClick
    }

    fun timeIsTolerable(prevClick: PointerInputChange, newClick: PointerInputChange): Boolean {
        val diff = newClick.uptimeMillis - prevClick.uptimeMillis
        return diff < viewConfiguration.doubleTapTimeoutMillis
    }

    fun positionIsTolerable(prevClick: PointerInputChange, newClick: PointerInputChange): Boolean {
        val diff = newClick.position - prevClick.position
        return diff.getDistance() < ClicksSlop
    }
}

internal suspend fun PointerInputScope.mouseSelectionDetector(
    observer: MouseSelectionObserver
) {
    forEachGesture {
        awaitPointerEventScope {
            val clicksCounter = ClicksCounter(viewConfiguration)
            while (true) {
                val down = awaitMouseEventDown()
                clicksCounter.update(down)
                val downChange = down.changes[0]
                if (down.keyboardModifiers.isShiftPressed) {
                    val started = observer.onExtend(downChange.position)
                    if (started) {
                        downChange.consume()
                        drag(downChange.id) {
                            if (observer.onExtendDrag(it.position)) {
                                it.consume()
                            }
                        }
                    }
                } else {
                    val selectionMode = when (clicksCounter.clicks) {
                        1 -> SelectionAdjustment.None
                        2 -> SelectionAdjustment.Word
                        else -> SelectionAdjustment.Paragraph
                    }
                    val started = observer.onStart(downChange.position, selectionMode)
                    if (started) {
                        downChange.consume()
                        drag(downChange.id) {
                            if (observer.onDrag(it.position, selectionMode)) {
                                it.consume()
                            }
                        }
                    }
                }
            }
        }
    }
}

private suspend fun AwaitPointerEventScope.awaitMouseEventDown(): PointerEvent {
    var event: PointerEvent
    do {
        event = awaitPointerEvent(PointerEventPass.Main)
    } while (
        !(
            event.buttons.isPrimaryPressed && event.changes.fastAll {
                it.type == PointerType.Mouse && it.changedToDown()
            }
            )
    )
    return event
}