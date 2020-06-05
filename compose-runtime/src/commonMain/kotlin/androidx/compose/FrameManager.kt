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

package androidx.compose

import androidx.compose.frames.Frame
import androidx.compose.frames.abortHandler
import androidx.compose.frames.commit
import androidx.compose.frames.commitHandler
import androidx.compose.frames.currentFrame
import androidx.compose.frames.inFrame
import androidx.compose.frames.open
import androidx.compose.frames.registerCommitObserver
import androidx.compose.frames.restore
import androidx.compose.frames.suspend
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * The frame manager manages the priority frame in the main thread.
 *
 * Once the FrameManager has started there is always an open frame in the main thread. If a model
 * object is committed in any frame then the frame manager schedules the current frame to commit
 * with the Choreographer and a new frame is open. Any model objects read during composition are
 * recorded in an invalidations map. If they are mutated during a frame the recompose scope that
 * was active during the read is invalidated.
 */
object FrameManager {
    private var started = false
    private var commitPending = false
    private var reclaimPending = false
    internal var composing = false
    private var invalidations = ObserverMap<Any, RecomposeScope>()
    private var removeCommitObserver: (() -> Unit)? = null
    private var immediateMap = ObserverMap<Frame, Any>()
    private var deferredMap = ObserverMap<Frame, Any>()
    private val lock = Any()

    /**
     * TODO: This will be merged later with the scopes used by [Recomposer]
     */
    private val scheduleScope = CoroutineScope(mainThreadCompositionDispatcher() + SupervisorJob())

    fun ensureStarted() {
        if (!started) {
            started = true
            removeCommitObserver = registerCommitObserver(commitObserver)
            open()
        }
    }

    internal fun close() {
        synchronized(lock) {
            invalidations.clear()
        }
        if (inFrame) commit()
        removeCommitObserver?.let { it() }
        started = false
        invalidations = ObserverMap()
    }

    internal inline fun <T> composing(block: () -> T): T {
        val wasComposing = composing
        composing = true
        try {
            return block()
        } finally {
            composing = wasComposing
        }
    }

    @TestOnly
    fun <T> isolated(block: () -> T): T {
        ensureStarted()
        try {
            return block()
        } finally {
            close()
        }
    }

    @TestOnly
    fun <T> unframed(block: () -> T): T {
        if (inFrame) {
            val frame = suspend()
            try {
                val result = block()
                if (inFrame) error("An unframed block left a frame uncommitted or aborted")
                return result
            } finally {
                restore(frame)
            }
        } else return block()
    }

    /**
     * Ensure that [block] is executed in a frame. If the code is not in a frame create one for the
     * code to run in that is committed when [block] commits.
     */
    fun <T> framed(block: () -> T): T {
        return if (inFrame) {
            block()
        } else {
            open(false)
            try {
                block()
            } catch (e: Throwable) {
                abortHandler()
                throw e
            } finally {
                commitHandler()
            }
        }
    }

    fun nextFrame() {
        if (inFrame) {
            commit()
            open()
        }
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

    private val readObserver: (read: Any) -> Unit = { read ->
        currentComposerInternal?.currentRecomposeScope?.let {
            synchronized(lock) {
                it.used = true
                invalidations.add(read, it)
            }
        }
    }

    /**
     * Records that [value], or one of its fields, read while composing and its values were
     * used during composition.
     *
     * This is the underlying mechanism used by [State] objects to allow composition to observe
     * changes made to model objects.
     */
    internal fun recordRead(value: Any) = readObserver(value)

    private val writeObserver: (write: Any, isNew: Boolean) -> Unit = { value, isNew ->
        if (!commitPending) {
            commitPending = true
            schedule {
                commitPending = false
                nextFrame()
            }
        }
        recordWrite(value, isNew)
    }

    /**
     * Records that [value], or one of its fields, was changed and the reads recorded by
     * [recordRead] might have changed value.
     *
     * Calling this method outside of composition is ignored. This is only intended for
     * invaliding composable lambdas while composing.
     */
    internal fun recordWrite(value: Any, isNew: Boolean) {
        if (!isNew && composing) {
            val currentInvalidations = synchronized(lock) {
                invalidations.getValueOf(value)
            }
            if (currentInvalidations.isNotEmpty()) {
                var hasDeferred = false
                var hasImminent = false
                for (index in 0 until currentInvalidations.size) {
                    val scope = currentInvalidations[index]
                    when (scope.invalidate()) {
                        InvalidationResult.DEFERRED -> hasDeferred = true
                        InvalidationResult.IMMINENT -> hasImminent = true
                        else -> { } // Nothing to do
                    }
                }
                if (hasDeferred || hasImminent) {
                    val frame = currentFrame()
                    if (hasDeferred)
                        deferredMap.add(frame, value)
                    if (hasImminent)
                        immediateMap.add(frame, value)
                }
            }
        }
    }

    private val commitObserver: (committed: Set<Any>, frame: Frame) -> Unit = { committed, frame ->
        trace("Model:commitTransaction") {
            val currentInvalidations = synchronized(lock) {
                val deferred = deferredMap.getValueOf(frame)
                val immediate = immediateMap.getValueOf(frame)
                // Ignore the object if its invalidations were all immediate for the frame.
                invalidations[committed.filter {
                    !immediate.contains(it) || deferred.contains(it)
                }]
            }
            if (currentInvalidations.isNotEmpty()) {
                if (!isMainThread()) {
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

    private fun open() {
        open(
            readObserver = readObserver,
            writeObserver = writeObserver
        )
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