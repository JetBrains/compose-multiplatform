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

@file:OptIn(InternalComposeApi::class)

package androidx.compose.runtime.snapshots

import androidx.compose.runtime.AtomicReference
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisallowComposableCalls
import androidx.compose.runtime.ExperimentalComposeApi
import androidx.compose.runtime.InternalComposeApi
import androidx.compose.runtime.SnapshotThreadLocal
import androidx.compose.runtime.synchronized
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

/**
 * A snapshot of the values return by mutable states and other state objects. All state object
 * will have the same value in the snapshot as they had when the snapshot was created unless they
 * are explicitly changed in the snapshot.
 *
 * To enter a snapshot call [enter]. The snapshot is the current snapshot as returned by
 * [currentSnapshot] until the control returns from the lambda (or until a nested [enter] is
 * called). All state objects will return the values associated with this snapshot, locally in the
 * thread, until [enter] returns. All other threads are unaffected.
 *
 * Snapshots can be nested by calling [takeNestedSnapshot].
 *
 * @see takeSnapshot
 * @see takeMutableSnapshot
 * @see androidx.compose.runtime.mutableStateOf
 * @see androidx.compose.runtime.mutableStateListOf
 * @see androidx.compose.runtime.mutableStateMapOf
 */
sealed class Snapshot(
    id: Int,

    /**
     * A set of all the snapshots that should be treated as invalid.
     */
    internal open var invalid: SnapshotIdSet
) {
    /**
     * The snapshot id of the snapshot. This is a unique number from a monotonically increasing
     * value for each snapshot taken.
     */
    open var id: Int = id
        internal set

    /**
     * The root snapshot for this snapshot. For non-nested snapshots this is always `this`. For
     * nested snapshot it is the parent's [root].
     */
    abstract val root: Snapshot

    /**
     * True if any change to a state object in this snapshot will throw.
     */
    abstract val readOnly: Boolean

    /**
     * Dispose the snapshot. Neglecting to dispose a snapshot will result in difficult to
     * diagnose memory leaks as it indirectly causes all state objects to maintain its value for
     * the un-disposed snapshot.
     */
    open fun dispose() {
        disposed = true
        sync {
            releasePinnedSnapshotLocked()
        }
    }

    /**
     * Take a snapshot of the state values in this snapshot. The resulting [Snapshot] is read-only.
     * All nested snapshots need to be disposed by calling [dispose] before resources associated
     * with this snapshot can be collected. Nested snapshots are still valid after the parent has
     * been disposed.
     */
    abstract fun takeNestedSnapshot(readObserver: ((Any) -> Unit)? = null): Snapshot

    /**
     * Whether there are any pending changes in this snapshot. These changes are not visible
     * until the snapshot is applied.
     */
    abstract fun hasPendingChanges(): Boolean

    /**
     * Enter the snapshot. In [block] all state objects have the value associated with this
     * snapshot. The value of [currentSnapshot] will be this snapshot until this [block] returns
     * or a nested call to [enter] is called. When [block] returns, the previous current snapshot
     * is restored if there was one.
     *
     * All changes to state objects inside [block] are isolated to this snapshot and are not
     * visible to other snapshot or as global state. If this is a [readOnly] snapshot, any
     * changes to state objects will throw an [IllegalStateException].
     *
     * For a [MutableSnapshot], changes made to a snapshot inside [block] can be applied
     * atomically to the global state (or to its parent snapshot if it is a nested snapshot) by
     * calling [MutableSnapshot.apply].
     *
     * @see androidx.compose.runtime.mutableStateOf
     * @see androidx.compose.runtime.mutableStateListOf
     * @see androidx.compose.runtime.mutableStateMapOf
     */
    inline fun <T> enter(block: () -> T): T {
        val previous = makeCurrent()
        try {
            return block()
        } finally {
            restoreCurrent(previous)
        }
    }

    @PublishedApi
    internal open fun makeCurrent(): Snapshot? {
        val previous = threadSnapshot.get()
        threadSnapshot.set(this)
        return previous
    }

    @PublishedApi
    internal open fun restoreCurrent(snapshot: Snapshot?) {
        threadSnapshot.set(snapshot)
    }

    /**
     * Enter the snapshot, returning the previous [Snapshot] for leaving this snapshot later
     * using [unsafeLeave]. Prefer [enter] or [asContextElement] instead of using [unsafeEnter]
     * directly to prevent mismatched [unsafeEnter]/[unsafeLeave] calls.
     *
     * After returning all state objects have the value associated with this snapshot.
     * The value of [currentSnapshot] will be this snapshot until [unsafeLeave] is called
     * with the returned [Snapshot] or another call to [unsafeEnter] or [enter]
     * is made.
     *
     * All changes to state objects until another snapshot is entered or this snapshot is left
     * are isolated to this snapshot and are not visible to other snapshot or as global state.
     * If this is a [readOnly] snapshot, any changes to state objects will throw an
     * [IllegalStateException].
     *
     * For a [MutableSnapshot], changes made to a snapshot can be applied
     * atomically to the global state (or to its parent snapshot if it is a nested snapshot) by
     * calling [MutableSnapshot.apply].
     */
    @ExperimentalComposeApi
    fun unsafeEnter(): Snapshot? = makeCurrent()

    /**
     * Leave the snapshot, restoring the [oldSnapshot] before returning.
     * See [unsafeEnter].
     */
    @ExperimentalComposeApi
    fun unsafeLeave(oldSnapshot: Snapshot?) {
        check(threadSnapshot.get() === this) {
            "Cannot leave snapshot; $this is not the current snapshot"
        }
        restoreCurrent(oldSnapshot)
    }

    internal var disposed = false

    /*
     * Handle to use when unpinning this snapshot. -1 if this snapshot has been unpinned.
     */
    private var pinningTrackingHandle =
        if (id != INVALID_SNAPSHOT) trackPinning(id, invalid) else -1

    internal inline val isPinned get() = pinningTrackingHandle >= 0

    /*
     * The read observer for the snapshot if there is one.
     */
    internal abstract val readObserver: ((Any) -> Unit)?

    /**
     * The write observer for the snapshot if there is one.
     */
    internal abstract val writeObserver: ((Any) -> Unit)?

    /**
     * Called when a nested snapshot of this snapshot is activated
     */
    internal abstract fun nestedActivated(snapshot: Snapshot)

    /**
     * Called when a nested snapshot of this snapshot is deactivated
     */
    internal abstract fun nestedDeactivated(snapshot: Snapshot)

    /**
     * Record that state was modified in the snapshot.
     */
    internal abstract fun recordModified(state: StateObject)

    /**
     * The set of state objects that have been modified in this snapshot.
     */
    internal abstract val modified: MutableSet<StateObject>?

    /**
     * Notify the snapshot that all objects created in this snapshot to this point should be
     * considered initialized. If any state object is are modified passed this point it will
     * appear as modified in the snapshot and any applicable snapshot write observer will be
     * called for the object and the object will be part of the a set of mutated objects sent to
     * any applicable snapshot apply observer.
     *
     * Unless [notifyObjectsInitialized] is called, state objects created in a snapshot are not
     * considered modified by the snapshot even if they are modified after construction.
     */
    internal abstract fun notifyObjectsInitialized()

    /**
     * Closes the snapshot by removing the snapshot id (an any previous id's) from the list of
     * open snapshots and unpinning snapshots that no longer are referenced by this snapshot.
     */
    internal fun closeAndReleasePinning() {
        sync {
            closeLocked()
            releasePinnedSnapshotsForCloseLocked()
        }
    }

    /**
     * Closes the snapshot by removing the snapshot id (and any previous ids) from the list of
     * open snapshots. Does not release pinned snapshots. See [releasePinnedSnapshotsForCloseLocked]
     * for the second half of [closeAndReleasePinning].
     *
     * Call while holding a `sync {}` lock.
     */
    internal open fun closeLocked() {
        openSnapshots = openSnapshots.clear(id)
    }

    /**
     * Releases all pinned snapshots required to perform a clean [closeAndReleasePinning].
     *
     * Call while holding a `sync {}` lock.
     *
     * See [closeAndReleasePinning], [closeLocked].
     */
    internal open fun releasePinnedSnapshotsForCloseLocked() {
        releasePinnedSnapshotLocked()
    }

    internal fun validateNotDisposed() {
        require(!disposed) { "Cannot use a disposed snapshot" }
    }

    internal fun releasePinnedSnapshotLocked() {
        if (pinningTrackingHandle >= 0) {
            releasePinningLocked(pinningTrackingHandle)
            pinningTrackingHandle = -1
        }
    }

    internal fun takeoverPinnedSnapshot(): Int =
        pinningTrackingHandle.also { pinningTrackingHandle = -1 }

    companion object {
        /**
         * Return the thread's active snapshot. If no thread snapshot is active then the current
         * global snapshot is used.
         */
        val current get() = currentSnapshot()

        /**
         * Take a snapshot of the current value of all state objects. The values are preserved until
         * [Snapshot.dispose] is called on the result.
         *
         * The [readObserver] parameter can be used to track when all state objects are read when in
         * [Snapshot.enter]. A snapshot apply observer can be registered using
         * [Snapshot.registerApplyObserver] to observe modification of state objects.
         *
         * An active snapshot (after it is created but before [Snapshot.dispose] is called) requires
         * resources to track the values in the snapshot. Once a snapshot is no longer needed it
         * should disposed by calling [Snapshot.dispose].
         *
         * Leaving a snapshot active could cause hard to diagnose memory leaks values as are
         * maintained by state objects for these unneeded snapshots. Take care to always call
         * [Snapshot.dispose] on all snapshots when they are no longer needed.
         *
         * Composition uses both of these to implicitly subscribe to changes to state object and
         * automatically update the composition when state objects read during composition change.
         *
         * A nested snapshot can be taken of a snapshot which is an independent read-only copy of
         * the snapshot and can be disposed independently. This is used by [takeSnapshot] when in
         * a read-only snapshot for API consistency allowing the result of [takeSnapshot] to be
         * disposed leaving the parent snapshot active.
         *
         * @param readObserver called when any state object is read in the lambda passed to
         * [Snapshot.enter] or in the [Snapshot.enter] of any nested snapshot.
         *
         * @see Snapshot
         * @see Snapshot.registerApplyObserver
         */
        fun takeSnapshot(
            readObserver: ((Any) -> Unit)? = null
        ): Snapshot = currentSnapshot().takeNestedSnapshot(readObserver)

        /**
         * Take a snapshot of the current value of all state objects that also allows the state
         * to be changed and later atomically applied when [MutableSnapshot.apply] is called. The
         * values are preserved until [Snapshot.dispose] is called on the result. The global
         * state will either see all the changes made as one atomic change, when [MutableSnapshot
         * .apply] is called, or none of the changes if the mutable state object is disposed
         * before being applied.
         *
         * The values in a snapshot can be modified by calling [Snapshot.enter] and then, in its
         * lambda, modify any state object. The new values of the state objects will only become
         * visible to the global state when [MutableSnapshot.apply] is called.
         *
         * An active snapshot (after it is created but before [Snapshot.dispose] is called) requires
         * resources to track the values in the snapshot. Once a snapshot is no longer needed it
         * should disposed by calling [Snapshot.dispose].
         *
         * Leaving a snapshot active could cause hard to diagnose memory leaks as values are
         * maintained by state objects for these unneeded snapshots. Take care to always call
         * [Snapshot.dispose] on all snapshots when they are no longer needed.
         *
         * A nested snapshot can be taken by calling [Snapshot.takeNestedSnapshot], for a read-only
         * snapshot, or [MutableSnapshot.takeNestedMutableSnapshot] for a snapshot that can be
         * changed. Nested mutable snapshots are applied to the this, the parent snapshot, when
         * their [MutableSnapshot.apply] is called. Their applied changes will be visible to in
         * this snapshot but will not be visible other snapshots (including other nested
         * snapshots) or the global state until this snapshot is applied by calling
         * [MutableSnapshot.apply].
         *
         * Once [MutableSnapshot.apply] is called on this, the parent snapshot, all calls to
         * [MutableSnapshot.apply] on an active nested snapshot will fail.
         *
         * Changes to a mutable snapshot are isolated, using snapshot isolation, from all other
         * snapshots. Their changes are only visible as global state or to new snapshots once
         * [MutableSnapshot.apply] is called.
         *
         * Applying a snapshot can fail if currently visible changes to the state object
         * conflicts with a change made in the snapshot.
         *
         * When in a mutable snapshot, [takeMutableSnapshot] creates a nested snapshot of the
         * current mutable snapshot. If the current snapshot is read-only, an exception is thrown.
         * The current snapshot is the result of calling [currentSnapshot] which is updated by
         * calling [Snapshot.enter] which makes the [Snapshot] the current snapshot while in its
         * lambda.
         *
         * Composition uses mutable snapshots to allow changes made in a [Composable] functions
         * to be temporarily isolated from the global state and is later applied to the global
         * state when the composition is applied. If [MutableSnapshot.apply] fails applying this
         * snapshot, the snapshot and the changes calculated during composition are disposed and
         * a new composition is scheduled to be calculated again.
         *
         * @param readObserver called when any state object is read in the lambda passed to
         * [Snapshot.enter] or in the [Snapshot.enter] of any nested snapshots.
         *
         * Composition, layout and draw use [readObserver] to implicitly subscribe to changes to
         * state objects to know when to update.
         *
         * @param writeObserver called when a state object is created or just before it is
         * written to the first time in the snapshot or a nested mutable snapshot. This might be
         * called several times for the same object if nested mutable snapshots are created.
         *
         * Composition uses [writeObserver] to track when a state object is modified during
         * composition in order to invalidate the reads that have not yet occurred. This allows a
         * single pass of composition for state objects that are written to before they are read
         * (such as modifying the value of a dynamic ambient provider).
         *
         * @see Snapshot.takeSnapshot
         * @see Snapshot
         * @see MutableSnapshot
         */
        fun takeMutableSnapshot(
            readObserver: ((Any) -> Unit)? = null,
            writeObserver: ((Any) -> Unit)? = null
        ): MutableSnapshot =
            (currentSnapshot() as? MutableSnapshot)?.takeNestedMutableSnapshot(
                readObserver,
                writeObserver
            ) ?: error("Cannot create a mutable snapshot of an read-only snapshot")

        /**
         * Escape the current snapshot, if there is one. All state objects will have the value
         * associated with the global while the [block] lambda is executing.
         *
         * @return the result of [block]
         */
        inline fun <T> global(block: () -> T): T {
            val previous = removeCurrent()
            return block().also { restoreCurrent(previous) }
        }

        /**
         * Take a [MutableSnapshot] and run [block] within it. When [block] returns successfully,
         * attempt to [MutableSnapshot.apply] the snapshot. Returns the result of [block] or throws
         * [SnapshotApplyConflictException] if snapshot changes attempted by [block] could not be
         * applied.
         *
         * Prior to returning, any changes made to snapshot state (e.g. state holders returned by
         * [androidx.compose.runtime.mutableStateOf] are not visible to other threads. When
         * [withMutableSnapshot] returns successfully those changes will be made visible to other
         * threads  and any snapshot observers (e.g. [androidx.compose.runtime.snapshotFlow]) will
         * be notified of changes.
         *
         * [block] must not suspend if [withMutableSnapshot] is called from a suspend function.
         */
        // TODO: determine a good way to prevent/discourage suspending in an inlined [block]
        inline fun <R> withMutableSnapshot(
            block: () -> R
        ): R = takeMutableSnapshot().run {
            try {
                enter(block).also { apply().check() }
            } finally {
                dispose()
            }
        }

        /**
         * Observe reads and or write of state objects in the current thread.
         *
         * This only affects the current snapshot (if any) and any new snapshots create from
         * [Snapshot.takeSnapshot] and [takeMutableSnapshot]. It will not affect any snapshots
         * previous created even if [Snapshot.enter] is called in [block].
         *
         * @param readObserver called when any state object is read.
         * @param writeObserver called when a state object is created or just before it is
         * written to the first time in the snapshot or a nested mutable snapshot. This might be
         * called several times for the same object if nested mutable snapshots are created.
         */
        fun <T> observe(
            readObserver: ((Any) -> Unit)? = null,
            writeObserver: ((Any) -> Unit)? = null,
            block: () -> T
        ): T {
            if (readObserver != null || writeObserver != null) {
                val currentSnapshot = threadSnapshot.get()
                val snapshot =
                    if (currentSnapshot == null || currentSnapshot is MutableSnapshot)
                        TransparentObserverMutableSnapshot(
                            previousSnapshot = currentSnapshot as? MutableSnapshot,
                            specifiedReadObserver = readObserver,
                            specifiedWriteObserver = writeObserver,
                            mergeParentObservers = true
                        )
                    else if (readObserver == null) return block()
                    else currentSnapshot.takeNestedSnapshot(readObserver)
                try {
                    return snapshot.enter(block)
                } finally {
                    snapshot.dispose()
                }
            } else return block()
        }

        @PublishedApi
        internal fun createNonObservableSnapshot(): Snapshot =
            createTransparentSnapshotWithNoParentReadObserver(
                previousSnapshot = threadSnapshot.get()
            )

        /**
         * Passed [block] will be run with all the currently set snapshot read observers disabled.
         */
        @OptIn(ExperimentalContracts::class)
        inline fun <T> withoutReadObservation(block: @DisallowComposableCalls () -> T): T {
            contract { callsInPlace(block, InvocationKind.EXACTLY_ONCE) }
            val snapshot = createNonObservableSnapshot()
            try {
                return snapshot.enter(block)
            } finally {
                snapshot.dispose()
            }
        }

        /**
         * Register an apply listener that is called back when snapshots are applied to the
         * global state.
         *
         * @return [ObserverHandle] to unregister [observer].
         */
        fun registerApplyObserver(observer: (Set<Any>, Snapshot) -> Unit): ObserverHandle {
            // Ensure observer does not see changes before this call.
            advanceGlobalSnapshot(emptyLambda)

            sync {
                applyObservers.add(observer)
            }
            return ObserverHandle {
                sync {
                    applyObservers.remove(observer)
                }
            }
        }

        /**
         * Register an observer of the first write to the global state of a global state object
         * since the last call to [sendApplyNotifications].
         *
         * Composition uses this to schedule a new composition whenever a state object that
         * was read in composition is modified.
         *
         * State objects can be sent to the apply observer that have not been sent to global write
         * observers. This happens for state objects inside [MutableSnapshot] that is later
         * applied by calling [MutableSnapshot.apply].
         *
         * This should only be used to determine if a call to [sendApplyNotifications] should be
         * scheduled to be called.
         *
         * @return [ObserverHandle] to unregister [observer].
         */
        fun registerGlobalWriteObserver(observer: ((Any) -> Unit)): ObserverHandle {
            sync {
                globalWriteObservers.add(observer)
            }
            advanceGlobalSnapshot()
            return ObserverHandle {
                sync {
                    globalWriteObservers.remove(observer)
                }
                advanceGlobalSnapshot()
            }
        }

        /**
         * Notify the snapshot that all objects created in this snapshot to this point should be
         * considered initialized. If any state object is are modified passed this point it will
         * appear as modified in the snapshot and any applicable snapshot write observer will be
         * called for the object and the object will be part of the a set of mutated objects sent to
         * any applicable snapshot apply observer.
         *
         * Unless [notifyObjectsInitialized] is called, state objects created in a snapshot are not
         * considered modified by the snapshot even if they are modified after construction.
         *
         * Compose uses this between phases of composition to allow observing changes to state
         * objects create in a previous phase.
         */
        fun notifyObjectsInitialized() = currentSnapshot().notifyObjectsInitialized()

        /**
         * Send any pending apply notifications for state objects changed outside a snapshot.
         *
         * Apply notifications for state objects modified outside snapshot are deferred until method
         * is called. This method is implicitly called whenever a non-nested [MutableSnapshot]
         * is applied making its changes visible to all new, non-nested snapshots.
         *
         * Composition schedules this to be called after changes to state objects are
         * detected an observer registered with [registerGlobalWriteObserver].
         */
        fun sendApplyNotifications() {
            val changes = sync {
                currentGlobalSnapshot.get().modified?.isNotEmpty() == true
            }
            if (changes)
                advanceGlobalSnapshot()
        }

        @InternalComposeApi
        fun openSnapshotCount() = openSnapshots.toList().size

        @PublishedApi
        internal fun removeCurrent(): Snapshot? {
            val previous = threadSnapshot.get()
            if (previous != null) threadSnapshot.set(null)
            return previous
        }

        @PublishedApi
        internal fun restoreCurrent(previous: Snapshot?) {
            if (previous != null) threadSnapshot.set(previous)
        }
    }
}

/**
 * Pin the snapshot and invalid set.
 *
 * @return returns a handle that should be passed to [releasePinningLocked] when the snapshot closes or
 * is disposed.
 */
internal fun trackPinning(id: Int, invalid: SnapshotIdSet): Int {
    val pinned = invalid.lowest(id)
    return sync {
        pinningTable.add(pinned)
    }
}

/**
 * Release the [handle] returned by [trackPinning]
 */
internal fun releasePinningLocked(handle: Int) {
    pinningTable.remove(handle)
}

/**
 * A snapshot of the values return by mutable states and other state objects. All state object
 * will have the same value in the snapshot as they had when the snapshot was created unless they
 * are explicitly changed in the snapshot.

 * To enter a snapshot call [enter]. The snapshot is the current snapshot as returned by
 * [currentSnapshot] until the control returns from the lambda (or until a nested [enter] is
 * called. All state objects will return the values associated with this snapshot, locally in the
 * thread, until [enter] returns. All other threads are unaffected.
 *
 * All changes made in a [MutableSnapshot] are snapshot isolated from all other snapshots and
 * their changes can only be seen globally, or by new shots, after [MutableSnapshot.apply] as been
 * called.
 *
 * Snapshots can be nested by calling [takeNestedSnapshot] or
 * [MutableSnapshot.takeNestedMutableSnapshot].
 *
 * @see Snapshot.takeMutableSnapshot
 * @see androidx.compose.runtime.mutableStateOf
 * @see androidx.compose.runtime.mutableStateListOf
 * @see androidx.compose.runtime.mutableStateMapOf
 */
open class MutableSnapshot internal constructor(
    id: Int,
    invalid: SnapshotIdSet,
    override val readObserver: ((Any) -> Unit)?,
    override val writeObserver: ((Any) -> Unit)?
) : Snapshot(id, invalid) {
    /**
     * Whether there are any pending changes in this snapshot. These changes are not visible
     * until the snapshot is applied.
     */
    override fun hasPendingChanges(): Boolean = modified?.isNotEmpty() == true

    /**
     * Take a mutable snapshot of the state values in this snapshot. Entering this snapshot by
     * calling [enter] allows state objects to be modified that are not visible to the this, the
     * parent snapshot, until the [apply] is called.
     *
     * Applying a nested snapshot, by calling [apply], applies its change to, this, the parent
     * snapshot. For a change to be visible globally, all the parent snapshots need to be applied
     * until the root snapshot is applied to the global state.
     *
     * All nested snapshots need to be disposed by calling [dispose] before resources associated
     * with this snapshot can be collected. Nested active snapshots are still valid after the parent
     * has been disposed but calling [apply] will fail.
     */
    open fun takeNestedMutableSnapshot(
        readObserver: ((Any) -> Unit)? = null,
        writeObserver: ((Any) -> Unit)? = null
    ): MutableSnapshot {
        validateNotDisposed()
        validateNotAppliedOrPinned()
        return advance {
            sync {
                val newId = nextSnapshotId++
                openSnapshots = openSnapshots.set(newId)
                val currentInvalid = invalid
                this.invalid = currentInvalid.set(newId)
                NestedMutableSnapshot(
                    newId,
                    currentInvalid.addRange(id + 1, newId),
                    mergedReadObserver(readObserver, this.readObserver),
                    mergedWriteObserver(writeObserver, this.writeObserver),
                    this
                )
            }
        }
    }

    /**
     * Apply the changes made to state objects in this snapshot to the global state, or to the
     * parent snapshot if this is a nested mutable snapshot.
     *
     * Once this method returns all changes made to this snapshot are atomically visible as the
     * global state of the state object or to the parent snapshot.
     *
     * While a snapshot is active (after it is created but before [apply] or [dispose] is called)
     * requires resources to track the values in the snapshot. Once a snapshot is no longer
     * needed it should be either applied by calling [apply] or disposed by calling [dispose]. A
     * snapshot that has been had is [apply] called can also have [dispose] called on it. However,
     * calling [apply] after calling [dispose] will throw an exception.
     *
     * Leaving a snapshot active could cause hard to diagnose memory leaks values are maintained
     * by state objects for unneeded snapshots. Take care to always call [dispose] on any snapshot.
     */
    open fun apply(): SnapshotApplyResult {
        // NOTE: the this algorithm is currently does not guarantee serializable snapshots as it
        // doesn't prevent crossing writes as described here https://arxiv.org/pdf/1412.2324.pdf

        // Just removing the snapshot from the active snapshot set is enough to make it part of the
        // next snapshot, however, this should only be done after first determining that there are no
        // colliding writes are being applied.

        // A write is considered colliding if any write occurred in a state object in a snapshot
        // applied since the snapshot was taken.
        val modified = modified
        val optimisticMerges = if (modified != null) optimisticMerges(
            currentGlobalSnapshot.get(),
            this,
            openSnapshots.clear(currentGlobalSnapshot.get().id)
        ) else null
        val (observers, globalModified) = sync {
            validateOpen(this)
            if (modified == null || modified.size == 0) {
                closeLocked()
                val previousGlobalSnapshot = currentGlobalSnapshot.get()
                takeNewGlobalSnapshot(previousGlobalSnapshot, emptyLambda)
                val globalModified = previousGlobalSnapshot.modified
                if (globalModified != null && globalModified.isNotEmpty())
                    applyObservers.toMutableList() to globalModified
                else
                    emptyList<(Set<Any>, Snapshot) -> Unit>() to null
            } else {
                val previousGlobalSnapshot = currentGlobalSnapshot.get()
                val result = innerApplyLocked(
                    nextSnapshotId,
                    optimisticMerges,
                    openSnapshots.clear(previousGlobalSnapshot.id)
                )
                if (result != SnapshotApplyResult.Success) return result

                closeLocked()

                // Take a new global snapshot that includes this one.
                takeNewGlobalSnapshot(previousGlobalSnapshot, emptyLambda)
                val globalModified = previousGlobalSnapshot.modified
                this.modified = null
                previousGlobalSnapshot.modified = null

                applyObservers.toMutableList() to globalModified
            }
        }

        // Mark as applied
        applied = true

        // Notify any apply observers that changes applied were seen
        if (globalModified != null && globalModified.isNotEmpty()) {
            observers.fastForEach {
                it(globalModified, this)
            }
        }

        if (modified != null && modified.isNotEmpty()) {
            observers.fastForEach {
                it(modified, this)
            }
        }

        // Wait to release pinned snapshots until after running observers.
        // This permits observers to safely take a nested snapshot of the one that was just applied
        // before unpinning records that need to be retained in this case.
        sync {
            releasePinnedSnapshotsForCloseLocked()
        }

        return SnapshotApplyResult.Success
    }

    override val readOnly: Boolean get() = false

    override val root: Snapshot get() = this

    override fun dispose() {
        if (!disposed) {
            super.dispose()
            nestedDeactivated(this)
        }
    }

    override fun takeNestedSnapshot(readObserver: ((Any) -> Unit)?): Snapshot {
        validateNotDisposed()
        validateNotAppliedOrPinned()
        val previousId = id
        return advance {
            sync {
                val readonlyId = nextSnapshotId++
                openSnapshots = openSnapshots.set(readonlyId)
                NestedReadonlySnapshot(
                    readonlyId,
                    invalid.addRange(previousId + 1, readonlyId),
                    readObserver,
                    this
                )
            }
        }
    }

    override fun nestedActivated(snapshot: Snapshot) { snapshots++ }

    override fun nestedDeactivated(snapshot: Snapshot) {
        require(snapshots > 0)
        if (--snapshots == 0) {
            if (!applied) {
                abandon()
            }
        }
    }

    override fun notifyObjectsInitialized() {
        if (applied || disposed) return
        advance()
    }

    override fun closeLocked() {
        // Remove itself and previous ids from the open set.
        openSnapshots = openSnapshots.clear(id).andNot(previousIds)
    }

    override fun releasePinnedSnapshotsForCloseLocked() {
        releasePreviouslyPinnedSnapshotsLocked()
        super.releasePinnedSnapshotsForCloseLocked()
    }

    internal fun validateNotApplied() {
        check(!applied) {
            "Unsupported operation on a snapshot that has been applied"
        }
    }

    internal fun validateNotAppliedOrPinned() {
        check(!applied || isPinned) {
            "Unsupported operation on a disposed or applied snapshot"
        }
    }

    /**
     * Abandon the snapshot. This does NOT [closeAndReleasePinning], which must be done
     * as an additional step by callers.
     */
    private fun abandon() {
        val modified = modified
        if (modified != null) {
            validateNotApplied()

            // Mark all state records created in this snapshot as invalid. This allows the snapshot
            // id to be forgotten as no state records will refer to it.
            this.modified = null
            val id = id
            for (state in modified) {
                var current: StateRecord? = state.firstStateRecord
                while (current != null) {
                    if (current.snapshotId == id || current.snapshotId in previousIds) {
                        current.snapshotId = INVALID_SNAPSHOT
                    }
                    current = current.next
                }
            }
        }

        // The snapshot can now be closed.
        closeAndReleasePinning()
    }

    internal fun innerApplyLocked(
        snapshotId: Int,
        optimisticMerges: Map<StateRecord, StateRecord>?,
        invalidSnapshots: SnapshotIdSet
    ): SnapshotApplyResult {
        // This must be called in a synchronized block

        // If there are modifications we need to ensure none of the modifications have
        // collisions.

        // A record is guaranteed not collide if no other write was performed to the record
        // by an applied snapshot since this snapshot was taken. No writes to a state object
        // occurred if, ignoring this snapshot, the readable records for the snapshots are
        // the same. If they are different then there is a potential collision and the state
        // object is asked if it can resolve the collision. If it can the updated state record
        // is for the apply.
        var mergedRecords: MutableList<Pair<StateObject, StateRecord>>? = null
        val start = this.invalid.set(id).or(this.previousIds)
        val modified = modified!!
        var statesToRemove: MutableList<StateObject>? = null
        for (state in modified) {
            val first = state.firstStateRecord
            // If either current or previous cannot be calculated the object was created
            // in a nested snapshot that was committed then changed.
            val current = readable(first, snapshotId, invalidSnapshots) ?: continue
            val previous = readable(first, id, start) ?: continue
            if (current != previous) {
                val applied = readable(first, id, this.invalid) ?: readError()
                val merged = optimisticMerges?.get(current) ?: run {
                    state.mergeRecords(previous, current, applied)
                }
                when (merged) {
                    null -> return SnapshotApplyResult.Failure(this)
                    applied -> {
                        // Nothing to do the merge policy says that the current changes
                        // obscure the current value so ignore the conflict
                    }
                    current -> {
                        (
                            mergedRecords ?: mutableListOf<Pair<StateObject, StateRecord>>().also {
                                mergedRecords = it
                            }
                            ).add(state to current.create())

                        // If we revert to current then the state is no longer modified.
                        (
                            statesToRemove ?: mutableListOf<StateObject>().also {
                                statesToRemove = it
                            }
                            ).add(state)
                    }
                    else -> {
                        (
                            mergedRecords ?: mutableListOf<Pair<StateObject, StateRecord>>().also {
                                mergedRecords = it
                            }
                            ).add(
                            if (merged != previous) state to merged
                            else state to previous.create()
                        )
                    }
                }
            }
        }

        mergedRecords?.let {
            // Ensure we have a new snapshot id
            advance()

            // Update all the merged records to have the new id.
            it.fastForEach { merged ->
                val (state, stateRecord) = merged
                stateRecord.snapshotId = id
                sync {
                    stateRecord.next = state.firstStateRecord
                    state.prependStateRecord(stateRecord)
                }
            }
        }

        statesToRemove?.let {
            // Remove from modified any state objects that have reverted to the parent value.
            modified.removeAll(it)
        }

        return SnapshotApplyResult.Success
    }

    internal inline fun <T> advance(block: () -> T): T {
        recordPrevious(id)
        return block().also {
            // Only advance this snapshot if it's possible for it to be applied later,
            // otherwise we don't need to bother.
            // This simplifies tracking of open snapshots when an apply observer takes
            // a nested snapshot of the snapshot that was just applied.
            if (!applied && !disposed) {
                val previousId = id
                sync {
                    id = nextSnapshotId++
                    openSnapshots = openSnapshots.set(id)
                }
                invalid = invalid.addRange(previousId + 1, id)
            }
        }
    }

    internal fun advance(): Unit = advance { }

    internal fun recordPrevious(id: Int) {
        sync {
            previousIds = previousIds.set(id)
        }
    }

    internal fun recordPreviousPinnedSnapshot(id: Int) {
        if (id >= 0)
            previousPinnedSnapshots = previousPinnedSnapshots + id
    }

    internal fun recordPreviousPinnedSnapshots(handles: IntArray) {
        // Avoid unnecessary copies implied by the `+` below.
        if (handles.isEmpty()) return
        val pinned = previousPinnedSnapshots
        if (pinned.isEmpty()) previousPinnedSnapshots = handles
        else previousPinnedSnapshots = pinned + handles
    }

    internal fun releasePreviouslyPinnedSnapshotsLocked() {
        for (index in previousPinnedSnapshots.indices) {
            releasePinningLocked(previousPinnedSnapshots[index])
        }
    }

    internal fun recordPreviousList(snapshots: SnapshotIdSet) {
        sync {
            previousIds = previousIds.or(snapshots)
        }
    }

    override fun recordModified(state: StateObject) {
        (modified ?: HashSet<StateObject>().also { modified = it }).add(state)
    }

    override var modified: MutableSet<StateObject>? = null

    /**
     * A set of the id's previously associated with this snapshot. When this snapshot closes
     * then these ids must be removed from the global as well.
     */
    internal var previousIds: SnapshotIdSet = SnapshotIdSet.EMPTY

    /**
     * A list of the pinned snapshots handles that must be released by this snapshot
     */
    internal var previousPinnedSnapshots: IntArray = IntArray(0)

    /**
     * The number of pending nested snapshots of this snapshot. To simplify the code, this
     * snapshot it, itself, counted as its own nested snapshot.
     */
    private var snapshots = 1

    /**
     * Tracks whether the snapshot has been applied.
     */
    internal var applied = false
}

/**
 * The result of a applying a mutable snapshot. [Success] indicates that the snapshot was
 * successfully applied and is now visible as the global state of the state object (or visible
 * in the parent snapshot for a nested snapshot). [Failure] indicates one or more state objects
 * were modified by both this snapshot and in the global (or parent) snapshot, and the changes from
 * this snapshot are **not** visible in the global or parent snapshot.
 */
sealed class SnapshotApplyResult {
    /**
     * Check the result of an apply. If the result is [Success] then this does does nothing. If
     * the result is [Failure] then a [SnapshotApplyConflictException] exception is thrown. Once
     * [check] as been called the snapshot is disposed.
     */
    abstract fun check()

    /**
     * True if the result is [Success].
     */
    abstract val succeeded: Boolean

    object Success : SnapshotApplyResult() {
        /**
         * Check the result of a snapshot apply. Calling [check] on a [Success] result is a noop.
         */
        override fun check() { }

        override val succeeded: Boolean get() = true
    }

    class Failure(val snapshot: Snapshot) : SnapshotApplyResult() {
        /**
         * Check the result of a snapshot apply. Calling [check] on a [Failure] result throws a
         * [SnapshotApplyConflictException] exception.
         */
        override fun check() {
            snapshot.dispose()
            throw SnapshotApplyConflictException(snapshot)
        }

        override val succeeded: Boolean get() = false
    }
}

/**
 * The type returned by observer registration methods that unregisters the observer when it is
 * disposed.
 */
@Suppress("CallbackName")
fun interface ObserverHandle {
    /**
     * Dispose the observer causing it to be unregistered from the snapshot system.
     */
    fun dispose()
}

/**
 * Return the thread's active snapshot. If no thread snapshot is active then the current global
 * snapshot is used.
 */
internal fun currentSnapshot(): Snapshot =
    threadSnapshot.get() ?: currentGlobalSnapshot.get()

/**
 * An exception that is thrown when [SnapshotApplyResult.check] is called on a result of a
 * [MutableSnapshot.apply] that fails to apply.
 */
class SnapshotApplyConflictException(
    @Suppress("unused") val snapshot: Snapshot
) : Exception()

/**
 * Snapshot local value of a state object.
 */
abstract class StateRecord {
    /**
     * The snapshot id of the snapshot in which the record was created.
     */
    internal var snapshotId: Int = currentSnapshot().id

    /**
     * Reference of the next state record. State records are stored in a linked list.
     *
     * Changes to [next] must preserve all existing records to all threads even during
     * intermediately changes. For example, it is safe to add the beginning or end of the list
     * but adding to the middle requires care. First the new record must have its [next] updated
     * then the [next] of its new predecessor can then be set to point to it. This implies that
     * records that are already in the list cannot be moved in the list as this the change must
     * be atomic to all threads that cannot happen without a lock which this list cannot afford.
     *
     * It is unsafe to remove a record as it might be in the process of being reused (see [used]).
     * If a record is removed care must be taken to ensure that it is not being claimed by some
     * other thread. This would require changes to [used].
     */
    internal var next: StateRecord? = null

    /**
     * Copy the value into this state record from another for the same state object.
     */
    abstract fun assign(value: StateRecord)

    /**
     * Create a new state record for the same state object.
     */
    abstract fun create(): StateRecord
}

/**
 * Interface implemented by all snapshot aware state objects. Used by this module to maintain the
 * state records of a state object.
 */
interface StateObject {
    /**
     * The first state record in a linked list of state records.
     */
    val firstStateRecord: StateRecord

    /**
     * Add a new state record to the beginning of a list. After this call [firstStateRecord] should
     * be [value].
     */
    fun prependStateRecord(value: StateRecord)

    /**
     * Produce a merged state based on the conflicting state changes.
     *
     * This method must not modify any of the records received and should treat the state records
     * as immutable, even the [applied] record.
     *
     * @param previous the state record that was used to create the [applied] record and is a state
     * that also (though indirectly) produced the [current] record.
     *
     * @param current the state record of the parent snapshot or global state.
     *
     * @param applied the state record that is being applied of the parent snapshot or global
     * state.
     *
     * @return the modified state or `null` if the values cannot be merged. If the states cannot
     * be merged the current apply will fail. Any of the parameters can be returned as a result.
     * If it is not one of the parameter values then it *must* be a new value that is created by
     * calling [StateRecord.create] on one of the records passed and then can be modified
     * to have the merged value before being returned. If a new record is returned
     * [MutableSnapshot.apply] will update the internal snapshot id and call
     * [prependStateRecord] if the record is used.
     */
    fun mergeRecords(
        previous: StateRecord,
        current: StateRecord,
        applied: StateRecord
    ): StateRecord? = null
}

/**
 * A snapshot whose state objects cannot be modified. If a state object is modified when in a
 * read-only snapshot a [IllegalStateException] is thrown.
 */
internal class ReadonlySnapshot internal constructor(
    id: Int,
    invalid: SnapshotIdSet,
    override val readObserver: ((Any) -> Unit)?
) : Snapshot(id, invalid) {
    /**
     * The number of nested snapshots that are active. To simplify the code, this snapshot counts
     * itself as a nested snapshot.
     */
    private var snapshots = 1
    override val readOnly: Boolean get() = true
    override val root: Snapshot get() = this
    override fun hasPendingChanges(): Boolean = false
    override val writeObserver: ((Any) -> Unit)? get() = null

    override var modified: HashSet<StateObject>?
        get() = null
        @Suppress("UNUSED_PARAMETER")
        set(value) = unsupported()

    override fun takeNestedSnapshot(readObserver: ((Any) -> Unit)?): Snapshot {
        validateOpen(this)
        return NestedReadonlySnapshot(id, invalid, readObserver, this)
    }

    override fun notifyObjectsInitialized() {
        // Nothing to do for read-only snapshots
    }

    override fun dispose() {
        if (!disposed) {
            nestedDeactivated(this)
            super.dispose()
        }
    }

    override fun nestedActivated(snapshot: Snapshot) { snapshots++ }

    override fun nestedDeactivated(snapshot: Snapshot) {
        if (--snapshots == 0) {
            // A read-only snapshot can be just be closed as it has no modifications.
            closeAndReleasePinning()
        }
    }

    override fun recordModified(state: StateObject) {
        reportReadonlySnapshotWrite()
    }
}

internal class NestedReadonlySnapshot(
    id: Int,
    invalid: SnapshotIdSet,
    readObserver: ((Any) -> Unit)?,
    val parent: Snapshot
) : Snapshot(id, invalid) {
    init { parent.nestedActivated(this) }
    override val readOnly get() = true
    override val root: Snapshot get() = parent.root
    override fun takeNestedSnapshot(readObserver: ((Any) -> Unit)?) =
        NestedReadonlySnapshot(id, invalid, readObserver, parent)
    override fun notifyObjectsInitialized() {
        // Nothing to do for read-only snapshots
    }
    override fun hasPendingChanges(): Boolean = false
    override val readObserver: ((Any) -> Unit)? =
        // Merge the read observers if necessary
        readObserver?.let {
            parent.readObserver?.let {
                { state: Any ->
                    readObserver(state)
                    it(state)
                }
            } ?: readObserver
        } ?: parent.readObserver

    override fun dispose() {
        if (!disposed) {
            if (id != parent.id) {
                closeAndReleasePinning()
            }
            parent.nestedDeactivated(this)
            super.dispose()
        }
    }

    override val modified: HashSet<StateObject>? get() = null
    override val writeObserver: ((Any) -> Unit)? get() = null
    override fun recordModified(state: StateObject) = reportReadonlySnapshotWrite()

    override fun nestedDeactivated(snapshot: Snapshot) = unsupported()
    override fun nestedActivated(snapshot: Snapshot) = unsupported()
}

private val emptyLambda: (invalid: SnapshotIdSet) -> Unit = { }

/**
 * A snapshot object that simplifies the code by treating the global state as a mutable snapshot.
 */
internal class GlobalSnapshot(id: Int, invalid: SnapshotIdSet) :
    MutableSnapshot(
        id, invalid, null,
        sync {
            // Take a defensive copy of the  globalWriteObservers list. This then avoids having to
            // synchronized access to writerObserver in places it is called and allows the list to
            // change while notifications are being dispatched. Changes to globalWriteObservers force
            // a new global snapshot to be created.
            (
                if (globalWriteObservers.isNotEmpty()) {
                    globalWriteObservers.toMutableList()
                } else null
                )?.let {
                it.singleOrNull() ?: { state: Any ->
                    it.fastForEach { it(state) }
                }
            }
        }
    ) {

    override fun takeNestedSnapshot(readObserver: ((Any) -> Unit)?): Snapshot =
        takeNewSnapshot { invalid ->
            ReadonlySnapshot(
                id = sync { nextSnapshotId++ },
                invalid = invalid,
                readObserver = readObserver
            )
        }

    override fun takeNestedMutableSnapshot(
        readObserver: ((Any) -> Unit)?,
        writeObserver: ((Any) -> Unit)?
    ): MutableSnapshot = takeNewSnapshot { invalid ->
        MutableSnapshot(
            id = sync { nextSnapshotId++ },
            invalid = invalid,

            // It is intentional that the global read observers are not merged with mutable
            // snapshots read observers.
            readObserver = readObserver,

            // It is intentional that global write observers are not merged with mutable
            // snapshots write observers.
            writeObserver = writeObserver
        )
    }

    override fun notifyObjectsInitialized() {
        advanceGlobalSnapshot()
    }

    override fun nestedDeactivated(snapshot: Snapshot) = unsupported()
    override fun nestedActivated(snapshot: Snapshot) = unsupported()
    override fun apply(): SnapshotApplyResult =
        error("Cannot apply the global snapshot directly. Call Snapshot.advanceGlobalSnapshot")

    override fun dispose() {
        sync {
            releasePinnedSnapshotLocked()
        }
    }
}

/**
 * A nested mutable snapshot created by [MutableSnapshot.takeNestedMutableSnapshot].
 */
internal class NestedMutableSnapshot(
    id: Int,
    invalid: SnapshotIdSet,
    readObserver: ((Any) -> Unit)?,
    writeObserver: ((Any) -> Unit)?,
    val parent: MutableSnapshot
) : MutableSnapshot(id, invalid, readObserver, writeObserver) {
    private var deactivated = false

    init { parent.nestedActivated(this) }

    override val root: Snapshot get() = parent.root

    override fun dispose() {
        if (!disposed) {
            super.dispose()
            deactivate()
        }
    }

    override fun apply(): SnapshotApplyResult {
        if (parent.applied || parent.disposed) return SnapshotApplyResult.Failure(this)

        // Applying a nested mutable snapshot applies its changes to the parent snapshot.

        // See MutableSnapshot.apply() for implantation notes.

        // The apply observer notification are for applying to the global scope so it is elided
        // here making this code a bit simpler than MutableSnapshot.apply.

        val modified = modified
        val id = id
        val optimisticMerges = if (modified != null)
            optimisticMerges(parent, this, parent.invalid)
        else null
        sync {
            validateOpen(this)
            if (modified == null || modified.size == 0) {
                closeAndReleasePinning()
            } else {
                val result = innerApplyLocked(parent.id, optimisticMerges, parent.invalid)
                if (result != SnapshotApplyResult.Success) return result

                // Add all modified objects in this set to the parent
                (
                    parent.modified ?: HashSet<StateObject>().also {
                        parent.modified = it
                    }
                    ).addAll(modified)
            }

            // Ensure the parent is newer than the current snapshot
            if (parent.id < id) {
                parent.advance()
            }

            // Make the snapshot visible in the parent snapshot
            parent.invalid = parent.invalid.clear(id).andNot(previousIds)

            // Ensure the ids associated with this snapshot are also applied by the parent.
            parent.recordPrevious(id)
            parent.recordPreviousPinnedSnapshot(takeoverPinnedSnapshot())
            parent.recordPreviousList(previousIds)
            parent.recordPreviousPinnedSnapshots(previousPinnedSnapshots)
        }

        applied = true
        deactivate()
        return SnapshotApplyResult.Success
    }

    private fun deactivate() {
        if (!deactivated) {
            deactivated = true
            parent.nestedDeactivated(this)
        }
    }
}

/**
 * A pseudo snapshot that doesn't introduce isolation but does introduce observers.
 */
internal class TransparentObserverMutableSnapshot(
    private val previousSnapshot: MutableSnapshot?,
    internal val specifiedReadObserver: ((Any) -> Unit)?,
    internal val specifiedWriteObserver: ((Any) -> Unit)?,
    private val mergeParentObservers: Boolean
) : MutableSnapshot(
    INVALID_SNAPSHOT,
    SnapshotIdSet.EMPTY,
    mergedReadObserver(
        specifiedReadObserver,
        previousSnapshot?.readObserver ?: currentGlobalSnapshot.get().readObserver,
        mergeParentObservers
    ),
    mergedWriteObserver(
        specifiedWriteObserver,
        previousSnapshot?.writeObserver ?: currentGlobalSnapshot.get().writeObserver
    )
) {
    private val currentSnapshot: MutableSnapshot
        get() = previousSnapshot ?: currentGlobalSnapshot.get()

    override fun dispose() {
        // Explicitly don't call super.dispose()
        disposed = true
    }

    override var id: Int
        get() = currentSnapshot.id
        @Suppress("UNUSED_PARAMETER")
        set(value) { unsupported() }

    override var invalid get() = currentSnapshot.invalid
        @Suppress("UNUSED_PARAMETER")
        set(value) = unsupported()

    override fun hasPendingChanges(): Boolean = currentSnapshot.hasPendingChanges()

    override var modified: MutableSet<StateObject>?
        get() = currentSnapshot.modified
        @Suppress("UNUSED_PARAMETER")
        set(value) = unsupported()

    override val readOnly: Boolean
        get() = currentSnapshot.readOnly

    override fun apply(): SnapshotApplyResult =
        currentSnapshot.apply()

    override fun recordModified(state: StateObject) =
        currentSnapshot.recordModified(state)

    override fun takeNestedSnapshot(readObserver: ((Any) -> Unit)?): Snapshot {
        val mergedReadObserver = mergedReadObserver(readObserver, this.readObserver)
        return if (!mergeParentObservers) {
            createTransparentSnapshotWithNoParentReadObserver(
                previousSnapshot = currentSnapshot.takeNestedSnapshot(null),
                readObserver = readObserver
            )
        } else {
            currentSnapshot.takeNestedSnapshot(mergedReadObserver)
        }
    }

    override fun takeNestedMutableSnapshot(
        readObserver: ((Any) -> Unit)?,
        writeObserver: ((Any) -> Unit)?
    ): MutableSnapshot {
        val mergedReadObserver = mergedReadObserver(readObserver, this.readObserver)
        val mergedWriteObserver = mergedWriteObserver(writeObserver, this.writeObserver)
        return if (!mergeParentObservers) {
            val nestedSnapshot = currentSnapshot.takeNestedMutableSnapshot(
                readObserver = null,
                writeObserver = mergedWriteObserver
            )
            TransparentObserverMutableSnapshot(
                previousSnapshot = nestedSnapshot,
                specifiedReadObserver = mergedReadObserver,
                specifiedWriteObserver = mergedWriteObserver,
                mergeParentObservers = false
            )
        } else {
            currentSnapshot.takeNestedMutableSnapshot(
                mergedReadObserver,
                mergedWriteObserver
            )
        }
    }

    override fun notifyObjectsInitialized() = currentSnapshot.notifyObjectsInitialized()

    /** Should never be called. */
    override fun nestedActivated(snapshot: Snapshot) = unsupported()

    override fun nestedDeactivated(snapshot: Snapshot) = unsupported()
}

/**
 * A pseudo snapshot that doesn't introduce isolation but does introduce observers.
 */
internal class TransparentObserverSnapshot(
    private val previousSnapshot: Snapshot?,
    specifiedReadObserver: ((Any) -> Unit)?,
    private val mergeParentObservers: Boolean
) : Snapshot(
    INVALID_SNAPSHOT,
    SnapshotIdSet.EMPTY,
) {
    override val readObserver: ((Any) -> Unit)? = mergedReadObserver(
        specifiedReadObserver,
        previousSnapshot?.readObserver ?: currentGlobalSnapshot.get().readObserver,
        mergeParentObservers
    )
    override val writeObserver: ((Any) -> Unit)? = null

    override val root: Snapshot = this

    private val currentSnapshot: Snapshot
        get() = previousSnapshot ?: currentGlobalSnapshot.get()

    override fun dispose() {
        // Explicitly don't call super.dispose()
        disposed = true
    }

    override var id: Int
        get() = currentSnapshot.id
        @Suppress("UNUSED_PARAMETER")
        set(value) { unsupported() }

    override var invalid get() = currentSnapshot.invalid
        @Suppress("UNUSED_PARAMETER")
        set(value) = unsupported()

    override fun hasPendingChanges(): Boolean = currentSnapshot.hasPendingChanges()

    override var modified: MutableSet<StateObject>?
        get() = currentSnapshot.modified
        @Suppress("UNUSED_PARAMETER")
        set(value) = unsupported()

    override val readOnly: Boolean
        get() = currentSnapshot.readOnly

    override fun recordModified(state: StateObject) =
        currentSnapshot.recordModified(state)

    override fun takeNestedSnapshot(readObserver: ((Any) -> Unit)?): Snapshot {
        val mergedReadObserver = mergedReadObserver(readObserver, this.readObserver)
        return if (!mergeParentObservers) {
            createTransparentSnapshotWithNoParentReadObserver(
                previousSnapshot = currentSnapshot.takeNestedSnapshot(null),
                readObserver = readObserver
            )
        } else {
            currentSnapshot.takeNestedSnapshot(mergedReadObserver)
        }
    }

    override fun notifyObjectsInitialized() = currentSnapshot.notifyObjectsInitialized()

    /** Should never be called. */
    override fun nestedActivated(snapshot: Snapshot) = unsupported()

    override fun nestedDeactivated(snapshot: Snapshot) = unsupported()
}

private fun createTransparentSnapshotWithNoParentReadObserver(
    previousSnapshot: Snapshot?,
    readObserver: ((Any) -> Unit)? = null,
): Snapshot = if (previousSnapshot is MutableSnapshot || previousSnapshot == null) {
    TransparentObserverMutableSnapshot(
        previousSnapshot = previousSnapshot as? MutableSnapshot,
        specifiedReadObserver = readObserver,
        specifiedWriteObserver = null,
        mergeParentObservers = false
    )
} else {
    TransparentObserverSnapshot(
        previousSnapshot = previousSnapshot,
        specifiedReadObserver = readObserver,
        mergeParentObservers = false
    )
}

private fun mergedReadObserver(
    readObserver: ((Any) -> Unit)?,
    parentObserver: ((Any) -> Unit)?,
    mergeReadObserver: Boolean = true
): ((Any) -> Unit)? {
    @Suppress("NAME_SHADOWING")
    val parentObserver = if (mergeReadObserver) parentObserver else null
    return if (readObserver != null && parentObserver != null && readObserver != parentObserver) {
        { state: Any ->
            readObserver(state)
            parentObserver(state)
        }
    } else readObserver ?: parentObserver
}

private fun mergedWriteObserver(
    writeObserver: ((Any) -> Unit)?,
    parentObserver: ((Any) -> Unit)?
): ((Any) -> Unit)? =
    if (writeObserver != null && parentObserver != null && writeObserver != parentObserver) {
        { state: Any ->
            writeObserver(state)
            parentObserver(state)
        }
    } else writeObserver ?: parentObserver

/**
 * Snapshot id of `0` is reserved as invalid and no state record with snapshot `0` is considered
 * valid.
 *
 * The value `0` was chosen as it is the default value of the Int snapshot id type and records
 * initially created will naturally have a snapshot id of 0. If this wasn't considered invalid
 * adding such a record to a state object will make the state record immediately visible to the
 * snapshots instead of being born invalid. Using `0` ensures all state records are created invalid
 * and must be explicitly marked as valid in to be visible in a snapshot.
 */
private const val INVALID_SNAPSHOT = 0

/**
 * Current thread snapshot
 */
private val threadSnapshot = SnapshotThreadLocal<Snapshot>()

/**
 * A global synchronization object. This synchronization object should be taken before modifying any
 * of the fields below.
 */
@PublishedApi
internal val lock = Any()

@PublishedApi
internal inline fun <T> sync(block: () -> T): T = synchronized(lock, block)

// The following variables should only be written when sync is taken

/**
 * A set of snapshots that are currently open and should be considered invalid for new snapshots.
 */
private var openSnapshots = SnapshotIdSet.EMPTY

/** The first snapshot created must be at least on more than the INVALID_SNAPSHOT */
private var nextSnapshotId = INVALID_SNAPSHOT + 1

/**
 * A tracking table for pinned snapshots. A pinned snapshot is the lowest snapshot id that the
 * snapshot is ignoring by considering them invalid. This is used to calculate when a snapshot
 * record can be reused.
 */
private val pinningTable = SnapshotDoubleIndexHeap()

/** A list of apply observers */
private val applyObservers = mutableListOf<(Set<Any>, Snapshot) -> Unit>()

/** A list of observers of writes to the global state. */
private val globalWriteObservers = mutableListOf<((Any) -> Unit)>()

private val currentGlobalSnapshot = AtomicReference(
    GlobalSnapshot(
        id = nextSnapshotId++,
        invalid = SnapshotIdSet.EMPTY
    ).also {
        openSnapshots = openSnapshots.set(it.id)
    }
)

/**
 * A value to use to initialize the snapshot local variable of writable below. The value of this
 * doesn't matter as it is just used to initialize the local that is immediately overwritten by
 * Snapshot.current. This is done to avoid a compiler error complaining that the var has not been
 * initialized. This can be removed once contracts are out of experimental; then we can mark sync
 * with the correct contracts so the compiler would be able to figure out that the variable is
 * initialized.
 */
@PublishedApi
internal val snapshotInitializer: Snapshot = currentGlobalSnapshot.get()

private fun <T> takeNewGlobalSnapshot(
    previousGlobalSnapshot: Snapshot,
    block: (invalid: SnapshotIdSet) -> T
): T {
    // Deactivate global snapshot. It is safe to just deactivate it because it cannot have
    // any conflicting writes as it is always closed before another snapshot is taken.
    val result = block(openSnapshots.clear(previousGlobalSnapshot.id))

    sync {
        val globalId = nextSnapshotId++
        openSnapshots = openSnapshots.clear(previousGlobalSnapshot.id)
        currentGlobalSnapshot.set(
            GlobalSnapshot(
                id = globalId,
                invalid = openSnapshots
            )
        )
        previousGlobalSnapshot.dispose()
        openSnapshots = openSnapshots.set(globalId)
    }

    return result
}

private fun <T> advanceGlobalSnapshot(block: (invalid: SnapshotIdSet) -> T): T {
    val previousGlobalSnapshot = currentGlobalSnapshot.get()
    val result = sync {
        takeNewGlobalSnapshot(previousGlobalSnapshot, block)
    }

    // If the previous global snapshot had any modified states then notify the registered apply
    // observers.
    val modified = previousGlobalSnapshot.modified
    if (modified != null) {
        val observers: List<(Set<Any>, Snapshot) -> Unit> = sync { applyObservers.toMutableList() }
        observers.fastForEach { observer ->
            observer(modified, previousGlobalSnapshot)
        }
    }

    return result
}

private fun advanceGlobalSnapshot() = advanceGlobalSnapshot { }

private fun <T : Snapshot> takeNewSnapshot(block: (invalid: SnapshotIdSet) -> T): T =
    advanceGlobalSnapshot { invalid ->
        val result = block(invalid)
        sync {
            openSnapshots = openSnapshots.set(result.id)
        }
        result
    }

private fun validateOpen(snapshot: Snapshot) {
    if (!openSnapshots.get(snapshot.id)) error("Snapshot is not open")
}

/**
 * A candidate snapshot is valid if the it is less than or equal to the current snapshot
 * and it wasn't specifically marked as invalid when the snapshot started.
 *
 * All snapshot active at when the snapshot was taken considered invalid for the snapshot
 * (they have not been applied and therefore are considered invalid).
 *
 * All snapshots taken after the current snapshot are considered invalid since they where taken
 * after the current snapshot was taken.
 *
 * INVALID_SNAPSHOT is reserved as an invalid snapshot id.
 */
private fun valid(currentSnapshot: Int, candidateSnapshot: Int, invalid: SnapshotIdSet): Boolean {
    return candidateSnapshot != INVALID_SNAPSHOT && candidateSnapshot <= currentSnapshot &&
        !invalid.get(candidateSnapshot)
}

// Determine if the given data is valid for the snapshot.
private fun valid(data: StateRecord, snapshot: Int, invalid: SnapshotIdSet): Boolean {
    return valid(snapshot, data.snapshotId, invalid)
}

private fun <T : StateRecord> readable(r: T, id: Int, invalid: SnapshotIdSet): T? {
    // The readable record is the valid record with the highest snapshotId
    var current: StateRecord? = r
    var candidate: StateRecord? = null
    while (current != null) {
        if (valid(current, id, invalid)) {
            candidate = if (candidate == null) current
            else if (candidate.snapshotId < current.snapshotId) current else candidate
        }
        current = current.next
    }
    if (candidate != null) {
        @Suppress("UNCHECKED_CAST")
        return candidate as T
    }
    return null
}

/**
 * Return the current readable state record for the current snapshot. It is assumed that [this]
 * is the first record of [state]
 */
fun <T : StateRecord> T.readable(state: StateObject): T =
    readable(state, currentSnapshot())

/**
 * Return the current readable state record for the [snapshot]. It is assumed that [this]
 * is the first record of [state]
 */
fun <T : StateRecord> T.readable(state: StateObject, snapshot: Snapshot): T {
    // invoke the observer associated with the current snapshot.
    snapshot.readObserver?.invoke(state)
    return readable(this, snapshot.id, snapshot.invalid) ?: readError()
}

private fun readError(): Nothing {
    error(
        "Reading a state that was created after the snapshot was taken or in a snapshot that " +
            "has not yet been applied"
    )
}

/**
 * A record can be reused if no other snapshot will see it as valid. This is always true for a
 * record created in an abandoned snapshot. It is also true if the record is valid in the
 * previous snapshot and is obscured by another record also valid in the previous state record.
 */
private fun used(state: StateObject): StateRecord? {
    var current: StateRecord? = state.firstStateRecord
    var validRecord: StateRecord? = null
    val reuseLimit = pinningTable.lowestOrDefault(nextSnapshotId) - 1
    val invalid = SnapshotIdSet.EMPTY
    while (current != null) {
        val currentId = current.snapshotId
        if (currentId == INVALID_SNAPSHOT) {
            // Any records that were marked invalid by an abandoned snapshot can be used
            // immediately.
            return current
        }
        if (valid(current, reuseLimit, invalid)) {
            if (validRecord == null) {
                validRecord = current
            } else {
                // If we have two valid records one must obscure the other. Return the
                // record with the lowest id
                return if (current.snapshotId < validRecord.snapshotId) current else validRecord
            }
        }
        current = current.next
    }
    return null
}

@PublishedApi
internal fun <T : StateRecord> T.writableRecord(state: StateObject, snapshot: Snapshot): T {
    if (snapshot.readOnly) {
        // If the snapshot is read-only, use the snapshot recordModified to report it.
        snapshot.recordModified(state)
    }
    val id = snapshot.id
    val readData = readable(this, id, snapshot.invalid) ?: readError()

    // If the readable data was born in this snapshot, it is writable.
    if (readData.snapshotId == snapshot.id) return readData

    // Otherwise, make a copy of the readable data and mark it as born in this snapshot, making it
    // writable.
    val newData = readData.newWritableRecord(state, snapshot)

    snapshot.recordModified(state)

    return newData
}

internal fun <T : StateRecord> T.overwritableRecord(
    state: StateObject,
    snapshot: Snapshot,
    candidate: T
): T {
    if (snapshot.readOnly) {
        // If the snapshot is read-only, use the snapshot recordModified to report it.
        snapshot.recordModified(state)
    }
    val id = snapshot.id

    if (candidate.snapshotId == id) return candidate

    val newData = newOverwritableRecord(state)
    newData.snapshotId = id

    snapshot.recordModified(state)

    return newData
}

internal fun <T : StateRecord> T.newWritableRecord(state: StateObject, snapshot: Snapshot): T {
    // Calling used() on a state object might return the same record for each thread calling
    // used() therefore selecting the record to reuse should be guarded.

    // Note: setting the snapshotId to Int.MAX_VALUE will make it invalid for all snapshots.
    // This means the lock can be released as used() will no longer select it. Using id could
    // also be used but it puts the object into a state where the reused value appears to be
    // the current valid value for the snapshot. This is not an issue if the snapshot is only
    // being read from a single thread but using Int.MAX_VALUE allows multiple readers,
    // single writer, of a snapshot. Note that threads reading a mutating snapshot should not
    // cache the result of readable() as the mutating thread calls to writable() can change the
    // result of readable().
    @Suppress("UNCHECKED_CAST")
    val newData = newOverwritableRecord(state)
    newData.assign(this)
    newData.snapshotId = snapshot.id
    return newData
}

internal fun <T : StateRecord> T.newOverwritableRecord(state: StateObject): T {
    // Calling used() on a state object might return the same record for each thread calling
    // used() therefore selecting the record to reuse should be guarded.

    // Note: setting the snapshotId to Int.MAX_VALUE will make it invalid for all snapshots.
    // This means the lock can be released as used() will no longer select it. Using id could
    // also be used but it puts the object into a state where the reused value appears to be
    // the current valid value for the snapshot. This is not an issue if the snapshot is only
    // being read from a single thread but using Int.MAX_VALUE allows multiple readers,
    // single writer, of a snapshot. Note that threads reading a mutating snapshot should not
    // cache the result of readable() as the mutating thread calls to writable() can change the
    // result of readable().
    @Suppress("UNCHECKED_CAST")
    return (used(state) as T?)?.apply {
        snapshotId = Int.MAX_VALUE
    } ?: create().apply {
        snapshotId = Int.MAX_VALUE
        this.next = state.firstStateRecord
        state.prependStateRecord(this as T)
    } as T
}

@PublishedApi
internal fun notifyWrite(snapshot: Snapshot, state: StateObject) {
    snapshot.writeObserver?.invoke(state)
}

/**
 * Call [block] with a writable state record for [snapshot] of the given record. It is
 * assumed that this is called for the first state record in a state object. If the snapshot is
 * read-only calling this will throw.
 */
inline fun <T : StateRecord, R> T.writable(
    state: StateObject,
    snapshot: Snapshot,
    block: T.() -> R
): R {
    // A writable record will always be the readable record (as all newer records are invalid it
    // must be the newest valid record). This means that if the readable record is not from the
    // current snapshot, a new record must be created. To create a new writable record, a record
    // can be reused, if possible, and the readable record is applied to it. If a record cannot
    // be reused, a new record is created and the readable record is applied to it. Once the
    // values are correct the record is made live by giving it the current snapshot id.

    // Writes need to be in a `sync` block as all writes in flight must be completed before a new
    // snapshot is take. Writing in a sync block ensures this is the case because new snapshots
    // are also in a sync block.
    return sync {
        this.writableRecord(state, snapshot).block()
    }.also { notifyWrite(snapshot, state) }
}

/**
 * Call [block] with a writable state record for the given record. It is assumed that this is
 * called for the first state record in a state object. A record is writable if it was created in
 * the current mutable snapshot.
 */
inline fun <T : StateRecord, R> T.writable(state: StateObject, block: T.() -> R): R {
    var snapshot: Snapshot = snapshotInitializer
    return sync {
        snapshot = Snapshot.current
        this.writableRecord(state, snapshot).block()
    }.also {
        notifyWrite(snapshot, state)
    }
}

/**
 * Call [block] with a writable state record for the given record. It is assumed that this is
 * called for the first state record in a state object. A record is writable if it was created in
 * the current mutable snapshot. This should only be used when the record will be overwritten in
 * its entirety (such as having only one field and that field is written to).
 *
 * WARNING: If the caller doesn't overwrite all the fields in the state record the object will be
 * inconsistent and the fields not written are almost guaranteed to be incorrect. If it is
 * possible that [block] will not write to all the fields use [writable] instead.
 *
 * @param state The object that has this record in its record list.
 * @param candidate The current for the snapshot record returned by [withCurrent]
 * @param block The block that will mutate all the field of the record.
 */
internal inline fun <T : StateRecord, R> T.overwritable(
    state: StateObject,
    candidate: T,
    block: T.() -> R
): R {
    var snapshot: Snapshot = snapshotInitializer
    return sync {
        snapshot = Snapshot.current
        this.overwritableRecord(state, snapshot, candidate).block()
    }.also {
        notifyWrite(snapshot, state)
    }
}

/**
 * Produce a set of optimistic merges of the state records, this is performed outside the
 * a synchronization block to reduce the amount of time taken in the synchronization block
 * reducing the thread contention of merging state values.
 */
private fun optimisticMerges(
    currentSnapshot: MutableSnapshot,
    applyingSnapshot: MutableSnapshot,
    invalidSnapshots: SnapshotIdSet
): Map<StateRecord, StateRecord>? {
    val modified = applyingSnapshot.modified
    val id = currentSnapshot.id
    if (modified == null) return null
    val start = applyingSnapshot.invalid.set(applyingSnapshot.id).or(applyingSnapshot.previousIds)
    var result: MutableMap<StateRecord, StateRecord>? = null
    for (state in modified) {
        val first = state.firstStateRecord
        val current = readable(first, id, invalidSnapshots) ?: continue
        val previous = readable(first, id, start) ?: continue
        if (current != previous) {
            // Try to produce a merged state record
            val applied = readable(first, applyingSnapshot.id, applyingSnapshot.invalid)
                ?: readError()
            val merged = state.mergeRecords(previous, current, applied)
            if (merged != null) {
                (
                    result ?: hashMapOf<StateRecord, StateRecord>().also {
                        result = it
                    }
                    )[current] = merged
            } else {
                // If one fails don't bother calculating the others as they are likely not going
                // to be used. There is an unlikely case that a optimistic merge cannot be
                // produced but the snapshot will apply because, once the synchronization is taken,
                // the current state can be merge. This routine errors on the side of reduced
                // overall work by not performing work that is likely to be ignored.
                return null
            }
        }
    }
    return result
}

private fun reportReadonlySnapshotWrite(): Nothing {
    error("Cannot modify a state object in a read-only snapshot")
}

/**
 * Returns the current record without notifying any read observers.
 */
@PublishedApi
internal fun <T : StateRecord> current(r: T, snapshot: Snapshot) =
    readable(r, snapshot.id, snapshot.invalid) ?: readError()

/**
 * Provides a [block] with the current record, without notifying any read observers.
 *
 * @see readable
 */
inline fun <T : StateRecord, R> T.withCurrent(block: (r: T) -> R): R =
    block(current(this, Snapshot.current))

/**
 * Helper routine to add a range of values ot a snapshot set
 */
internal fun SnapshotIdSet.addRange(from: Int, until: Int): SnapshotIdSet {
    var result = this
    for (invalidId in from until until)
        result = result.set(invalidId)
    return result
}
