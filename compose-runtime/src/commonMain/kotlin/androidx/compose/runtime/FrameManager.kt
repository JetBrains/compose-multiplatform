/*
 * Copyright 2019 The Android Open Source Project
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

@file:OptIn(
    ExperimentalComposeApi::class,
    InternalComposeApi::class
)

package androidx.compose.runtime

import androidx.compose.runtime.snapshots.MutableSnapshot
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.runtime.snapshots.SnapshotApplyResult
import androidx.compose.runtime.snapshots.SnapshotReadObserver
import androidx.compose.runtime.snapshots.SnapshotWriteObserver
import androidx.compose.runtime.snapshots.currentSnapshot
import androidx.compose.runtime.snapshots.takeMutableSnapshot
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * The frame manager manages how changes to state objects are observed.
 *
 * The [FrameManager] observers state reads during composition and records where in the
 * composition the state read occur. If any of the state objects are modified it will
 * invalidate the composition causing the associated [Recomposer] to schedule a recomposition.
 */
object FrameManager {
    private var started = false
    private var commitPending = false
    private var reclaimPending = false
    internal var composing = false
    private var invalidations = ObserverMap<Any, RecomposeScope>()
    private var removeApplyObserver: (() -> Unit)? = null
    private var removeWriteObserver: (() -> Unit)? = null
    private var alreadyProcessed = ObserverMap<Snapshot, Any>()
    private var needsInvalidate = ObserverMap<Snapshot, Any>()
    private val lock = Any()

    /**
     * TODO: This will be merged later with the scopes used by [Recomposer]
     */
    private val scheduleScope = CoroutineScope(Recomposer.current().embeddingContext
        .mainThreadCompositionContext() + SupervisorJob())

    fun ensureStarted() {
        if (!started) {
            started = true
            removeApplyObserver = Snapshot.registerApplyObserver(applyObserver)
            removeWriteObserver = Snapshot.registerGlobalWriteObserver(globalWriteObserver)
        }
    }

    internal fun close() {
        synchronized(lock) {
            invalidations.clear()
        }
        removeApplyObserver?.invoke()
        removeWriteObserver?.invoke()
        started = false
        invalidations = ObserverMap()
    }

    internal fun readObserverOf(composer: Composer<*>): SnapshotReadObserver {
        return { value -> recordRead(value, composer) }
    }

    internal inline fun <T> composing(composer: Composer<*>, block: () -> T): T {
        val wasComposing = composing
        composing = true
        val snapshot = takeMutableSnapshot(readObserverOf(composer), writeObserver)
        try {
            return snapshot.enter(block)
        } finally {
            composing = wasComposing
            applyAndCheck(snapshot)
        }
    }

    internal fun applyAndCheck(snapshot: MutableSnapshot) {
        val applyResult = snapshot.apply()
        if (applyResult is SnapshotApplyResult.Failure) {
            error("Unsupported concurrent change during composition. A state object was " +
                    "modified by composition as well as being modified outside composition.")
            // TODO(chuckj): Consider lifting this restriction by forcing a recompose
        }
    }

    @TestOnly
    @Deprecated(
        "This is no longer needed. The frame manager no longer maintains a 'frame' in the main " +
                "thread", replaceWith = ReplaceWith("block()"))
    fun <T> isolated(block: () -> T): T {
        ensureStarted()
        try {
            return block()
        } finally {
            close()
        }
    }

    @TestOnly
    @Deprecated(
        "This is no longer needed. The frame manager no longer maintains a 'frame' in the main " +
                "thread", replaceWith = ReplaceWith("block()"))
    fun <T> unframed(block: () -> T): T = block()

    /**
     * Ensure that [block] is executed in a frame. If the code is not in a frame create one for the
     * code to run in that is committed when [block] commits.
     */
    @Deprecated(
        "This is no longer needed. The frame manager no longer maintains a 'frame' in the main " +
                "thread", replaceWith = ReplaceWith("block()"))
    fun <T> framed(block: () -> T): T = block()

    @Deprecated(
        "The frame manager no longer manages a 'frame' in the main thread but global state does " +
                "not send change notifications until Snapshot.applyGlobalSnapshot() is called. " +
                "Consider calling Snapshot.sendApplyNotifications() instead.",
        ReplaceWith("Snapshot.applyGlobalSnapshot()", "androidx.compose.runtime.snapshots.Snapshot")
    )
    fun nextFrame() {
        Snapshot.sendApplyNotifications()
    }

    internal fun scheduleCleanup() {
        if (started && !reclaimPending && synchronized(lock) {
                if (!reclaimPending) {
                    reclaimPending = true
                    true
                } else false
            }) {
            schedule(reclaimInvalid)
        }
    }

    /**
     * Records that [value], or one of its fields, read while composing and its values were
     * used by [composer] while composing.
     *
     * This is the underlying mechanism used by [State] objects to allow composition to observe
     * changes made to them.
     */
    internal fun recordRead(value: Any, composer: Composer<*>) {
        composer.currentRecomposeScope?.let {
            synchronized(lock) {
                it.used = true
                invalidations.add(value, it)
            }
        }
    }

    private val globalWriteObserver: SnapshotWriteObserver = {
        if (!commitPending) {
            commitPending = true
            schedule {
                commitPending = false
                Snapshot.sendApplyNotifications()
            }
        }
    }

    private val writeObserver: SnapshotWriteObserver = ::recordWrite

    /**
     * Records that [value], or one of its fields, was changed and the reads recorded by
     * [recordRead] might have changed value.
     *
     * Calling this method outside of composition is ignored. This is only intended for
     * invaliding composable lambdas while composing.
     */
    internal fun recordWrite(value: Any) {
        if (composing) {
            val currentInvalidations = synchronized(lock) {
                invalidations.getValueOf(value)
            }
            if (currentInvalidations.isNotEmpty()) {
                var invalidateNeeeded = false
                var processed = false
                for (index in 0 until currentInvalidations.size) {
                    val scope = currentInvalidations[index]
                    when (scope.invalidate()) {
                        InvalidationResult.DEFERRED ->
                            // Even if we already processed elsewhere we need to invalidate this
                            // object because of a write after read.
                            invalidateNeeeded = true
                        InvalidationResult.IGNORED,
                        InvalidationResult.IMMINENT ->
                            // Either the write happened before the composition landed or no
                            // reads of the state occurred during composition before the write.
                            // In either case a valid composition was will be generated that
                            // incorporates the change.
                            processed = true
                        else -> { } // Nothing to do
                    }
                }
                if (invalidateNeeeded || processed) {
                    val snapshot = currentSnapshot().root
                    if (invalidateNeeeded)
                        this.needsInvalidate.add(snapshot, value)
                    if (processed)
                        alreadyProcessed.add(snapshot, value)
                }
            }
        }
    }

    private val applyObserver: (committed: Set<Any>, snapshot: Snapshot) -> Unit =
        { committed, snapshot ->
            trace("Model:appliedSnapshot") {
                val currentInvalidations = synchronized(lock) {
                    val invalidateNeeded = needsInvalidate.getValueOf(snapshot)
                    val processed = alreadyProcessed.getValueOf(snapshot)
                    // Ignore the object if its invalidations was already processed for this
                    // snapshot and was not marked as needing an invalidate.
                    invalidations[committed.filter {
                        !processed.contains(it) || invalidateNeeded.contains(it)
                    }]
                }
                if (currentInvalidations.isNotEmpty()) {
                    if (!Recomposer.current().embeddingContext.isMainThread()) {
                        schedule {
                            currentInvalidations.forEach { scope -> scope.invalidate() }
                        }
                    } else {
                        currentInvalidations.forEach { scope -> scope.invalidate() }
                    }
                }
            }
        }

    /**
     * Remove all invalidation scopes not currently part of a composition
     */
    private val reclaimInvalid: () -> Unit = {
        trace("Model:reclaimInvalid") {
            synchronized(this) {
                if (reclaimPending) {
                    reclaimPending = false
                    invalidations.clearValues { !it.valid }
                }
            }
        }
    }

    /**
     * List of deferred callbacks to run serially. Guarded by its own monitor lock.
     */
    private val scheduledCallbacks = mutableListOf<() -> Unit>()
    /**
     * Pending [Job] that will execute [scheduledCallbacks].
     * Guarded by [scheduledCallbacks]'s monitor lock.
     */
    private var callbackRunner: Job? = null

    /**
     * Synchronously executes any outstanding callbacks and brings the [FrameManager] into a
     * consistent, updated state.
     */
    internal fun synchronize() {
        synchronized(scheduledCallbacks) {
            scheduledCallbacks.forEach { it.invoke() }
            scheduledCallbacks.clear()
            callbackRunner?.cancel()
            callbackRunner = null
        }
    }

    private fun schedule(block: () -> Unit) {
        synchronized(scheduledCallbacks) {
            scheduledCallbacks.add(block)
            if (callbackRunner == null) {
                callbackRunner = scheduleScope.launch {
                    synchronize()
                }
            }
        }
    }
}