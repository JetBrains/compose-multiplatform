package org.jetbrains.compose.codeeditor.keyevent

import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isAltPressed
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type

internal class KeyEventHandlerImpl(
    val name: String = ""
) : KeyEventHandler {
    companion object {
        private const val PROCESS_ALL_CHARS: Char = 0.toChar()
        private val PROCESS_ANY_KEY: Key = Key(-2000000000)
    }

    private val keyDownActions = HashMap<Key, MutableList<KeyEventAction>>()
    private val keyUpActions = HashMap<Key, MutableList<KeyEventAction>>()
    private val keyTypedActions = HashMap<Char, MutableList<KeyEventAction>>()
    private val eventPreHandlers = ArrayList<KeyEventHandler>()
    private val eventPostHandlers = ArrayList<KeyEventHandler>()

    override fun onKeyEvent(event: KeyEvent): Boolean {
        eventPreHandlers.forEach { if (it.onKeyEvent(event)) return true }

        val keyModifiers = getKeyModifiers(event)

        if (
            when (event.type) {
                KeyEventType.KeyDown ->
                    processKeyEvent(
                        keyEventActions = keyDownActions.getOrDefault(event.key,
                            keyDownActions.getOrDefault(PROCESS_ANY_KEY,
                                emptyList())),
                        modifiers = keyModifiers,
                        event = event
                    )

                KeyEventType.KeyUp ->
                    processKeyEvent(
                        keyEventActions = keyUpActions.getOrDefault(event.key,
                            keyUpActions.getOrDefault(PROCESS_ANY_KEY,
                                emptyList())),
                        modifiers = keyModifiers,
                        event = event
                    )

                KeyEventType.Unknown -> {
                    return if (event.nativeKeyEvent.id == java.awt.event.KeyEvent.KEY_TYPED) {
                        processKeyEvent(
                            keyEventActions = keyTypedActions.getOrDefault(event.nativeKeyEvent.keyChar,
                                keyTypedActions.getOrDefault(PROCESS_ALL_CHARS,
                                    emptyList())),
                            modifiers = keyModifiers,
                            event = event
                        )
                    } else {
                        false
                    }
                }

                else -> false
            }
        ) return true

        eventPostHandlers.forEach { if (it.onKeyEvent(event)) return true }

        return false
    }

    fun addPreEventHandler(handler: KeyEventHandler): KeyEventHandlerImpl {
        eventPreHandlers.add(handler)
        return this
    }

    fun addPostEventHandler(handler: KeyEventHandler): KeyEventHandlerImpl {
        eventPostHandlers.add(handler)
        return this
    }

    fun addKeyDownAction(
        key: Key = PROCESS_ANY_KEY,
        vararg keyModifiers: Int = intArrayOf(0),
        action: (event: KeyEvent) -> Boolean = { true }
    ): KeyEventHandlerImpl {
        keyModifiers.forEach {
            keyDownActions.getOrPut(key, ::ArrayList).add(KeyEventAction(action, it))
        }
        return this
    }

    fun addKeyUpAction(
        key: Key = PROCESS_ANY_KEY,
        vararg keyModifiers: Int = intArrayOf(0),
        action: (event: KeyEvent) -> Boolean = { true }
    ): KeyEventHandlerImpl {
        keyModifiers.forEach {
            keyUpActions.getOrPut(key, ::ArrayList).add(KeyEventAction(action, it))
        }
        return this
    }

    fun addKeyTypeAction(
        ch: Char = PROCESS_ALL_CHARS,
        vararg keyModifiers: Int = intArrayOf(0),
        action: (event: KeyEvent) -> Boolean = { true }
    ): KeyEventHandlerImpl {
        keyModifiers.forEach {
            keyTypedActions.getOrPut(ch, ::ArrayList).add(KeyEventAction(action, it))
        }
        return this
    }

    private fun processKeyEvent(
        keyEventActions: List<KeyEventAction>,
        modifiers: Int = 0,
        event: KeyEvent
    ): Boolean {
        val eventActions = keyEventActions.filter { it.keyModifiers == modifiers || it.keyModifiers == KeyModifier.ANY_MODIFIERS }
        eventActions.forEach {
            if (it.action(event)) return true
        }
        return false
    }

    private fun getKeyModifiers(e: KeyEvent): Int {
        var modifier = 0
        if (e.isShiftPressed) modifier += KeyModifier.SHIFT
        if (e.isCtrlPressed) modifier += KeyModifier.CTRL
        if (e.isAltPressed) modifier += KeyModifier.ALT
        if (e.isMetaPressed) modifier += KeyModifier.META
        return modifier
    }
}

private class KeyEventAction(
    val action: (event: KeyEvent) -> Boolean,
    val keyModifiers: Int
)

internal object KeyModifier {
    const val NO_MODIFIERS = 0
    const val SHIFT = 1
    const val CTRL = 2
    const val ALT = 4
    const val META = 8
    const val ANY_MODIFIERS = -1
}
