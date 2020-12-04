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

package androidx.compose.runtime

import androidx.compose.runtime.dispatch.BroadcastFrameClock
import androidx.compose.runtime.dispatch.DefaultMonotonicFrameClock
import androidx.compose.runtime.dispatch.MonotonicFrameClock
import androidx.compose.runtime.snapshots.MutableSnapshot
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.runtime.snapshots.SnapshotApplyResult
import androidx.compose.runtime.snapshots.SnapshotReadObserver
import androidx.compose.runtime.snapshots.SnapshotWriteObserver
import androidx.compose.runtime.snapshots.takeMutableSnapshot
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ClosedSendChannelException
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.Continuation
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.coroutineContext
import kotlin.coroutines.resume

// TODO: Can we use rootKey for this since all compositions will have an eventual Recomposer parent?
private const val RecomposerCompoundHashKey = 1000

/**
 * Runs [block] with a new, active [Recomposer] applying changes in the calling [CoroutineContext].
 */
suspend fun <R> withRunningRecomposer(
    block: suspend CoroutineScope.(recomposer: Recomposer) -> R
): R = coroutineScope {
    val recomposerJob = Job(coroutineContext[Job])
    val recomposer = Recomposer(coroutineContext + recomposerJob)
    // Will be cancelled when recomposerJob cancels
    launch { recomposer.runRecomposeAndApplyChanges() }
    try {
        block(recomposer)
    } finally {
        recomposerJob.cancel()
    }
}

/**
 * The scheduler for performing recomposition and applying updates to one or more [Composition]s.
 */
// RedundantVisibilityModifier suppressed because metalava picks up internal function overrides
// if 'internal' is not explicitly specified - b/171342041
@Suppress("RedundantVisibilityModifier")
@OptIn(
    ExperimentalComposeApi::class,
    InternalComposeApi::class
)
class Recomposer(
    effectCoroutineContext: CoroutineContext
) : CompositionReference() {

    /**
     * This collection is its own lock, shared with [invalidComposersAwaiter]
     */
    private val invalidComposers = mutableSetOf<Composer<*>>()

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

    private val broadcastFrameClock = BroadcastFrameClock {
        synchronized(invalidComposers) {
            invalidComposersAwaiter?.let {
                invalidComposersAwaiter = null
                idlingLatch.closeLatch()
                it.resume(Unit)
            }
        }
    }

    private val runningRecomposeJobOrException = AtomicReference<Any?>(null)

    /**
     * A [Job] used as a parent of any effects created by this [Recomposer]'s compositions.
     */
    private val effectJob = Job(effectCoroutineContext[Job]).apply {
        invokeOnCompletion { throwable ->
            // Since the running recompose job is operating in a disjoint job if present,
            // kick it out and make sure no new ones start.
            val cancellation = throwable ?: CancellationException("Recomposer completed")
            val old = runningRecomposeJobOrException.getAndSet(cancellation)
            if (old is Job) {
                old.cancel(CancellationException("Recomposer cancelled", cancellation))
            }
        }
    }

    /**
     * The [effectCoroutineContext] is derived from [effectCoroutineContext]
     */
    internal override val effectCoroutineContext: CoroutineContext =
        effectCoroutineContext + broadcastFrameClock + effectJob

    /**
     * This collection is its own lock. When a composer performs initial composition it is added
     * to this collection; when it is unregistered it is removed. Any composers in this set will
     * receive state modification events from this [Recomposer] when snapshots change.
     */
    private val recomposableComposers = mutableSetOf<Composer<*>>()

    /**
     * Record any changes in [snapshotChanges] with the currently registered
     * [recomposableComposers], then drains [appliedChanges] of any further snapshot change sets
     * and records them with the composers before returning.
     */
    private fun recordModificationsForComposers(
        snapshotChanges: Set<Any>,
        appliedChanges: ReceiveChannel<Set<Any>>
    ) {
        synchronized(recomposableComposers) {
            recomposableComposers.forEach {
                it.recordModificationsOf(snapshotChanges)
            }
            // Strange loop to keep nullability smart casts happy
            while (true) {
                appliedChanges.poll()?.also { additionalChanges ->
                    recomposableComposers.forEach {
                        it.recordModificationsOf(additionalChanges)
                    }
                } ?: break
            }
        }
    }

    /**
     * Await the invalidation of any associated [Composer]s, recompose them, and apply their
     * changes to their associated [Composition]s if recomposition is successful.
     *
     * While [runRecomposeAndApplyChanges] is running, [awaitIdle] will suspend until there are no
     * more invalid composers awaiting recomposition.
     *
     * This method never returns. Cancel the calling [CoroutineScope] to stop.
     */
    @Suppress("DEPRECATION") // Remove once recomposeAndApplyChanges is removed from API
    suspend fun runRecomposeAndApplyChanges(): Nothing {
        recomposeAndApplyChanges(Long.MAX_VALUE)
        error("this function never returns")
    }

    /**
     * Await the invalidation of any associated [Composer]s, recompose them, and apply their
     * changes to their associated [Composition]s if recomposition is successful.
     *
     * While [runRecomposeAndApplyChanges] is running, [awaitIdle] will suspend until there are no
     * more invalid composers awaiting recomposition.
     *
     * This method returns after recomposing [frameCount] times, or throws [CancellationException]
     * if the [Recomposer] is [shutDown] or if the [effectCoroutineContext] used to construct the
     * [Recomposer] is cancelled.
     */
    @Deprecated("Drive the calling MonotonicFrameClock to control recomposition by frame")
    suspend fun recomposeAndApplyChanges(frameCount: Long) {
        val parentFrameClock = coroutineContext[MonotonicFrameClock] ?: DefaultMonotonicFrameClock
        withContext(broadcastFrameClock) {
            // Enforce mutual exclusion of callers
            val myJob = coroutineContext[Job]
            while (true) {
                when (val old = runningRecomposeJobOrException.get()) {
                    is Exception -> throw CancellationException("Recomposer cancelled", old)
                    is Job -> error("Recomposition is already running")
                    null -> if (runningRecomposeJobOrException.compareAndSet(null, myJob)) break
                }
            }

            var framesRemaining = frameCount
            val toRecompose = mutableListOf<Composer<*>>()

            // Due to a breaking change in Coroutines 1.4.0, some update events might be dropped.
            // see: https://github.com/Kotlin/kotlinx.coroutines/releases/tag/1.4.0-M1
            // For Compose, we prefer them not to be lost hence we try to re-offer them back to
            // the channel.
            // see: https://kotlin.github.io/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines.channels/-channel/
            var addChangeBack: ((value: Set<Any>) -> Unit)? = null
            val appliedChanges = Channel<Set<Any>>(Channel.UNLIMITED) { undeliveredChanges ->
                addChangeBack?.invoke(undeliveredChanges)
            }
            addChangeBack = {
                try {
                    appliedChanges.offer(it)
                } catch (ignored: ClosedSendChannelException) {}
            }
            // unregisterApplyObserver is called as part of the big finally below
            val unregisterApplyObserver = Snapshot.registerApplyObserver { changed, _ ->
                appliedChanges.offer(changed)
            }

            try {
                idlingLatch.closeLatch()
                synchronized(recomposableComposers) {
                    // Invalidate everyone we know about since we weren't listening
                    // for snapshot changes while the recomposer wasn't running
                    recomposableComposers.forEach { it.invalidateAll() }
                }
                while (frameCount == Long.MAX_VALUE || framesRemaining-- > 0L) {
                    // Don't hold the monitor lock across suspension.
                    val hasInvalidComposers = synchronized(invalidComposers) {
                        invalidComposers.isNotEmpty()
                    }
                    if (!hasInvalidComposers && !broadcastFrameClock.hasAwaiters) {
                        // Listen for snapshot invalidations while we're suspending for reasons
                        // to perform a frame. Recording modifications with a composer might
                        // make us wake up. invalidator will cancelAndJoin below after wakeup
                        // ensuring we will not work with any one composer in parallel during
                        // the frame.
                        val invalidator = launch {
                            for (snapshotChanges in appliedChanges) {
                                recordModificationsForComposers(snapshotChanges, appliedChanges)
                            }
                        }
                        // Suspend until we have something to do. Work might come from:
                        // * Waking up for new invalidations as a result of the launched
                        //   invalidator job above
                        // * Waking up for new invalidations as a result of other external
                        //   composer invalidate calls, e.g. the manual invalidate composable
                        // * Waking up for new broadcast frame awaiters
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
                        // Stop listening to frame invalidations for now, we'll catch the rest
                        // when we actually reach the next frame below. It's important that we not
                        // modify/operate a composer in parallel with itself, which might happen
                        // with recording changes vs. recomposing if we are on a parallel
                        // dispatcher.
                        invalidator.cancelAndJoin()
                    }

                    // Align work with the next frame to coalesce changes.
                    // Note: it is possible to resume from the above with no recompositions pending,
                    // instead someone might be awaiting our frame clock dispatch below.
                    // We use the cached frame clock from above not just so that we don't locate it
                    // each time, but because we've installed the broadcastFrameClock as the scope
                    // clock above for user code to locate.
                    parentFrameClock.withFrameNanos { frameTime ->
                        trace("recomposeFrame") {
                            // Propagate the frame time to anyone who is awaiting from the
                            // recomposer clock.
                            broadcastFrameClock.sendFrame(frameTime)

                            // Ensure any global changes are observed
                            Snapshot.sendApplyNotifications()

                            // Drain any composer invalidations from snapshot changes
                            appliedChanges.poll()?.let { snapshotChanges ->
                                recordModificationsForComposers(snapshotChanges, appliedChanges)
                            }

                            // ...and pick up any stragglers as a result of the above snapshot sync
                            synchronized(invalidComposers) {
                                toRecompose.addAll(invalidComposers)
                                invalidComposers.clear()
                            }

                            // Actually perform recomposition for any invalidated composers
                            if (toRecompose.isNotEmpty()) {
                                for (i in 0 until toRecompose.size) {
                                    performRecompose(toRecompose[i])
                                }
                                toRecompose.clear()
                            }
                        }
                    }
                }
            } finally {
                unregisterApplyObserver()
                // Only replace the value if it currently matches; a new caller may have already
                // set its own job as a replacement before we resume to cancel.
                runningRecomposeJobOrException.compareAndSet(myJob, null)
                // If we're not still running frames, we're effectively idle.
                idlingLatch.openLatch()
            }
        }
    }

    /**
     * Permanently shut down this [Recomposer] for future use. All ongoing recompositions will stop,
     * new composer invalidations with this [Recomposer] at the root will no longer occur,
     * and any [LaunchedEffect]s currently running in compositions managed by this [Recomposer]
     * will be cancelled. Any [rememberCoroutineScope] scopes from compositions managed by this
     * [Recomposer] will also be cancelled. See [join] to await the completion of all of these
     * outstanding tasks.
     */
    fun shutDown() {
        effectJob.cancel()
    }

    /**
     * Await the completion of a [shutDown] operation.
     */
    suspend fun join() {
        effectJob.join()
    }

    internal override fun composeInitial(
        composer: Composer<*>,
        composable: @Composable () -> Unit
    ) {
        val composerWasComposing = composer.isComposing
        composing(composer) {
            composer.composeInitial(composable)
        }
        // TODO(b/143755743)
        if (!composerWasComposing) {
            Snapshot.notifyObjectsInitialized()
        }
        composer.applyChanges()

        synchronized(recomposableComposers) {
            recomposableComposers.add(composer)
        }

        if (!composerWasComposing) {
            // Ensure that any state objects created during applyChanges are seen as changed
            // if modified after this call.
            Snapshot.notifyObjectsInitialized()
        }
    }

    private fun performRecompose(composer: Composer<*>): Boolean {
        if (composer.isComposing || composer.isDisposed) return false
        return composing(composer) {
            composer.recompose().also {
                Snapshot.notifyObjectsInitialized()
                composer.applyChanges()
            }
        }
    }

    private fun readObserverOf(composer: Composer<*>): SnapshotReadObserver {
        return { value -> composer.recordReadOf(value) }
    }

    private fun writeObserverOf(composer: Composer<*>): SnapshotWriteObserver {
        return { value -> composer.recordWriteOf(value) }
    }

    private inline fun <T> composing(composer: Composer<*>, block: () -> T): T {
        val snapshot = takeMutableSnapshot(
            readObserverOf(composer), writeObserverOf(composer)
        )
        try {
            return snapshot.enter(block)
        } finally {
            applyAndCheck(snapshot)
        }
    }

    private fun applyAndCheck(snapshot: MutableSnapshot) {
        val applyResult = snapshot.apply()
        if (applyResult is SnapshotApplyResult.Failure) {
            error(
                "Unsupported concurrent change during composition. A state object was " +
                    "modified by composition as well as being modified outside composition."
            )
            // TODO(chuckj): Consider lifting this restriction by forcing a recompose
        }
    }

    /**
     * Returns true if any pending invalidations have been scheduled.
     */
    fun hasInvalidations(): Boolean =
        !idlingLatch.isOpen || synchronized(invalidComposers) { invalidComposers.isNotEmpty() }

    /**
     * Suspends until the currently pending recomposition frame is complete.
     * Any recomposition for this recomposer triggered by actions before this call begins
     * will be complete and applied (if recomposition was successful) when this call returns.
     *
     * If [runRecomposeAndApplyChanges] is not currently running the [Recomposer] is considered idle
     * and this method will not suspend.
     */
    suspend fun awaitIdle(): Unit = idlingLatch.await()

    // Recomposer always starts with a constant compound hash
    internal override val compoundHashKey: Int
        get() = RecomposerCompoundHashKey

    // Collecting key sources happens at the level of a composer; starts as false
    internal override val collectingKeySources: Boolean
        get() = false

    // Collecting parameter happens at the level of a composer; starts as false
    internal override val collectingParameterInformation: Boolean
        get() = false

    internal override fun recordInspectionTable(table: MutableSet<CompositionData>) {
        // TODO: The root recomposer might be a better place to set up inspection
        // than the current configuration with an ambient
    }

    internal override fun registerComposerWithRoot(composer: Composer<*>) {
        // Do nothing.
    }

    internal override fun unregisterComposerWithRoot(composer: Composer<*>) {
        synchronized(recomposableComposers) {
            recomposableComposers.remove(composer)
        }
        synchronized(invalidComposers) {
            invalidComposers.remove(composer)
        }
    }

    internal override fun invalidate(composer: Composer<*>) {
        synchronized(invalidComposers) {
            invalidComposers.add(composer)
            invalidComposersAwaiter?.let {
                invalidComposersAwaiter = null
                idlingLatch.closeLatch()
                it.resume(Unit)
            }
        }
    }

    companion object {
        @OptIn(ExperimentalCoroutinesApi::class)
        private val mainRecomposer: Recomposer by lazy {
            val embeddingContext = EmbeddingContext()
            val mainScope = CoroutineScope(
                NonCancellable + embeddingContext.mainThreadCompositionContext()
            )

            Recomposer(mainScope.coroutineContext).also {
                // NOTE: Launching undispatched so that compositions created with the
                // Recomposer.current() singleton instance can assume the recomposer is running
                // when they perform initial composition. The relevant Recomposer code is
                // appropriately thread-safe for this.
                mainScope.launch(start = CoroutineStart.UNDISPATCHED) {
                    it.runRecomposeAndApplyChanges()
                }
            }
        }

        /**
         * Retrieves [Recomposer] for the current thread. Needs to be the main thread.
         */
        @TestOnly
        fun current(): Recomposer = mainRecomposer
    }
}