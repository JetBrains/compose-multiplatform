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
import kotlinx.collections.immutable.PersistentSet
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.takeWhile
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.coroutineContext

// TODO: Can we use rootKey for this since all compositions will have an eventual Recomposer parent?
private const val RecomposerCompoundHashKey = 1000

/**
 * Runs [block] with a new, active [Recomposer] applying changes in the calling [CoroutineContext].
 */
suspend fun <R> withRunningRecomposer(
    block: suspend CoroutineScope.(recomposer: Recomposer) -> R
): R = coroutineScope {
    val recomposer = Recomposer(coroutineContext)
    // Will be cancelled when recomposerJob cancels
    launch { recomposer.runRecomposeAndApplyChanges() }
    block(recomposer).also {
        recomposer.shutDown()
    }
}

private inline fun <reified T> Array<T>.without(toRemove: T): Array<T> {
    val foundAt = indexOf(toRemove)
    if (foundAt < 0) return this
    return Array(size - 1) { i ->
        if (i < foundAt) this[i] else this[i + 1]
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
     * This is a running count of the number of times the recomposer awoke and applied changes to
     * one or more composers. This count is unaffected if the composer awakes and recomposed but
     * composition did not produce changes to apply.
     */
    var changeCount = 0
        private set

    private val broadcastFrameClock = BroadcastFrameClock {
        while (true) {
            val old = _state.value
            // If we're shutting down or shut down, we're not going to wake back up.
            // Cancel the BroadcastFrameClock with an exception to free any current or future
            // awaiters.
            val new = old.withFrameClockAwaiters() ?: throw CancellationException(
                "Recomposer shutdown; frame clock awaiter will never resume",
                old.closeCause
            )
            if (_state.compareAndSet(old, new)) break
        }
    }

    /**
     * A [Job] used as a parent of any effects created by this [Recomposer]'s compositions.
     */
    private val effectJob = Job(effectCoroutineContext[Job]).apply {
        invokeOnCompletion { throwable ->
            // Since the running recompose job is operating in a disjoint job if present,
            // kick it out and make sure no new ones start if we have one.
            val cancellation = CancellationException("Recomposer effect job completed", throwable)

            while (true) {
                val old = _state.value
                if (old.runnerJob != null) {
                    // If we have a runner job we need to cancel it and wait until it's complete
                    // before we consider the recomposer to be fully shut down. We are still
                    // in the _process_ of shutting down until then.
                    val new = old.shuttingDown(cancellation)
                    if (_state.compareAndSet(old, new)) {
                        old.runnerJob.cancel(cancellation)
                        old.runnerJob.invokeOnCompletion { runnerJobCause ->
                            _state.value = old.shutDown(
                                throwable?.apply {
                                    runnerJobCause
                                        ?.takeIf { it !is CancellationException }
                                        ?.let { addSuppressed(it) }
                                }
                            )
                        }
                        break
                    }
                } else {
                    // If we didn't have a runner job to await, the completion of the effect job
                    // represents the full and successful shutdown of the Recomposer.
                    val new = old.shutDown(cancellation)
                    if (_state.compareAndSet(old, new)) break
                }
            }
        }
    }

    /**
     * The [effectCoroutineContext] is derived from the parameter of the same name.
     */
    internal override val effectCoroutineContext: CoroutineContext =
        effectCoroutineContext + broadcastFrameClock + effectJob

    /**
     * Valid operational states of a [Recomposer].
     */
    enum class State {
        /**
         * [shutDown] was called on the [Recomposer] and all cleanup work has completed.
         * The [Recomposer] is no longer available for use.
         */
        ShutDown,

        /**
         * [shutDown] was called on the [Recomposer] and it is no longer available for use.
         * Cleanup work has not yet been fully completed and composition effect coroutines may
         * still be running.
         */
        ShuttingDown,

        /**
         * The [Recomposer] is not tracking invalidations for known composers and it will not
         * recompose them in response to changes. Call [runRecomposeAndApplyChanges] to await and
         * perform work. This is the initial state of a newly constructed [Recomposer].
         */
        Inactive,

        /**
         * The [Recomposer] is [Inactive] but at least one effect associated with a managed
         * composition is awaiting a frame. This frame will not be produced until the [Recomposer]
         * is [running][runRecomposeAndApplyChanges].
         */
        InactivePendingWork,

        /**
         * The [Recomposer] is tracking composition and snapshot invalidations but there is
         * currently no work to do.
         */
        Idle,

        /**
         * The [Recomposer] has been notified of pending work it must perform and is either
         * actively performing it or awaiting the appropriate opportunity to perform it.
         * This work may include invalidated composers that must be recomposed, snapshot state
         * changes that must be presented to known composers to check for invalidated
         * compositions, or coroutines awaiting a frame using the Recomposer's
         * [MonotonicFrameClock].
         */
        PendingWork
    }

    /**
     * Primary atomic/immutable state object used with [_state].
     * Each `with` method creates and returns a copy with the requested changes.
     * If a copy method returns `null`, the requested state cannot be reached from
     * the current state.
     */
    private class StateInfo private constructor(
        val state: State,
        val runnerJob: Job?,
        val closeCause: Throwable?,
        private val knownComposers: PersistentSet<Composer<*>>,
        private val snapshotInvalidations: Any?,
        private val composerInvalidations: Any?,
        val hasFrameClockAwaiters: Boolean
    ) {
        /**
         * Internal implementation detail for other utilities that copy while preserving
         * required invariants. Generates the appropriate [state] for the new [StateInfo].
         */
        private fun copy(
            runnerJob: Job? = this.runnerJob,
            closeCause: Throwable? = this.closeCause,
            knownComposers: PersistentSet<Composer<*>> = this.knownComposers,
            snapshotInvalidations: Any? = this.snapshotInvalidations,
            composerInvalidations: Any? = this.composerInvalidations,
            hasFrameClockAwaiters: Boolean = this.hasFrameClockAwaiters
        ): StateInfo {
            val newRunnerJob = runnerJob.takeIf { state > State.ShuttingDown }
            val newSnapshotInvalidations = snapshotInvalidations.takeIf { newRunnerJob != null }
            val newComposerInvalidations = composerInvalidations.takeIf { newRunnerJob != null }
            val hasWork = hasFrameClockAwaiters ||
                newSnapshotInvalidations != null ||
                newComposerInvalidations != null
            val newState = when {
                // Maintain existing shutdown state if copied
                state <= State.ShuttingDown -> state
                newRunnerJob != null -> {
                    if (hasWork) State.PendingWork else State.Idle
                }
                else -> {
                    if (hasWork) State.InactivePendingWork else State.Inactive
                }
            }
            return StateInfo(
                state = newState,
                runnerJob = newRunnerJob,
                closeCause = closeCause,
                knownComposers = knownComposers,
                snapshotInvalidations = newSnapshotInvalidations,
                composerInvalidations = newComposerInvalidations,
                hasFrameClockAwaiters = hasFrameClockAwaiters
            )
        }

        /**
         * `true` if the current state has pending work that requires performing a frame
         */
        val hasFrameWork: Boolean
            get() = hasFrameClockAwaiters || hasComposerInvalidations

        val hasSnapshotChanges: Boolean
            get() = snapshotInvalidations != null

        /**
         * `true` if one or more [Composer]s want to recompose.
         */
        val hasComposerInvalidations: Boolean
            get() = composerInvalidations != null

        fun withKnownComposer(
            composer: Composer<*>
        ) = if (state <= State.ShuttingDown) null else copy(
            knownComposers = knownComposers.add(composer)
        )

        @Suppress("UNCHECKED_CAST")
        fun withoutKnownComposer(
            composer: Composer<*>
        ) = if (composer !in knownComposers) this else copy(
            knownComposers = knownComposers.remove(composer),
            composerInvalidations = when (composerInvalidations) {
                null -> null
                is Composer<*> -> composerInvalidations.takeUnless<Any?> { it === composer }
                is Array<*> -> (composerInvalidations as Array<Composer<*>>).without(composer)
                else -> error("invalid composerInvalidations $composerInvalidations")
            }
        )

        inline fun forEachKnownComposer(
            block: (Composer<*>) -> Unit
        ) {
            knownComposers.forEach(block)
        }

        fun withRunnerJob(runnerJob: Job) = when {
            closeCause != null -> throw closeCause
            this.runnerJob != null -> error("Recomposer is already running")
            else -> copy(runnerJob = runnerJob)
        }

        fun withoutRunnerJob(runnerJob: Job) = when {
            this.runnerJob !== runnerJob -> null
            state < State.Idle -> null
            else -> copy(
                runnerJob = null,
                composerInvalidations = null,
                snapshotInvalidations = null
            )
        }

        fun withSnapshotInvalidation(changed: Set<Any>) =
            if (state < State.Idle) null else copy(
                snapshotInvalidations = @Suppress("UNCHECKED_CAST") when (snapshotInvalidations) {
                    null -> changed
                    is Set<*> -> arrayOf(snapshotInvalidations, changed)
                    is Array<*> -> (snapshotInvalidations as Array<Set<Any>>) + changed
                    else -> error("invalid snapshotInvalidations $snapshotInvalidations")
                }
            )

        inline fun forEachSnapshotInvalidation(block: (Set<Any>) -> Unit) {
            @Suppress("UNCHECKED_CAST")
            when (snapshotInvalidations) {
                null -> return
                is Set<*> -> block(snapshotInvalidations as Set<Any>)
                is Array<*> -> for (changed in snapshotInvalidations) {
                    block(changed as Set<Any>)
                }
                else -> error("invalid snapshotInvalidations $snapshotInvalidations")
            }
        }

        fun hasComposerInvalidation(composer: Composer<*>): Boolean = when (composerInvalidations) {
            null -> false
            is Composer<*> -> composerInvalidations === composer
            is Array<*> -> composer in composerInvalidations
            else -> error("invalid composerInvalidations $composerInvalidations")
        }

        fun withComposerInvalidation(composer: Composer<*>) =
            if (state < State.Idle) null else copy(
                composerInvalidations = @Suppress("UNCHECKED_CAST") when (composerInvalidations) {
                    null -> composer
                    is Composer<*> -> arrayOf(composerInvalidations, composer)
                    is Array<*> -> (composerInvalidations as Array<Composer<*>>) + composer
                    else -> error("invalid composerInvalidations $composerInvalidations")
                }
            )

        inline fun forEachInvalidComposer(block: (Composer<*>) -> Unit) {
            when (composerInvalidations) {
                null -> return
                is Composer<*> -> block(composerInvalidations)
                is Array<*> -> for (composer in composerInvalidations) {
                    block(composer as Composer<*>)
                }
                else -> error("invalid composerInvalidations $composerInvalidations")
            }
        }

        fun withFrameClockAwaiters() = if (state <= State.ShuttingDown) null else copy(
            hasFrameClockAwaiters = true
        )

        fun withoutInvalidSnapshots() = if (state <= State.ShuttingDown) null else copy(
            snapshotInvalidations = null
        )

        fun withoutFrameClockAwaiters() = if (state <= State.ShuttingDown) null else copy(
            hasFrameClockAwaiters = false
        )

        fun withoutInvalidComposers() = if (state <= State.ShuttingDown) null else copy(
            composerInvalidations = null
        )

        fun shuttingDown(cause: Throwable) = StateInfo(
            state = State.ShuttingDown,
            runnerJob = runnerJob,
            closeCause = cause,
            knownComposers = knownComposers,
            snapshotInvalidations = null,
            composerInvalidations = null,
            hasFrameClockAwaiters = false
        )

        fun shutDown(cause: Throwable? = null) = StateInfo(
            state = State.ShutDown,
            runnerJob = null,
            closeCause = cause,
            knownComposers = persistentSetOf(),
            snapshotInvalidations = null,
            composerInvalidations = null,
            hasFrameClockAwaiters = false
        )

        companion object {
            val Inactive = StateInfo(
                state = State.Inactive,
                runnerJob = null,
                closeCause = null,
                knownComposers = persistentSetOf(),
                snapshotInvalidations = null,
                composerInvalidations = null,
                hasFrameClockAwaiters = false
            )
        }
    }

    /**
     * The primary state driving the recomposer. Always updated via compareAndSet.
     */
    private val _state = MutableStateFlow(StateInfo.Inactive)

    /**
     * The current [State] of this [Recomposer]. See each [State] value for its meaning.
     */
    public val state: Flow<State> = _state.map { it.state }.distinctUntilChanged()

    private fun recordComposerModifications() {
        while (true) {
            val old = _state.value
            val minusSnapshots = old.withoutInvalidSnapshots() ?: break
            if (_state.compareAndSet(old, minusSnapshots)) {
                old.forEachSnapshotInvalidation { changes ->
                    old.forEachKnownComposer { composer ->
                        composer.recordModificationsOf(changes)
                    }
                }
                break
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
    suspend fun runRecomposeAndApplyChanges(): Nothing {
        val parentFrameClock = coroutineContext[MonotonicFrameClock] ?: DefaultMonotonicFrameClock
        withContext(broadcastFrameClock) {
            // Enforce mutual exclusion of callers; register self as current runner
            val runnerJob = coroutineContext.job
            while (true) {
                val old = _state.value
                val new = old.withRunnerJob(runnerJob)
                if (_state.compareAndSet(old, new)) break
            }

            // Observe snapshot changes and propagate them to known composers only from
            // this caller's dispatcher, never working with the same composer in parallel.
            // unregisterApplyObserver is called as part of the big finally below
            val unregisterApplyObserver = Snapshot.registerApplyObserver { changed, _ ->
                while (true) {
                    val old = _state.value
                    val new = old.withSnapshotInvalidation(changed) ?: break
                    if (_state.compareAndSet(old, new)) break
                }
            }

            try {
                // Invalidate all registered composers when we start since we weren't observing
                // snapshot changes on their behalf. Assume anything could have changed.
                _state.value.forEachKnownComposer { it.invalidateAll() }

                // Used to suspend until there is work to process
                val awaitPendingWorkFlow = _state.takeWhile { it.state != State.PendingWork }

                while (true) {
                    // Await something to do
                    awaitPendingWorkFlow.collect()

                    recordComposerModifications()

                    if (!_state.value.hasFrameWork) continue

                    // Align work with the next frame to coalesce changes.
                    // Note: it is possible to resume from the above with no recompositions pending,
                    // instead someone might be awaiting our frame clock dispatch below.
                    // We use the cached frame clock from above not just so that we don't locate it
                    // each time, but because we've installed the broadcastFrameClock as the scope
                    // clock above for user code to locate.
                    parentFrameClock.withFrameNanos { frameTime ->
                        trace("recomposeFrame") {
                            // Dispatch MonotonicFrameClock frames first; this may produce new
                            // composer invalidations that we must handle during the same frame.
                            if (_state.value.hasFrameClockAwaiters) {
                                // Remove the hasFrameClockAwaiters bit before sending a frame.
                                // New awaiters *during* the frame may set it back and we'll
                                // produce another frame later.
                                while (true) {
                                    val old = _state.value
                                    val new = old.withoutFrameClockAwaiters() ?: break
                                    if (_state.compareAndSet(old, new)) break
                                }

                                // Propagate the frame time to anyone who is awaiting from the
                                // recomposer clock.
                                broadcastFrameClock.sendFrame(frameTime)

                                // Ensure any global changes are observed
                                Snapshot.sendApplyNotifications()
                            }

                            // Drain any composer invalidations from snapshot changes
                            recordComposerModifications()

                            // Perform recomposition for any invalidated composers
                            var changes = false
                            while (true) {
                                val old = _state.value
                                val new = old.withoutInvalidComposers() ?: break
                                if (_state.compareAndSet(old, new)) {
                                    old.forEachInvalidComposer { composer ->
                                        changes = performRecompose(composer) || changes
                                    }
                                    break
                                }
                            }
                            if (changes) changeCount++
                        }
                    }
                }
            } finally {
                unregisterApplyObserver()
                while (true) {
                    val old = _state.value
                    val new = old.withoutRunnerJob(runnerJob) ?: break
                    if (_state.compareAndSet(old, new)) break
                }
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

        while (true) {
            val old = _state.value
            val new = old.withKnownComposer(composer) ?: break
            if (_state.compareAndSet(old, new)) break
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
            composer.recompose()
        }.also {
            composer.applyChanges()
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
    fun hasInvalidations(): Boolean = with(_state.value) {
        hasSnapshotChanges || hasFrameWork
    }

    /**
     * Suspends until the currently pending recomposition frame is complete.
     * Any recomposition for this recomposer triggered by actions before this call begins
     * will be complete and applied (if recomposition was successful) when this call returns.
     *
     * If [runRecomposeAndApplyChanges] is not currently running the [Recomposer] is considered idle
     * and this method will not suspend.
     */
    suspend fun awaitIdle() {
        state.takeWhile { it > State.Idle }.collect()
    }

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
        while (true) {
            val old = _state.value
            val new = old.withoutKnownComposer(composer)
            if (_state.compareAndSet(old, new)) break
        }
    }

    internal override fun invalidate(composer: Composer<*>) {
        while (true) {
            val old = _state.value
            if (old.hasComposerInvalidation(composer)) break
            val new = old.withComposerInvalidation(composer) ?: break
            if (_state.compareAndSet(old, new)) break
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
