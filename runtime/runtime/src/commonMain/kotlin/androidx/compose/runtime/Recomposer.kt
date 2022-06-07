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

import androidx.compose.runtime.collection.IdentityArraySet
import androidx.compose.runtime.snapshots.MutableSnapshot
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.runtime.snapshots.SnapshotApplyResult
import androidx.compose.runtime.snapshots.fastForEach
import androidx.compose.runtime.snapshots.fastMap
import androidx.compose.runtime.snapshots.fastMapNotNull
import androidx.compose.runtime.tooling.CompositionData
import androidx.compose.runtime.external.kotlinx.collections.immutable.persistentSetOf
import androidx.compose.runtime.snapshots.fastAny
import androidx.compose.runtime.snapshots.fastGroupBy
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.takeWhile
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.Continuation
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.coroutineContext
import kotlin.coroutines.resume
import kotlin.native.concurrent.ThreadLocal

// TODO: Can we use rootKey for this since all compositions will have an eventual Recomposer parent?
private const val RecomposerCompoundHashKey = 1000

/**
 * Runs [block] with a new, active [Recomposer] applying changes in the calling [CoroutineContext].
 * The [Recomposer] will be [closed][Recomposer.close] after [block] returns.
 * [withRunningRecomposer] will return once the [Recomposer] is [Recomposer.State.ShutDown]
 * and all child jobs launched by [block] have [joined][Job.join].
 */
suspend fun <R> withRunningRecomposer(
    block: suspend CoroutineScope.(recomposer: Recomposer) -> R
): R = coroutineScope {
    val recomposer = Recomposer(coroutineContext)
    // Will be cancelled when recomposerJob cancels
    launch { recomposer.runRecomposeAndApplyChanges() }
    block(recomposer).also {
        recomposer.close()
        recomposer.join()
    }
}

/**
 * Read-only information about a [Recomposer]. Used when code should only monitor the activity of
 * a [Recomposer], and not attempt to alter its state or create new compositions from it.
 */
interface RecomposerInfo {
    /**
     * The current [State] of the [Recomposer]. See each [State] value for its meaning.
     */
    // TODO: Mirror the currentState/StateFlow API change here once we can safely add
    // default interface methods. https://youtrack.jetbrains.com/issue/KT-47000
    val state: Flow<Recomposer.State>

    /**
     * `true` if the [Recomposer] has been assigned work to do and it is currently performing
     * that work or awaiting an opportunity to do so.
     */
    val hasPendingWork: Boolean

    /**
     * The running count of the number of times the [Recomposer] awoke and applied changes to
     * one or more [Composer]s. This count is unaffected if the composer awakes and recomposed but
     * composition did not produce changes to apply.
     */
    val changeCount: Long
}

/**
 * The scheduler for performing recomposition and applying updates to one or more [Composition]s.
 */
// RedundantVisibilityModifier suppressed because metalava picks up internal function overrides
// if 'internal' is not explicitly specified - b/171342041
// NotCloseable suppressed because this is Kotlin-only common code; [Auto]Closeable not available.
@Suppress("RedundantVisibilityModifier", "NotCloseable")
@OptIn(InternalComposeApi::class)
class Recomposer(
    effectCoroutineContext: CoroutineContext
) : CompositionContext() {
    /**
     * This is a running count of the number of times the recomposer awoke and applied changes to
     * one or more composers. This count is unaffected if the composer awakes and recomposed but
     * composition did not produce changes to apply.
     */
    var changeCount = 0L
        private set

    private val broadcastFrameClock = BroadcastFrameClock {
        synchronized(stateLock) {
            deriveStateLocked().also {
                if (_state.value <= State.ShuttingDown) throw CancellationException(
                    "Recomposer shutdown; frame clock awaiter will never resume",
                    closeCause
                )
            }
        }?.resume(Unit)
    }

    /**
     * A [Job] used as a parent of any effects created by this [Recomposer]'s compositions.
     * Its cleanup is used to advance to [State.ShuttingDown] or [State.ShutDown].
     */
    private val effectJob = Job(effectCoroutineContext[Job]).apply {
        invokeOnCompletion { throwable ->
            // Since the running recompose job is operating in a disjoint job if present,
            // kick it out and make sure no new ones start if we have one.
            val cancellation = CancellationException("Recomposer effect job completed", throwable)

            var continuationToResume: CancellableContinuation<Unit>? = null
            synchronized(stateLock) {
                val runnerJob = runnerJob
                if (runnerJob != null) {
                    _state.value = State.ShuttingDown
                    // If the recomposer is closed we will let the runnerJob return from
                    // runRecomposeAndApplyChanges normally and consider ourselves shut down
                    // immediately.
                    if (!isClosed) {
                        // This is the job hosting frameContinuation; no need to resume it otherwise
                        runnerJob.cancel(cancellation)
                    } else if (workContinuation != null) {
                        continuationToResume = workContinuation
                    }
                    workContinuation = null
                    runnerJob.invokeOnCompletion { runnerJobCause ->
                        synchronized(stateLock) {
                            closeCause = throwable?.apply {
                                runnerJobCause
                                    ?.takeIf { it !is CancellationException }
                                    ?.let { addSuppressed(it) }
                            }
                            _state.value = State.ShutDown
                        }
                    }
                } else {
                    closeCause = cancellation
                    _state.value = State.ShutDown
                }
            }
            continuationToResume?.resume(Unit)
        }
    }

    /**
     * The [effectCoroutineContext] is derived from the parameter of the same name.
     */
    internal override val effectCoroutineContext: CoroutineContext =
        effectCoroutineContext + broadcastFrameClock + effectJob

    internal override val recomposeCoroutineContext: CoroutineContext
        get() = EmptyCoroutineContext

    /**
     * Valid operational states of a [Recomposer].
     */
    enum class State {
        /**
         * [cancel] was called on the [Recomposer] and all cleanup work has completed.
         * The [Recomposer] is no longer available for use.
         */
        ShutDown,

        /**
         * [cancel] was called on the [Recomposer] and it is no longer available for use.
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

    private val stateLock = Any()

    // Begin properties guarded by stateLock
    private var runnerJob: Job? = null
    private var closeCause: Throwable? = null
    private val knownCompositions = mutableListOf<ControlledComposition>()
    private val snapshotInvalidations = mutableListOf<Set<Any>>()
    private val compositionInvalidations = mutableListOf<ControlledComposition>()
    private val compositionsAwaitingApply = mutableListOf<ControlledComposition>()
    private val compositionValuesAwaitingInsert = mutableListOf<MovableContentStateReference>()
    private val compositionValuesRemoved =
        mutableMapOf<MovableContent<Any?>, MutableList<MovableContentStateReference>>()
    private val compositionValueStatesAvailable =
        mutableMapOf<MovableContentStateReference, MovableContentState>()
    private var workContinuation: CancellableContinuation<Unit>? = null
    private var concurrentCompositionsOutstanding = 0
    private var isClosed: Boolean = false
    // End properties guarded by stateLock

    private val _state = MutableStateFlow(State.Inactive)

    /**
     * Determine the new value of [_state]. Call only while locked on [stateLock].
     * If it returns a continuation, that continuation should be resumed after releasing the lock.
     */
    private fun deriveStateLocked(): CancellableContinuation<Unit>? {
        if (_state.value <= State.ShuttingDown) {
            knownCompositions.clear()
            snapshotInvalidations.clear()
            compositionInvalidations.clear()
            compositionsAwaitingApply.clear()
            compositionValuesAwaitingInsert.clear()
            workContinuation?.cancel()
            workContinuation = null
            return null
        }

        val newState = when {
            runnerJob == null -> {
                snapshotInvalidations.clear()
                compositionInvalidations.clear()
                if (broadcastFrameClock.hasAwaiters) State.InactivePendingWork else State.Inactive
            }
            compositionInvalidations.isNotEmpty() ||
                snapshotInvalidations.isNotEmpty() ||
                compositionsAwaitingApply.isNotEmpty() ||
                compositionValuesAwaitingInsert.isNotEmpty() ||
                concurrentCompositionsOutstanding > 0 ||
                broadcastFrameClock.hasAwaiters -> State.PendingWork
            else -> State.Idle
        }

        _state.value = newState
        return if (newState == State.PendingWork) {
            workContinuation.also {
                workContinuation = null
            }
        } else null
    }

    /**
     * `true` if there is still work to do for an active caller of [runRecomposeAndApplyChanges]
     */
    private val shouldKeepRecomposing: Boolean
        get() = synchronized(stateLock) { !isClosed } ||
            effectJob.children.any { it.isActive }

    /**
     * The current [State] of this [Recomposer]. See each [State] value for its meaning.
     */
    @Deprecated("Replaced by currentState as a StateFlow", ReplaceWith("currentState"))
    public val state: Flow<State>
        get() = currentState

    /**
     * The current [State] of this [Recomposer], available synchronously.
     */
    public val currentState: StateFlow<State>
        get() = _state

    // A separate private object to avoid the temptation of casting a RecomposerInfo
    // to a Recomposer if Recomposer itself were to implement RecomposerInfo.
    private inner class RecomposerInfoImpl : RecomposerInfo {
        override val state: Flow<State>
            get() = this@Recomposer.currentState
        override val hasPendingWork: Boolean
            get() = this@Recomposer.hasPendingWork
        override val changeCount: Long
            get() = this@Recomposer.changeCount
        fun invalidateGroupsWithKey(key: Int) {
            val compositions: List<ControlledComposition> = synchronized(stateLock) {
                knownCompositions.toMutableList()
            }
            compositions
                .fastMapNotNull { it as? CompositionImpl }
                .fastForEach { it.invalidateGroupsWithKey(key) }
        }
        fun saveStateAndDisposeForHotReload(): List<HotReloadable> {
            val compositions: List<ControlledComposition> = synchronized(stateLock) {
                knownCompositions.toMutableList()
            }
            return compositions
                .fastMapNotNull { it as? CompositionImpl }
                .fastMap { HotReloadable(it).apply { clearContent() } }
        }
    }

    private class HotReloadable(
        private val composition: CompositionImpl
    ) {
        private var composable: @Composable () -> Unit = composition.composable
        fun clearContent() {
            if (composition.isRoot) {
                composition.setContent { }
            }
        }

        fun resetContent() {
            composition.composable = composable
        }

        fun recompose() {
            if (composition.isRoot) {
                composition.setContent(composable)
            }
        }
    }

    private val recomposerInfo = RecomposerInfoImpl()

    /**
     * Obtain a read-only [RecomposerInfo] for this [Recomposer].
     */
    fun asRecomposerInfo(): RecomposerInfo = recomposerInfo

    private fun recordComposerModificationsLocked() {
        if (snapshotInvalidations.isNotEmpty()) {
            snapshotInvalidations.fastForEach { changes ->
                knownCompositions.fastForEach { composition ->
                    composition.recordModificationsOf(changes)
                }
            }
            snapshotInvalidations.clear()
            if (deriveStateLocked() != null) {
                error("called outside of runRecomposeAndApplyChanges")
            }
        }
    }

    private inline fun recordComposerModificationsLocked(
        onEachInvalidComposition: (ControlledComposition) -> Unit
    ) {
        if (snapshotInvalidations.isNotEmpty()) {
            snapshotInvalidations.fastForEach { changes ->
                knownCompositions.fastForEach { composition ->
                    composition.recordModificationsOf(changes)
                }
            }
            snapshotInvalidations.clear()
        }
        compositionInvalidations.fastForEach(onEachInvalidComposition)
        compositionInvalidations.clear()
        if (deriveStateLocked() != null) {
            error("called outside of runRecomposeAndApplyChanges")
        }
    }

    private fun registerRunnerJob(callingJob: Job) {
        synchronized(stateLock) {
            closeCause?.let { throw it }
            if (_state.value <= State.ShuttingDown) error("Recomposer shut down")
            if (runnerJob != null) error("Recomposer already running")
            runnerJob = callingJob
            deriveStateLocked()
        }
    }

    /**
     * Await the invalidation of any associated [Composer]s, recompose them, and apply their
     * changes to their associated [Composition]s if recomposition is successful.
     *
     * While [runRecomposeAndApplyChanges] is running, [awaitIdle] will suspend until there are no
     * more invalid composers awaiting recomposition.
     *
     * This method will not return unless the [Recomposer] is [close]d and all effects in managed
     * compositions complete.
     * Unhandled failure exceptions from child coroutines will be thrown by this method.
     */
    suspend fun runRecomposeAndApplyChanges() = recompositionRunner { parentFrameClock ->
        val toRecompose = mutableListOf<ControlledComposition>()
        val toInsert = mutableListOf<MovableContentStateReference>()
        val toApply = mutableListOf<ControlledComposition>()
        val toLateApply = mutableSetOf<ControlledComposition>()
        val toComplete = mutableSetOf<ControlledComposition>()

        fun fillToInsert() {
            toInsert.clear()
            synchronized(stateLock) {
                compositionValuesAwaitingInsert.fastForEach { toInsert += it }
                compositionValuesAwaitingInsert.clear()
            }
        }

        while (shouldKeepRecomposing) {
            awaitWorkAvailable()

            // Don't await a new frame if we don't have frame-scoped work
            if (
                synchronized(stateLock) {
                    if (!hasFrameWorkLocked) {
                        recordComposerModificationsLocked()
                        !hasFrameWorkLocked
                    } else false
                }
            ) continue

            // Align work with the next frame to coalesce changes.
            // Note: it is possible to resume from the above with no recompositions pending,
            // instead someone might be awaiting our frame clock dispatch below.
            // We use the cached frame clock from above not just so that we don't locate it
            // each time, but because we've installed the broadcastFrameClock as the scope
            // clock above for user code to locate.
            parentFrameClock.withFrameNanos { frameTime ->
                // Dispatch MonotonicFrameClock frames first; this may produce new
                // composer invalidations that we must handle during the same frame.
                if (broadcastFrameClock.hasAwaiters) {
                    trace("Recomposer:animation") {
                        // Propagate the frame time to anyone who is awaiting from the
                        // recomposer clock.
                        broadcastFrameClock.sendFrame(frameTime)

                        // Ensure any global changes are observed
                        Snapshot.sendApplyNotifications()
                    }
                }

                trace("Recomposer:recompose") {
                    // Drain any composer invalidations from snapshot changes and record
                    // composers to work on
                    synchronized(stateLock) {
                        recordComposerModificationsLocked()

                        compositionInvalidations.fastForEach { toRecompose += it }
                        compositionInvalidations.clear()
                    }

                    // Perform recomposition for any invalidated composers
                    val modifiedValues = IdentityArraySet<Any>()
                    val alreadyComposed = IdentityArraySet<ControlledComposition>()
                    while (toRecompose.isNotEmpty() || toInsert.isNotEmpty()) {
                        try {
                            toRecompose.fastForEach { composition ->
                                alreadyComposed.add(composition)
                                performRecompose(composition, modifiedValues)?.let {
                                    toApply += it
                                }
                            }
                        } finally {
                            toRecompose.clear()
                        }

                        // Find any trailing recompositions that need to be composed because
                        // of a value change by a composition. This can happen, for example, if
                        // a CompositionLocal changes in a parent and was read in a child
                        // composition that was otherwise valid.
                        if (modifiedValues.isNotEmpty()) {
                            synchronized(stateLock) {
                                knownCompositions.fastForEach { value ->
                                    if (
                                        value !in alreadyComposed &&
                                        value.observesAnyOf(modifiedValues)
                                    ) {
                                        toRecompose += value
                                    }
                                }
                            }
                        }

                        if (toRecompose.isEmpty()) {
                            fillToInsert()
                            while (toInsert.isNotEmpty()) {
                                toLateApply += performInsertValues(toInsert, modifiedValues)
                                fillToInsert()
                            }
                        }
                    }

                    if (toApply.isNotEmpty()) {
                        changeCount++

                        // Perform apply changes
                        try {
                            toComplete += toApply
                            toApply.fastForEach { composition ->
                                composition.applyChanges()
                            }
                        } finally {
                            toApply.clear()
                        }
                    }

                    if (toLateApply.isNotEmpty()) {
                        try {
                            toComplete += toLateApply
                            toLateApply.forEach { composition ->
                                composition.applyLateChanges()
                            }
                        } finally {
                            toLateApply.clear()
                        }
                    }

                    if (toComplete.isNotEmpty()) {
                        try {
                            toComplete.forEach { composition ->
                                composition.changesApplied()
                            }
                        } finally {
                            toComplete.clear()
                        }
                    }

                    discardUnusedValues()

                    synchronized(stateLock) {
                        deriveStateLocked()
                    }
                }
            }
        }
    }

    /**
     * Await the invalidation of any associated [Composer]s, recompose them, and apply their
     * changes to their associated [Composition]s if recomposition is successful.
     *
     * While [runRecomposeConcurrentlyAndApplyChanges] is running, [awaitIdle] will suspend until
     * there are no more invalid composers awaiting recomposition.
     *
     * Recomposition of invalidated composers will occur in [recomposeCoroutineContext].
     * [recomposeCoroutineContext] must not contain a [Job].
     *
     * This method will not return unless the [Recomposer] is [close]d and all effects in managed
     * compositions complete.
     * Unhandled failure exceptions from child coroutines will be thrown by this method.
     */
    @ExperimentalComposeApi
    suspend fun runRecomposeConcurrentlyAndApplyChanges(
        recomposeCoroutineContext: CoroutineContext
    ) = recompositionRunner { parentFrameClock ->
        require(recomposeCoroutineContext[Job] == null) {
            "recomposeCoroutineContext may not contain a Job; found " +
                recomposeCoroutineContext[Job]
        }
        val recomposeCoroutineScope = CoroutineScope(
            coroutineContext + recomposeCoroutineContext + Job(coroutineContext.job)
        )
        val frameSignal = ProduceFrameSignal()
        val frameLoop = launch { runFrameLoop(parentFrameClock, frameSignal) }
        while (shouldKeepRecomposing) {
            awaitWorkAvailable()

            // Don't await a new frame if we don't have frame-scoped work
            synchronized(stateLock) {
                recordComposerModificationsLocked { composition ->
                    concurrentCompositionsOutstanding++
                    recomposeCoroutineScope.launch(composition.recomposeCoroutineContext) {
                        val changedComposition = performRecompose(composition, null)
                        synchronized(stateLock) {
                            changedComposition?.let { compositionsAwaitingApply += it }
                            concurrentCompositionsOutstanding--
                            deriveStateLocked()
                        }?.resume(Unit)
                    }
                }
                if (hasConcurrentFrameWorkLocked) {
                    frameSignal.requestFrameLocked()
                } else null
            }?.resume(Unit)
        }
        recomposeCoroutineScope.coroutineContext.job.cancelAndJoin()
        frameLoop.cancelAndJoin()
    }

    private suspend fun runFrameLoop(
        parentFrameClock: MonotonicFrameClock,
        frameSignal: ProduceFrameSignal
    ) {
        val toRecompose = mutableListOf<ControlledComposition>()
        val toApply = mutableListOf<ControlledComposition>()
        while (true) {
            frameSignal.awaitFrameRequest(stateLock)
            // Align applying changes to the frame.
            // Note: it is possible to resume from the above with no recompositions pending,
            // instead someone might be awaiting our frame clock dispatch below.
            // We use the cached frame clock from above not just so that we don't locate it
            // each time, but because we've installed the broadcastFrameClock as the scope
            // clock above for user code to locate.
            parentFrameClock.withFrameNanos { frameTime ->
                // Dispatch MonotonicFrameClock frames first; this may produce new
                // composer invalidations that we must handle during the same frame.
                if (broadcastFrameClock.hasAwaiters) {
                    trace("Recomposer:animation") {
                        // Propagate the frame time to anyone who is awaiting from the
                        // recomposer clock.
                        broadcastFrameClock.sendFrame(frameTime)

                        // Ensure any global changes are observed
                        Snapshot.sendApplyNotifications()
                    }
                }

                trace("Recomposer:recompose") {
                    // Drain any composer invalidations from snapshot changes and record
                    // composers to work on.
                    // We'll do these synchronously to make the current frame.
                    synchronized(stateLock) {
                        recordComposerModificationsLocked()

                        compositionsAwaitingApply.fastForEach { toApply += it }
                        compositionsAwaitingApply.clear()
                        compositionInvalidations.fastForEach { toRecompose += it }
                        compositionInvalidations.clear()
                        frameSignal.takeFrameRequestLocked()
                    }

                    // Perform recomposition for any invalidated composers
                    val modifiedValues = IdentityArraySet<Any>()
                    try {
                        toRecompose.fastForEach { composer ->
                            performRecompose(composer, modifiedValues)?.let {
                                toApply += it
                            }
                        }
                    } finally {
                        toRecompose.clear()
                    }

                    // Perform any value inserts

                    if (toApply.isNotEmpty()) changeCount++

                    // Perform apply changes
                    try {
                        toApply.fastForEach { composition ->
                            composition.applyChanges()
                        }
                    } finally {
                        toApply.clear()
                    }

                    synchronized(stateLock) {
                        deriveStateLocked()
                    }
                }
            }
        }
    }

    private val hasSchedulingWork: Boolean
        get() = synchronized(stateLock) {
            snapshotInvalidations.isNotEmpty() ||
                compositionInvalidations.isNotEmpty() ||
                broadcastFrameClock.hasAwaiters
        }

    private suspend fun awaitWorkAvailable() {
        if (!hasSchedulingWork) {
            suspendCancellableCoroutine<Unit> { co ->
                synchronized(stateLock) {
                    if (hasSchedulingWork) {
                        co.resume(Unit)
                    } else {
                        workContinuation = co
                    }
                }
            }
        }
    }

    @OptIn(ExperimentalComposeApi::class)
    private suspend fun recompositionRunner(
        block: suspend CoroutineScope.(parentFrameClock: MonotonicFrameClock) -> Unit
    ) {
        val parentFrameClock = coroutineContext.monotonicFrameClock
        withContext(broadcastFrameClock) {
            // Enforce mutual exclusion of callers; register self as current runner
            val callingJob = coroutineContext.job
            registerRunnerJob(callingJob)

            // Observe snapshot changes and propagate them to known composers only from
            // this caller's dispatcher, never working with the same composer in parallel.
            // unregisterApplyObserver is called as part of the big finally below
            val unregisterApplyObserver = Snapshot.registerApplyObserver { changed, _ ->
                synchronized(stateLock) {
                    if (_state.value >= State.Idle) {
                        snapshotInvalidations += changed
                        deriveStateLocked()
                    } else null
                }?.resume(Unit)
            }

            addRunning(recomposerInfo)

            try {
                // Invalidate all registered composers when we start since we weren't observing
                // snapshot changes on their behalf. Assume anything could have changed.
                synchronized(stateLock) {
                    knownCompositions.fastForEach { it.invalidateAll() }
                    // Don't need to deriveStateLocked here; invalidate will do it if needed.
                }

                coroutineScope {
                    block(parentFrameClock)
                }
            } finally {
                unregisterApplyObserver.dispose()
                synchronized(stateLock) {
                    if (runnerJob === callingJob) {
                        runnerJob = null
                    }
                    deriveStateLocked()
                }
                removeRunning(recomposerInfo)
            }
        }
    }

    /**
     * Permanently shut down this [Recomposer] for future use. [currentState] will immediately
     * reflect [State.ShuttingDown] (or a lower state) before this call returns.
     * All ongoing recompositions will stop, new composer invalidations with this [Recomposer] at
     * the root will no longer occur, and any [LaunchedEffect]s currently running in compositions
     * managed by this [Recomposer] will be cancelled. Any [rememberCoroutineScope] scopes from
     * compositions managed by this [Recomposer] will also be cancelled. See [join] to await the
     * completion of all of these outstanding tasks.
     */
    fun cancel() {
        // Move to State.ShuttingDown immediately rather than waiting for effectJob to join
        // if we're cancelling to shut down the Recomposer. This permits other client code
        // to use `state.first { it < State.Idle }` or similar to reliably and immediately detect
        // that the recomposer can no longer be used.
        // It looks like a CAS loop would be more appropriate here, but other occurrences
        // of taking stateLock assume that the state cannot change without holding it.
        synchronized(stateLock) {
            if (_state.value >= State.Idle) {
                _state.value = State.ShuttingDown
            }
        }
        effectJob.cancel()
    }

    /**
     * Close this [Recomposer]. Once all effects launched by managed compositions complete,
     * any active call to [runRecomposeAndApplyChanges] will return normally and this [Recomposer]
     * will be [State.ShutDown]. See [join] to await the completion of all of these outstanding
     * tasks.
     */
    fun close() {
        if (effectJob.complete()) {
            synchronized(stateLock) {
                isClosed = true
            }
        }
    }

    /**
     * Await the completion of a [cancel] operation.
     */
    suspend fun join() {
        currentState.first { it == State.ShutDown }
    }

    internal override fun composeInitial(
        composition: ControlledComposition,
        content: @Composable () -> Unit
    ) {
        val composerWasComposing = composition.isComposing
        composing(composition, null) {
            composition.composeContent(content)
        }
        // TODO(b/143755743)
        if (!composerWasComposing) {
            Snapshot.notifyObjectsInitialized()
        }

        synchronized(stateLock) {
            if (_state.value > State.ShuttingDown) {
                if (composition !in knownCompositions) {
                    knownCompositions += composition
                }
            }
        }

        performInitialMovableContentInserts(composition)
        composition.applyChanges()
        composition.applyLateChanges()

        if (!composerWasComposing) {
            // Ensure that any state objects created during applyChanges are seen as changed
            // if modified after this call.
            Snapshot.notifyObjectsInitialized()
        }
    }

    private fun performInitialMovableContentInserts(composition: ControlledComposition) {
        synchronized(stateLock) {
            if (!compositionValuesAwaitingInsert.fastAny { it.composition == composition }) return
        }
        val toInsert = mutableListOf<MovableContentStateReference>()
        fun fillToInsert() {
            toInsert.clear()
            synchronized(stateLock) {
                val iterator = compositionValuesAwaitingInsert.iterator()
                while (iterator.hasNext()) {
                    val value = iterator.next()
                    if (value.composition == composition) {
                        toInsert.add(value)
                        iterator.remove()
                    }
                }
            }
        }
        fillToInsert()
        while (toInsert.isNotEmpty()) {
            performInsertValues(toInsert, null)
            fillToInsert()
        }
    }

    private fun performRecompose(
        composition: ControlledComposition,
        modifiedValues: IdentityArraySet<Any>?
    ): ControlledComposition? {
        if (composition.isComposing || composition.isDisposed) return null
        return if (
            composing(composition, modifiedValues) {
                if (modifiedValues?.isNotEmpty() == true) {
                    // Record write performed by a previous composition as if they happened during
                    // composition.
                    composition.prepareCompose {
                        modifiedValues.forEach { composition.recordWriteOf(it) }
                    }
                }
                composition.recompose()
            }
        ) composition else null
    }

    private fun performInsertValues(
        references: List<MovableContentStateReference>,
        modifiedValues: IdentityArraySet<Any>?
    ): List<ControlledComposition> {
        val tasks = references.fastGroupBy { it.composition }
        for ((composition, refs) in tasks) {
            runtimeCheck(!composition.isComposing)
            composing(composition, modifiedValues) {
                // Map insert movable content to movable content states that have been released
                // during `performRecompose`.
                // during `performRecompose`.
                val pairs = synchronized(stateLock) {
                    refs.fastMap { reference ->
                        reference to
                            compositionValuesRemoved.removeLastMultiValue(reference.content)
                    }
                }
                composition.insertMovableContent(pairs)
            }
        }
        return tasks.keys.toList()
    }

    private fun discardUnusedValues() {
        val unusedValues = synchronized(stateLock) {
            if (compositionValuesRemoved.isNotEmpty()) {
                val references = compositionValuesRemoved.values.flatten()
                compositionValuesRemoved.clear()
                val unusedValues = references.fastMap {
                    it to compositionValueStatesAvailable[it]
                }
                compositionValueStatesAvailable.clear()
                unusedValues
            } else emptyList()
        }
        unusedValues.fastForEach { (reference, state) ->
            if (state != null) {
                reference.composition.disposeUnusedMovableContent(state)
            }
        }
    }

    private fun readObserverOf(composition: ControlledComposition): (Any) -> Unit {
        return { value -> composition.recordReadOf(value) }
    }

    private fun writeObserverOf(
        composition: ControlledComposition,
        modifiedValues: IdentityArraySet<Any>?
    ): (Any) -> Unit {
        return { value ->
            composition.recordWriteOf(value)
            modifiedValues?.add(value)
        }
    }

    private inline fun <T> composing(
        composition: ControlledComposition,
        modifiedValues: IdentityArraySet<Any>?,
        block: () -> T
    ): T {
        val snapshot = Snapshot.takeMutableSnapshot(
            readObserverOf(composition), writeObserverOf(composition, modifiedValues)
        )
        try {
            return snapshot.enter(block)
        } finally {
            applyAndCheck(snapshot)
        }
    }

    private fun applyAndCheck(snapshot: MutableSnapshot) {
        try {
            val applyResult = snapshot.apply()
            if (applyResult is SnapshotApplyResult.Failure) {
                error(
                    "Unsupported concurrent change during composition. A state object was " +
                        "modified by composition as well as being modified outside composition."
                )
                // TODO(chuckj): Consider lifting this restriction by forcing a recompose
            }
        } finally {
            snapshot.dispose()
        }
    }

    /**
     * `true` if this [Recomposer] has any pending work scheduled, regardless of whether or not
     * it is currently [running][runRecomposeAndApplyChanges].
     */
    val hasPendingWork: Boolean
        get() = synchronized(stateLock) {
            snapshotInvalidations.isNotEmpty() ||
                compositionInvalidations.isNotEmpty() ||
                concurrentCompositionsOutstanding > 0 ||
                compositionsAwaitingApply.isNotEmpty() ||
                broadcastFrameClock.hasAwaiters
        }

    private val hasFrameWorkLocked: Boolean
        get() = compositionInvalidations.isNotEmpty() || broadcastFrameClock.hasAwaiters

    private val hasConcurrentFrameWorkLocked: Boolean
        get() = compositionsAwaitingApply.isNotEmpty() || broadcastFrameClock.hasAwaiters

    /**
     * Suspends until the currently pending recomposition frame is complete.
     * Any recomposition for this recomposer triggered by actions before this call begins
     * will be complete and applied (if recomposition was successful) when this call returns.
     *
     * If [runRecomposeAndApplyChanges] is not currently running the [Recomposer] is considered idle
     * and this method will not suspend.
     */
    suspend fun awaitIdle() {
        currentState.takeWhile { it > State.Idle }.collect()
    }

    // Recomposer always starts with a constant compound hash
    internal override val compoundHashKey: Int
        get() = RecomposerCompoundHashKey

    // Collecting parameter happens at the level of a composer; starts as false
    internal override val collectingParameterInformation: Boolean
        get() = false

    internal override fun recordInspectionTable(table: MutableSet<CompositionData>) {
        // TODO: The root recomposer might be a better place to set up inspection
        // than the current configuration with an CompositionLocal
    }

    internal override fun registerComposition(composition: ControlledComposition) {
        // Do nothing.
    }

    internal override fun unregisterComposition(composition: ControlledComposition) {
        synchronized(stateLock) {
            knownCompositions -= composition
            compositionInvalidations -= composition
            compositionsAwaitingApply -= composition
        }
    }

    internal override fun invalidate(composition: ControlledComposition) {
        synchronized(stateLock) {
            if (composition !in compositionInvalidations) {
                compositionInvalidations += composition
                deriveStateLocked()
            } else null
        }?.resume(Unit)
    }

    internal override fun invalidateScope(scope: RecomposeScopeImpl) {
        synchronized(stateLock) {
            snapshotInvalidations += setOf(scope)
            deriveStateLocked()
        }?.resume(Unit)
    }

    internal override fun insertMovableContent(reference: MovableContentStateReference) {
        synchronized(stateLock) {
            compositionValuesAwaitingInsert += reference
            deriveStateLocked()
        }?.resume(Unit)
    }

    internal override fun deletedMovableContent(reference: MovableContentStateReference) {
        synchronized(stateLock) {
            compositionValuesRemoved.addMultiValue(reference.content, reference)
        }
    }

    internal override fun movableContentStateReleased(
        reference: MovableContentStateReference,
        data: MovableContentState
    ) {
        synchronized(stateLock) {
            compositionValueStatesAvailable[reference] = data
        }
    }

    override fun movableContentStateResolve(
        reference: MovableContentStateReference
    ): MovableContentState? =
        synchronized(stateLock) {
            compositionValueStatesAvailable.remove(reference)
        }

    /**
     * hack: the companion object is thread local in Kotlin/Native to avoid freezing
     * [_runningRecomposers] with the current memory model. As a side effect,
     * recomposers are now forced to be single threaded in Kotlin/Native targets.
     *
     * This annotation WILL BE REMOVED with the new memory model of Kotlin/Native.
     */
    @ThreadLocal
    companion object {

        private val _runningRecomposers = MutableStateFlow(persistentSetOf<RecomposerInfoImpl>())

        /**
         * An observable [Set] of [RecomposerInfo]s for currently
         * [running][runRecomposeAndApplyChanges] [Recomposer]s.
         * Emitted sets are immutable.
         */
        val runningRecomposers: StateFlow<Set<RecomposerInfo>>
            get() = _runningRecomposers

        private fun addRunning(info: RecomposerInfoImpl) {
            while (true) {
                val old = _runningRecomposers.value
                val new = old.add(info)
                if (old === new || _runningRecomposers.compareAndSet(old, new)) break
            }
        }

        private fun removeRunning(info: RecomposerInfoImpl) {
            while (true) {
                val old = _runningRecomposers.value
                val new = old.remove(info)
                if (old === new || _runningRecomposers.compareAndSet(old, new)) break
            }
        }

        internal fun saveStateAndDisposeForHotReload(): Any {
            // NOTE: when we move composition/recomposition onto multiple threads, we will want
            // to ensure that we pause recompositions before this call.
            return _runningRecomposers.value.flatMap { it.saveStateAndDisposeForHotReload() }
        }

        internal fun loadStateAndComposeForHotReload(token: Any) {
            // NOTE: when we move composition/recomposition onto multiple threads, we will want
            // to ensure that we pause recompositions before this call.
            @Suppress("UNCHECKED_CAST")
            val holders = token as List<HotReloadable>
            holders.fastForEach { it.resetContent() }
            holders.fastForEach { it.recompose() }
        }

        internal fun invalidateGroupsWithKey(key: Int) {
            _runningRecomposers.value.forEach {
                it.invalidateGroupsWithKey(key)
            }
        }
    }
}

/**
 * Sentinel used by [ProduceFrameSignal]
 */
private val ProduceAnotherFrame = Any()
private val FramePending = Any()

/**
 * Multiple producer, single consumer conflated signal that tells concurrent composition when it
 * should try to produce another frame. This class is intended to be used along with a lock shared
 * between producers and consumer.
 */
private class ProduceFrameSignal {
    private var pendingFrameContinuation: Any? = null

    /**
     * Suspend until a frame is requested. After this method returns the signal is in a
     * [FramePending] state which must be acknowledged by a call to [takeFrameRequestLocked]
     * once all data that will be used to produce the frame has been claimed.
     */
    suspend fun awaitFrameRequest(lock: Any) {
        synchronized(lock) {
            if (pendingFrameContinuation === ProduceAnotherFrame) {
                pendingFrameContinuation = FramePending
                return
            }
        }
        suspendCancellableCoroutine<Unit> { co ->
            synchronized(lock) {
                if (pendingFrameContinuation === ProduceAnotherFrame) {
                    pendingFrameContinuation = FramePending
                    co
                } else {
                    pendingFrameContinuation = co
                    null
                }
            }?.resume(Unit)
        }
    }

    /**
     * Signal from the frame request consumer that the frame is beginning with data that was
     * available up until this point. (Synchronizing access to that data is up to the caller.)
     */
    fun takeFrameRequestLocked() {
        check(pendingFrameContinuation === FramePending) { "frame not pending" }
        pendingFrameContinuation = null
    }

    fun requestFrameLocked(): Continuation<Unit>? = when (val co = pendingFrameContinuation) {
        is Continuation<*> -> {
            pendingFrameContinuation = FramePending
            @Suppress("UNCHECKED_CAST")
            co as Continuation<Unit>
        }
        ProduceAnotherFrame, FramePending -> null
        null -> {
            pendingFrameContinuation = ProduceAnotherFrame
            null
        }
        else -> error("invalid pendingFrameContinuation $co")
    }
}

// Allow treating a mutable map of shape MutableMap<K, MutableMap<V>> as a multi-value map
internal fun <K, V> MutableMap<K, MutableList<V>>.addMultiValue(key: K, value: V) =
    getOrPut(key) { mutableListOf() }.add(value)

internal fun <K, V> MutableMap<K, MutableList<V>>.removeLastMultiValue(key: K): V? =
    get(key)?.let { list ->
        list.removeFirst().also {
            if (list.isEmpty())
                remove(key)
        }
    }