package androidx.compose.foundation.gestures

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.PointerMatcher
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
import androidx.compose.ui.platform.ViewConfiguration
import androidx.compose.ui.util.fastAll
import androidx.compose.ui.util.fastAny
import androidx.compose.ui.util.fastForEach
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

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

    // After long click was detected and dispatched, we should wait for the pointer button release.
    // In this case the event has been already dispatched, therefore, the release event is not required to
    // have the matching keyboard modifiers.
    val longClickReleaseFilter: (PointerEvent) -> Boolean = {
        matcher.matches(it)
    }

    // keyboardModifiersDontMatch is used in `cancelIf` of `awaitReleaseOrCancelled to cancel the await.
    // It prevents an infinite suspension when we wait for a release event. An infinite suspension might occur when
    // a user depresses a keyboard modifier button before releasing a pointer button.
    val keyboardModifiersDontMatch: (PointerEvent) -> Boolean = {
        !keyboardModifiers(it.keyboardModifiers)
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
                awaitReleaseOrCancelled(filter = filter, cancelIf = keyboardModifiersDontMatch).apply {
                    this?.changes?.fastForEach { it.consume() }
                    cancelled = this == null
                }
            }

            if (cancelled) {
                pressScope.cancel()
                return@awaitPointerEventScope
            } else if (firstRelease != null) {
                pressScope.release()
            }

            if (firstRelease == null) {
                if (onLongPress != null && !cancelled) {
                    onLongPress(down.changes[0].position)
                    awaitReleaseOrCancelled(
                        consumeUntilRelease = true,
                        filter = longClickReleaseFilter
                    )
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
                        awaitReleaseOrCancelled(filter = filter, cancelIf = keyboardModifiersDontMatch).apply {
                            this?.changes?.fastForEach { it.consume() }
                            cancelled = this == null
                        }
                    }

                    if (cancelled) {
                        pressScope.cancel()
                        return@awaitPointerEventScope
                    } else if (secondRelease != null) {
                        pressScope.release()
                    }

                    if (secondRelease == null) {
                        if (onLongPress != null && !cancelled) {
                            onLongPress(secondPress.changes[0].position)
                            awaitReleaseOrCancelled(
                                consumeUntilRelease = true,
                                filter = longClickReleaseFilter
                            )
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
    cancelIf: (PointerEvent) -> Boolean = { false },
    filter: (PointerEvent) -> Boolean
): PointerEvent? {
    var event: PointerEvent? = null

    while (event == null) {
        event = awaitPointerEvent()

        val cancelled = event.changes.fastAny {
            it.isOutOfBounds(size, Size.Zero)
        }

        if (cancelled || cancelIf(event)) return null

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
