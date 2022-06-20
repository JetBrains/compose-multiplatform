/*
 * Copyright 2019 The Android Open Source Project
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
package androidx.compose.ui.test

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.node.RootForTest

internal expect fun createInputDispatcher(
    testContext: TestContext,
    root: RootForTest
): InputDispatcher

/**
 * Dispatcher to inject any kind of input. An [InputDispatcher] is created at the
 * beginning of [performMultiModalInput] or the single modality alternatives, and disposed at the
 * end of that method. The state of all input modalities is persisted and restored on the next
 * invocation of [performMultiModalInput] (or an alternative).
 *
 * Dispatching input happens in two stages. In the first stage, all events are generated
 * (enqueued), using the `enqueue*` methods, and in the second stage all events are injected.
 * Clients of [InputDispatcher] should only call methods for the first stage listed below, the
 * second stage is handled by [performMultiModalInput] and friends.
 *
 * Touch input:
 * * [getCurrentTouchPosition]
 * * [enqueueTouchDown]
 * * [enqueueTouchMove]
 * * [updateTouchPointer]
 * * [enqueueTouchUp]
 * * [enqueueTouchCancel]
 *
 * Mouse input:
 * * [currentMousePosition]
 * * [enqueueMousePress]
 * * [enqueueMouseMove]
 * * [updateMousePosition]
 * * [enqueueMouseRelease]
 * * [enqueueMouseCancel]
 * * [enqueueMouseScroll]
 *
 * Rotary input:
 * * [enqueueRotaryScrollHorizontally]
 * * [enqueueRotaryScrollVertically]
 *
 * Key input:
 * * [enqueueKeyDown]
 * * [enqueueKeyUp]
 *
 * Chaining methods:
 * * [advanceEventTime]
 */
internal abstract class InputDispatcher(
    private val testContext: TestContext,
    private val root: RootForTest
) {
    companion object {
        /**
         * The default time between two successively injected events, 16 milliseconds. Events are
         * normally sent on every frame and thus follow the frame rate. On a 60Hz screen this is
         * ~16ms per frame.
         */
        var eventPeriodMillis = 16L
            internal set

        /**
         * The delay between a down event on a particular [Key] and the first repeat event on that
         * same key.
         */
        const val InitialRepeatDelay = 500L

        /**
         * The interval between subsequent repeats (after the initial repeat) on a particular key.
         */
        const val SubsequentRepeatDelay = 50L
    }

    /**
     * The eventTime of the next event.
     */
    protected var currentTime = testContext.currentTime

    /**
     * The state of the current touch gesture. If `null`, no touch gesture is in progress.
     */
    protected var partialGesture: PartialGesture? = null

    /**
     * The state of the mouse. The mouse state is always available. It starts at [Offset.Zero] in
     * not-entered state.
     */
    protected var mouseInputState: MouseInputState = MouseInputState()

    /**
     * The state of the keyboard keys. The key input state is always available.
     * It starts with no keys pressed down and the [KeyInputState.downTime] set to zero.
     */
    protected var keyInputState: KeyInputState = KeyInputState()

    /**
     * The state of the rotary button.
     */
    protected var rotaryInputState: RotaryInputState = RotaryInputState()

    /**
     * Indicates if a gesture is in progress or not. A gesture is in progress if at least one
     * finger is (still) touching the screen.
     */
    val isTouchInProgress: Boolean
        get() = partialGesture != null

    /**
     * Indicates whether caps lock is on or not.
     */
    val isCapsLockOn: Boolean get() = keyInputState.capsLockOn

    /**
     * Indicates whether num lock is on or not.
     */
    val isNumLockOn: Boolean get() = keyInputState.numLockOn

    /**
     * Indicates whether scroll lock is on or not.
     */
    val isScrollLockOn: Boolean get() = keyInputState.scrollLockOn

    init {
        val rootHash = identityHashCode(root)
        val state = testContext.states.remove(rootHash)
        if (state != null) {
            partialGesture = state.partialGesture
            mouseInputState = state.mouseInputState
            keyInputState = state.keyInputState
        }
    }

    protected open fun saveState(root: RootForTest?) {
        if (root != null) {
            val rootHash = identityHashCode(root)
            testContext.states[rootHash] =
                InputDispatcherState(
                    partialGesture,
                    mouseInputState,
                    keyInputState
                )
        }
    }

    @OptIn(InternalTestApi::class)
    private val TestContext.currentTime
        get() = testOwner.mainClock.currentTime

    private val RootForTest.bounds get() = semanticsOwner.rootSemanticsNode.boundsInRoot

    protected fun isWithinRootBounds(position: Offset): Boolean = root.bounds.contains(position)

    /**
     * Increases the current event time by [durationMillis].
     *
     * Depending on the [keyInputState], there may be repeat key events that need to be sent within
     * the given duration. If there are, the clock will be forwarded until it is time for the repeat
     * key event, the key event will be sent, and then the clock will be forwarded by the remaining
     * duration.
     *
     * @param durationMillis The duration of the delay. Must be positive
     */
    fun advanceEventTime(durationMillis: Long = eventPeriodMillis) {
        require(durationMillis >= 0) {
            "duration of a delay can only be positive, not $durationMillis"
        }

        val endTime = currentTime + durationMillis
        keyInputState.sendRepeatKeysIfNeeded(endTime)
        currentTime = endTime
    }

    /**
     * During a touch gesture, returns the position of the last touch event of the given
     * [pointerId]. Returns `null` if no touch gesture is in progress for that [pointerId].
     *
     * @param pointerId The id of the pointer for which to return the current position
     * @return The current position of the pointer with the given [pointerId], or `null` if the
     * pointer is not currently in use
     */
    fun getCurrentTouchPosition(pointerId: Int): Offset? {
        return partialGesture?.lastPositions?.get(pointerId)
    }

    /**
     * The current position of the mouse. If no mouse event has been sent yet, will be
     * [Offset.Zero].
     */
    val currentMousePosition: Offset get() = mouseInputState.lastPosition

    /**
     * Indicates if the given [key] is pressed down or not.
     *
     * @param key The key to be checked.
     * @return true if given [key] is pressed, otherwise false.
     */
    fun isKeyDown(key: Key): Boolean = keyInputState.isKeyDown(key)

    /**
     * Generates a down touch event at [position] for the pointer with the given [pointerId].
     * Starts a new touch gesture if no other [pointerId]s are down. Only possible if the
     * [pointerId] is not currently being used, although pointer ids may be reused during a touch
     * gesture.
     *
     * @param pointerId The id of the pointer, can be any number not yet in use by another pointer
     * @param position The coordinate of the down event
     *
     * @see enqueueTouchMove
     * @see updateTouchPointer
     * @see enqueueTouchUp
     * @see enqueueTouchCancel
     */
    fun enqueueTouchDown(pointerId: Int, position: Offset) {
        var gesture = partialGesture

        // Check if this pointer is not already down
        require(gesture == null || !gesture.lastPositions.containsKey(pointerId)) {
            "Cannot send DOWN event, a gesture is already in progress for pointer $pointerId"
        }

        if (mouseInputState.hasAnyButtonPressed) {
            // If mouse buttons are down, a touch gesture cancels the mouse gesture
            mouseInputState.enqueueCancel()
        } else if (mouseInputState.isEntered) {
            // If no mouse buttons were down, we may have been in hovered state
            mouseInputState.exitHover()
        }

        // Send a MOVE event if pointers have changed since the last event
        gesture?.flushPointerUpdates()

        // Start a new gesture, or add the pointerId to the existing gesture
        if (gesture == null) {
            gesture = PartialGesture(currentTime, position, pointerId)
            partialGesture = gesture
        } else {
            gesture.lastPositions[pointerId] = position
        }

        // Send the DOWN event
        gesture.enqueueDown(pointerId)
    }

    /**
     * Generates a move touch event without moving any of the pointers. Use this to commit all
     * changes in pointer location made with [updateTouchPointer]. The generated event will contain
     * the current position of all pointers.
     *
     * @see enqueueTouchDown
     * @see updateTouchPointer
     * @see enqueueTouchUp
     * @see enqueueTouchCancel
     * @see enqueueTouchMoves
     */
    fun enqueueTouchMove() {
        val gesture = checkNotNull(partialGesture) {
            "Cannot send MOVE event, no gesture is in progress"
        }
        gesture.enqueueMove()
        gesture.hasPointerUpdates = false
    }

    /**
     * Enqueue the current time+coordinates as a move event, with the historical parameters
     * preceding it (so that they are ultimately available from methods like
     * MotionEvent.getHistoricalX).
     *
     * @see enqueueTouchMove
     * @see TouchInjectionScope.moveWithHistory
     */
    fun enqueueTouchMoves(
        relativeHistoricalTimes: List<Long>,
        historicalCoordinates: List<List<Offset>>
    ) {
        val gesture = checkNotNull(partialGesture) {
            "Cannot send MOVE event, no gesture is in progress"
        }
        gesture.enqueueMoves(relativeHistoricalTimes, historicalCoordinates)
        gesture.hasPointerUpdates = false
    }

    /**
     * Updates the position of the touch pointer with the given [pointerId] to the given
     * [position], but does not generate a move touch event. Use this to move multiple pointers
     * simultaneously. To generate the next move touch event, which will contain the current
     * position of _all_ pointers (not just the moved ones), call [enqueueTouchMove]. If you move
     * one or more pointers and then call [enqueueTouchDown], without calling [enqueueTouchMove]
     * first, a move event will be generated right before that down event.
     *
     * @param pointerId The id of the pointer to move, as supplied in [enqueueTouchDown]
     * @param position The position to move the pointer to
     *
     * @see enqueueTouchDown
     * @see enqueueTouchMove
     * @see enqueueTouchUp
     * @see enqueueTouchCancel
     */
    fun updateTouchPointer(pointerId: Int, position: Offset) {
        val gesture = partialGesture

        // Check if this pointer is in the gesture
        check(gesture != null) {
            "Cannot move pointers, no gesture is in progress"
        }
        require(gesture.lastPositions.containsKey(pointerId)) {
            "Cannot move pointer $pointerId, it is not active in the current gesture"
        }

        gesture.lastPositions[pointerId] = position
        gesture.hasPointerUpdates = true
    }

    /**
     * Generates an up touch event for the given [pointerId] at the current position of that
     * pointer.
     *
     * @param pointerId The id of the pointer to lift up, as supplied in [enqueueTouchDown]
     *
     * @see enqueueTouchDown
     * @see updateTouchPointer
     * @see enqueueTouchMove
     * @see enqueueTouchCancel
     */
    fun enqueueTouchUp(pointerId: Int) {
        val gesture = partialGesture

        // Check if this pointer is in the gesture
        check(gesture != null) {
            "Cannot send UP event, no gesture is in progress"
        }
        require(gesture.lastPositions.containsKey(pointerId)) {
            "Cannot send UP event for pointer $pointerId, it is not active in the current gesture"
        }

        // First send the UP event
        gesture.enqueueUp(pointerId)

        // Then remove the pointer, and end the gesture if no pointers are left
        gesture.lastPositions.remove(pointerId)
        if (gesture.lastPositions.isEmpty()) {
            partialGesture = null
        }
    }

    /**
     * Generates a cancel touch event for the current touch gesture. Sent automatically when
     * mouse events are sent while a touch gesture is in progress.
     *
     * @see enqueueTouchDown
     * @see updateTouchPointer
     * @see enqueueTouchMove
     * @see enqueueTouchUp
     */
    fun enqueueTouchCancel() {
        val gesture = checkNotNull(partialGesture) {
            "Cannot send CANCEL event, no gesture is in progress"
        }
        gesture.enqueueCancel()
        partialGesture = null
    }

    /**
     * Generates a move event with all pointer locations, if any of the pointers has been moved by
     * [updateTouchPointer] since the last move event.
     */
    private fun PartialGesture.flushPointerUpdates() {
        if (hasPointerUpdates) {
            enqueueTouchMove()
        }
    }

    /**
     * Generates a mouse button pressed event for the given [buttonId]. This will generate all
     * required associated events as well, such as a down event if it is the first button being
     * pressed and an optional hover exit event.
     *
     * @param buttonId The id of the mouse button. This is platform dependent, use the values
     * defined by [MouseButton.buttonId].
     */
    fun enqueueMousePress(buttonId: Int) {
        val mouse = mouseInputState

        check(!mouse.isButtonPressed(buttonId)) {
            "Cannot send mouse button down event, button $buttonId is already pressed"
        }
        check(isWithinRootBounds(currentMousePosition) || mouse.hasAnyButtonPressed) {
            "Cannot start a mouse gesture outside the Compose root bounds, mouse position is " +
                "$currentMousePosition and bounds are ${root.bounds}"
        }
        if (partialGesture != null) {
            enqueueTouchCancel()
        }

        // Down time is when the first button is pressed
        if (mouse.hasNoButtonsPressed) {
            mouse.downTime = currentTime
        }
        mouse.setButtonBit(buttonId)

        // Exit hovering if necessary
        if (mouse.isEntered) {
            mouse.exitHover()
        }
        // down/move + press
        mouse.enqueuePress(buttonId)
    }

    /**
     * Generates a mouse move or hover event to the given [position]. If buttons are pressed, a
     * move event is generated, otherwise generates a hover event.
     *
     * @param position The new mouse position
     */
    fun enqueueMouseMove(position: Offset) {
        val mouse = mouseInputState

        // Touch needs to be cancelled, even if mouse is out of bounds
        if (partialGesture != null) {
            enqueueTouchCancel()
        }

        updateMousePosition(position)
        val isWithinBounds = isWithinRootBounds(position)

        if (isWithinBounds && !mouse.isEntered && mouse.hasNoButtonsPressed) {
            // If not yet hovering and no buttons pressed, enter hover state
            mouse.enterHover()
        } else if (!isWithinBounds && mouse.isEntered) {
            // If hovering, exit now
            mouse.exitHover()
        }
        mouse.enqueueMove()
    }

    /**
     * Updates the mouse position without sending an event. Useful if down, up or scroll events
     * need to be injected on a different location than the preceding move event.
     *
     * @param position The new mouse position
     */
    fun updateMousePosition(position: Offset) {
        mouseInputState.lastPosition = position
        // Contrary to touch input, we don't need to store that the position has changed, because
        // all events that are affected send the current position regardless.
    }

    /**
     * Generates a mouse button released event for the given [buttonId]. This will generate all
     * required associated events as well, such as an up and hover enter event if it is the last
     * button being released.
     *
     * @param buttonId The id of the mouse button. This is platform dependent, use the values
     * defined by [MouseButton.buttonId].
     */
    fun enqueueMouseRelease(buttonId: Int) {
        val mouse = mouseInputState

        check(mouse.isButtonPressed(buttonId)) {
            "Cannot send mouse button up event, button $buttonId is not pressed"
        }
        check(partialGesture == null) {
            "Touch gesture can't be in progress, mouse buttons are down"
        }

        mouse.unsetButtonBit(buttonId)
        mouse.enqueueRelease(buttonId)

        // When no buttons remaining, enter hover state immediately
        if (mouse.hasNoButtonsPressed && isWithinRootBounds(currentMousePosition)) {
            mouse.enterHover()
            mouse.enqueueMove()
        }
    }

    /**
     * Generates a mouse hover enter event on the given [position].
     *
     * @param position The new mouse position
     */
    fun enqueueMouseEnter(position: Offset) {
        val mouse = mouseInputState

        check(!mouse.isEntered) {
            "Cannot send mouse hover enter event, mouse is already hovering"
        }
        check(mouse.hasNoButtonsPressed) {
            "Cannot send mouse hover enter event, mouse buttons are down"
        }
        check(isWithinRootBounds(position)) {
            "Cannot send mouse hover enter event, $position is out of bounds"
        }

        updateMousePosition(position)
        mouse.enterHover()
    }

    /**
     * Generates a mouse hover exit event on the given [position].
     *
     * @param position The new mouse position
     */
    fun enqueueMouseExit(position: Offset) {
        val mouse = mouseInputState

        check(mouse.isEntered) {
            "Cannot send mouse hover exit event, mouse is not hovering"
        }

        updateMousePosition(position)
        mouse.exitHover()
    }

    /**
     * Generates a mouse cancel event. Can only be done if no mouse buttons are currently
     * pressed. Sent automatically if a touch event is sent while mouse buttons are down.
     */
    fun enqueueMouseCancel() {
        val mouse = mouseInputState
        check(mouse.hasAnyButtonPressed) {
            "Cannot send mouse cancel event, no mouse buttons are pressed"
        }
        mouse.clearButtonState()
        mouse.enqueueCancel()
    }

    /**
     * Generates a scroll event on [scrollWheel] by [delta]. Negative values correspond to
     * rotating the scroll wheel leftward or upward, positive values correspond to rotating the
     * scroll wheel rightward or downward.
     */
    // TODO(fresen): verify the sign of the horizontal scroll axis (is left negative or positive?)
    @OptIn(ExperimentalTestApi::class)
    fun enqueueMouseScroll(delta: Float, scrollWheel: ScrollWheel) {
        val mouse = mouseInputState

        // A scroll is always preceded by a move(/hover) event
        enqueueMouseMove(currentMousePosition)
        if (isWithinRootBounds(currentMousePosition)) {
            mouse.enqueueScroll(delta, scrollWheel)
        }
    }

    /**
     * Generates a key down event for the given [key].
     *
     * @param key The keyboard key to be pushed down. Platform specific.
     */
    fun enqueueKeyDown(key: Key) {
        val keyboard = keyInputState

        check(!keyboard.isKeyDown(key)) {
            "Cannot send key down event, Key($key) is already pressed down."
        }

        // TODO(Onadim): Figure out whether key input needs to enqueue a touch cancel.
        // Down time is the time of the most recent key down event, which is now.
        keyboard.downTime = currentTime

        // Add key to pressed keys.
        keyboard.setKeyDown(key)

        keyboard.enqueueDown(key)
    }

    /**
     * Generates a key up event for the given [key].
     *
     * @param key The keyboard key to be released. Platform specific.
     */
    fun enqueueKeyUp(key: Key) {
        val keyboard = keyInputState

        check(keyboard.isKeyDown(key)) {
            "Cannot send key up event, Key($key) is not pressed down."
        }

        // TODO(Onadim): Figure out whether key input needs to enqueue a touch cancel.
        // Remove key from pressed keys.
        keyboard.setKeyUp(key)

        // Send the up event
        keyboard.enqueueUp(key)
    }

    fun enqueueRotaryScrollHorizontally(horizontalScrollPixels: Float) {
        // TODO(b/214437966): figure out if ongoing scroll events need to be cancelled.
        rotaryInputState.enqueueRotaryScrollHorizontally(horizontalScrollPixels)
    }

    fun enqueueRotaryScrollVertically(verticalScrollPixels: Float) {
        // TODO(b/214437966): figure out if ongoing scroll events need to be cancelled.
        rotaryInputState.enqueueRotaryScrollHorizontally(verticalScrollPixels)
    }

    private fun MouseInputState.enterHover() {
        enqueueEnter()
        isEntered = true
    }

    private fun MouseInputState.exitHover() {
        enqueueExit()
        isEntered = false
    }

    /**
     * Sends any and all repeat key events that are required between [currentTime] and [endTime].
     *
     * Mutates the value of [currentTime] in order to send each of the repeat events at exactly the
     * time it should be sent.
     *
     * @param endTime All repeats set to occur before this time will be sent.
     */
    // TODO(b/236623354): Extend repeat key event support to [MainTestClock.advanceTimeBy].
    private fun KeyInputState.sendRepeatKeysIfNeeded(endTime: Long) {

        // Return if there is no key to repeat or if it is not yet time to repeat it.
        if (repeatKey == null || endTime - downTime < InitialRepeatDelay) return

        // Initial repeat
        if (lastRepeatTime <= downTime) {
            // Not yet had a repeat on this key, but it needs at least the initial one.
            check(repeatCount == 0) {
                "repeatCount should be reset to 0 when downTime updates"
            }
            repeatCount = 1

            lastRepeatTime = downTime + InitialRepeatDelay
            currentTime = lastRepeatTime

            enqueueRepeat()
        }

        // Subsequent repeats
        val numRepeats: Int = ((endTime - lastRepeatTime) / SubsequentRepeatDelay).toInt()

        repeat(numRepeats) {
            repeatCount += 1
            lastRepeatTime += SubsequentRepeatDelay
            currentTime = lastRepeatTime
            enqueueRepeat()
        }
    }

    /**
     * Enqueues a key down event on the repeat key, if there is one. If the repeat key is null,
     * an [IllegalStateException] is thrown.
     */
    private fun KeyInputState.enqueueRepeat() {
        val repKey = checkNotNull(repeatKey) {
            "A repeat key event cannot be sent if the repeat key is null."
        }
        keyInputState.enqueueDown(repKey)
    }

    /**
     * Sends all enqueued events and blocks while they are dispatched. If an exception is
     * thrown during the process, all events that haven't yet been dispatched will be dropped.
     */
    abstract fun flush()

    protected abstract fun PartialGesture.enqueueDown(pointerId: Int)

    protected abstract fun PartialGesture.enqueueMove()

    protected abstract fun PartialGesture.enqueueMoves(
        relativeHistoricalTimes: List<Long>,
        historicalCoordinates: List<List<Offset>>
    )

    protected abstract fun PartialGesture.enqueueUp(pointerId: Int)

    protected abstract fun PartialGesture.enqueueCancel()

    protected abstract fun MouseInputState.enqueuePress(buttonId: Int)

    protected abstract fun MouseInputState.enqueueMove()

    protected abstract fun MouseInputState.enqueueRelease(buttonId: Int)

    protected abstract fun MouseInputState.enqueueEnter()

    protected abstract fun MouseInputState.enqueueExit()

    protected abstract fun MouseInputState.enqueueCancel()

    protected abstract fun KeyInputState.enqueueDown(key: Key)

    protected abstract fun KeyInputState.enqueueUp(key: Key)

    @OptIn(ExperimentalTestApi::class)
    protected abstract fun MouseInputState.enqueueScroll(delta: Float, scrollWheel: ScrollWheel)

    protected abstract fun RotaryInputState.enqueueRotaryScrollHorizontally(
        horizontalScrollPixels: Float
    )

    protected abstract fun RotaryInputState.enqueueRotaryScrollVertically(
        verticalScrollPixels: Float
    )

    /**
     * Called when this [InputDispatcher] is about to be discarded, from
     * [InjectionScope.dispose].
     */
    fun dispose() {
        saveState(root)
        onDispose()
    }

    /**
     * Override this method to take platform specific action when this dispatcher is disposed.
     * E.g. to recycle event objects that the dispatcher still holds on to.
     */
    protected open fun onDispose() {}
}

/**
 * The state of the current gesture. Contains the current position of all pointers and the
 * down time (start time) of the gesture. For the current time, see [InputDispatcher.currentTime].
 *
 * @param downTime The time of the first down event of this gesture
 * @param startPosition The position of the first down event of this gesture
 * @param pointerId The pointer id of the first down event of this gesture
 */
internal class PartialGesture(val downTime: Long, startPosition: Offset, pointerId: Int) {
    val lastPositions = mutableMapOf(Pair(pointerId, startPosition))
    var hasPointerUpdates: Boolean = false
}

/**
 * The current mouse state. Contains the current mouse position, which buttons are pressed, if it
 * is hovering over the current node and the down time of the mouse (which is the time of the
 * last mouse down event).
 */
internal class MouseInputState {
    var downTime: Long = 0
    val pressedButtons: MutableSet<Int> = mutableSetOf()
    var lastPosition: Offset = Offset.Zero
    var isEntered: Boolean = false

    val hasAnyButtonPressed get() = pressedButtons.isNotEmpty()
    val hasOneButtonPressed get() = pressedButtons.size == 1
    val hasNoButtonsPressed get() = pressedButtons.isEmpty()

    fun isButtonPressed(buttonId: Int): Boolean {
        return pressedButtons.contains(buttonId)
    }

    fun setButtonBit(buttonId: Int) {
        pressedButtons.add(buttonId)
    }

    fun unsetButtonBit(buttonId: Int) {
        pressedButtons.remove(buttonId)
    }

    fun clearButtonState() {
        pressedButtons.clear()
    }
}

/**
 * The current key input state. Contains the keys that are pressed, the down time of the
 * keyboard (which is the time of the last key down event), the state of the lock keys and
 * the device ID.
 */
internal class KeyInputState {
    private val downKeys: HashSet<Key> = hashSetOf()

    var downTime = 0L
    var repeatKey: Key? = null
    var repeatCount = 0
    var lastRepeatTime = downTime
    var capsLockOn = false
    var numLockOn = false
    var scrollLockOn = false

    fun isKeyDown(key: Key): Boolean = downKeys.contains(key)

    fun setKeyUp(key: Key) {
        downKeys.remove(key)
        if (key == repeatKey) {
            repeatKey = null
            repeatCount = 0
        }
    }

    fun setKeyDown(key: Key) {
        downKeys.add(key)
        repeatKey = key
        repeatCount = 0
        updateLockKeys(key)
    }

    /**
     * Updates lock key state values.
     *
     * Note that lock keys may not be toggled in the same way across all platforms.
     *
     * Take caps lock as an example; consistently, all platforms turn caps lock on upon the first
     * key down event, and it stays on after the subsequent key up. However, on some platforms caps
     * lock will turn off immediately upon the next key down event (MacOS for example), whereas
     * other platforms (e.g. linux) wait for the next key up event before turning caps lock off.
     *
     * By calling this function whenever a lock key is pressed down, MacOS-like behaviour is
     * achieved.
     */
    // TODO(Onadim): Investigate how lock key toggling is handled in Android, ChromeOS and Windows.
    @OptIn(ExperimentalComposeUiApi::class)
    private fun updateLockKeys(key: Key) {
        when (key) {
            Key.CapsLock -> capsLockOn = !capsLockOn
            Key.NumLock -> numLockOn = !numLockOn
            Key.ScrollLock -> scrollLockOn = !scrollLockOn
        }
    }
}

/**
 * We don't have any state associated with RotaryInput, but we use a RotaryInputState class for
 * consistency with the other APIs.
 */
internal class RotaryInputState

/**
 * The state of an [InputDispatcher], saved when the [GestureScope] is disposed and restored
 * when the [GestureScope] is recreated.
 *
 * @param partialGesture The state of an incomplete gesture. If no gesture was in progress
 * when the state of the [InputDispatcher] was saved, this will be `null`.
 * @param mouseInputState The state of the mouse.
 * @param keyInputState The state of the keyboard.
 */
internal data class InputDispatcherState(
    val partialGesture: PartialGesture?,
    val mouseInputState: MouseInputState,
    val keyInputState: KeyInputState
)
