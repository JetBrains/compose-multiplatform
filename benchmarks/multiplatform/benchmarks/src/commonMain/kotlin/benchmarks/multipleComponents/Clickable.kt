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

// copy of https://github.com/JetBrains/compose-multiplatform-core/blob/d9e875b62e7bb4dd47b6b155d3a787251ff5bd38/compose/foundation/foundation/src/desktopMain/kotlin/androidx/compose/foundation/Clickable.desktop.kt#L78
// inefficient implementation of clickable for benchmarking purposes (Modifier.Node variant in AOSP is more optimized)

package benchmarks.multipleComponents

import androidx.compose.foundation.Indication
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType.Companion.KeyDown
import androidx.compose.ui.input.key.KeyEventType.Companion.KeyUp
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.AwaitPointerEventScope
import androidx.compose.ui.input.pointer.PointerButtons
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.PointerKeyboardModifiers
import androidx.compose.ui.input.pointer.changedToDown
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.ui.input.pointer.isOutOfBounds
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChangeConsumed
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.SemanticsModifierNode
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.platform.debugInspectorInfo
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsPropertyReceiver
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.onLongClick
import androidx.compose.ui.unit.center
import androidx.compose.ui.unit.toOffset
import androidx.compose.ui.util.fastAll
import androidx.compose.ui.util.fastAny
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Immutable
class MouseClickScope(
    val buttons: PointerButtons,
    val keyboardModifiers: PointerKeyboardModifiers
)

@Suppress("DEPRECATION")
internal val EmptyClickContext = MouseClickScope(
    PointerButtons(0), PointerKeyboardModifiers(0)
)

fun Modifier.mouseClickable(
    enabled: Boolean = true,
    onClickLabel: String? = null,
    role: Role? = null,
    @Suppress("DEPRECATION") onClick: MouseClickScope.() -> Unit
) = composed(
    factory = {
        val onClickState = rememberUpdatedState(onClick)
        val centreOffset = remember { mutableStateOf(Offset.Zero) }
        val currentKeyPressInteractions = remember { mutableMapOf<Key, PressInteraction.Press>() }
        val focusRequester = remember { FocusRequester() }
        val gesture = if (enabled) {
            Modifier.pointerInput(Unit) {
                centreOffset.value = size.center.toOffset()
                detectTapWithContext(
                    onTap = { down, _ ->
                        focusRequester.requestFocus()
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
            .focusRequester(focusRequester)
            .genericClickableWithoutGesture(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                indicationScope = rememberCoroutineScope(),
                keyClickOffset = centreOffset,
                enabled = enabled,
                onClickLabel = onClickLabel,
                currentKeyPressInteractions = currentKeyPressInteractions,
                role = role,
                onLongClickLabel = null,
                onLongClick = null,
                onClick = { onClick(EmptyClickContext) }
            )
            .then(gesture)
    },
    inspectorInfo = debugInspectorInfo {
        name = "clickable"
        properties["enabled"] = enabled
        properties["onClickLabel"] = onClickLabel
        properties["role"] = role
        properties["onClick"] = onClick
    }
)

internal suspend fun PointerInputScope.detectTapWithContext(
    onTap: ((PointerEvent, PointerEvent) -> Unit)? = null
) {
    awaitEachGesture {
        val down = awaitEventFirstDown().also {
            it.changes.forEach { it.consume() }
        }

        val up = waitForFirstInboundUpOrCancellation()
        if (up != null) {
            up.changes.forEach { it.consume() }
            onTap?.invoke(down, up)
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

private suspend fun AwaitPointerEventScope.waitForFirstInboundUpOrCancellation(): PointerEvent? {
    while (true) {
        val event = awaitPointerEvent(PointerEventPass.Main)
        if (event.changes.fastAll { it.changedToUp() }) {
            // All pointers are up
            return event
        }

        if (event.changes.fastAny {
                it.consumed.downChange || it.isOutOfBounds(size, extendedTouchPadding)
            }
        ) {
            return null // Canceled
        }

        // Check for cancel by position consumption. We can look on the Final pass of the
        // existing pointer event because it comes after the Main pass we checked above.
        val consumeCheck = awaitPointerEvent(PointerEventPass.Final)
        if (consumeCheck.changes.fastAny { it.positionChangeConsumed() }) {
            return null
        }
    }
}

internal fun Modifier.genericClickableWithoutGesture(
    interactionSource: MutableInteractionSource,
    indication: Indication?,
    indicationScope: CoroutineScope,
    currentKeyPressInteractions: MutableMap<Key, PressInteraction.Press>,
    keyClickOffset: State<Offset>,
    enabled: Boolean = true,
    onClickLabel: String? = null,
    role: Role? = null,
    onLongClickLabel: String? = null,
    onLongClick: (() -> Unit)? = null,
    onClick: () -> Unit
): Modifier {
    fun Modifier.detectPressAndClickFromKey() = this.onKeyEvent { keyEvent ->
        when {
            enabled && keyEvent.isPress -> {
                // If the key already exists in the map, keyEvent is a repeat event.
                // We ignore it as we only want to emit an interaction for the initial key press.
                if (!currentKeyPressInteractions.containsKey(keyEvent.key)) {
                    val press = PressInteraction.Press(keyClickOffset.value)
                    currentKeyPressInteractions[keyEvent.key] = press
                    indicationScope.launch { interactionSource.emit(press) }
                    true
                } else {
                    false
                }
            }
            enabled && keyEvent.isClick -> {
                currentKeyPressInteractions.remove(keyEvent.key)?.let {
                    indicationScope.launch {
                        interactionSource.emit(PressInteraction.Release(it))
                    }
                }
                onClick()
                true
            }
            else -> false
        }
    }
    return this then
        ClickableSemanticsElement(
            enabled = enabled,
            role = role,
            onLongClickLabel = onLongClickLabel,
            onLongClick = onLongClick,
            onClickLabel = onClickLabel,
            onClick = onClick
        )
            .detectPressAndClickFromKey()
            .indication(interactionSource, indication)
            .hoverable(enabled = enabled, interactionSource = interactionSource)
            .focusable(enabled = enabled, interactionSource = interactionSource)
}

private class ClickableSemanticsElement(
    private val enabled: Boolean,
    private val role: Role?,
    private val onLongClickLabel: String?,
    private val onLongClick: (() -> Unit)?,
    private val onClickLabel: String?,
    private val onClick: () -> Unit
) : ModifierNodeElement<ClickableSemanticsNode>() {
    override fun create() = ClickableSemanticsNode(
        enabled = enabled,
        role = role,
        onLongClickLabel = onLongClickLabel,
        onLongClick = onLongClick,
        onClickLabel = onClickLabel,
        onClick = onClick
    )

    override fun update(node: ClickableSemanticsNode) {
        node.update(enabled, onClickLabel, role, onClick, onLongClickLabel, onLongClick)
    }

    override fun InspectorInfo.inspectableProperties() = Unit

    override fun hashCode(): Int {
        var result = enabled.hashCode()
        result = 31 * result + role.hashCode()
        result = 31 * result + onLongClickLabel.hashCode()
        result = 31 * result + onLongClick.hashCode()
        result = 31 * result + onClickLabel.hashCode()
        result = 31 * result + onClick.hashCode()
        return result
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ClickableSemanticsElement) return false

        if (enabled != other.enabled) return false
        if (role != other.role) return false
        if (onLongClickLabel != other.onLongClickLabel) return false
        if (onLongClick !== other.onLongClick) return false
        if (onClickLabel != other.onClickLabel) return false
        if (onClick !== other.onClick) return false

        return true
    }
}

internal class ClickableSemanticsNode(
    private var enabled: Boolean,
    private var onClickLabel: String?,
    private var role: Role?,
    private var onClick: () -> Unit,
    private var onLongClickLabel: String?,
    private var onLongClick: (() -> Unit)?,
) : SemanticsModifierNode, Modifier.Node() {
    fun update(
        enabled: Boolean,
        onClickLabel: String?,
        role: Role?,
        onClick: () -> Unit,
        onLongClickLabel: String?,
        onLongClick: (() -> Unit)?,
    ) {
        this.enabled = enabled
        this.onClickLabel = onClickLabel
        this.role = role
        this.onClick = onClick
        this.onLongClickLabel = onLongClickLabel
        this.onLongClick = onLongClick
    }

    override val shouldMergeDescendantSemantics: Boolean
        get() = true

    override fun SemanticsPropertyReceiver.applySemantics() {
        if (this@ClickableSemanticsNode.role != null) {
            role = this@ClickableSemanticsNode.role!!
        }
        onClick(
            action = { onClick(); true },
            label = onClickLabel
        )
        if (onLongClick != null) {
            onLongClick(
                action = { onLongClick?.invoke(); true },
                label = onLongClickLabel
            )
        }
        if (!enabled) {
            disabled()
        }
    }
}


/**
 * Whether the specified [KeyEvent] should trigger a press for a clickable component, i.e. whether
 * it is associated with a press of an enter key or dpad centre.
 */
private val KeyEvent.isPress: Boolean
    get() = type == KeyDown && isEnter

/**
 * Whether the specified [KeyEvent] should trigger a click for a clickable component, i.e. whether
 * it is associated with a release of an enter key or dpad centre.
 */
private val KeyEvent.isClick: Boolean
    get() = type == KeyUp && isEnter

private val KeyEvent.isEnter: Boolean
    get() =
        when (key) {
            Key.DirectionCenter,
            Key.Enter,
            Key.NumPadEnter,
            Key.Spacebar -> true
            else -> false
        }
