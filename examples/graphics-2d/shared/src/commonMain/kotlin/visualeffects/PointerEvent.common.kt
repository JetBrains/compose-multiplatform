package visualeffects

import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.forEachGesture
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.pointerInput


enum class PointerEventKind {
    Move,
    In,
    Out
}

class Position(val x: Int, val y: Int)

expect fun Modifier.onPointerEvent(
    eventKind: PointerEventKind,
    onEvent: Position.() -> Unit
): Modifier

fun Modifier.onPointerEventMobileImpl(
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
                        Position(
                            event.changes.first().position.x.toInt(),
                            event.changes.first().position.y.toInt()
                        ).onEvent()
                    }

                } while (event.changes.any { it.pressed })

                if (eventKind == PointerEventKind.Out) {
                    Position(0, 0).onEvent()
                }
            }
        }
    }
}
