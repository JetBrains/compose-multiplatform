/*
 * Copyright 2022 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.compose.foundation

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.PointerKeyboardModifiers
import androidx.compose.ui.input.pointer.pointerInput

/**
 * Configure component to receive clicks, double clicks and long clicks via input only (no accessibility "click" event)
 * within the component's bounds.
 *
 * It allows configuration based on a pointer type via [matcher].
 * By default, matcher uses [PointerMatcher.Primary].
 * [matcher] should declare supported pointer types (mouse, touch, stylus, eraser) by listing them and
 * declaring required properties for them, such as: required button (primary, secondary, etc.).
 *
 * Consider using [clickable] if it's necessary to handle only primary clicks. Unlike [clickable],
 * [onClick] doesn't add [Modifier.indication], [Modifier.hoverable], [Modifier.focusable], click by Enter key, etc.
 * If necessary, one has to add those manually when using [onClick].
 *
 * @param enabled Controls the enabled state. When `false`, [onClick], [onLongClick] or
 * [onDoubleClick] won't be invoked
 * @param matcher defines supported pointer types and required properties
 * @param keyboardModifiers defines a condition that [PointerEvent.keyboardModifiers] has to match
 * @param onLongClick will be called when user long presses on the element
 * @param onDoubleClick will be called when user double clicks on the element
 * @param onClick will be called when user clicks on the element
 */
@ExperimentalFoundationApi
fun Modifier.onClick(
    enabled: Boolean = true,
    matcher: PointerMatcher = PointerMatcher.Primary,
    keyboardModifiers: PointerKeyboardModifiers.() -> Boolean = { true },
    onDoubleClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
    onClick: () -> Unit
) = composed {
    Modifier.onClick(
        enabled = enabled,
        matcher = matcher,
        keyboardModifiers = keyboardModifiers,
        interactionSource = remember { MutableInteractionSource() },
        onDoubleClick = onDoubleClick,
        onLongClick = onLongClick,
        onClick = onClick
    )
}

/**
 * Configure component to receive clicks, double clicks and long clicks via input only (no accessibility "click" event)
 * within the component's bounds.
 *
 * It allows configuration based on a pointer type via [matcher].
 * By default, matcher uses [PointerMatcher.Primary].
 * [matcher] should declare supported pointer types (mouse, touch, stylus, eraser) by listing them and
 * declaring required properties for them, such as: required button (primary, secondary, etc.).
 *
 * Consider using [clickable] if it's necessary to handle only primary clicks. Unlike [clickable],
 * [onClick] doesn't add [Modifier.indication], [Modifier.hoverable], [Modifier.focusable], click by Enter key, etc.
 * If necessary, one has to add those manually when using [onClick].
 *
 * @param interactionSource [MutableInteractionSource] that will be used to emit
 * [PressInteraction.Press] when this clickable is pressed. Only the initial (first) press will be
 * recorded and emitted with [MutableInteractionSource].
 * @param enabled Controls the enabled state. When `false`, [onClick], [onLongClick] or
 * [onDoubleClick] won't be invoked
 * @param matcher defines supported pointer types and required properties
 * @param keyboardModifiers defines a condition that [PointerEvent.keyboardModifiers] has to match
 * @param onLongClick will be called when user long presses on the element
 * @param onDoubleClick will be called when user double clicks on the element
 * @param onClick will be called when user clicks on the element
 */
@ExperimentalFoundationApi
fun Modifier.onClick(
    enabled: Boolean = true,
    interactionSource: MutableInteractionSource,
    matcher: PointerMatcher = PointerMatcher.Primary,
    keyboardModifiers: PointerKeyboardModifiers.() -> Boolean = { true },
    onDoubleClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
    onClick: () -> Unit
) = composed(
    inspectorInfo = {
        name = "onClick"
        properties["enabled"] = enabled
        properties["matcher"] = matcher
        properties["keyboardModifiers"] = keyboardModifiers
        properties["onDoubleClick"] = onDoubleClick
        properties["onLongClick"] = onLongClick
        properties["onClick"] = onClick
        properties["interactionSource"] = interactionSource
    },
    factory = {

        val gestureModifier = if (enabled) {
            val pressedInteraction = remember { mutableStateOf<PressInteraction.Press?>(null) }
            val onClickState = rememberUpdatedState(onClick)
            val on2xClickState = rememberUpdatedState(onDoubleClick)
            val onLongClickState = rememberUpdatedState(onLongClick)
            val keyboardModifiersState = rememberUpdatedState(keyboardModifiers)
            val focusRequester = remember { FocusRequester() }

            val hasLongClick = onLongClick != null
            val hasDoubleClick = onDoubleClick != null

            DisposableEffect(hasLongClick) {
                onDispose {
                    pressedInteraction.value?.let { oldValue ->
                        val interaction = PressInteraction.Cancel(oldValue)
                        interactionSource.tryEmit(interaction)
                        pressedInteraction.value = null
                    }
                }
            }
            PressedInteractionSourceDisposableEffect(
                interactionSource = interactionSource, pressedInteraction = pressedInteraction
            )

            val matcherState = rememberUpdatedState(matcher)

            Modifier.pointerInput(interactionSource, hasLongClick, hasDoubleClick) {
                detectTapGestures(
                    matcher = matcherState.value,
                    keyboardModifiers = {
                        keyboardModifiersState.value(this)
                    },
                    onDoubleTap = if (hasDoubleClick) {
                        {
                            focusRequester.requestFocus()
                            on2xClickState.value!!.invoke()
                        }
                    } else {
                        null
                    },
                    onLongPress = if (hasLongClick) {
                        {
                            focusRequester.requestFocus()
                            onLongClickState.value!!.invoke()
                        }
                    } else {
                        null
                    },
                    onTap = {
                        focusRequester.requestFocus()
                        onClickState.value()
                    },
                    onPress = {
                        handlePressInteraction(
                            pressPoint = it,
                            interactionSource = interactionSource,
                            pressedInteraction = pressedInteraction,
                            delayPressInteraction = mutableStateOf({ false })
                        )
                    }
                )
            }.focusRequester(focusRequester)
        } else {
            Modifier
        }

        gestureModifier
    }
)
