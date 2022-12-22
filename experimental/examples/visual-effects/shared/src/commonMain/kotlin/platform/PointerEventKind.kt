package org.jetbrains.compose.demo.visuals.platform

import androidx.compose.ui.Modifier

enum class PointerEventKind {
    Move,
    In,
    Out
}

class Position(val x: Int, val y: Int)

expect fun Modifier.onPointerEvent(eventKind: PointerEventKind, onEvent: Position.() -> Unit): Modifier
