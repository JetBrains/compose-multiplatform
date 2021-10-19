package org.jetbrains.compose.codeeditor.search

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.input.key.Key
import org.jetbrains.compose.codeeditor.keyevent.KeyEventHandlerImpl
import org.jetbrains.compose.codeeditor.keyevent.KeyModifier.CTRL
import org.jetbrains.compose.codeeditor.keyevent.KeyModifier.SHIFT

@OptIn(ExperimentalComposeUiApi::class)
internal fun KeyEventHandlerImpl.onEscape(action: () -> Unit): KeyEventHandlerImpl {
    addKeyDownAction(Key.Escape) {
        action()
        true
    }
    return this
}

@OptIn(ExperimentalComposeUiApi::class)
internal fun KeyEventHandlerImpl.onCtrlEnter(action: () -> Unit): KeyEventHandlerImpl {
    addKeyDownAction(Key.Enter, CTRL) {
        action()
        true
    }
    return this
}

@OptIn(ExperimentalComposeUiApi::class)
internal fun KeyEventHandlerImpl.onF3(action: () -> Unit): KeyEventHandlerImpl {
    addKeyDownAction(Key.F3) {
        action()
        true
    }
    return this
}

@OptIn(ExperimentalComposeUiApi::class)
internal fun KeyEventHandlerImpl.onShiftF3(action: () -> Unit): KeyEventHandlerImpl {
    addKeyDownAction(Key.F3, SHIFT) {
        action()
        true
    }
    return this
}

@OptIn(ExperimentalComposeUiApi::class)
internal fun KeyEventHandlerImpl.onEnter(action: () -> Unit): KeyEventHandlerImpl {
    addKeyDownAction(Key.Enter) {
        action()
        true
    }
    addKeyTypeAction('\n')
    return this
}

@OptIn(ExperimentalComposeUiApi::class)
internal fun KeyEventHandlerImpl.onShiftEnter(action: () -> Unit): KeyEventHandlerImpl {
    addKeyDownAction(Key.Enter, SHIFT) {
        action()
        true
    }
    addKeyTypeAction('\n', SHIFT)
    return this
}

@OptIn(ExperimentalComposeUiApi::class)
internal fun KeyEventHandlerImpl.onUp(action: () -> Unit): KeyEventHandlerImpl {
    addKeyDownAction(Key.DirectionUp) {
        action()
        true
    }
    return this
}

@OptIn(ExperimentalComposeUiApi::class)
internal fun KeyEventHandlerImpl.onDown(action: () -> Unit): KeyEventHandlerImpl {
    addKeyDownAction(Key.DirectionDown) {
        action()
        true
    }
    return this
}
