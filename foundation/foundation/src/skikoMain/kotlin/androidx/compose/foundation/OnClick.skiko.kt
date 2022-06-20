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

import androidx.compose.foundation.gestures.GestureCancellationException
import androidx.compose.foundation.gestures.PressGestureScope
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.input.pointer.AwaitPointerEventScope
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.PointerKeyboardModifiers
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.input.pointer.changedToDown
import androidx.compose.ui.input.pointer.changedToDownIgnoreConsumed
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.ui.input.pointer.changedToUpIgnoreConsumed
import androidx.compose.ui.input.pointer.isOutOfBounds
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.ViewConfiguration
import androidx.compose.ui.unit.Density
import androidx.compose.ui.util.fastAll
import androidx.compose.ui.util.fastAny
import androidx.compose.ui.util.fastForEach
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex

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
 * [onClick] doesn't add [Modifier.indication], [Modifier.hoverable], click by Enter key, etc.
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
 * [onClick] doesn't add [Modifier.indication], [Modifier.hoverable], click by Enter key, etc.
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

            Modifier.pointerInput(interactionSource, matcher, hasLongClick, hasDoubleClick) {
                detectTapGestures(
                    matcher = matcher,
                    keyboardModifiers = {
                        keyboardModifiersState.value(this)
                    },
                    onDoubleTap = if (hasDoubleClick) {
                        { on2xClickState.value!!.invoke() }
                    } else {
                        null
                    },
                    onLongPress = if (hasLongClick) {
                        { onLongClickState.value!!.invoke() }
                    } else {
                        null
                    },
                    onTap = {
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
            }
        } else {
            Modifier
        }

        gestureModifier
    }
)

private val EmptyPointerEvent = PointerEvent(emptyList())

/**
 * Detects tap, double-tap, and long press gestures and calls [onTap], [onDoubleTap], and
 * [onLongPress], respectively, when detected. [onPress] is called when the press is detected
 * and the [PressGestureScope.tryAwaitRelease] and [PressGestureScope.awaitRelease] can be
 * used to detect when pointers have released or the gesture was canceled.
 * The first pointer down and final pointer up are consumed, and in the
 * case of long press, all changes after the long press is detected are consumed.
 *
 * When [onDoubleTap] is provided, the tap gesture is detected only after
 * the [ViewConfiguration.doubleTapMinTimeMillis] has passed and [onDoubleTap] is called if the
 * second tap is started before [ViewConfiguration.doubleTapTimeoutMillis]. If [onDoubleTap] is not
 * provided, then [onTap] is called when the pointer up has been received.
 *
 * If the first down event was consumed, the entire gesture will be skipped, including
 * [onPress]. If the first down event was not consumed, if any other gesture consumes the down or
 * up events, the pointer moves out of the input area, or the position change is consumed,
 * the gestures are considered canceled. [onDoubleTap], [onLongPress], and [onTap] will not be
 * called after a gesture has been canceled.
 *
 * @param matcher defines supported pointer types and required properties
 * @param keyboardModifiers defines a condition that [PointerEvent.keyboardModifiers] has to match
 */
@ExperimentalFoundationApi
suspend fun PointerInputScope.detectTapGestures(
    matcher: PointerMatcher = PointerMatcher.Primary,
    keyboardModifiers: PointerKeyboardModifiers.() -> Boolean = { true },
    onDoubleTap: ((Offset) -> Unit)? = null,
    onLongPress: ((Offset) -> Unit)? = null,
    onPress: suspend PressGestureScope.(Offset) -> Unit = { },
    onTap: ((Offset) -> Unit)? = null
) = coroutineScope {
    // special signal to indicate to the sending side that it shouldn't intercept and consume
    // cancel/up events as we're only require down events
    val pressScope = PressGestureScopeImpl(this@detectTapGestures)

    val filter: (PointerEvent) -> Boolean = {
        matcher.matches(it) && keyboardModifiers(it.keyboardModifiers)
    }

    while (currentCoroutineContext().isActive) {
        awaitPointerEventScope {
            pressScope.reset()

            val down = awaitPress(filter = filter, requireUnconsumed = true).apply { changes[0].consume() }

            launch { pressScope.onPress(down.changes[0].position) }

            val longPressTimeout = onLongPress?.let {
                viewConfiguration.longPressTimeoutMillis
            } ?: (Long.MAX_VALUE / 2)

            var cancelled = false

            // `firstRelease` will be null if either event is cancelled or it's timed out
            // use `cancelled` flag to distinguish between two cases

            val firstRelease = withTimeoutOrNull(longPressTimeout) {
                awaitReleaseOrCancelled(filter = filter).apply {
                    this?.changes?.fastForEach { it.consume() }
                    cancelled = this == null
                }
            }

            if (cancelled) {
                pressScope.cancel()
            } else if (firstRelease != null) {
                pressScope.release()
            }

            if (firstRelease == null) {
                if (onLongPress != null && !cancelled) {
                    onLongPress(down.changes[0].position)
                    awaitReleaseOrCancelled(consumeUntilRelease = true, filter = filter)
                    pressScope.release()
                }
            } else if (onDoubleTap == null) {
                onTap?.invoke(firstRelease.changes[0].position)
            } else {
                val secondPress = awaitSecondPressUnconsumed(
                    firstRelease.changes[0],
                    filter
                )?.apply {
                    changes.fastForEach { it.consume() }
                }
                if (secondPress == null) {
                    onTap?.invoke(firstRelease.changes[0].position)
                } else {
                    pressScope.reset()
                    launch { pressScope.onPress(secondPress.changes[0].position) }

                    cancelled = false

                    val secondRelease = withTimeoutOrNull(longPressTimeout) {
                        awaitReleaseOrCancelled(filter = filter).apply {
                            this?.changes?.fastForEach { it.consume() }
                            cancelled = this == null
                        }
                    }

                    if (cancelled) {
                        pressScope.cancel()
                    } else if (secondRelease != null) {
                        pressScope.release()
                    }

                    if (secondRelease == null) {
                        if (onLongPress != null && !cancelled) {
                            onLongPress(secondPress.changes[0].position)
                            awaitReleaseOrCancelled(consumeUntilRelease = true, filter = filter)
                            pressScope.release()
                        }
                    } else if (!cancelled) {
                        onDoubleTap(secondRelease.changes[0].position)
                    }
                }
            }

            Unit
        }
    }
}


internal suspend fun AwaitPointerEventScope.awaitPress(
    filter: (PointerEvent) -> Boolean,
    requireUnconsumed: Boolean = true
): PointerEvent {
    var event: PointerEvent? = null

    while (event == null) {
        event = awaitPointerEvent().takeIf {
            it.isAllPressedDown(requireUnconsumed = requireUnconsumed) && filter(it)
        }
    }

    return event
}

private suspend fun AwaitPointerEventScope.awaitSecondPressUnconsumed(
    firstUp: PointerInputChange,
    filter: (PointerEvent) -> Boolean
): PointerEvent? = withTimeoutOrNull(viewConfiguration.doubleTapTimeoutMillis) {
    val minUptime = firstUp.uptimeMillis + viewConfiguration.doubleTapMinTimeMillis
    var event: PointerEvent
    var change: PointerInputChange
    // The second tap doesn't count if it happens before DoubleTapMinTime of the first tap
    do {
        event = awaitPress(filter)
        change = event.changes[0]
    } while (change.uptimeMillis < minUptime)
    event
}

private suspend fun AwaitPointerEventScope.awaitReleaseOrCancelled(
    consumeUntilRelease: Boolean = false,
    filter: (PointerEvent) -> Boolean
): PointerEvent? {
    var event: PointerEvent? = null

    while (event == null) {
        event = awaitPointerEvent()

        val cancelled = event.changes.fastAny {
            it.isOutOfBounds(size, Size.Zero)
        }

        if (cancelled) return null

        event = event.takeIf {
            it.isAllPressedUp(requireUnconsumed = true) && filter(it)
        }

        if (consumeUntilRelease) {
            currentEvent.changes.fastForEach { it.consume() }
        }

        // Check for cancel by position consumption. We can look on the Final pass of the
        // existing pointer event because it comes after the Main pass we checked above.
        val consumeCheck = awaitPointerEvent(PointerEventPass.Final)
        if (consumeCheck.changes.fastAny { it.isConsumed }) {
            return null
        }
    }

    return event
}

private fun PointerEvent.isAllPressedDown(requireUnconsumed: Boolean = true) =
    type == PointerEventType.Press &&
        changes.fastAll { it.type == PointerType.Mouse && (!requireUnconsumed || !it.isConsumed) } ||
        changes.fastAll { if (requireUnconsumed) it.changedToDown() else it.changedToDownIgnoreConsumed() }

private fun PointerEvent.isAllPressedUp(requireUnconsumed: Boolean = true) =
    type == PointerEventType.Release &&
        changes.fastAll { it.type == PointerType.Mouse && (!requireUnconsumed || !it.isConsumed) } ||
        changes.fastAll { if (requireUnconsumed) it.changedToUp() else it.changedToUpIgnoreConsumed() }

private class PressGestureScopeImpl( // copy-pasted from TapGestureDetector. Remove when upstreaming into commonMain
    density: Density
) : PressGestureScope, Density by density {
    private var isReleased = false
    private var isCanceled = false
    private val mutex = Mutex(locked = false)

    /**
     * Called when a gesture has been canceled.
     */
    fun cancel() {
        isCanceled = true
        mutex.unlock()
    }

    /**
     * Called when all pointers are up.
     */
    fun release() {
        isReleased = true
        mutex.unlock()
    }

    /**
     * Called when a new gesture has started.
     */
    fun reset() {
        mutex.tryLock() // If tryAwaitRelease wasn't called, this will be unlocked.
        isReleased = false
        isCanceled = false
    }

    override suspend fun awaitRelease() {
        if (!tryAwaitRelease()) {
            throw GestureCancellationException("The press gesture was canceled.")
        }
    }

    override suspend fun tryAwaitRelease(): Boolean {
        if (!isReleased && !isCanceled) {
            mutex.lock()
        }
        return isReleased
    }
}

