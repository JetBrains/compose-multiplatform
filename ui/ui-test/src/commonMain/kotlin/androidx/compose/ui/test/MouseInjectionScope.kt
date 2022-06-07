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

package androidx.compose.ui.test

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.geometry.lerp
import androidx.compose.ui.platform.ViewConfiguration
import androidx.compose.ui.util.lerp
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * The time between the button pressed and button released event in a mouse click. Determined by
 * empirical sampling.
 */
private const val SingleClickDelayMillis = 60L

/**
 * The default duration of mouse gestures with configurable time (e.g. [animateTo]).
 */
private const val DefaultMouseGestureDurationMillis: Long = 300L

/**
 * The receiver scope of the mouse input injection lambda from [performMouseInput].
 *
 * The functions in [MouseInjectionScope] can roughly be divided into two groups: full gestures
 * and individual mouse events. The individual mouse events are: [press], [moveTo] and friends,
 * [release], [cancel], [scroll] and [advanceEventTime]. Full gestures are all the other
 * functions, like [MouseInjectionScope.click], [MouseInjectionScope.doubleClick],
 * [MouseInjectionScope.animateTo], etc. These are built on top of the individual events and
 * serve as a good example on how you can build your own full gesture functions.
 *
 * A mouse move event can be sent with [moveTo] and [moveBy]. The mouse position can be updated
 * with [updatePointerTo] and [updatePointerBy], which will not send an event and only update the
 * position internally. This can be useful if you want to send an event that is not a move event
 * with a location other then the current location, but without sending a preceding move event.
 * Use [press] and [release] to send button pressed and button released events. This will also
 * send all other necessary events that keep the stream of mouse events consistent with actual
 * mouse input, such as a hover exit event. A [cancel] event can be sent at any time when at
 * least one button is pressed. Use [scroll] to send a mouse scroll event.
 *
 * The entire event injection state is shared between all `perform.*Input` methods, meaning you
 * can continue an unfinished mouse gesture in a subsequent invocation of [performMouseInput] or
 * [performMultiModalInput]. Note however that while the mouse's position is retained across
 * invocation of `perform.*Input` methods, it is always manipulated in the current node's local
 * coordinate system. That means that two subsequent invocations of [performMouseInput] on
 * different nodes will report a different [currentPosition], even though it is actually the same
 * position on the screen.
 *
 * All events sent by these methods are batched together and sent as a whole after
 * [performMouseInput] has executed its code block.
 *
 * Example of performing a mouse click:
 * @sample androidx.compose.ui.test.samples.mouseInputClick
 *
 * Example of scrolling the mouse wheel while the mouse button is pressed:
 * @sample androidx.compose.ui.test.samples.mouseInputScrollWhileDown
 *
 * @see InjectionScope
 */
@Suppress("NotCloseable")
@ExperimentalTestApi
interface MouseInjectionScope : InjectionScope {
    /**
     * Returns the current position of the mouse. The position is returned in the local
     * coordinate system of the node with which we're interacting. (0, 0) is the top left corner
     * of the node. If none of the move or updatePointer methods have been used yet, the mouse's
     * position will be (0, 0) in the Compose host's coordinate system, which will be
     * `-[topLeft]` in the node's local coordinate system.
     */
    val currentPosition: Offset

    /**
     * Sends a move event [delayMillis] after the last sent event on the associated node, with
     * the position of the mouse updated to [position]. The [position] is in the node's local
     * coordinate system, where (0, 0) is the top left corner of the node.
     *
     * If no mouse buttons are pressed, a hover event will be sent instead of a move event. If
     * the mouse wasn't hovering yet, a hover enter event is sent as well.
     *
     * @param position The new position of the mouse, in the node's local coordinate system
     * @param delayMillis The time between the last sent event and this event.
     * [eventPeriodMillis] by default.
     */
    fun moveTo(position: Offset, delayMillis: Long = eventPeriodMillis)

    /**
     * Sends a move event [delayMillis] after the last sent event on the associated node, with
     * the position of the mouse moved by the given [delta].
     *
     * If no mouse buttons are pressed, a hover event will be sent instead of a move event. If
     * the mouse wasn't hovering yet, a hover enter event is sent as well.
     *
     * @param delta The position for this move event, relative to the current position of the
     * mouse. For example, `delta = Offset(10.px, -10.px) will add 10.px to the mouse's
     * x-position, and subtract 10.px from the mouse's y-position.
     * @param delayMillis The time between the last sent event and this event.
     * [eventPeriodMillis] by default.
     */
    fun moveBy(delta: Offset, delayMillis: Long = eventPeriodMillis) {
        moveTo(currentPosition + delta, delayMillis)
    }

    /**
     * Updates the position of the mouse to the given [position], but does not send a move or
     * hover event. This can be useful to adjust the mouse position before sending for example a
     * [press] event. The [position] is in the node's local coordinate system, where (0.px, 0.px)
     * is the top left corner of the node.
     *
     * @param position The new position of the mouse, in the node's local coordinate system
     */
    fun updatePointerTo(position: Offset)

    /**
     * Updates the position of the mouse by the given [delta], but does not send a move or hover
     * event. This can be useful to adjust the mouse position before sending for example a
     * [press] event.
     *
     * @param delta The position for this move event, relative to the current position of the
     * mouse. For example, `delta = Offset(10.px, -10.px) will add 10.px to the mouse's
     * x-position, and subtract 10.px from the mouse's y-position.
     */
    fun updatePointerBy(delta: Offset) {
        updatePointerTo(currentPosition + delta)
    }

    /**
     * Sends a down and button pressed event for the given [button] on the associated node. When
     * no buttons were down yet, this will exit hovering mode before the button is pressed. All
     * events will be sent at the current event time.
     *
     * Throws an [IllegalStateException] if the [button] is already pressed.
     *
     * @param button The mouse button that is pressed. By default the primary mouse button.
     */
    fun press(button: MouseButton = MouseButton.Primary)

    /**
     * Sends a button released and up event for the given [button] on the associated node. If
     * this was the last button to be released, the mouse will enter hovering mode and send an
     * accompanying mouse move event after the button has been released. All events will be sent
     * at the current event time.
     *
     * Throws an [IllegalStateException] if the [button] is not pressed.
     *
     * @param button The mouse button that is released. By default the primary mouse button.
     */
    fun release(button: MouseButton = MouseButton.Primary)

    /**
     * Sends a cancel event [delayMillis] after the last sent event to cancel a stream of mouse
     * events with pressed mouse buttons. All buttons will be released as a result. A mouse
     * cancel event can only be sent when mouse buttons are pressed.
     *
     * @param delayMillis The time between the last sent event and this event.
     * [eventPeriodMillis] by default.
     */
    fun cancel(delayMillis: Long = eventPeriodMillis)

    /**
     * Sends a hover enter event at the given [position], [delayMillis] after the last sent event.
     *
     * Note that it is discouraged to manually send a hover enter event followed by a [moveTo]
     * event. [moveTo] does this for you automatically, making sure the event times of the two
     * events are in sync. Only use this method for special scenarios where the hover enter event
     * is not sent as a side effect of another event.
     *
     * An [IllegalStateException] will be thrown when mouse buttons are down, or if the mouse is
     * already hovering.
     *
     * The [position] is in the node's local coordinate system, where (0, 0) is the top left
     * corner of the node.
     *
     * @param position The new position of the mouse, in the node's local coordinate system.
     * [currentPosition] by default.
     * @param delayMillis The time between the last sent event and this event.
     * [eventPeriodMillis] by default.
     */
    fun enter(position: Offset = currentPosition, delayMillis: Long = eventPeriodMillis)

    /**
     * Sends a hover exit event at the given [position], [delayMillis] after the last sent event.
     *
     * Note that it is discouraged to manually send a hover exit event followed by a [moveTo]
     * that is outside the boundaries of the Compose root or [press]ing a button. These methods
     * do this for you automatically, making sure the event times of the two events are in sync.
     * Only use this method for special scenarios where the hover exit event is not sent as a
     * side effect of another event.
     *
     * An [IllegalStateException] will be thrown if the mouse was not hovering.
     *
     * The [position] is in the node's local coordinate system, where (0, 0) is the top left
     * corner of the node.
     *
     * @param position The new position of the mouse, in the node's local coordinate system
     * [currentPosition] by default.
     * @param delayMillis The time between the last sent event and this event.
     * [eventPeriodMillis] by default.
     */
    fun exit(position: Offset = currentPosition, delayMillis: Long = eventPeriodMillis)

    /**
     * Sends a scroll event with the given [delta] on the given [scrollWheel]. The event will be
     * sent at the current event time. Negative [delta] values correspond to rotating the scroll
     * wheel leftward or upward, positive values correspond to rotating the scroll wheel
     * rightward or downward.
     *
     * @param delta The amount of scroll
     * @param scrollWheel Which scroll wheel to rotate. Can be either [ScrollWheel.Vertical] (the
     * default) or [ScrollWheel.Horizontal].
     */
    fun scroll(delta: Float, scrollWheel: ScrollWheel = ScrollWheel.Vertical)
}

@ExperimentalTestApi
internal class MouseInjectionScopeImpl(
    private val baseScope: MultiModalInjectionScopeImpl
) : MouseInjectionScope, InjectionScope by baseScope {
    private val inputDispatcher get() = baseScope.inputDispatcher
    private fun localToRoot(position: Offset) = baseScope.localToRoot(position)

    override val currentPosition: Offset
        get() = baseScope.rootToLocal(inputDispatcher.currentMousePosition)

    override fun moveTo(position: Offset, delayMillis: Long) {
        advanceEventTime(delayMillis)
        val positionInRoot = localToRoot(position)
        inputDispatcher.enqueueMouseMove(positionInRoot)
    }

    override fun updatePointerTo(position: Offset) {
        val positionInRoot = localToRoot(position)
        inputDispatcher.updateMousePosition(positionInRoot)
    }

    override fun press(button: MouseButton) {
        inputDispatcher.enqueueMousePress(button.buttonId)
    }

    override fun release(button: MouseButton) {
        inputDispatcher.enqueueMouseRelease(button.buttonId)
    }

    override fun enter(position: Offset, delayMillis: Long) {
        advanceEventTime(delayMillis)
        val positionInRoot = localToRoot(position)
        inputDispatcher.enqueueMouseEnter(positionInRoot)
    }

    override fun exit(position: Offset, delayMillis: Long) {
        advanceEventTime(delayMillis)
        val positionInRoot = localToRoot(position)
        inputDispatcher.enqueueMouseExit(positionInRoot)
    }

    override fun cancel(delayMillis: Long) {
        advanceEventTime(delayMillis)
        inputDispatcher.enqueueMouseCancel()
    }

    override fun scroll(delta: Float, scrollWheel: ScrollWheel) {
        inputDispatcher.enqueueMouseScroll(delta, scrollWheel)
    }
}

/**
 * Click on [position], or on the current mouse position if [position] is
 * [unspecified][Offset.Unspecified]. The [position] is in the node's local coordinate system,
 * where (0, 0) is the top left corner of the node.
 *
 * @param position The position where to click, in the node's local coordinate system. If
 * omitted, the [center] of the node will be used. If [unspecified][Offset.Unspecified], clicks
 * on the current mouse position.
 */
@ExperimentalTestApi
fun MouseInjectionScope.click(position: Offset = center) {
    if (position.isSpecified) {
        updatePointerTo(position)
    }
    press(MouseButton.Primary)
    advanceEventTime(SingleClickDelayMillis)
    release(MouseButton.Primary)
}

/**
 * Secondary-click on [position], or on the current mouse position if [position] is
 * [unspecified][Offset.Unspecified]. While the secondary mouse button is not necessarily the
 * right mouse button (e.g. on left-handed mice), this method is still called `rightClick` for
 * it's widespread use. The [position] is in the node's local coordinate system, where (0, 0) is
 * the top left corner of the node.
 *
 * @param position The position where to click, in the node's local coordinate system. If
 * omitted, the [center] of the node will be used. If [unspecified][Offset.Unspecified], clicks
 * on the current mouse position.
 */
@ExperimentalTestApi
fun MouseInjectionScope.rightClick(position: Offset = center) {
    if (position.isSpecified) {
        updatePointerTo(position)
    }
    press(MouseButton.Secondary)
    advanceEventTime(SingleClickDelayMillis)
    release(MouseButton.Secondary)
}

// The average of min and max is a safe default
private val ViewConfiguration.defaultDoubleTapDelayMillis: Long
    get() = (doubleTapMinTimeMillis + doubleTapTimeoutMillis) / 2

/**
 * Double-click on [position], or on the current mouse position if [position] is
 * [unspecified][Offset.Unspecified]. The [position] is in the node's local coordinate system,
 * where (0, 0) is the top left corner of the node.
 *
 * @param position The position where to click, in the node's local coordinate system. If
 * omitted, the [center] of the node will be used. If [unspecified][Offset.Unspecified], clicks
 * on the current mouse position.
 */
@ExperimentalTestApi
fun MouseInjectionScope.doubleClick(position: Offset = center) {
    click(position)
    advanceEventTime(viewConfiguration.defaultDoubleTapDelayMillis)
    click(position)
}

/**
 * Triple-click on [position], or on the current mouse position if [position] is
 * [unspecified][Offset.Unspecified]. The [position] is in the node's local coordinate system,
 * where (0, 0) is the top left corner of the node.
 *
 * @param position The position where to click, in the node's local coordinate system. If
 * omitted, the [center] of the node will be used. If [unspecified][Offset.Unspecified], clicks
 * on the current mouse position.
 */
@ExperimentalTestApi
fun MouseInjectionScope.tripleClick(position: Offset = center) {
    click(position)
    advanceEventTime(viewConfiguration.defaultDoubleTapDelayMillis)
    click(position)
    advanceEventTime(viewConfiguration.defaultDoubleTapDelayMillis)
    click(position)
}

/**
 * Long-click on [position], or on the current mouse position if [position] is
 * [unspecified][Offset.Unspecified]. The [position] is in the node's local coordinate system,
 * where (0, 0) is the top left corner of the node.
 *
 * @param position The position where to click, in the node's local coordinate system. If
 * omitted, the [center] of the node will be used. If [unspecified][Offset.Unspecified], clicks
 * on the current mouse position.
 */
@ExperimentalTestApi
fun MouseInjectionScope.longClick(position: Offset = center) {
    if (position.isSpecified) {
        updatePointerTo(position)
    }
    press(MouseButton.Primary)
    advanceEventTime(viewConfiguration.longPressTimeoutMillis + 100L)
    release(MouseButton.Primary)
}

/**
 * Move the mouse from the [current position][MouseInjectionScope.currentPosition] to the given
 * [position], sending a stream of move events to get an animated path of [durationMillis]
 * milliseconds. [Move][moveTo] the mouse to the desired start position if you want to start from
 * a different position. The [position] is in the node's local coordinate system, where (0, 0) is
 * the top left corner of the node.
 *
 * @param position The position where to move the mouse to, in the node's local coordinate system
 * @param durationMillis The duration of the gesture. By default 300 milliseconds.
 */
@ExperimentalTestApi
fun MouseInjectionScope.animateTo(
    position: Offset,
    durationMillis: Long = DefaultMouseGestureDurationMillis
) {
    val durationFloat = durationMillis.toFloat()
    val start = currentPosition
    animateAlong(
        curve = { lerp(start, position, it / durationFloat) },
        durationMillis = durationMillis
    )
}

/**
 * Move the mouse from the [current position][MouseInjectionScope.currentPosition] by the given
 * [delta], sending a stream of move events to get an animated path of [durationMillis]
 * milliseconds.
 *
 * @param delta The position where to move the mouse to, relative to the current position of the
 * mouse. For example, `delta = Offset(100.px, -100.px) will move the mouse 100 pixels to the
 * right and 100 pixels upwards.
 * @param durationMillis The duration of the gesture. By default 300 milliseconds.
 */
@ExperimentalTestApi
fun MouseInjectionScope.animateBy(
    delta: Offset,
    durationMillis: Long = DefaultMouseGestureDurationMillis
) {
    animateTo(currentPosition + delta, durationMillis)
}

/**
 * Move the mouse along the given [curve], sending a stream of move events to get an animated
 * path of [durationMillis] milliseconds. The mouse will initially be moved to the start of the
 * path, `curve(0)`, if it is not already there. The positions defined by the [curve] are in the
 * node's local coordinate system, where (0, 0) is the top left corner of the node.
 *
 * @param curve The function that defines the position of the mouse over time for this gesture,
 * in the node's local coordinate system.
 * @param durationMillis The duration of the gesture. By default 300 milliseconds.
 */
@ExperimentalTestApi
fun MouseInjectionScope.animateAlong(
    curve: (Long) -> Offset,
    durationMillis: Long = DefaultMouseGestureDurationMillis
) {
    require(durationMillis > 0) {
        "Duration is 0"
    }
    val start = curve(0)
    if (start != currentPosition) {
        // Instantly move to the start position to maintain the total durationMillis
        moveTo(curve(0), delayMillis = 0)
    }

    var step = 0
    // How many steps will we take in durationMillis?
    // At least 1, and a number that will bring as as close to eventPeriod as possible
    val steps = max(1, (durationMillis / eventPeriodMillis.toFloat()).roundToInt())

    var tPrev = 0L
    while (step++ < steps) {
        val progress = step / steps.toFloat()
        val t = lerp(0, durationMillis, progress)
        moveTo(curve(t), delayMillis = t - tPrev)
        tPrev = t
    }
}

/**
 * Drag and drop something from [start] to [end] in [durationMillis] milliseconds. This gesture
 * uses the primary mouse button to drag with, and does not reset any mouse buttons prior to
 * starting the gesture. The mouse position is [updated][MouseInjectionScope.updatePointerTo] to
 * the start position before starting the gesture. The positions defined by the [start] and [end]
 * are in the node's local coordinate system, where (0, 0) is the top left corner of the node.
 *
 * @param start The position where to press the primary mouse button and initiate the drag, in
 * the node's local coordinate system.
 * @param end The position where to release the primary mouse button and end the drag, in the
 * node's local coordinate system.
 * @param durationMillis The duration of the gesture. By default 300 milliseconds.
 */
@ExperimentalTestApi
fun MouseInjectionScope.dragAndDrop(
    start: Offset,
    end: Offset,
    durationMillis: Long = DefaultMouseGestureDurationMillis
) {
    updatePointerTo(start)
    press(MouseButton.Primary)
    animateTo(end, durationMillis)
    release(MouseButton.Primary)
}

/**
 * Rotate the mouse's [scrollWheel] by the given [scrollAmount]. The total scroll delta is
 * linearly smoothed out over a stream of scroll events with roughly the
 * [InjectionScope.eventPeriodMillis] between each scroll event. Negative [scrollAmount] values
 * correspond to rotating the scroll wheel leftward or downward, positive values correspond to
 * rotating the scroll wheel rightward or upward.
 *
 * @param scrollAmount The total delta to scroll the [scrollWheel] by
 * @param durationMillis The duration of the gesture. By default 300 milliseconds.
 * @param scrollWheel Which scroll wheel will be rotated. By default [ScrollWheel.Vertical].
 */
@ExperimentalTestApi
fun MouseInjectionScope.smoothScroll(
    scrollAmount: Float,
    durationMillis: Long = DefaultMouseGestureDurationMillis,
    scrollWheel: ScrollWheel = ScrollWheel.Vertical
) {
    var step = 0
    // How many steps will we take in durationMillis?
    // At least 1, and a number that will bring as as close to eventPeriod as possible
    val steps = max(1, (durationMillis / eventPeriodMillis.toFloat()).roundToInt())

    var tPrev = 0L
    var valuePrev = 0f
    while (step++ < steps) {
        val progress = step / steps.toFloat()
        val t = lerp(0, durationMillis, progress)
        val value = lerp(0f, scrollAmount, progress)
        advanceEventTime(t - tPrev)
        scroll(value - valuePrev, scrollWheel)
        tPrev = t
        valuePrev = value
    }
}
