package visualeffects

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent


@OptIn(ExperimentalComposeUiApi::class)
actual fun Modifier.onPointerEvent(
    eventKind: PointerEventKind,
    onEvent: Position.() -> Unit
): Modifier {

    val eventType: PointerEventType = when (eventKind) {
        PointerEventKind.Move -> PointerEventType.Move
        PointerEventKind.In -> PointerEventType.Enter
        PointerEventKind.Out -> PointerEventType.Exit
    }

    return this.onPointerEvent(eventType) {
        Position(
            it.changes.first().position.x.toInt(),
            it.changes.first().position.y.toInt()
        ).onEvent()
    }
}
