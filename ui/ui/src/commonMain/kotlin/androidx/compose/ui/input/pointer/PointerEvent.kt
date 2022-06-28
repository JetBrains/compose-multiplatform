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

package androidx.compose.ui.input.pointer

import androidx.compose.runtime.Immutable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.input.pointer.PointerEventPass.Final
import androidx.compose.ui.input.pointer.PointerEventPass.Initial
import androidx.compose.ui.input.pointer.PointerEventPass.Main
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.internal.JvmDefaultWithCompatibility

/**
 * A [Modifier.Element] that can interact with pointer input.
 */
@JvmDefaultWithCompatibility
interface PointerInputModifier : Modifier.Element {
    val pointerInputFilter: PointerInputFilter
}

/**
 * A PointerInputFilter represents a single entity that receives [PointerInputChange]s),
 * interprets them, and consumes the aspects of the changes that it is react to such that other
 * PointerInputFilters don't also react to them.
 */
abstract class PointerInputFilter {

    /**
     * Invoked when pointers that previously hit this [PointerInputFilter] have changed.
     *
     * @param pointerEvent The list of [PointerInputChange]s with positions relative to this
     * [PointerInputFilter].
     * @param pass The [PointerEventPass] in which this function is being called.
     * @param bounds The width and height associated with this [PointerInputFilter].
     * @return The list of [PointerInputChange]s after any aspect of the changes have been consumed.
     *
     * @see PointerInputChange
     * @see PointerEventPass
     */
    abstract fun onPointerEvent(
        pointerEvent: PointerEvent,
        pass: PointerEventPass,
        bounds: IntSize
    )

    /**
     * Invoked to notify the handler that no more calls to [PointerInputFilter] will be made, until
     * at least new pointers exist.  This can occur for a few reasons:
     * 1. Android dispatches ACTION_CANCEL to Compose.
     * 2. This [PointerInputFilter] is no longer associated with a LayoutNode.
     * 3. This [PointerInputFilter]'s associated LayoutNode is no longer in the composition tree.
     */
    abstract fun onCancel()

    internal var layoutCoordinates: LayoutCoordinates? = null

    /**
     * The layout size assigned to this [PointerInputFilter].
     */
    val size: IntSize
        get() = layoutCoordinates?.size ?: IntSize.Zero

    internal var isAttached: Boolean = false

    /**
     * Intercept pointer input that children receive even if the pointer is out of bounds.
     *
     * If `true`, and a child has been moved out of this layout and receives an event, this
     * will receive that event. If `false`, a child receiving pointer input outside of the
     * bounds of this layout will not trigger any events in this.
     */
    open val interceptOutOfBoundsChildEvents: Boolean
        get() = false

    /**
     * If `false`, then this [PointerInputFilter] will not allow siblings under it to respond
     * to events. If `true`, this will have the first chance to respond and the next sibling
     * under will then get a chance to respond as well. This trigger acts as at the Layout
     * level, so if any [PointerInputFilter]s on a Layout has [shareWithSiblings] set to `true`
     * then the Layout will share with siblings.
     */
    @Suppress("OPT_IN_MARKER_ON_WRONG_TARGET")
    @ExperimentalComposeUiApi
    @get:ExperimentalComposeUiApi
    open val shareWithSiblings: Boolean
        get() = false
}

/**
 * Describes a pointer input change event that has occurred at a particular point in time.
 */
expect class PointerEvent internal constructor(
    changes: List<PointerInputChange>,
    internalPointerEvent: InternalPointerEvent?
) {
    /**
     * @param changes The changes.
     */
    constructor(changes: List<PointerInputChange>)

    /**
     * The changes.
     */
    val changes: List<PointerInputChange>

    /**
     * The state of buttons (e.g. mouse or stylus buttons) during this event.
     */
    val buttons: PointerButtons

    /**
     * The state of modifier keys during this event.
     */
    val keyboardModifiers: PointerKeyboardModifiers

    /**
     * The primary reason the [PointerEvent] was sent.
     */
    var type: PointerEventType
        internal set
}

// TODO mark internal once https://youtrack.jetbrains.com/issue/KT-36695 is fixed
/* internal */ expect class NativePointerButtons

/**
 * Contains the state of pointer buttons (e.g. mouse and stylus buttons).
 */
@kotlin.jvm.JvmInline
value class PointerButtons(internal val packedValue: NativePointerButtons)

/**
 * `true` when the primary button (left mouse button) is pressed or `false` when
 * it isn't pressed.
 */
expect val PointerButtons.isPrimaryPressed: Boolean

/**
 * `true` when the secondary button (right mouse button) is pressed or `false` when
 * it isn't pressed.
 */
expect val PointerButtons.isSecondaryPressed: Boolean

/**
 * `true` when the tertiary button (middle mouse button) is pressed or `false` when
 * it isn't pressed.
 */
expect val PointerButtons.isTertiaryPressed: Boolean

/**
 * `true` when the back button (mouse back button) is pressed or `false` when it isn't pressed or
 * there is no mouse button assigned to "back."
 */
expect val PointerButtons.isBackPressed: Boolean

/**
 * `true` when the forward button (mouse forward button) is pressed or `false` when it isn't pressed
 * or there is no button assigned to "forward."
 */
expect val PointerButtons.isForwardPressed: Boolean

/**
 * Returns `true` when the button at [buttonIndex] is pressed and `false` when it isn't pressed.
 * This method can handle buttons that haven't been assigned a designated purpose like
 * [isPrimaryPressed] and [isSecondaryPressed].
 */
expect fun PointerButtons.isPressed(buttonIndex: Int): Boolean

/**
 * Returns `true` if any button is pressed or `false` if all buttons are released.
 */
expect val PointerButtons.areAnyPressed: Boolean

/**
 * Returns the index of first button pressed as used in [isPressed] or `-1` if no button is pressed.
 */
expect fun PointerButtons.indexOfFirstPressed(): Int

/**
 * Returns the index of last button pressed as used in [isPressed] or `-1` if no button is pressed.
 */
expect fun PointerButtons.indexOfLastPressed(): Int

// TODO mark internal once https://youtrack.jetbrains.com/issue/KT-36695 is fixed
/* internal */ expect class NativePointerKeyboardModifiers

/**
 * Contains the state of modifier keys, such as Shift, Control, and Alt, as well as the state
 * of the lock keys, such as Caps Lock and Num Lock.
 */
@kotlin.jvm.JvmInline
value class PointerKeyboardModifiers(internal val packedValue: NativePointerKeyboardModifiers)

// helps initialize `WindowInfo.keyboardModifiers` with a non-null value
internal expect fun EmptyPointerKeyboardModifiers(): PointerKeyboardModifiers

/**
 * `true` when the Control key is pressed.
 */
expect val PointerKeyboardModifiers.isCtrlPressed: Boolean

/**
 * `true` when the Meta key is pressed. This is commonly associated with the Windows or Command
 * key on some keyboards.
 */
expect val PointerKeyboardModifiers.isMetaPressed: Boolean

/**
 * `true` when the Alt key is pressed. This is commonly associated with the Option key on some
 * keyboards.
 */
expect val PointerKeyboardModifiers.isAltPressed: Boolean

/**
 * `true` when the AltGraph key is pressed.
 */
expect val PointerKeyboardModifiers.isAltGraphPressed: Boolean

/**
 * `true` when the Sym key is pressed.
 */
expect val PointerKeyboardModifiers.isSymPressed: Boolean

/**
 * `true` when the Shift key is pressed.
 */
expect val PointerKeyboardModifiers.isShiftPressed: Boolean

/**
 * `true` when the Function key is pressed.
 */
expect val PointerKeyboardModifiers.isFunctionPressed: Boolean

/**
 * `true` when the keyboard's Caps Lock is on.
 */
expect val PointerKeyboardModifiers.isCapsLockOn: Boolean

/**
 * `true` when the keyboard's Scroll Lock is on.
 */
expect val PointerKeyboardModifiers.isScrollLockOn: Boolean

/**
 * `true` when the keyboard's Num Lock is on.
 */
expect val PointerKeyboardModifiers.isNumLockOn: Boolean

/**
 * The device type that produces a [PointerInputChange], such as a mouse or stylus.
 */
@kotlin.jvm.JvmInline
value class PointerType private constructor(private val value: Int) {

    override fun toString(): String = when (value) {
        1 -> "Touch"
        2 -> "Mouse"
        3 -> "Stylus"
        4 -> "Eraser"
        else -> "Unknown"
    }

    companion object {
        /**
         * An unknown device type or the device type isn't relevant.
         */
        val Unknown = PointerType(0)

        /**
         * Touch (finger) input.
         */
        val Touch = PointerType(1)

        /**
         * A mouse pointer.
         */
        val Mouse = PointerType(2)

        /**
         * A stylus.
         */
        val Stylus = PointerType(3)

        /**
         * An eraser or an inverted stylus.
         */
        val Eraser = PointerType(4)
    }
}

/**
 * Indicates the primary reason that the [PointerEvent] was sent.
 */
@kotlin.jvm.JvmInline
value class PointerEventType private constructor(internal val value: Int) {
    companion object {
        /**
         * An unknown reason for the event.
         */
        val Unknown = PointerEventType(0)

        /**
         * A button on the device was pressed or a new pointer was detected.
         */
        val Press = PointerEventType(1)

        /**
         * A button on the device was released or a pointer was raised.
         */
        val Release = PointerEventType(2)

        /**
         * The cursor or one or more touch pointers was moved.
         */
        val Move = PointerEventType(3)

        /**
         * The cursor has entered the input region. This will only be sent after the cursor is
         * hovering when in the input region.
         *
         * For example, the user's cursor is outside the input region and presses the button
         * prior to entering the input region. The [Enter] event will be sent when the button
         * is released inside the input region.
         */
        val Enter = PointerEventType(4)

        /**
         * A cursor device or elevated stylus exited the input region. This will only follow
         * an [Enter] event, so if a cursor with the button pressed enters and exits region,
         * neither [Enter] nor [Exit] will be sent for the input region. However, if a cursor
         * enters the input region, then a button is pressed, then the cursor exits and reenters,
         * [Enter], [Exit], and [Enter] will be received.
         */
        val Exit = PointerEventType(5)

        /**
         * A scroll event was sent. This can happen, for example, due to a mouse scroll wheel.
         * This event indicates that the [PointerInputChange.scrollDelta]'s [Offset] is non-zero.
         */
        val Scroll = PointerEventType(6)
    }

    override fun toString(): String = when (this) {
        Press -> "Press"
        Release -> "Release"
        Move -> "Move"
        Enter -> "Enter"
        Exit -> "Exit"
        Scroll -> "Scroll"
        else -> "Unknown"
    }
}

/**
 * Describes a change that has occurred for a particular pointer, as well as how much of the change
 * has been consumed (meaning, used by a node in the UI).
 *
 * The [position] represents the position of the pointer relative to the element that
 * this [PointerInputChange] is being dispatched to.
 *
 * The [previousPosition] represents the position of the pointer offset to the current
 * position of the pointer relative to the screen.
 *
 * This means that [position] and [previousPosition] can always be used to understand how
 * much a pointer has moved relative to an element, even if that element is moving along with the
 * changes to the pointer.  For example, if a pointer touches a 1x1 pixel box in the middle,
 * [position] will report a position of (0, 0) when dispatched to it.  If the next event
 * moves x position 5 pixels, [position] will report (5, 0) and [previousPosition] will
 * report (0, 0). If the box moves all 5 pixels, and the next event represents the pointer moving
 * along the x axis for 5 more pixels, [position] will again report (5, 0) and
 * [previousPosition] will report (0, 0).
 *
 * @param id The unique id of the pointer associated with this [PointerInputChange].
 * @param uptimeMillis The time of the current pointer event, in milliseconds. The start (`0`) time
 * is platform-dependent
 * @param position The [Offset] of the current pointer event, relative to the containing
 * element
 * @param pressed `true` if the pointer event is considered "pressed." For example, finger
 * touching the screen or a mouse button is pressed [pressed] would be `true`.
 * @param previousUptimeMillis The [uptimeMillis] of the previous pointer event
 * @param previousPosition The [Offset] of the previous pointer event, offset to the
 * [position] and relative to the containing element.
 * @param previousPressed `true` if the pointer event was considered "pressed." For example , if
 * a finger was touching the screen or a mouse button was pressed, [previousPressed] would be
 * `true`.
 * @param isInitiallyConsumed whether the change was consumed from the start or not. This value
 * can change over time as change is propagated through the pointer handlers. To query the
 * actual status of the change use [isConsumed]
 * @param type The device type that produced the event, such as [mouse][PointerType.Mouse],
 * or [touch][PointerType.Touch].git
 * @param scrollDelta The amount of scroll wheel movement in the horizontal and vertical directions.
 */
@Immutable
@OptIn(ExperimentalComposeUiApi::class)
class PointerInputChange(
    val id: PointerId,
    val uptimeMillis: Long,
    val position: Offset,
    val pressed: Boolean,
    val previousUptimeMillis: Long,
    val previousPosition: Offset,
    val previousPressed: Boolean,
    isInitiallyConsumed: Boolean,
    val type: PointerType = PointerType.Touch,
    val scrollDelta: Offset = Offset.Zero
) {

    @Deprecated(
        level = DeprecationLevel.HIDDEN,
        replaceWith = ReplaceWith(
            "this(id, uptimeMillis, position, pressed, previousUptimeMillis," +
                " previousPosition, previousPressed," +
                " consumed.downChange || consumed.positionChange, type, Offset.Zero)"
        ),
        message = "Use another constructor with `scrollDelta` and without `ConsumedData` instead"
    )
    @Suppress("DEPRECATION")
    constructor(
        id: PointerId,
        uptimeMillis: Long,
        position: Offset,
        pressed: Boolean,
        previousUptimeMillis: Long,
        previousPosition: Offset,
        previousPressed: Boolean,
        consumed: ConsumedData,
        type: PointerType = PointerType.Touch
    ) : this(
        id,
        uptimeMillis,
        position,
        pressed,
        previousUptimeMillis,
        previousPosition,
        previousPressed,
        consumed.downChange || consumed.positionChange,
        type,
        Offset.Zero
    )

    internal constructor(
        id: PointerId,
        uptimeMillis: Long,
        position: Offset,
        pressed: Boolean,
        previousUptimeMillis: Long,
        previousPosition: Offset,
        previousPressed: Boolean,
        isInitiallyConsumed: Boolean,
        type: PointerType,
        historical: List<HistoricalChange>,
        scrollDelta: Offset,
    ) : this(
        id,
        uptimeMillis,
        position,
        pressed,
        previousUptimeMillis,
        previousPosition,
        previousPressed,
        isInitiallyConsumed,
        type,
        scrollDelta
    ) {
        _historical = historical
    }

    /**
     * Optional high-frequency pointer moves in between the last two dispatched events.
     * Can be used for extra accuracy when touchscreen rate exceeds framerate.
     */
    // With these experimental annotations, the API can be either cleanly removed or
    // stabilized. It doesn't appear in current.txt; and in experimental_current.txt,
    // it has the same effect as a primary constructor val.
    @Suppress("OPT_IN_MARKER_ON_WRONG_TARGET")
    @ExperimentalComposeUiApi
    @get:ExperimentalComposeUiApi
    val historical: List<HistoricalChange>
        get() = _historical ?: listOf()
    private var _historical: List<HistoricalChange>? = null

    /**
     * Indicates whether the change was consumed or not. Note that the change must be consumed in
     * full as there's no partial consumption system provided.
     */
    @Suppress("DEPRECATION")
    val isConsumed: Boolean
        get() = consumed.downChange || consumed.positionChange

    /**
     * Consume change event, claiming all the corresponding change info to the caller. This is
     * usually needed when, button, when being clicked, consumed the "up" event so no other parents
     * of this button could consume this "up" again.
     *
     * "Consumption" is just an indication of the claim and each pointer input handler
     * implementation must manually check this flag to respect it.
     */
    @Suppress("DEPRECATION")
    fun consume() {
        consumed.downChange = true
        consumed.positionChange = true
    }

    @Deprecated("use isConsumed and consume() pair of methods instead")
    @Suppress("DEPRECATION")
    var consumed: ConsumedData =
        ConsumedData(downChange = isInitiallyConsumed, positionChange = isInitiallyConsumed)
        private set

    @Deprecated(
        level = DeprecationLevel.HIDDEN,
        replaceWith = ReplaceWith(
            "copy(id,currentTime, currentPosition, currentPressed, previousTime," +
                "previousPosition, previousPressed, consumed, type, this.scrollDelta)"
        ),
        message = "Use another copy() method with scrollDelta parameter instead"
    )
    @Suppress("DEPRECATION")
    fun copy(
        id: PointerId = this.id,
        currentTime: Long = this.uptimeMillis,
        currentPosition: Offset = this.position,
        currentPressed: Boolean = this.pressed,
        previousTime: Long = this.previousUptimeMillis,
        previousPosition: Offset = this.previousPosition,
        previousPressed: Boolean = this.previousPressed,
        consumed: ConsumedData = this.consumed,
        type: PointerType = this.type,
    ): PointerInputChange = PointerInputChange(
        id,
        currentTime,
        currentPosition,
        currentPressed,
        previousTime,
        previousPosition,
        previousPressed,
        consumed.downChange || consumed.positionChange,
        type,
        this.historical,
        this.scrollDelta
    ).also {
        this.consumed = consumed
    }

    /**
     * Make a shallow copy of the [PointerInputChange]
     *
     * **NOTE:** Due to the need of the inner contract of the [PointerInputChange], this method
     * performs a shallow copy of the [PointerInputChange]. Any [consume] call between any of the
     * copies will consume any other copy automatically. Therefore, copy with the new [isConsumed]
     * is not possible. Consider creating a new [PointerInputChange]
     */
    @Suppress("DEPRECATION")
    fun copy(
        id: PointerId = this.id,
        currentTime: Long = this.uptimeMillis,
        currentPosition: Offset = this.position,
        currentPressed: Boolean = this.pressed,
        previousTime: Long = this.previousUptimeMillis,
        previousPosition: Offset = this.previousPosition,
        previousPressed: Boolean = this.previousPressed,
        type: PointerType = this.type,
        scrollDelta: Offset = this.scrollDelta
    ): PointerInputChange = PointerInputChange(
        id,
        currentTime,
        currentPosition,
        currentPressed,
        previousTime,
        previousPosition,
        previousPressed,
        isInitiallyConsumed = false, // doesn't matter, we will pass a holder anyway
        type,
        this.historical,
        scrollDelta
    ).also {
        it.consumed = this.consumed
    }

    @Suppress("DEPRECATION")
    @Deprecated(
        "Partial consumption has been deprecated. Use copy() instead without `consumed` " +
            "parameter to create a shallow copy or a constructor to create a new " +
            "PointerInputChange",
        replaceWith = ReplaceWith(
            "copy(id, currentTime, currentPosition, currentPressed, previousTime, " +
                "previousPosition, previousPressed, type, scrollDelta)"
        )
    )
    fun copy(
        id: PointerId = this.id,
        currentTime: Long = this.uptimeMillis,
        currentPosition: Offset = this.position,
        currentPressed: Boolean = this.pressed,
        previousTime: Long = this.previousUptimeMillis,
        previousPosition: Offset = this.previousPosition,
        previousPressed: Boolean = this.previousPressed,
        consumed: ConsumedData,
        type: PointerType = this.type,
        scrollDelta: Offset = this.scrollDelta
    ): PointerInputChange = PointerInputChange(
        id,
        currentTime,
        currentPosition,
        currentPressed,
        previousTime,
        previousPosition,
        previousPressed,
        consumed.downChange || consumed.positionChange,
        type,
        this.historical,
        scrollDelta
    ).also {
        this.consumed = consumed
    }

    /**
     * Make a shallow copy of the [PointerInputChange]
     *
     * **NOTE:** Due to the need of the inner contract of the [PointerInputChange], this method
     * performs a shallow copy of the [PointerInputChange]. Any [consume] call between any of the
     * copies will consume any other copy automatically. Therefore, copy with the new [isConsumed]
     * is not possible. Consider creating a new [PointerInputChange].
     */
    @ExperimentalComposeUiApi
    @Suppress("DEPRECATION")
    fun copy(
        id: PointerId = this.id,
        currentTime: Long = this.uptimeMillis,
        currentPosition: Offset = this.position,
        currentPressed: Boolean = this.pressed,
        previousTime: Long = this.previousUptimeMillis,
        previousPosition: Offset = this.previousPosition,
        previousPressed: Boolean = this.previousPressed,
        type: PointerType = this.type,
        historical: List<HistoricalChange>,
        scrollDelta: Offset = this.scrollDelta
    ): PointerInputChange = PointerInputChange(
        id,
        currentTime,
        currentPosition,
        currentPressed,
        previousTime,
        previousPosition,
        previousPressed,
        isInitiallyConsumed = false, // doesn't matter, we will pass a holder anyway
        type,
        historical,
        scrollDelta
    ).also {
        it.consumed = this.consumed
    }

    override fun toString(): String {
        return "PointerInputChange(id=$id, " +
            "uptimeMillis=$uptimeMillis, " +
            "position=$position, " +
            "pressed=$pressed, " +
            "previousUptimeMillis=$previousUptimeMillis, " +
            "previousPosition=$previousPosition, " +
            "previousPressed=$previousPressed, " +
            "isConsumed=$isConsumed, " +
            "type=$type, " +
            "historical=$historical," +
            "scrollDelta=$scrollDelta)"
    }
}

/**
 * Data structure for "historical" pointer moves.
 *
 * Optional high-frequency pointer moves in between the last two dispatched events:
 * can be used for extra accuracy when touchscreen rate exceeds framerate.
 *
 * @param uptimeMillis The time of the historical pointer event, in milliseconds. In between
 * the current and previous pointer event times.
 * @param position The [Offset] of the historical pointer event, relative to the containing
 * element.
 */
@Immutable
@ExperimentalComposeUiApi
class HistoricalChange(
    val uptimeMillis: Long,
    val position: Offset
) {
    override fun toString(): String {
        return "HistoricalChange(uptimeMillis=$uptimeMillis, " +
            "position=$position)"
    }
}

/**
 * An ID for a given pointer.
 *
 * @param value The actual value of the id.
 */
@kotlin.jvm.JvmInline
value class PointerId(val value: Long)

/**
 * Describes what aspects of a change has been consumed.
 *
 * @param positionChange True if a position change in this event has been consumed.
 * @param downChange True if a change to down or up has been consumed.
 */
@Deprecated("Use PointerInputChange.isConsumed and PointerInputChange.consume() instead")
class ConsumedData(
    @Suppress("GetterSetterNames")
    @get:Suppress("GetterSetterNames")
    @Deprecated("Partial consumption was deprecated. Use PointerEvent.isConsumed " +
        "and PointerEvent.consume() instead.")
    var positionChange: Boolean = false,

    @Suppress("GetterSetterNames")
    @get:Suppress("GetterSetterNames")
    @Deprecated("Partial consumption was deprecated. Use PointerEvent.isConsumed " +
        "and PointerEvent.consume() instead.")
    var downChange: Boolean = false
)

/**
 * The enumeration of passes where [PointerInputChange] traverses up and down the UI tree.
 *
 * PointerInputChanges traverse throw the hierarchy in the following passes:
 *
 * 1. [Initial]: Down the tree from ancestor to descendant.
 * 2. [Main]: Up the tree from descendant to ancestor.
 * 3. [Final]: Down the tree from ancestor to descendant.
 *
 * These passes serve the following purposes:
 *
 * 1. Initial: Allows ancestors to consume aspects of [PointerInputChange] before descendants.
 * This is where, for example, a scroller may block buttons from getting tapped by other fingers
 * once scrolling has started.
 * 2. Main: The primary pass where gesture filters should react to and consume aspects of
 * [PointerInputChange]s. This is the primary path where descendants will interact with
 * [PointerInputChange]s before parents. This allows for buttons to respond to a tap before a
 * container of the bottom to respond to a tap.
 * 3. Final: This pass is where children can learn what aspects of [PointerInputChange]s were
 * consumed by parents during the [Main] pass. For example, this is how a button determines that
 * it should no longer respond to fingers lifting off of it because a parent scroller has
 * consumed movement in a [PointerInputChange].
 */
enum class PointerEventPass {
    Initial, Main, Final
}

/**
 * True if this [PointerInputChange] represents a pointer coming in contact with the screen and
 * that change has not been consumed.
 */
fun PointerInputChange.changedToDown() = !isConsumed && !previousPressed && pressed

/**
 * True if this [PointerInputChange] represents a pointer coming in contact with the screen,
 * whether or not that change has been consumed.
 */
fun PointerInputChange.changedToDownIgnoreConsumed() = !previousPressed && pressed

/**
 * True if this [PointerInputChange] represents a pointer breaking contact with the screen and
 * that change has not been consumed.
 */
fun PointerInputChange.changedToUp() = !isConsumed && previousPressed && !pressed

/**
 * True if this [PointerInputChange] represents a pointer breaking contact with the screen, whether
 * or not that change has been consumed.
 */
fun PointerInputChange.changedToUpIgnoreConsumed() = previousPressed && !pressed

/**
 * True if this [PointerInputChange] represents a pointer moving on the screen and some of that
 * movement has not been consumed.
 */
fun PointerInputChange.positionChanged() =
    this.positionChangeInternal(false) != Offset.Companion.Zero

/**
 * True if this [PointerInputChange] represents a pointer moving on the screen ignoring how much
 * of that movement may have been consumed.
 */
fun PointerInputChange.positionChangedIgnoreConsumed() =
    this.positionChangeInternal(true) != Offset.Companion.Zero

/**
 * The distance that the pointer has moved on the screen minus any distance that has been consumed.
 */
fun PointerInputChange.positionChange() = this.positionChangeInternal(false)

/**
 * The distance that the pointer has moved on the screen, ignoring the fact that it might have
 * been consumed.
 */
fun PointerInputChange.positionChangeIgnoreConsumed() = this.positionChangeInternal(true)

private fun PointerInputChange.positionChangeInternal(ignoreConsumed: Boolean = false): Offset {
    val previousPosition = previousPosition
    val currentPosition = position

    val offset = currentPosition - previousPosition

    return if (!ignoreConsumed && isConsumed) Offset.Zero else offset
}

/**
 * True if this [PointerInputChange]'s movement has been consumed.
 */
@Deprecated(
    "Partial consumption has been deprecated. Use isConsumed instead",
    replaceWith = ReplaceWith("isConsumed")
)
fun PointerInputChange.positionChangeConsumed() = isConsumed

/**
 * True if any aspect of this [PointerInputChange] has been consumed.
 */
@Deprecated(
    "Partial consumption has been deprecated. Use isConsumed instead",
    replaceWith = ReplaceWith("isConsumed")
)
fun PointerInputChange.anyChangeConsumed() = isConsumed

/**
 * Consume the up or down change of this [PointerInputChange] if there is an up or down change to
 * consume.
 */
@Deprecated(
    "Partial consumption has been deprecated. Use consume() instead.",
    replaceWith = ReplaceWith("if (pressed != previousPressed) consume()")
)
fun PointerInputChange.consumeDownChange() {
    if (pressed != previousPressed) {
        consume()
    }
}

/**
 * Consume position change if there is any
 */
@Deprecated(
    "Partial consumption has been deprecated. Use consume() instead.",
    replaceWith = ReplaceWith("if (positionChange() != Offset.Zero) consume()")
)
fun PointerInputChange.consumePositionChange() {
    if (positionChange() != Offset.Zero) {
        consume()
    }
}

/**
 * Consumes all changes associated with the [PointerInputChange]
 */
@Deprecated("Use consume() instead", replaceWith = ReplaceWith("consume()"))
fun PointerInputChange.consumeAllChanges() {
    consume()
}

/**
 * Returns `true` if the pointer has moved outside of the region of
 * `(0, 0, size.width, size.height)` or `false` if the current pointer is up or it is inside the
 * given bounds.
 */
@Deprecated(
    message = "Use isOutOfBounds() that supports minimum touch target",
    replaceWith = ReplaceWith("this.isOutOfBounds(size, extendedTouchPadding)")
)
fun PointerInputChange.isOutOfBounds(size: IntSize): Boolean {
    val position = position
    val x = position.x
    val y = position.y
    val width = size.width
    val height = size.height
    return x < 0f || x > width || y < 0f || y > height
}

/**
 * Returns `true` if the pointer has moved outside of the pointer region. For Touch
 * events, this is (-extendedTouchPadding.width, -extendedTouchPadding.height,
 * size.width + extendedTouchPadding.width, size.height + extendedTouchPadding.height) and
 * for other events, this is `(0, 0, size.width, size.height)`. Returns`false` if the
 * current pointer is up or it is inside the pointer region.
 */
fun PointerInputChange.isOutOfBounds(size: IntSize, extendedTouchPadding: Size): Boolean {
    if (type != PointerType.Touch) {
        @Suppress("DEPRECATION")
        return isOutOfBounds(size)
    }
    val position = position
    val x = position.x
    val y = position.y
    val minX = -extendedTouchPadding.width
    val maxX = size.width + extendedTouchPadding.width
    val minY = -extendedTouchPadding.height
    val maxY = size.height + extendedTouchPadding.height
    return x < minX || x > maxX || y < minY || y > maxY
}
