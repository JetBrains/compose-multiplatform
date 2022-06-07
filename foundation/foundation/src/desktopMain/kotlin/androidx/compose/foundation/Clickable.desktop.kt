/*
 * Copyright 2021 The Android Open Source Project
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

import androidx.compose.foundation.gestures.forEachGesture
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType.Companion.KeyUp
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.nativeKeyCode
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.AwaitPointerEventScope
import androidx.compose.ui.input.pointer.PointerButtons
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.PointerKeyboardModifiers
import androidx.compose.ui.input.pointer.changedToDown
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.ui.input.pointer.isOutOfBounds
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.debugInspectorInfo
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.util.fastAll
import java.awt.event.KeyEvent.VK_ENTER
import kotlinx.coroutines.coroutineScope

@Composable
internal actual fun isComposeRootInScrollableContainer(): () -> Boolean = { false }

// TODO: b/168524931 - should this depend on the input device?
internal actual val TapIndicationDelay: Long = 0L

/**
 * Whether the specified [KeyEvent] represents a user intent to perform a click.
 * (eg. When you press Enter on a focused button, it should perform a click).
 */
internal actual val KeyEvent.isClick: Boolean
    get() = type == KeyUp && when (key.nativeKeyCode) {
        VK_ENTER -> true
        else -> false
    }

@Immutable @ExperimentalFoundationApi
class MouseClickScope constructor(
    val buttons: PointerButtons,
    val keyboardModifiers: PointerKeyboardModifiers
)

@ExperimentalFoundationApi
internal val EmptyClickContext = MouseClickScope(
    PointerButtons(0), PointerKeyboardModifiers(0)
)

/**
 * Creates modifier similar to [Modifier.clickable] but provides additional context with
 * information about pressed buttons and keyboard modifiers
 *
 */
@ExperimentalFoundationApi
fun Modifier.mouseClickable(
    enabled: Boolean = true,
    onClickLabel: String? = null,
    role: Role? = null,
    onClick: MouseClickScope.() -> Unit
) = composed(
    factory = {
        val onClickState = rememberUpdatedState(onClick)
        val gesture = if (enabled) {
            Modifier.pointerInput(Unit) {
                detectTapWithContext(
                    onTap = { down, _ ->
                        onClickState.value.invoke(
                            MouseClickScope(
                                down.buttons,
                                down.keyboardModifiers
                            )
                        )
                    }
                )
            }
        } else {
            Modifier
        }
        Modifier
            .genericClickableWithoutGesture(
                gestureModifiers = gesture,
                enabled = enabled,
                onClickLabel = onClickLabel,
                role = role,
                onLongClickLabel = null,
                onLongClick = null,
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
                onClick = { onClick(EmptyClickContext) }
            )
    },
    inspectorInfo = debugInspectorInfo {
        name = "clickable"
        properties["enabled"] = enabled
        properties["onClickLabel"] = onClickLabel
        properties["role"] = role
        properties["onClick"] = onClick
    }
)

@OptIn(ExperimentalFoundationApi::class)
internal suspend fun PointerInputScope.detectTapWithContext(
    onTap: ((PointerEvent, PointerEvent) -> Unit)? = null
) {
    forEachGesture {
        coroutineScope {
            awaitPointerEventScope {

                val down = awaitEventFirstDown().also {
                    it.changes.forEach { it.consume() }
                }

                val up = waitForFirstInboundUp()
                if (up != null) {
                    up.changes.forEach { it.consume() }
                    onTap?.invoke(down, up)
                }
            }
        }
    }
}

private suspend fun AwaitPointerEventScope.awaitEventFirstDown(): PointerEvent {
    var event: PointerEvent
    do {
        event = awaitPointerEvent()
    } while (
        !event.changes.fastAll { it.changedToDown() }
    )
    return event
}

private suspend fun AwaitPointerEventScope.waitForFirstInboundUp(): PointerEvent? {
    while (true) {
        val event = awaitPointerEvent()
        val change = event.changes[0]
        if (change.changedToUp()) {
            return if (change.isOutOfBounds(size, extendedTouchPadding)) {
                null
            } else {
                event
            }
        }
    }
}
