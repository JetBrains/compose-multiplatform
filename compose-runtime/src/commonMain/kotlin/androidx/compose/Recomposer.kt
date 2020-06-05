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

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.Continuation
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.resume

/**
 * Runs [block] with a new, active [Recomposer] applying changes in the calling [CoroutineContext].
 * [frameClock] is used to align changes with display frames.
 */
suspend fun withRunningRecomposer(
    frameClock: CompositionFrameClock,
    block: suspend CoroutineScope.(recomposer: Recomposer) -> Unit
): Unit = coroutineScope {
    val recomposer = Recomposer()
    val recompositionJob = launch { recomposer.runRecomposeAndApplyChanges(frameClock) }
    block(recomposer)
    recompositionJob.cancel()
}

/**
 * The scheduler for performing recomposition and applying updates to one or more [Composition]s.
 * [frameClock] is used to align changes with display frames.
 */
class Recomposer {

    /**
     * This collection is its own lock, shared with [invalidComposersAwaiter]
     */
    private val invalidComposers = mutableListOf<Composer<*>>()

    /**
     * The continuation to resume when there are invalid composers to process.
     */
    private var invalidComposersAwaiter: Continuation<Unit>? = null

    /**
     * Track if any outstanding invalidated composers are awaiting recomposition.
     * This latch is closed any time we resume invalidComposersAwaiter and opened
     * by [recomposeAndApplyChanges] when it suspends when it has no further work to do.
     */
    private val idlingLatch = Latch()

    /**
     * Enforces that only one caller of [runRecomposeAndApplyChanges] is active at a time
     * while carrying its calling scope. Used to [launchEffect] on the apply dispatcher.
     */
    // TODO(adamp) convert to atomicfu once ready
    private val applyingScope = AtomicReference<CoroutineScope?>(null)

    private val broadcastFrameClock = BroadcastFrameClock {
        synchronized(invalidComposers) {
            invalidComposersAwaiter?.let {
                invalidComposersAwaiter = null
                idlingLatch.closeLatch()
                it.resume(Unit)
            }
        }
    }
    val frameClock: CompositionFrameClock get() = broadcastFrameClock

    /**
     * Await the invalidation of any associated [Composer]s, recompose them, and apply their
     * changes to their associated [Composition]s if recomposition is successful.
     *
     * While [runRecomposeAndApplyChanges] is running, [awaitIdle] will suspend until there are no
     * more invalid composers awaiting recomposition.
     *
     * This method never returns. Cancel the calling [CoroutineScope] to stop.
     */
    suspend fun runRecomposeAndApplyChanges(
        frameClock: CompositionFrameClock
    ): Nothing {
        coroutineScope {
            recomposeAndApplyChanges(this, frameClock, Long.MAX_VALUE)
        }
        error("this function never returns")
    }

    /**
     * Await the invalidation of any associated [Composer]s, recompose them, and apply their
     * changes to their associated [Composition]s if recomposition is successful. Any launched
     * effects of composition will be launched into the receiver [CoroutineScope].
     *
     * While [runRecomposeAndApplyChanges] is running, [awaitIdle] will suspend until there are no
     * more invalid composers awaiting recomposition.
     *
     * This method returns after recomposing [frameCount] times.
     */
    suspend fun recomposeAndApplyChanges(
        applyCoroutineScope: CoroutineScope,
        frameClock: CompositionFrameClock,
        frameCount: Long
    ) {
        var framesRemaining = frameCount
        val toRecompose = mutableSetOf<Composer<*>>()

        if (!applyingScope.compareAndSet(null, applyCoroutineScope)) {
            error("already recomposing and applying changes")
        }

        try {
            idlingLatch.closeLatch()
            while (true) {
                // Suspend until we have something to do
                if (toRecompose.isEmpty()) {
                    // Don't hold the monitor lock across suspension.
                    val shouldSuspend = synchronized(invalidComposers) {
                        if (invalidComposers.isEmpty()) true else {
                            toRecompose.addAll(invalidComposers)
                            invalidComposers.clear()
                            false
                        }
                    }

                    if (shouldSuspend) {
                        suspendCancellableCoroutine<Unit> { co ->
                            synchronized(invalidComposers) {
                                if (invalidComposers.isEmpty()) {
                                    invalidComposersAwaiter = co
                                    idlingLatch.openLatch()
                                } else {
                                    // We raced and lost, someone invalidated between our check
                                    // and suspension. Resume immediately.
                                    co.resume(Unit)
                                    return@suspendCancellableCoroutine
                                }
                            }
                            co.invokeOnCancellation {
                                synchronized(invalidComposers) {
                                    if (invalidComposersAwaiter === co) {
                                        invalidComposersAwaiter = null
                                    }
                                }
                            }
                        }
                        synchronized(invalidComposers) {
                            toRecompose.addAll(invalidComposers)
                            invalidComposers.clear()
                        }
                    }
                }

                // Align work with the next frame to coalesce changes.
                // Note: it is possible to resume from the above with no recompositions pending,
                // instead someone might be awaiting our frame clock dispatch below.
                frameClock.awaitFrameNanos { frameTime ->
                    trace("recomposeFrame") {
                        // Propagate the frame time to anyone who is awaiting from the
                        // recomposer clock.
                        broadcastFrameClock.sendFrame(frameTime)

                        // Ensure any committed frames in other threads are visible
                        FrameManager.nextFrame()

                        // ...and make sure we know about any pending invalidations the commit
                        // may have caused before recomposing - Handler messages can't run between
                        // input processing and the frame clock pulse!
                        FrameManager.synchronize()

                        // ...and pick up any stragglers as a result of the above frame sync
                        synchronized(invalidComposers) {
                            toRecompose.addAll(invalidComposers)
                            invalidComposers.clear()
                        }

                        if (toRecompose.isNotEmpty()) {
                            toRecompose.forEach { performRecompose(it) }
                            toRecompose.clear()
                        }

                        // Ensure any changes made during composition are now visible to other
                        // threads.
                        FrameManager.nextFrame()
                    }
                }

                if (framesRemaining < Long.MAX_VALUE) {
                    framesRemaining--
                    if (framesRemaining == 0L) break
                }

                // Check to see if anyone else wanted to be recomposed
                // while we were busy applying changes
                synchronized(invalidComposers) {
                    toRecompose.addAll(invalidComposers)
                    invalidComposers.clear()
                }
            }
        } finally {
            applyingScope.set(null)
            // If we're not still running frames, we're effectively idle.
            idlingLatch.openLatch()
        }
    }

    private class CompositionCoroutineScopeImpl(
        override val coroutineContext: CoroutineContext,
        frameClock: CompositionFrameClock
    ) : CompositionCoroutineScope(), CompositionFrameClock by frameClock

    /**
     * Implementation note: we launch effects undispatched so they can begin immediately during
     * the apply step. This function is only called internally by [launchInComposition]
     * implementations during [CompositionLifecycleObserver] callbacks dispatched on the
     * applying scope, so we consider this safe.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    internal fun launchEffect(
        block: suspend CompositionCoroutineScope.() -> Unit
    ): Job = applyingScope.get()?.launch(start = CoroutineStart.UNDISPATCHED) {
        CompositionCoroutineScopeImpl(coroutineContext, frameClock).block()
    } ?: error("apply scope missing; runRecomposeAndApplyChanges must be running")

    @Suppress("PLUGIN_WARNING", "PLUGIN_ERROR")
    internal fun composeInitial(
        composable: @Composable () -> Unit,
        composer: Composer<*>
    ) {
        val composerWasComposing = composer.isComposing
        val prevComposer = currentComposerInternal
        try {
            try {
                composer.isComposing = true
                currentComposerInternal = composer
                FrameManager.composing {
                    trace("Compose:recompose") {
                        var complete = false
                        try {
                            composer.startRoot()
                            composer.startGroup(invocationKey, invocation)
                            invokeComposable(composer, composable)
                            composer.endGroup()
                            composer.endRoot()
                            complete = true
                        } finally {
                            if (!complete) composer.abortRoot()
                        }
                    }
                }
            } finally {
                composer.isComposing = composerWasComposing
            }
            // TODO(b/143755743)
            if (!composerWasComposing) {
                FrameManager.nextFrame()
            }
            composer.applyChanges()
            if (!composerWasComposing) {
                FrameManager.nextFrame()
            }
        } finally {
            currentComposerInternal = prevComposer
        }
    }

    private fun performRecompose(composer: Composer<*>): Boolean {
        if (composer.isComposing) return false
        val prevComposer = currentComposerInternal
        val hadChanges: Boolean
        try {
            currentComposerInternal = composer
            composer.isComposing = true
            hadChanges = FrameManager.composing {
                composer.recompose()
            }
            composer.applyChanges()
        } finally {
            composer.isComposing = false
            currentComposerInternal = prevComposer
        }
        return hadChanges
    }

    fun hasPendingChanges(): Boolean =
        !idlingLatch.isOpen || synchronized(invalidComposers) { invalidComposers.isNotEmpty() }

    internal fun scheduleRecompose(composer: Composer<*>) {
        synchronized(invalidComposers) {
            invalidComposers.add(composer)
            invalidComposersAwaiter?.let {
                invalidComposersAwaiter = null
                idlingLatch.closeLatch()
                it.resume(Unit)
            }
        }
    }

    /**
     * Suspends until the currently pending recomposition frame is complete.
     * Any recomposition for this recomposer triggered by actions before this call begins
     * will be complete and applied (if recomposition was successful) when this call returns.
     *
     * If [runRecomposeAndApplyChanges] is not currently running the [Recomposer] is considered idle
     * and this method will not suspend.
     */
    suspend fun awaitIdle(): Unit = idlingLatch.await()

    companion object {

        /**
         * Check if there's pending changes to be recomposed in this thread
         *
         * @return true if there're pending changes in this thread, false otherwise
         */
        @Deprecated(
            "Use the Recomposer instance fun instead",
            ReplaceWith(
                "Recomposer.current().hasPendingChanges()",
                "androidx.compose.Recomposer"
            )
        )
        fun hasPendingChanges() = current().hasPendingChanges()

        /**
         * Retrieves [Recomposer] for the current thread. Needs to be the main thread.
         */
        @TestOnly
        fun current(): Recomposer {
            require(isMainThread()) { "No Recomposer for this Thread" }

            return mainRecomposer ?: run {
                val mainScope = CoroutineScope(NonCancellable + mainThreadCompositionDispatcher())

                Recomposer().also {
                    mainRecomposer = it
                    @OptIn(ExperimentalCoroutinesApi::class)
                    mainScope.launch(start = CoroutineStart.UNDISPATCHED) {
                        it.runRecomposeAndApplyChanges(mainThreadCompositionFrameClock())
                    }
                }
            }
        }

        private var mainRecomposer: Recomposer? = null
    }
}