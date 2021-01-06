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

import android.view.Choreographer
import androidx.compose.animation.core.AnimationClockObserver
import androidx.compose.animation.core.ManualAnimationClock
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.TestAnimationClock
import androidx.compose.ui.test.junit4.runOnUiThread

/**
 * An animation clock driven by an external time source, that can be queried for idleness and has
 * the ability to seamlessly detach the clock from that time source and pump it manually.
 * Normally there is no need to instantiate this class by yourself, it will be done for you by
 * a test rule.
 *
 * This [TestAnimationClock] is resumed by default.
 *
 * @see isIdle
 * @see pauseClock
 * @see advanceClock
 * @see resumeClock
 */
@ExperimentalTestApi
internal class AndroidTestAnimationClock : TestAnimationClock {

    /**
     * If not initializing on the main thread, a message will be posted on the main thread to
     * fetch the Choreographer, and initialization blocks until that fetch is completed.
     */
    @Suppress("DEPRECATION")
    private val mainChoreographer: Choreographer = runOnUiThread {
        Choreographer.getInstance()
    }

    private val lock = Any()
    private val clock = ManualAnimationClock(0, false)
    private var advancedTime = 0L
    private var needsToDispatch = false
    private var isDispatching = false
    private var choreographerPaused = false
    private var isDisposed = false

    override val isPaused: Boolean
        get() = choreographerPaused

    override val isIdle: Boolean
        get() = synchronized(lock) {
            return choreographerPaused || !needsToDispatch
        }

    // FrameCallback with which we receive timestamps from the choreographer
    private val frameCallback = Choreographer.FrameCallback { frameTimeNanos ->
        synchronized(lock) {
            // The choreographer can be paused before we entered
            // our synchronized block. Ignore this frame
            if (!choreographerPaused) {
                handleFrameTimeLocked(frameTimeNanos / 1_000_000 + advancedTime)
            }
        }
    }

    override fun subscribe(observer: AnimationClockObserver) {
        synchronized(lock) {
            check(!isDisposed) { "Can't subscribe to a disposed clock" }
            clock.subscribe(observer)
            if (!needsToDispatch) {
                // Didn't need to dispatch before, but now we do
                postFrameCallbackLocked()
                needsToDispatch = true
            }
        }
    }

    override fun unsubscribe(observer: AnimationClockObserver) {
        clock.unsubscribe(observer)
    }

    override fun advanceClock(milliseconds: Long) {
        require(milliseconds >= 0) { "Can only advance the clock forward" }

        // Pause the clock, because it might tick between our two manual ticks
        val wasPaused = isPaused.also { if (!it) pauseClock() }

        // Start with ticking the clock at the current time, to set
        // the start time of animations that must be started now.
        advanceClockOnMainThread(0L)

        // If the requested amount was 0ms, it was intentional to just pump
        // the clock but not advance it. Don't pump it a second time.
        if (milliseconds > 0) {
            advanceClockOnMainThread(milliseconds)
        }

        // Resume the clock if it was running before
        if (!wasPaused) resumeClock()
    }

    private fun advanceClockOnMainThread(milliseconds: Long) {
        @Suppress("DEPRECATION")
        runOnUiThread {
            synchronized(lock) {
                check(!isDispatching) { "Can't advance clock while dispatching a frame time" }
                advancedTime += milliseconds
                handleFrameTimeLocked(clock.clockTimeMillis + milliseconds)
            }
        }
    }

    private fun handleFrameTimeLocked(frameTimeMillis: Long) {
        synchronized(lock) {
            // Start dispatching
            isDispatching = true

            // Dispatch to the backing clock
            clock.clockTimeMillis = frameTimeMillis

            // If we still have observers, we want another frame
            needsToDispatch = clock.hasObservers
            if (needsToDispatch) {
                postFrameCallbackLocked()
            }

            // Finish dispatching
            isDispatching = false
        }
    }

    private fun postFrameCallbackLocked() {
        if (!choreographerPaused) {
            mainChoreographer.postFrameCallback(frameCallback)
        }
    }

    override fun pauseClock() {
        synchronized(lock) {
            /**
             * Simply remove the link between the choreographer and us. Our observers are still
             * there and will be notified on the next call to [advanceClock] or [resumeClock].
             */
            mainChoreographer.removeFrameCallback(frameCallback)
            choreographerPaused = true
        }
    }

    override fun resumeClock() {
        synchronized(lock) {
            if (choreographerPaused) {
                choreographerPaused = false
                if (needsToDispatch) {
                    postFrameCallbackLocked()
                }
            }
        }
    }

    /**
     * Cancels pending frames and prevents future subscription to this clock. This clock can not
     * be reused after this method.
     */
    fun dispose() {
        synchronized(lock) {
            mainChoreographer.removeFrameCallback(frameCallback)
            needsToDispatch = false
            isDisposed = true
            // TODO(b/159102826): Reinstate check when both tests and animations switched to
            //  coroutines
            /*
            if (clock.hasObservers) {
                throw AssertionError("Animation clock still has observer(s) after it is disposed." +
                        " Are there still animations running?")
            }
            */
        }
    }
}
