/*
 * Copyright 2022 The Android Open Source Project
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

import androidx.compose.ui.test.FrameDeferringContinuationInterceptor.FrameDeferredContinuation
import androidx.compose.ui.test.internal.DelayPropagatingContinuationInterceptorWrapper
import kotlin.coroutines.Continuation
import kotlin.coroutines.ContinuationInterceptor
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.CoroutineDispatcher

/**
 * A [ContinuationInterceptor] that wraps continuations in a [FrameDeferredContinuation] to
 * defer dispatching while frame callbacks are being ran by [TestMonotonicFrameClock.performFrame].
 *
 * Only delegates to the parent/wrapped interceptor if it is a [CoroutineDispatcher] that says
 * the continuation needs to be dispatched.
 */
@OptIn(InternalTestApi::class)
internal class FrameDeferringContinuationInterceptor(
    parentInterceptor: ContinuationInterceptor?
) : DelayPropagatingContinuationInterceptorWrapper(parentInterceptor) {
    private val parentDispatcher = parentInterceptor as? CoroutineDispatcher
    private val toRunTrampolined = ArrayDeque<TrampolinedTask<*>>()
    private val lock = Any()
    private var isDeferringContinuations = false

    val hasTrampolinedTasks: Boolean
        get() = synchronized(lock) {
            toRunTrampolined.isNotEmpty()
        }

    /**
     * Runs [block] so that any continuations that are resumed on this dispatcher will not be
     * dispatched until after [block] returns. All such continuations will be dispatched before
     * this function returns.
     */
    fun runWithoutResumingCoroutines(block: () -> Unit) {
        synchronized(lock) {
            check(!isDeferringContinuations)
            isDeferringContinuations = true
        }

        // TODO(aosp/2121435) Tested in CL that adds onPerformTraversals callback.
        try {
            block()
        } catch (e: Throwable) {
            // We still need to resume any queued trampoline tasks so they don't hang, but any
            // individual failures need to also report the traversal failure in case they win the
            // race to cancel the test job. See the kdoc on resumeWithSuppressed for more
            // information.
            finishFrameTasks {
                it.resumeWithSuppressed(e)
            }
            throw e
        }

        // Resume any continuations that were dispatched inside the frame callbacks, as well as the
        // coroutines that called withFrameNanos.
        finishFrameTasks {
            it.resume()
        }
    }

    override fun <T> interceptContinuation(continuation: Continuation<T>): Continuation<T> {
        val deferringContinuation = FrameDeferredContinuation(continuation)
        // If we also ask the parent dispatcher to intercept the continuation when it doesn't
        // need to dispatch, it may return a DispatchedContinuation and dispatch anyway.
        return if (parentDispatcher?.isDispatchNeeded(continuation.context) == true) {
            // Since the parent wraps the deferred continuation, if the parent is a dispatcher
            // it will immediately _dispatch_ the continuation using whatever mechanism it
            // otherwise would, but the underlying continuation won't actually resume until
            // after the frame callbacks. If instead we asked the parent to wrap the original
            // continuation and then wrapped the parent's in a FrameDeferredContinuation, the
            // dispatch itself would not happen until after the frame callback. In practice,
            // both should be effectively the same as long as nothing else is dispatching
            // directly to the parent dispatcher without going through this wrapper (which it
            // shouldn't, since the compose test harness installs both the clock and the
            // dispatcher).
            parentDispatcher.interceptContinuation(deferringContinuation)
        } else {
            deferringContinuation
        }
    }

    /**
     * Dequeues every [toRunTrampolined] task and passes it to [block], then sets
     * [runningFrameCallbacks] to false. Does the proper synchronization to ensure that no
     * trampolined tasks will be enqueued but not processed by this call until
     * [runningFrameCallbacks] is explicitly set to true again.
     */
    private inline fun finishFrameTasks(block: (TrampolinedTask<*>) -> Unit) {
        do {
            var task = nextTrampolinedTask()
            while (task != null) {
                block(task)
                task = nextTrampolinedTask()
            }
        } while (
        // We don't dispatch holding the lock so that other tasks can get in on our
        // trampolining time slice, but once we're done, make sure nothing added a new task
        // before we set runningFrameCallbacks = false, which would prevent the next dispatch
        // from being correctly scheduled. Loop to run these stragglers now.
            synchronized(lock) {
                if (toRunTrampolined.isEmpty()) {
                    // Setting this to false means that once the lock is released, any dispatches
                    // will be delegated directly to the parent dispatcher again.
                    isDeferringContinuations = false
                    false
                } else true
            }
        )
    }

    private fun nextTrampolinedTask(): TrampolinedTask<*>? = synchronized(lock) {
        toRunTrampolined.removeFirstOrNull()
    }

    private class TrampolinedTask<T>(
        private val continuation: Continuation<T>,
        private val result: Result<T>
    ) {
        fun resume() = continuation.resumeWith(result)

        /**
         * Resume the continuation with [result], but include [cause] as a suppressed exception if
         * the result is a failure.
         *
         * If this method is being called, it means the [runWithoutResumingCoroutines]' block failed
         * somehow after executing individual `withFrameNanos` callbacks. That failure is
         * significant and should be reported as at least part of the test failure, by cancelling
         * the root test job.
         * If all the frame callbacks have a success result, then [performFrame] will throw its
         * exception and cancel the test job, failing the test.
         * However, if a `withFrameNanos` callback failed, and the underlying dispatcher is
         * unconfined and the coroutine call stack doesn't block the exception, resuming the
         * continuation with the exception result may end up bubbling up to the test job first
         * and cancelling it and failing the test before the more general frame failure has a
         * chance to. If that happens, we still want to report the frame failure somewhere, so
         * we add it to the suppressed list of the individual failure's exception.
         *
         * TODO(b/255802670): It's still possible for a coroutine that is resumed successfully and
         *  dispatched synchronously to immediately throw _after_ returning, and thus still beat us
         *  to failing the test.
         */
        fun resumeWithSuppressed(cause: Throwable) {
            result.exceptionOrNull()?.addSuppressed(cause)
            continuation.resumeWith(result)
        }
    }

    /**
     * A [Continuation] wrapper that defers dispatching continuations that are resumed while
     * [TestMonotonicFrameClock.performFrame] is running frame callbacks. Such continuations are
     * instead dispatched after all the frame callbacks have finished executing.
     */
    private inner class FrameDeferredContinuation<T>(
        private val continuation: Continuation<T>
    ) : Continuation<T> {
        override val context: CoroutineContext
            get() = continuation.context

        override fun resumeWith(result: Result<T>) {
            val defer = synchronized(lock) {
                if (isDeferringContinuations) {
                    // Defer all continuations resumed while running frame callbacks until
                    // after the frame callbacks have finished running. This needs to be
                    // done while holding the lock to ensure the task actually gets executed
                    // by the current performFrame.
                    toRunTrampolined.addLast(TrampolinedTask(continuation, result))
                    true
                } else {
                    false
                }
            }

            // If resuming immediately, do so outside the critical section above to avoid
            // deadlocking if the continuation tries to resume another continuation.
            if (!defer) {
                continuation.resumeWith(result)
            }
        }
    }
}