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

package androidx.compose.ui.gesture.scrollorientationlocking

import androidx.compose.ui.input.pointer.CustomEvent
import androidx.compose.ui.input.pointer.CustomEventDispatcher
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.PointerInputFilter
import androidx.compose.ui.input.pointer.changedToDownIgnoreConsumed
import androidx.compose.ui.input.pointer.changedToUpIgnoreConsumed
import androidx.compose.ui.util.annotation.VisibleForTesting
import androidx.compose.ui.util.fastForEach

// TODO(shepshapard): This should be in a more generic place.
enum class Orientation {
    Vertical, Horizontal
}

/**
 * Manages scroll orientation locking amongst all participating gesture filters.
 *
 * Does so by allowing gesture filters to do the following:
 *
 * 1. Request that pointers be locked to particular [Orientation]s
 * 2. Query which pointers from a given set are allowed to be considered for a given orientation.
 *
 * A specific contract must also be followed in order to setup and tear down scroll orientation
 * locking.  Please see [onPointerInputSetup], [onPointerInputTearDown], [onCustomEvent], and
 * [onCancel] to use
 * this correctly.
 */
class ScrollOrientationLocker(private val customEventDispatcher: CustomEventDispatcher) {

    private var locker: InternalScrollOrientationLocker? = null
    private var lockerOwner = false

    /**
     * Handles the initialization of scroll orientation locking management, including internally
     * managed distribution of shared state.
     *
     * For proper initialization to work, [onCustomEvent] must also be called in
     * [PointerInputFilter.onCustomEvent].
     *
     * This method should simply be called toward the top of [PointerInputFilter.onPointerInput]
     * for all [PointerEventPass]es, before [attemptToLockPointers] or [getPointersFor] are called.
     *
     * @see onPointerInputTearDown
     * @see onCancel
     * @see onCustomEvent
     */
    fun onPointerInputSetup(changes: List<PointerInputChange>, pass: PointerEventPass) {
        if (pass != PointerEventPass.Initial) {
            return
        }

        if (locker == null && changes.all { it.changedToDownIgnoreConsumed() }) {
            lockerOwner = true
            locker = InternalScrollOrientationLocker().also {
                customEventDispatcher.dispatchCustomEvent(
                    ShareScrollOrientationLockerEvent(it)
                )
            }
        } else if (lockerOwner && changes.any { it.changedToDownIgnoreConsumed() }) {
            // TODO(shepshapard): This is always doing some extra work given that some of the
            //  child gesture filters may already have received the InternalScrollOrientationLocker.
            //  It is functionality correct, but could be optimized (though it would likely
            //  not provide that much of a performance boost in the real world.
            customEventDispatcher.dispatchCustomEvent(
                ShareScrollOrientationLockerEvent(locker!!)
            )
        }
    }

    /**
     * Handles the tear down of internal state.
     *
     * This method must be called toward the bottom of [PointerInputFilter.onPointerInput]
     * for all [PointerEventPass]es, and [attemptToLockPointers] or [getPointersFor] should not
     * be called afterwards.
     *
     * @see onPointerInputSetup
     * @see onCancel
     * @see onCustomEvent
     */
    fun onPointerInputTearDown(changes: List<PointerInputChange>, pass: PointerEventPass) {
        if (pass == PointerEventPass.Final && changes.all { it.changedToUpIgnoreConsumed() }) {
            reset()
        }
    }

    /**
     * Handles the tear down of internal state due to a call to [PointerInputFilter.onCancel].
     *
     * Must be called in [PointerInputFilter.onCancel].
     *
     * @see onPointerInputSetup
     * @see onPointerInputTearDown
     * @see onCustomEvent
     */
    fun onCancel() {
        reset()
    }

    /**
     * Handles the other half of [onPointerInputSetup] and must be called in all calls to
     * [PointerInputFilter.onCustomEvent].
     *
     * @see onPointerInputSetup
     * @see onPointerInputTearDown
     * @see onCancel
     * @throws [IllegalStateException] if this [ScrollOrientationLocker] receives a
     * [ShareScrollOrientationLockerEvent] via this method after having already dispatched a
     * [ShareScrollOrientationLockerEvent] itself.
     */
    fun onCustomEvent(customEvent: CustomEvent, pass: PointerEventPass) {

        if (pass == PointerEventPass.Initial &&
            customEvent is ShareScrollOrientationLockerEvent
        ) {
            if (lockerOwner) {
                throw IllegalStateException(
                    "This instance of ScrollOrientationLocker should " +
                        "never receive a ShareScrollOrientationLockerEvent because it already " +
                        "dispatched one, and thus should be the only one in it's subtree to " +
                        "dispatch one."
                )
            }
            locker = customEvent.scrollOrientationLocker
        }
    }

    /**
     * Locks the [PointerId]s associated with the [changes] to [orientation].
     *
     * This effects which [changes] are returned by [getPointersFor].
     *
     * Pointers that are already locked to a given orientation cannot be later locked to a different
     * orientation.
     *
     * @throws [IllegalStateException] if this method is called when this
     * [ScrollOrientationLocker] has not yet been initialized.  This is likely happening because
     * [onPointerInputSetup] is not being called with every [PointerEventPass] before this method
     * is being called.
     */
    fun attemptToLockPointers(changes: List<PointerInputChange>, orientation: Orientation) {
        if (locker == null) {
            throw IllegalStateException(
                "Internal state has not been set.  This method should not" +
                    " be called in any place but after calls to onPointerInputSetup and before " +
                    "calls to onPointerInputTearDown or onCancel. Also, onCustomEvent must be " +
                    "called appropriately.  See docs for details."
            )
        }
        locker!!.attemptToLockPointers(changes, orientation)
    }

    /**
     * Filters the [changes] for those that are allowed to be acted upon for the [orientation].
     *
     * A change can be acted on if it was not already locked in the other orientation.
     *
     * For example, if pointer 1 was previously locked to Horizontal via [attemptToLockPointers]
     * then calling this method with pointers 1 and 2 and the following orientation will result
     * in the following pointers being returned:
     *
     * - Orientation.Horizontal -> (1, 2)
     * - Orientation.Vertical -> (2)
     *
     * @throws [IllegalStateException] if this method is called when this
     * [ScrollOrientationLocker] has not yet been initialized.  This is likely happening because
     * [onPointerInputSetup] is not being called with every [PointerEventPass] before this method
     * is being called.
     */
    fun getPointersFor(
        changes: List<PointerInputChange>,
        orientation: Orientation
    ): List<PointerInputChange> {
        if (locker == null) {
            throw IllegalStateException(
                "Internal state has not been set.  This method should not" +
                    " be called in any place but after calls to onPointerInputSetup and before " +
                    "calls to onPointerInputTearDown or onCancel. Also, onCustomEvent must be " +
                    "called appropriately.  See docs for details."
            )
        }
        return locker!!.getPointersFor(changes, orientation)
    }

    private fun reset() {
        locker = null
        lockerOwner = false
    }
}

@VisibleForTesting
internal class InternalScrollOrientationLocker {
    private val pointerLocks: MutableMap<PointerId, Orientation> = mutableMapOf()

    fun attemptToLockPointers(pointerIds: List<PointerInputChange>, orientation: Orientation) {
        pointerIds.fastForEach {
            if (pointerLocks[it.id] == null) {
                pointerLocks[it.id] = orientation
            }
        }
    }

    fun getPointersFor(
        pointerIds: List<PointerInputChange>,
        orientation: Orientation
    ): List<PointerInputChange> {
        return pointerIds
            .filter { pointerLocks[it.id] == null || pointerLocks[it.id] == orientation }
    }
}

@VisibleForTesting
internal class ShareScrollOrientationLockerEvent(
    val scrollOrientationLocker: InternalScrollOrientationLocker
) : CustomEvent