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

package kotlinx.coroutines.test

import kotlinx.atomicfu.atomic
import kotlinx.atomicfu.update
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Delay
import kotlinx.coroutines.DisposableHandle
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.Runnable
import kotlinx.coroutines.internal.ThreadSafeHeap
import kotlinx.coroutines.internal.ThreadSafeHeapNode
import kotlin.coroutines.CoroutineContext
import kotlin.math.max

@OptIn(InternalCoroutinesApi::class)
public class TestCoroutineDispatcher: CoroutineDispatcher(), Delay, DelayController {
    private var dispatchImmediately = true
        set(value) {
            field = value
            if (value) {
                // there may already be tasks from setup code we need to run
                advanceUntilIdle()
            }
        }

    // The ordered queue for the runnable tasks.
    @OptIn(InternalCoroutinesApi::class)
    private val queue = ThreadSafeHeap<TimedRunnable>()

    // The per-scheduler global order counter.
    private val _counter = atomic(0L)

    // Storing time in nanoseconds internally.
    private val _time = atomic(0L)

    /** @suppress */
    override fun dispatch(context: CoroutineContext, block: Runnable) {
        if (dispatchImmediately) {
            block.run()
        } else {
            post(block)
        }
    }

    /** @suppress */
    @InternalCoroutinesApi
    override fun dispatchYield(context: CoroutineContext, block: Runnable) {
        post(block)
    }

    /** @suppress */
    override fun scheduleResumeAfterDelay(timeMillis: Long, continuation: CancellableContinuation<Unit>) {
        postDelayed(CancellableContinuationRunnable(continuation) { resumeUndispatched(Unit) }, timeMillis)
    }

    /** @suppress */
    override fun invokeOnTimeout(timeMillis: Long, block: Runnable, context: CoroutineContext): DisposableHandle {
        val node = postDelayed(block, timeMillis)
        return object : DisposableHandle {
            override fun dispose() {
                queue.remove(node)
            }
        }
    }

    /** @suppress */
    override fun toString(): String {
        return "TestCoroutineDispatcher[currentTime=${currentTime}ms, queued=${queue.size}]"
    }

    private fun post(block: Runnable) =
        queue.addLast(TimedRunnable(block, _counter.getAndIncrement()))

    private fun postDelayed(block: Runnable, delayTime: Long) =
        TimedRunnable(block, _counter.getAndIncrement(), safePlus(currentTime, delayTime))
            .also {
                queue.addLast(it)
            }

    private fun safePlus(currentTime: Long, delayTime: Long): Long {
        check(delayTime >= 0)
        val result = currentTime + delayTime
        if (result < currentTime) return Long.MAX_VALUE // clam on overflow
        return result
    }

    private fun doActionsUntil(targetTime: Long) {
        while (true) {
            val current = queue.removeFirstIf { it.time <= targetTime } ?: break
            // If the scheduled time is 0 (immediate) use current virtual time
            if (current.time != 0L) _time.value = current.time
            current.run()
        }
    }

    /** @suppress */
    override val currentTime: Long get() = _time.value

    /** @suppress */
    override fun advanceTimeBy(delayTimeMillis: Long): Long {
        val oldTime = currentTime
        advanceUntilTime(oldTime + delayTimeMillis)
        return currentTime - oldTime
    }

    /**
     * Moves the CoroutineContext's clock-time to a particular moment in time.
     *
     * @param targetTime The point in time to which to move the CoroutineContext's clock (milliseconds).
     */
    private fun advanceUntilTime(targetTime: Long) {
        doActionsUntil(targetTime)
        _time.update {  currentValue -> max(currentValue, targetTime) }
    }

    /** @suppress */
    override fun advanceUntilIdle(): Long {
        val oldTime = currentTime
        while(!queue.isEmpty) {
            runCurrent()
            val next = queue.peek() ?: break
            advanceUntilTime(next.time)
        }
        return currentTime - oldTime
    }

    /** @suppress */
    override fun runCurrent(): Unit  = doActionsUntil(currentTime)

    /** @suppress */
    override suspend fun pauseDispatcher(block: suspend () -> Unit) {
        val previous = dispatchImmediately
        dispatchImmediately = false
        try {
            block()
        } finally {
            dispatchImmediately = previous
        }
    }

    /** @suppress */
    override fun pauseDispatcher() {
        dispatchImmediately = false
    }

    /** @suppress */
    override fun resumeDispatcher() {
        dispatchImmediately = true
    }

    /** @suppress */
    override fun cleanupTestCoroutines() {
        // process any pending cancellations or completions, but don't advance time
        doActionsUntil(currentTime)

        // run through all pending tasks, ignore any submitted coroutines that are not active
        val pendingTasks = mutableListOf<TimedRunnable>()
        while (true) {
            pendingTasks += queue.removeFirstOrNull() ?: break
        }
        val activeDelays = pendingTasks
            .mapNotNull { it.runnable as? CancellableContinuationRunnable<*> }
            .filter { it.continuation.isActive }

        val activeTimeouts = pendingTasks.filter { it.runnable !is CancellableContinuationRunnable<*> }
        if (activeDelays.isNotEmpty() || activeTimeouts.isNotEmpty()) {
            throw UncompletedCoroutinesError(
                "Unfinished coroutines during teardown. Ensure all coroutines are" +
                    " completed or cancelled by your test."
            )
        }
    }
}

/**
 * This class exists to allow cleanup code to avoid throwing for cancelled continuations scheduled
 * in the future.
 */
private class CancellableContinuationRunnable<T>(
    val continuation: CancellableContinuation<T>,
    private val block: CancellableContinuation<T>.() -> Unit
) : Runnable {
    override fun run() = continuation.block()
}

/**
 * A Runnable for our event loop that represents a task to perform at a time.
 */
@OptIn(InternalCoroutinesApi::class)
private class TimedRunnable(
    val runnable: Runnable,
    private val count: Long = 0,
    val time: Long = 0
) : Comparable<TimedRunnable>, Runnable by runnable, ThreadSafeHeapNode {
    override var heap: ThreadSafeHeap<*>? = null
    override var index: Int = 0

    override fun compareTo(other: TimedRunnable) = if (time == other.time) {
        count.compareTo(other.count)
    } else {
        time.compareTo(other.time)
    }

    override fun toString() = "TimedRunnable(time=$time, run=$runnable)"
}