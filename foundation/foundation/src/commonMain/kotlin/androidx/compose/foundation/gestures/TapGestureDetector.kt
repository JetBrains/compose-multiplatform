/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.compose.foundation.gestures

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.AwaitPointerEventScope
import androidx.compose.ui.input.pointer.PointerEventTimeoutCancellationException
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.changedToDown
import androidx.compose.ui.input.pointer.changedToDownIgnoreConsumed
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.ui.input.pointer.isOutOfBounds
import androidx.compose.ui.platform.ViewConfiguration
import androidx.compose.ui.unit.Density
import androidx.compose.ui.util.fastAll
import androidx.compose.ui.util.fastAny
import androidx.compose.ui.util.fastForEach
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlin.jvm.JvmDefaultWithCompatibility

/**
 * Receiver scope for [detectTapGestures]'s `onPress` lambda. This offers
 * two methods to allow waiting for the press to be released.
 */
@JvmDefaultWithCompatibility
interface PressGestureScope : Density {
    /**
     * Waits for the press to be released before returning. If the gesture was canceled by
     * motion being consumed by another gesture, [GestureCancellationException] will be
     * thrown.
     */
    suspend fun awaitRelease()

    /**
     * Waits for the press to be released before returning. If the press was released,
     * `true` is returned, or if the gesture was canceled by motion being consumed by
     * another gesture, `false` is returned .
     */
    suspend fun tryAwaitRelease(): Boolean
}

private val NoPressGesture: suspend PressGestureScope.(Offset) -> Unit = { }

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
 */
suspend fun PointerInputScope.detectTapGestures(
    onDoubleTap: ((Offset) -> Unit)? = null,
    onLongPress: ((Offset) -> Unit)? = null,
    onPress: suspend PressGestureScope.(Offset) -> Unit = NoPressGesture,
    onTap: ((Offset) -> Unit)? = null
) = coroutineScope {
    // special signal to indicate to the sending side that it shouldn't intercept and consume
    // cancel/up events as we're only require down events
    val pressScope = PressGestureScopeImpl(this@detectTapGestures)

    forEachGesture {
        awaitPointerEventScope {
            val down = awaitFirstDown()
            down.consume()
            pressScope.reset()
            if (onPress !== NoPressGesture) launch {
                pressScope.onPress(down.position)
            }
            val longPressTimeout = onLongPress?.let {
                viewConfiguration.longPressTimeoutMillis
            } ?: (Long.MAX_VALUE / 2)
            var upOrCancel: PointerInputChange? = null
            try {
                // wait for first tap up or long press
                upOrCancel = withTimeout(longPressTimeout) {
                    waitForUpOrCancellation()
                }
                if (upOrCancel == null) {
                    pressScope.cancel() // tap-up was canceled
                } else {
                    upOrCancel.consume()
                    pressScope.release()
                }
            } catch (_: PointerEventTimeoutCancellationException) {
                onLongPress?.invoke(down.position)
                consumeUntilUp()
                pressScope.release()
            }

            if (upOrCancel != null) {
                // tap was successful.
                if (onDoubleTap == null) {
                    onTap?.invoke(upOrCancel.position) // no need to check for double-tap.
                } else {
                    // check for second tap
                    val secondDown = awaitSecondDown(upOrCancel)

                    if (secondDown == null) {
                        onTap?.invoke(upOrCancel.position) // no valid second tap started
                    } else {
                        // Second tap down detected
                        pressScope.reset()
                        if (onPress !== NoPressGesture) {
                            launch { pressScope.onPress(secondDown.position) }
                        }

                        try {
                            // Might have a long second press as the second tap
                            withTimeout(longPressTimeout) {
                                val secondUp = waitForUpOrCancellation()
                                if (secondUp != null) {
                                    secondUp.consume()
                                    pressScope.release()
                                    onDoubleTap(secondUp.position)
                                } else {
                                    pressScope.cancel()
                                    onTap?.invoke(upOrCancel.position)
                                }
                            }
                        } catch (e: PointerEventTimeoutCancellationException) {
                            // The first tap was valid, but the second tap is a long press.
                            // notify for the first tap
                            onTap?.invoke(upOrCancel.position)

                            // notify for the long press
                            onLongPress?.invoke(secondDown.position)
                            consumeUntilUp()
                            pressScope.release()
                        }
                    }
                }
            }
        }
    }
}

/**
 * Consumes all pointer events until nothing is pressed and then returns. This method assumes
 * that something is currently pressed.
 */
private suspend fun AwaitPointerEventScope.consumeUntilUp() {
    do {
        val event = awaitPointerEvent()
        event.changes.fastForEach { it.consume() }
    } while (event.changes.fastAny { it.pressed })
}

/**
 * Waits for [ViewConfiguration.doubleTapTimeoutMillis] for a second press event. If a
 * second press event is received before the time out, it is returned or `null` is returned
 * if no second press is received.
 */
private suspend fun AwaitPointerEventScope.awaitSecondDown(
    firstUp: PointerInputChange
): PointerInputChange? = withTimeoutOrNull(viewConfiguration.doubleTapTimeoutMillis) {
    val minUptime = firstUp.uptimeMillis + viewConfiguration.doubleTapMinTimeMillis
    var change: PointerInputChange
    // The second tap doesn't count if it happens before DoubleTapMinTime of the first tap
    do {
        change = awaitFirstDown()
    } while (change.uptimeMillis < minUptime)
    change
}

/**
 * Shortcut for cases when we only need to get press/click logic, as for cases without long press
 * and double click we don't require channelling or any other complications.
 */
internal suspend fun PointerInputScope.detectTapAndPress(
    onPress: suspend PressGestureScope.(Offset) -> Unit = NoPressGesture,
    onTap: ((Offset) -> Unit)? = null
) {
    val pressScope = PressGestureScopeImpl(this)
    forEachGesture {
        coroutineScope {
            pressScope.reset()
            awaitPointerEventScope {

                val down = awaitFirstDown().also { it.consume() }

                if (onPress !== NoPressGesture) {
                    launch { pressScope.onPress(down.position) }
                }

                val up = waitForUpOrCancellation()
                if (up == null) {
                    pressScope.cancel() // tap-up was canceled
                } else {
                    up.consume()
                    pressScope.release()
                    onTap?.invoke(up.position)
                }
            }
        }
    }
}

/**
 * Reads events until the first down is received. If [requireUnconsumed] is `true` and the first
 * down is consumed in the [PointerEventPass.Main] pass, that gesture is ignored.
 */
suspend fun AwaitPointerEventScope.awaitFirstDown(
    requireUnconsumed: Boolean = true
): PointerInputChange =
    awaitFirstDownOnPass(pass = PointerEventPass.Main, requireUnconsumed = requireUnconsumed)

internal suspend fun AwaitPointerEventScope.awaitFirstDownOnPass(
    pass: PointerEventPass,
    requireUnconsumed: Boolean
): PointerInputChange {
    var event: PointerEvent
    do {
        event = awaitPointerEvent(pass)
    } while (
        !event.changes.fastAll {
            if (requireUnconsumed) it.changedToDown() else it.changedToDownIgnoreConsumed()
        }
    )
    return event.changes[0]
}

/**
 * Reads events until all pointers are up or the gesture was canceled. The gesture
 * is considered canceled when a pointer leaves the event region, a position change
 * has been consumed or a pointer down change event was consumed in the [PointerEventPass.Main]
 * pass. If the gesture was not canceled, the final up change is returned or `null` if the
 * event was canceled.
 */
suspend fun AwaitPointerEventScope.waitForUpOrCancellation(): PointerInputChange? {
    while (true) {
        val event = awaitPointerEvent(PointerEventPass.Main)
        if (event.changes.fastAll { it.changedToUp() }) {
            // All pointers are up
            return event.changes[0]
        }

        if (event.changes.fastAny {
                it.isConsumed || it.isOutOfBounds(size, extendedTouchPadding)
            }
        ) {
            return null // Canceled
        }

        // Check for cancel by position consumption. We can look on the Final pass of the
        // existing pointer event because it comes after the Main pass we checked above.
        val consumeCheck = awaitPointerEvent(PointerEventPass.Final)
        if (consumeCheck.changes.fastAny { it.isConsumed }) {
            return null
        }
    }
}

/**
 * [detectTapGestures]'s implementation of [PressGestureScope].
 */
private class PressGestureScopeImpl(
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