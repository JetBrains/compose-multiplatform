@file:Suppress("EXPERIMENTAL_IS_NOT_ENABLED")

package org.jetbrains.compose.codeeditor.codecompletion

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.isAltPressed
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import org.jetbrains.compose.codeeditor.keyevent.KeyEventHandlerImpl
import org.jetbrains.compose.codeeditor.keyevent.KeyModifier.ANY_MODIFIERS
import java.awt.event.KeyEvent.VK_ALT
import java.awt.event.KeyEvent.VK_CONTROL
import java.awt.event.KeyEvent.VK_META
import java.awt.event.KeyEvent.VK_SHIFT

@OptIn(ExperimentalComposeUiApi::class)
internal fun KeyEventHandlerImpl.onNavigationKey(
    up: () -> Boolean,
    down: () -> Boolean,
    left: (() -> Boolean)? = null,
    right: (() -> Boolean)? = null,
    pageUp: () -> Boolean,
    pageDown: () -> Boolean,
    home: () -> Boolean,
    end: () -> Boolean,
    hide: () -> Unit
): KeyEventHandlerImpl {
    mapOf(
        Key.DirectionUp to up,
        Key.DirectionDown to down,
        Key.DirectionLeft to left,
        Key.DirectionRight to right,
        Key.PageUp to pageUp,
        Key.PageDown to pageDown,
        Key.MoveHome to home,
        Key.MoveEnd to end
    ).forEach { (key, action) ->
        action?.let {
            addKeyDownAction(key) { it() }
        }
        addKeyDownAction(key, ANY_MODIFIERS) {
            hide()
            false
        }
    }
    return this
}

@OptIn(ExperimentalComposeUiApi::class)
internal fun KeyEventHandlerImpl.onEnter(
    consume: () -> Boolean,
    action: () -> Boolean,
    hide: () -> Unit
): KeyEventHandlerImpl {
    addKeyDownAction(Key.Enter) {
        if (consume()) {
            true
        } else {
            hide()
            false
        }
    }
    addKeyTypeAction('\n') { consume() }
    addKeyUpAction(Key.Enter) {
        if (consume()) {
            action()
        } else {
            hide()
            false
        }
    }
    return this
}

@OptIn(ExperimentalComposeUiApi::class)
internal fun KeyEventHandlerImpl.onTab(hide: () -> Unit): KeyEventHandlerImpl {
    addKeyDownAction(Key.Tab, ANY_MODIFIERS) {
        hide()
        false
    }
    return this
}

internal fun KeyEventHandlerImpl.onBackspace(action: () -> Boolean): KeyEventHandlerImpl {
    addKeyTypeAction(keyModifiers = intArrayOf(ANY_MODIFIERS)) {
        if (it.nativeKeyEvent.keyChar == '\b') {
            return@addKeyTypeAction action()
        }
        false
    }
    return this
}

internal fun KeyEventHandlerImpl.onCharacter(action: (Char) -> Boolean): KeyEventHandlerImpl {
    addKeyDownAction(keyModifiers = intArrayOf(ANY_MODIFIERS)) {
        action(it.nativeKeyEvent.keyChar)
    }
    return this
}

@OptIn(ExperimentalComposeUiApi::class)
internal fun KeyEventHandlerImpl.onEscape(hide: () -> Unit): KeyEventHandlerImpl {
    addKeyDownAction(Key.Escape) {
        hide()
        true
    }
    return this
}

@OptIn(ExperimentalComposeUiApi::class)
internal fun KeyEventHandlerImpl.onAnyOtherKeyExceptCharactersAndBackspace(
    noSuggestions: () -> Boolean,
    hide: () -> Unit
): KeyEventHandlerImpl {
    addKeyDownAction(keyModifiers = intArrayOf(ANY_MODIFIERS)) {
        if (noSuggestions()) {
            hide()
            return@addKeyDownAction false
        }
        val keyCode = it.nativeKeyEvent.keyCode
        if (keyCode == VK_SHIFT || keyCode == VK_CONTROL || keyCode == VK_ALT || keyCode == VK_META) {
            return@addKeyDownAction false
        } else if (it.isCtrlPressed || it.isAltPressed || it.isMetaPressed) {
            hide()
        } else {
            val keyChar = it.nativeKeyEvent.keyChar
            if (!keyChar.isLetterOrDigit() && keyChar != '_') {
                hide()
            }
        }
        false
    }
    addKeyTypeAction(keyModifiers = intArrayOf(ANY_MODIFIERS)) {
        val keyCode = it.nativeKeyEvent.keyCode
        if (keyCode == VK_SHIFT || keyCode == VK_CONTROL || keyCode == VK_ALT || keyCode == VK_META) {
            return@addKeyTypeAction false
        } else if (it.isShiftPressed || it.isCtrlPressed || it.isAltPressed || it.isMetaPressed) {
            hide()
        } else {
            if (it.key != Key.Backspace) {
                hide()
            }
        }
        false
    }
    return this
}
