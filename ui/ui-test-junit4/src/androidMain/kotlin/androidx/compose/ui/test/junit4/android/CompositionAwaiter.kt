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

package androidx.compose.ui.test.junit4.android

import android.os.Handler
import android.os.Looper
import android.view.Choreographer
import androidx.compose.runtime.ExperimentalComposeApi
import androidx.compose.runtime.Recomposer
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.test.junit4.runOnUiThread

internal class CompositionAwaiter(private val composeIdlingResource: ComposeIdlingResource) {

    private enum class State {
        Initialized, Running, Finished, Cancelled
    }

    private val lock = Any()
    private var state = State.Initialized

    private val handler = Handler(Looper.getMainLooper())
    @Suppress("DEPRECATION")
    private val choreographer = runOnUiThread { Choreographer.getInstance() }

    /**
     * Starts this awaiter, if it wasn't started, cancelled or finished yet.
     */
    fun start() {
        ifStateIsIn(State.Initialized) {
            state = State.Running
            startIdlingResource()
            handler.post(callback)
        }
    }

    /**
     * Cancels this awaiter if it is running or not yet started. Does nothing if it was already
     * finished or cancelled.
     */
    fun cancel() {
        ifStateIsIn(State.Initialized, State.Running) {
            state = State.Cancelled
            stopIdlingResource()
            handler.removeCallbacks(callback)
            choreographer.removeFrameCallback(callback)
        }
    }

    private fun startIdlingResource() {
        composeIdlingResource.addCompositionAwaiter()
    }

    private fun stopIdlingResource() {
        composeIdlingResource.removeCompositionAwaiter()
    }

    /**
     * Runs the given [block] if the current [state] is the [validStates]. Synchronizes on
     * [lock] to make it thread-safe.
     */
    private inline fun ifStateIsIn(vararg validStates: State, block: () -> Unit) {
        try {
            synchronized(lock) {
                if (state in validStates) {
                    block()
                }
            }
        } catch (t: Throwable) {
            cancel()
            throw t
        }
    }

    @OptIn(ExperimentalComposeApi::class)
    private fun isIdle(): Boolean {
        return !Snapshot.current.hasPendingChanges() && !Recomposer.current().hasInvalidations()
    }

    private val callback = object : Runnable, Choreographer.FrameCallback {
        override fun run() {
            ifStateIsIn(State.Running) {
                if (!isIdle()) {
                    // not idle, restart check. this makes sure our frame callback
                    // will be executed _after_ potentially scheduled onCommits
                    handler.postDelayed(this, 10)
                } else {
                    // Is idle. Either nothing is scheduled on the choreographer, in which
                    // case our callback will be the only one, or something is scheduled on
                    // the choreographer, in which case our callback will be after it
                    choreographer.postFrameCallback(this)
                }
            }
        }

        override fun doFrame(frameTime: Long) {
            ifStateIsIn(State.Running) {
                if (!isIdle()) {
                    // not idle, restart check. onCommits have triggered a recomposition
                    handler.postDelayed(this, 10)
                } else {
                    // is idle. onCommits have _not_ triggered a
                    // recomposition, or there were no onCommits
                    state = State.Finished
                    stopIdlingResource()
                }
            }
        }
    }
}