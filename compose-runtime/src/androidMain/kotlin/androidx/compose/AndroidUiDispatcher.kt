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

package androidx.compose

import android.view.Choreographer
import androidx.core.os.HandlerCompat
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Runnable
import kotlinx.coroutines.runBlocking
import kotlin.coroutines.CoroutineContext

/**
 * A [CoroutineDispatcher] that will perform dispatch during [choreographer]'s animation frame
 * stage. Suspending will always wait until the next frame.
 */
@OptIn(ExperimentalStdlibApi::class)
class AndroidUiDispatcher private constructor(
    val choreographer: Choreographer,
    private val handler: android.os.Handler
) : CoroutineDispatcher() {

    // Guards all properties in this class
    private val lock = Any()

    private val toRunTrampolined = ArrayDeque<Runnable>()
    private var toRunOnFrame = mutableListOf<ChoreographerFrameCallback>()
    private var spareToRunOnFrame = mutableListOf<ChoreographerFrameCallback>()
    private var scheduledTrampolineDispatch = false
    private var scheduledFrameDispatch = false

    private val dispatchCallback = object : ChoreographerFrameCallback, java.lang.Runnable {
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
            // This callback can't throw, see AndroidUiCompositionFrameClock
            toRun[i].doFrame(frameTimeNanos)
        }
        toRun.clear()
    }

    internal fun postFrameCallback(callback: ChoreographerFrameCallback) {
        synchronized(lock) {
            toRunOnFrame.add(callback)
            if (!scheduledFrameDispatch) {
                scheduledFrameDispatch = true
                choreographer.postFrameCallback(dispatchCallback)
            }
        }
    }

    internal fun removeFrameCallback(callback: ChoreographerFrameCallback) {
        synchronized(lock) {
            toRunOnFrame.remove(callback)
        }
    }

    val compositionFrameClock: CompositionFrameClock = AndroidUiCompositionFrameClock(choreographer)

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
        val Main by lazy {
            AndroidUiDispatcher(
                if (isMainThread()) Choreographer.getInstance()
                else runBlocking(Dispatchers.Main) { Choreographer.getInstance() },
                HandlerCompat.createAsync(Looper.getMainLooper())
            )
        }

        private val currentThread = ThreadLocal {
            AndroidUiDispatcher(
                Choreographer.getInstance(),
                HandlerCompat.createAsync(Looper.myLooper() ?: error("no Looper on this thread"))
            )
        }
        val CurrentThread: AndroidUiDispatcher get() = currentThread.get()
    }
}