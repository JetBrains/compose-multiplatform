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

package androidx.compose.dispatch

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Runnable
import kotlinx.coroutines.suspendCancellableCoroutine
import java.awt.event.ActionListener
import javax.swing.SwingUtilities.invokeLater
import javax.swing.Timer
import kotlin.coroutines.CoroutineContext

private typealias Action = (Long) -> Unit
private typealias Queue = ArrayList<Action>

/**
 * Ticks scheduler for Desktop. It tries to mimic Android's Choreographer and has multiple levels of
 * callbacks: "just" callbacks and "after" callbacks (Choreographer has more, but two is enough for us)
 * It's necessary because recomposition and drawing should be synchronized, otherwise races and
 * inconsistencies are possible. On Android, this synchronization is achieved implicitly by executing
 * recomposition and scheduling of redrawing as different types of callbacks. It guarantees,
 * that after recomposition, redrawing happens in the exactly same tick.
 *
 * There are some plans to make possible redrawing based on composition snapshots,
 * so maybe some requirements for dispatcher will be mitigated in the future.
 **/
class DesktopUiDispatcher : CoroutineDispatcher() {
    private val lock = Any()
    private var callbacks = Queue()
    private var afterCallbacks = Queue()

    @Volatile
    private var scheduled = false

    private fun scheduleIfNeeded() {
        synchronized(lock) {
            if (!scheduled && (callbacks.isNotEmpty() || afterCallbacks.isNotEmpty())) {
                invokeLater { tick() }
                scheduled = true
            }
        }
    }

    val frameClock: MonotonicFrameClock = object :
        MonotonicFrameClock {
        override suspend fun <R> withFrameNanos(
            onFrame: (Long) -> R
        ): R {
            return suspendCancellableCoroutine { co ->
                val callback = { now: Long ->
                    val res = runCatching {
                        onFrame(now)
                    }
                    co.resumeWith(res)
                }
                scheduleCallback(callback)
                co.invokeOnCancellation {
                    removeCallback(callback)
                }
            }
        }
    }

    private fun tick() {
        scheduled = false
        val now = System.nanoTime()
        runCallbacks(now, callbacks)
        runCallbacks(now, afterCallbacks)
        scheduleIfNeeded()
    }

    fun scheduleCallback(action: Action) {
        synchronized(lock) {
            callbacks.add(action)
            scheduleIfNeeded()
        }
    }

    fun scheduleAfterCallback(action: Action) {
        synchronized(lock) {
            afterCallbacks.add(action)
        }
    }

    fun removeCallback(action: (Long) -> Unit) {
        synchronized(lock) {
            callbacks.remove(action)
        }
    }

    fun scheduleCallbackWithDelay(delay: Int, action: Action) {
        Timer(delay,
            ActionListener {
                scheduleCallback { action(System.nanoTime()) }
            }).apply {
            isRepeats = false
            start()
        }
    }

    private fun runCallbacks(now: Long, callbacks: Queue) {
        synchronized(lock) {
            callbacks.toList().also { callbacks.clear() }
        }.forEach { it(now) }
    }

    override fun dispatch(context: CoroutineContext, block: Runnable) {
        scheduleCallback { block.run() }
    }

    companion object {
        val Dispatcher: DesktopUiDispatcher by lazy { DesktopUiDispatcher() }
        val Main: CoroutineContext by lazy {
            Dispatcher + Dispatcher.frameClock
        }
    }
}