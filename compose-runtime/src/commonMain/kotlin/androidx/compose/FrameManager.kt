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
import androidx.compose.frames.open
import androidx.compose.frames.commit
import androidx.compose.frames.currentFrame
import androidx.compose.frames.suspend
import androidx.compose.frames.restore
import androidx.compose.frames.registerCommitObserver
import androidx.compose.frames.inFrame

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

    private val handler by lazy { Handler(LooperWrapper.getMainLooper()) }

    fun ensureStarted() {
        if (!started) {
            started = true
            removeCommitObserver =
                registerCommitObserver(commitObserver)
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

    @TestOnly
    fun <T> framed(block: () -> T): T {
        if (inFrame) {
            return block()
        } else {
            open()
            try {
                return block()
            } finally {
                commit()
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
        currentComposer?.currentRecomposeScope?.let {
            synchronized(lock) {
                it.used = true
                invalidations.add(read, it)
            }
        }
    }

    private val writeObserver: (write: Any) -> Unit = {
        if (!commitPending) {
            commitPending = true
            schedule {
                commitPending = false
                nextFrame()
            }
        }
        if (composing) {
            val currentInvalidations = synchronized(lock) {
                invalidations.getValueOf(it)
            }
            val results = currentInvalidations.map { scope -> scope.invalidate() }
            val frame = currentFrame()
            if (results.any { it == InvalidationResult.DEFERRED }) deferredMap.add(frame, it)
            if (results.any { it == InvalidationResult.IMMINENT }) immediateMap.add(frame, it)
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
                } ]
            }
            currentInvalidations.forEach { scope -> scope.invalidate() }
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

    private fun schedule(block: () -> Unit) {
        handler.postAtFrontOfQueue(block)
    }
}