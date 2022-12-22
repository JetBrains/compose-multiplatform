package org.jetbrains.compose.demo.visuals.platform

import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.forEachGesture
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.*

actual fun Modifier.onPointerEvent(
    eventKind: PointerEventKind,
    onEvent: Position.() -> Unit
): Modifier {

    return this.pointerInput(Unit) {
        forEachGesture {

            awaitPointerEventScope {

                awaitFirstDown()
                if (eventKind == PointerEventKind.In) {
                    Position(0, 0).onEvent()
                    return@awaitPointerEventScope
                }

                do {

                    val event: PointerEvent = awaitPointerEvent()

                    if (eventKind == PointerEventKind.Move) {
                        Position(event.changes.first().position.x.toInt(), event.changes.first().position.y.toInt()).onEvent()
                    }

                } while (event.changes.any { it.pressed })

                if (eventKind == PointerEventKind.Out) {
                    Position(0, 0).onEvent()
                }
            }
        }
    }
}
