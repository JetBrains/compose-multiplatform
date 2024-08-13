package visualeffects

import androidx.compose.ui.Modifier

actual fun Modifier.onPointerEvent(
    eventKind: PointerEventKind,
    onEvent: Position.() -> Unit
): Modifier = this.onPointerEventMobileImpl(eventKind, onEvent)
