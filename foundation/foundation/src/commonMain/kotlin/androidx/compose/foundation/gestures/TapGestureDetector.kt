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

import androidx.compose.foundation.gestures.TapGestureEvent.AllUp
import androidx.compose.foundation.gestures.TapGestureEvent.Cancel
import androidx.compose.foundation.gestures.TapGestureEvent.Down
import androidx.compose.foundation.gestures.TapGestureEvent.Up
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.AwaitPointerEventScope
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.changedToDown
import androidx.compose.ui.input.pointer.changedToDownIgnoreConsumed
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.ui.input.pointer.consumeAllChanges
import androidx.compose.ui.input.pointer.consumeDownChange
import androidx.compose.ui.input.pointer.isOutOfBounds
import androidx.compose.ui.input.pointer.positionChangeConsumed
import androidx.compose.ui.platform.ViewConfiguration
import androidx.compose.ui.unit.Density
import androidx.compose.ui.util.fastAll
import androidx.compose.ui.util.fastAny
import androidx.compose.ui.util.fastForEach
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Receiver scope for [detectTapGestures]'s `onPress` lambda. This offers
 * two methods to allow waiting for the press to be released.
 */
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
    // special signal to indicate to the sending side that it needs to consume(!) all the events
    // until all the pointers will be up
    val consumeAllUntilUpSignal = mutableStateOf(false)
    // special signal to indicate to the sending side that it shouldn't intercept and consume
    // cancel/up events as we're only require down events
    val consumeOnlyDownsSignal = mutableStateOf(false)
    val channel = Channel<TapGestureEvent>(capacity = Channel.UNLIMITED)
    val pressScope = PressGestureScopeImpl(this@detectTapGestures)

    launch {
        while (isActive) {
            consumeAllUntilUpSignal.value = false
            val down = awaitChannelDown(consumeOnlyDownsSignal, channel)
            pressScope.reset()
            if (onPress !== NoPressGesture) launch {
                pressScope.onPress(down.position)
            }
            val longPressTimeout = onLongPress?.let { viewConfiguration.longPressTimeoutMillis }
            var upOrCancel: TapGestureEvent? = null
            try {
                // wait for first tap up or long press
                upOrCancel = withNullableTimeout(this, longPressTimeout) {
                    awaitChannelUpOrCancel(channel)
                }
                if (upOrCancel is Cancel) {
                    pressScope.cancel() // tap-up was canceled
                } else {
                    pressScope.release()
                }
            } catch (_: TimeoutCancellationException) {
                onLongPress?.invoke(down.position)
                awaitChannelAllUp(consumeAllUntilUpSignal, channel)
                pressScope.release()
            }

            if (upOrCancel != null && upOrCancel is Up) {
                // tap was successful.
                if (onDoubleTap == null) {
                    onTap?.invoke(upOrCancel.position) // no need to check for double-tap.
                } else {
                    // check for second tap
                    val secondDown = awaitChannelSecondDown(
                        channel,
                        consumeOnlyDownsSignal,
                        viewConfiguration,
                        upOrCancel
                    )

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
                            withNullableTimeout(this, longPressTimeout) {
                                val secondUp = awaitChannelUpOrCancel(channel)
                                if (secondUp is Up) {
                                    pressScope.release()
                                    onDoubleTap(secondUp.position)
                                } else {
                                    pressScope.cancel()
                                    onTap?.invoke(upOrCancel.position)
                                }
                            }
                        } catch (e: TimeoutCancellationException) {
                            // The first tap was valid, but the second tap is a long press.
                            // notify for the first tap
                            onTap?.invoke(upOrCancel.position)

                            // notify for the long press
                            onLongPress?.invoke(secondDown.position)
                            awaitChannelAllUp(consumeAllUntilUpSignal, channel)
                            pressScope.release()
                        }
                    }
                }
            }
        }
    }
    forEachGesture {
        awaitPointerEventScope {
            translatePointerEventsToChannel(
                this@coroutineScope,
                channel,
                consumeOnlyDownsSignal,
                consumeAllUntilUpSignal
            )
        }
    }
}

private suspend fun <T> withNullableTimeout(
    scope: CoroutineScope,
    timeout: Long?,
    block: suspend CoroutineScope.() -> T
): T {
    return if (timeout != null) {
        withTimeout(timeout, block)
    } else {
        with(scope) {
            block()
        }
    }
}

/**
 * Await down from the channel and return it when it happens
 */
private suspend fun awaitChannelDown(
    onlyDownsSignal: MutableState<Boolean>,
    channel: ReceiveChannel<TapGestureEvent>
): Down {
    onlyDownsSignal.value = true
    var event = channel.receive()
    while (event !is Down) {
        event = channel.receive()
    }
    onlyDownsSignal.value = false
    return event
}

/**
 * Reads input for second tap down event from the [channel]. If the second tap is within
 * [ViewConfiguration.doubleTapMinTimeMillis] of [firstUp] uptime, the event is discarded. If the
 * second down is not detected within [ViewConfiguration.doubleTapTimeoutMillis] of [firstUp],
 * `null` is returned. Otherwise, the down event is returned.
 */
private suspend fun awaitChannelSecondDown(
    channel: ReceiveChannel<TapGestureEvent>,
    onlyDownsSignal: MutableState<Boolean>,
    viewConfiguration: ViewConfiguration,
    firstUp: Up
): Down? {
    return withTimeoutOrNull(viewConfiguration.doubleTapTimeoutMillis) {
        val minUptime = firstUp.uptimeMillis + viewConfiguration.doubleTapMinTimeMillis
        var change: Down
        // The second tap doesn't count if it happens before DoubleTapMinTime of the first tap
        do {
            change = awaitChannelDown(onlyDownsSignal, channel)
        } while (change.uptimeMillis < minUptime)
        change
    }
}

/**
 * Special case to wait for all ups after long press has been fired. This sets a state value to
 * true, indicating to the channel producer to consume all events until it will send an [AllUp]
 * event. When all up happens and producer itself flips the value back to false, this method
 * returns.
 */
private suspend fun awaitChannelAllUp(
    consumeAllSignal: MutableState<Boolean>,
    channel: ReceiveChannel<TapGestureEvent>
) {
    consumeAllSignal.value = true
    var event = channel.receive()
    while (event != AllUp) {
        event = channel.receive()
    }
}

/**
 * Await up or cancel event from the channel and return either [Up] or [Cancel]
 */
private suspend fun awaitChannelUpOrCancel(
    channel: ReceiveChannel<TapGestureEvent>
): TapGestureEvent {
    var event = channel.receive()
    while (event !is Up && event !is Cancel) {
        event = channel.receive()
    }
    return event
}

private sealed class TapGestureEvent {
    class Down(val position: Offset, val uptimeMillis: Long) : TapGestureEvent()
    class Up(val position: Offset, val uptimeMillis: Long) : TapGestureEvent()

    // special case, the notification sent when we were consuming all previous events before all
    // the pointers are up. AllUp means that we can restart the cycle after long press fired
    object AllUp : TapGestureEvent()
    object Cancel : TapGestureEvent()
}

/**
 * Method to await domain specific [TapGestureEvent] from the [AwaitPointerEventScope] and send
 * them to the specified [channel].
 *
 * Note: [consumeAllUntilUp] is a switch for a special case which happens when the long press has
 * been fired, after which we want to block all the events until all fingers are up. This methods
 * stars to consume all the events when [consumeAllUntilUp] is `true` and when all pointers are
 * up it flips the [consumeAllUntilUp] itself, so it can suspend on the [AwaitPointerEventScope
 * .awaitPointerEvent] again.
 */
private suspend fun AwaitPointerEventScope.translatePointerEventsToChannel(
    scope: CoroutineScope,
    channel: SendChannel<TapGestureEvent>,
    detectDownsOnly: State<Boolean>,
    consumeAllUntilUp: MutableState<Boolean>
) {
    while (scope.isActive) {
        // operate normally, scan all downs / ups / cancels and push them to the channel
        val event = awaitPointerEvent()
        if (consumeAllUntilUp.value) {
            event.changes.fastForEach { it.consumeAllChanges() }
            // check the signal if we just need to consume everything on the initial pass for
            // cases when the long press has fired and we block everything before all pointers
            // are up
            if (!allPointersUp()) {
                do {
                    val initialEvent = awaitPointerEvent(PointerEventPass.Initial)
                    initialEvent.changes.fastForEach { it.consumeAllChanges() }
                } while (initialEvent.changes.fastAny { it.pressed })
                // wait for the main pass of the initial event we already have eaten above
                awaitPointerEvent()
            }
            channel.trySend(AllUp)
            consumeAllUntilUp.value = false
        } else if (event.changes.fastAll { it.changedToDown() }) {
            val change = event.changes[0]
            change.consumeDownChange()
            channel.trySend(Down(change.position, change.uptimeMillis))
        } else if (!detectDownsOnly.value) {
            if (event.changes.fastAll { it.changedToUp() }) {
                // All pointers are up
                val change = event.changes[0]
                change.consumeDownChange()
                channel.trySend(Up(change.position, change.uptimeMillis))
            } else if (
                event.changes.fastAny { it.consumed.downChange || it.isOutOfBounds(size) }
            ) {
                channel.trySend(Cancel)
            } else {
                // Check for cancel by position consumption. We can look on the Final pass of the
                // existing pointer event because it comes after the Main pass we checked above.
                val consumeCheck = awaitPointerEvent(PointerEventPass.Final)
                if (consumeCheck.changes.fastAny { it.positionChangeConsumed() }) {
                    channel.trySend(Cancel)
                }
            }
        }
    }
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

                val down = awaitFirstDown().also { it.consumeDownChange() }

                if (onPress !== NoPressGesture) {
                    launch { pressScope.onPress(down.position) }
                }

                val up = waitForUpOrCancellation()
                if (up == null) {
                    pressScope.cancel() // tap-up was canceled
                } else {
                    up.consumeDownChange()
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
): PointerInputChange {
    var event: PointerEvent
    do {
        event = awaitPointerEvent()
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

        if (event.changes.fastAny { it.consumed.downChange || it.isOutOfBounds(size) }) {
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