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

package androidx.compose.ui.platform

import android.os.Looper
import android.view.Choreographer
import androidx.compose.runtime.MonotonicFrameClock
import androidx.core.os.HandlerCompat
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlin.coroutines.CoroutineContext

/**
 * A [CoroutineDispatcher] that will perform dispatch during a [handler] callback or
 * [choreographer]'s animation frame stage, whichever comes first. Use [Main] to obtain
 * a dispatcher for the process's main thread (i.e. the activity thread) or [CurrentThread]
 * to obtain a dispatcher for the current thread.
 */
// Implementation note: the constructor is private to direct users toward the companion object
// accessors for the main/current threads. A choreographer must be obtained from its current
// thread as per the only public API surface for obtaining one as of this writing, and the
// choreographer and handler must match. Constructing an AndroidUiDispatcher with a handler
// not marked as async will adversely affect dispatch behavior but not to the point of
// incorrectness; more operations would be deferred to the choreographer frame as racing handler
// messages would wait behind a frame barrier.
class AndroidUiDispatcher private constructor(
    val choreographer: Choreographer,
    private val handler: android.os.Handler
) : CoroutineDispatcher() {

    // Guards all properties in this class
    private val lock = Any()

    private val toRunTrampolined = ArrayDeque<Runnable>()
    private var toRunOnFrame = mutableListOf<Choreographer.FrameCallback>()
    private var spareToRunOnFrame = mutableListOf<Choreographer.FrameCallback>()
    private var scheduledTrampolineDispatch = false
    private var scheduledFrameDispatch = false

    private val dispatchCallback = object : Choreographer.FrameCallback, Runnable {
        override fun run() {
            performTrampolineDispatch()
            synchronized(lock) {
                if (toRunOnFrame.isEmpty()) {
                    choreographer.removeFrameCallback(this)
                    scheduledFrameDispatch = false
                }
            }
        }

        override fun doFrame(frameTimeNanos: Long) {
            handler.removeCallbacks(this)
            performTrampolineDispatch()
            performFrameDispatch(frameTimeNanos)
        }
    }

    private fun nextTask(): Runnable? = synchronized(lock) {
        toRunTrampolined.removeFirstOrNull()
    }

    private fun performTrampolineDispatch() {
        do {
            var task = nextTask()
            while (task != null) {
                task.run()
                task = nextTask()
            }
        } while (
            // We don't dispatch holding the lock so that other tasks can get in on our
            // trampolining time slice, but once we're done, make sure nothing added a new task
            // before we set scheduledDispatch = false, which would prevent the next dispatch
            // from being correctly scheduled. Loop to run these stragglers now.
            synchronized(lock) {
                if (toRunTrampolined.isEmpty()) {
                    scheduledTrampolineDispatch = false
                    false
                } else true
            }
        )
    }

    private fun performFrameDispatch(frameTimeNanos: Long) {
        val toRun = synchronized(lock) {
            if (!scheduledFrameDispatch) return
            scheduledFrameDispatch = false
            val result = toRunOnFrame
            toRunOnFrame = spareToRunOnFrame
            spareToRunOnFrame = result
            result
        }
        for (i in 0 until toRun.size) {
            // This callback will not and must not throw, see AndroidUiFrameClock
            toRun[i].doFrame(frameTimeNanos)
        }
        toRun.clear()
    }

    internal fun postFrameCallback(callback: Choreographer.FrameCallback) {
        synchronized(lock) {
            toRunOnFrame.add(callback)
            if (!scheduledFrameDispatch) {
                scheduledFrameDispatch = true
                choreographer.postFrameCallback(dispatchCallback)
            }
        }
    }

    internal fun removeFrameCallback(callback: Choreographer.FrameCallback) {
        synchronized(lock) {
            toRunOnFrame.remove(callback)
        }
    }

    /**
     * A [MonotonicFrameClock] associated with this [AndroidUiDispatcher]'s [choreographer]
     * that may be used to await [Choreographer] frame dispatch.
     */
    val frameClock: MonotonicFrameClock = AndroidUiFrameClock(choreographer)

    override fun dispatch(context: CoroutineContext, block: Runnable) {
        synchronized(lock) {
            toRunTrampolined.addLast(block)
            if (!scheduledTrampolineDispatch) {
                scheduledTrampolineDispatch = true
                handler.post(dispatchCallback)
                if (!scheduledFrameDispatch) {
                    scheduledFrameDispatch = true
                    choreographer.postFrameCallback(dispatchCallback)
                }
            }
        }
    }

    companion object {
        /**
         * The [CoroutineContext] containing the [AndroidUiDispatcher] and its [frameClock] for the
         * process's main thread.
         */
        val Main: CoroutineContext by lazy {
            val dispatcher = AndroidUiDispatcher(
                if (isMainThread()) Choreographer.getInstance()
                else runBlocking(Dispatchers.Main) { Choreographer.getInstance() },
                HandlerCompat.createAsync(Looper.getMainLooper())
            )

            dispatcher + dispatcher.frameClock
        }

        private val currentThread: ThreadLocal<CoroutineContext> =
            object : ThreadLocal<CoroutineContext>() {
                override fun initialValue(): CoroutineContext = AndroidUiDispatcher(
                    Choreographer.getInstance(),
                    HandlerCompat.createAsync(
                        Looper.myLooper()
                            ?: error("no Looper on this thread")
                    )
                ).let { it + it.frameClock }
            }

        /**
         * The canonical [CoroutineContext] containing the [AndroidUiDispatcher] and its
         * [frameClock] for the calling thread. Returns [Main] if accessed from the process's
         * main thread.
         *
         * Throws [IllegalStateException] if the calling thread does not have
         * both a [Choreographer] and an active [Looper].
         */
        val CurrentThread: CoroutineContext get() = if (isMainThread()) Main else {
            currentThread.get() ?: error("no AndroidUiDispatcher for this thread")
        }
    }
}

private fun isMainThread() = Looper.myLooper() === Looper.getMainLooper()
