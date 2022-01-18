package androidx.compose.ui.awt

import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.pointer.PointerEvent

/**
 * The original raw native event from AWT
 */
@Deprecated(
    "Use awtEventOrNull. `awtEvent` will be removed in Compose 1.3",
    replaceWith = ReplaceWith("awtEventOrNull")
)
val PointerEvent.awtEvent: java.awt.event.MouseEvent get() {
    require(nativeEvent is java.awt.event.MouseEvent) {
        "nativeEvent wasn't sent by AWT. Make sure, that you use AWT backed API" +
                " (from androidx.compose.ui.awt.* or from androidx.compose.ui.window.*)"
    }
    return nativeEvent
}

/**
 * The original raw native event from AWT
 */
@Deprecated(
    "Use awtEventOrNull. `awtEvent` will be removed in Compose 1.3",
    replaceWith = ReplaceWith("awtEventOrNull")
)
val KeyEvent.awtEvent: java.awt.event.KeyEvent get() {
    require(nativeKeyEvent is java.awt.event.KeyEvent) {
        "nativeKeyEvent wasn't sent by AWT. Make sure, that you use AWT backed API" +
                " (from androidx.compose.ui.awt.* or from androidx.compose.ui.window.*)"
    }
    return nativeKeyEvent
}

/**
 * The original raw native event from AWT.
 *
 * Null if:
 * - the native event is sent by another framework (when Compose UI is embed into it)
 * - there no native event (in tests, for example)
 * - there was a synthetic move event sent by compose on relayout
 * - there was a synthetic move event sent by compose when move is missing between two non-move events
 */
@Suppress("DEPRECATION")
val PointerEvent.awtEventOrNull: java.awt.event.MouseEvent? get() {
    if (nativeEvent is SyntheticMouseEvent) return null
    return nativeEvent as? java.awt.event.MouseEvent
}

/**
 * The original raw native event from AWT.
 *
 * Null if:
 * - the native event is sent by another framework (when Compose UI is embed into it)
 * - there no native event (in tests, for example)
 */
val KeyEvent.awtEventOrNull: java.awt.event.KeyEvent? get() {
    return nativeKeyEvent as? java.awt.event.KeyEvent
}