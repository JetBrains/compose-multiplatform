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

import androidx.compose.ui.node.Owner

internal abstract class PersistingInputDispatcher(
    private val testContext: TestContext,
    private val owner: Owner?
) : InputDispatcher() {

    init {
        val state = testContext.states.remove(owner)
        if (state?.partialGesture != null) {
            nextDownTime = state.nextDownTime
            gestureLateness = state.gestureLateness
            partialGesture = state.partialGesture
        }
    }

    protected open fun saveState(owner: Owner?) {
        if (owner != null) {
            testContext.states[owner] =
                InputDispatcherState(nextDownTime, gestureLateness, partialGesture)
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
    internal data class InputDispatcherState(
        val nextDownTime: Long,
        var gestureLateness: Long?,
        val partialGesture: PartialGesture?
    )
}
