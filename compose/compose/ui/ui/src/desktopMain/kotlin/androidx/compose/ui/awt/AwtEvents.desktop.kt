package androidx.compose.ui.awt

import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.pointer.PointerEvent

/**
 * The original raw native event from AWT.
 *
 * Null if:
 * - the native event is sent by another framework (when Compose UI is embed into it)
 * - there is no native event (in tests, for example)
 * - there was a synthetic move event sent by compose on re-layout
 * - there was a synthetic move event sent by compose when move is missing between two non-move events
 *
 * Always check for null when you want to handle the native event.
 */
val PointerEvent.awtEventOrNull: java.awt.event.MouseEvent? get() {
    return nativeEvent as? java.awt.event.MouseEvent
}

/**
 * The original raw native event from AWT.
 *
 * Null if:
 * - the native event is sent by another framework (when Compose UI is embed into it)
 * - there is no native event (in tests, for example)
 *
 * Always check for null when you want to handle the native event.
 */
val KeyEvent.awtEventOrNull: java.awt.event.KeyEvent? get() {
    return nativeKeyEvent as? java.awt.event.KeyEvent
}