package org.jetbrains.compose.web.events

import androidx.compose.web.events.SyntheticEvent
import org.w3c.dom.events.EventTarget
import org.w3c.dom.events.KeyboardEvent

class SyntheticKeyboardEvent internal constructor(
    nativeEvent: KeyboardEvent
) : SyntheticEvent<EventTarget>(nativeEvent) {

    private val keyboardEvent = nativeEvent

    val altKey: Boolean = nativeEvent.altKey
    val code: String = nativeEvent.code
    val ctrlKey: Boolean = nativeEvent.ctrlKey
    val isComposing: Boolean = nativeEvent.isComposing
    val key: String = nativeEvent.key
    val locale: String = nativeEvent.asDynamic().locale.toString()
    val location: Int = nativeEvent.location
    val metaKey: Boolean = nativeEvent.metaKey
    val repeat: Boolean = nativeEvent.repeat
    val shiftKey: Boolean = nativeEvent.shiftKey

    fun getModifierState(keyArg: String): Boolean = keyboardEvent.getModifierState(keyArg)

    fun getNormalizedKey(): String = key.let {
        normalizedKeys[it] ?: it
    }
}

private val normalizedKeys = mapOf(
    "Esc" to "Escape",
    "Spacebar" to " ",
    "Left" to "ArrowLeft",
    "Up" to "ArrowUp",
    "Right" to "ArrowRight",
    "Down" to "ArrowDown",
    "Del" to "Delete",
    "Apps" to "ContextMenu",
    "Menu" to "ContextMenu",
    "Scroll" to "ScrollLock",
    "MozPrintableKey" to "Unidentified",
)
// Firefox bug for Windows key https://bugzilla.mozilla.org/show_bug.cgi?id=1232918
