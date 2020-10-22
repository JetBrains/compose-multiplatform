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

package androidx.compose.ui.test

import java.util.WeakHashMap
import androidx.compose.ui.node.Owner

internal abstract class PersistingInputDispatcher(private val owner: Owner?) : InputDispatcher() {
    protected companion object {
        /**
         * Stores the [InputDispatcherState] of each [Owner]. The state will be restored in an
         * [InputDispatcher] when it is created for an owner that has a state stored.
         */
        private val states = WeakHashMap<Owner, InputDispatcherState>()

        /**
         * Removes the state associated with the given [owner] from the persisted states. Call
         * this when the state no longer needs to be persisted, most notably when the owner is
         * disposed.
         */
        fun removeState(owner: Owner) {
            states.remove(owner)
        }
    }

    init {
        val state = states.remove(owner)
        if (state?.partialGesture != null) {
            nextDownTime = state.nextDownTime
            gestureLateness = state.gestureLateness
            partialGesture = state.partialGesture
        }
    }

    protected open fun saveState(owner: Owner?) {
        if (owner != null) {
            states[owner] = InputDispatcherState(nextDownTime, gestureLateness, partialGesture)
        }
    }

    final override fun dispose() {
        saveState(owner)
        onDispose()
    }

    open fun onDispose() {}

    /**
     * The state of an [InputDispatcher], saved when the [GestureScope] is disposed and restored
     * when the [GestureScope] is recreated.
     *
     * @param nextDownTime The downTime of the start of the next gesture, when chaining gestures.
     * This property will only be restored if an incomplete gesture was in progress when the
     * state of the [InputDispatcher] was saved.
     * @param gestureLateness The time difference in milliseconds between enqueuing the first
     * event of the gesture and dispatching it. Depending on the implementation of
     * [InputDispatcher], this may or may not be used.
     * @param partialGesture The state of an incomplete gesture. If no gesture was in progress
     * when the state of the [InputDispatcher] was saved, this will be `null`.
     */
    private data class InputDispatcherState(
        val nextDownTime: Long,
        var gestureLateness: Long?,
        val partialGesture: PartialGesture?
    )
}
