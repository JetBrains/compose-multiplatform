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

import android.os.Handler
import android.os.Looper
import androidx.compose.frames.open
import androidx.compose.frames.commit
import androidx.compose.frames.suspend
import androidx.compose.frames.restore
import androidx.compose.frames.registerCommitObserver
import androidx.compose.frames.inFrame

/**
 * The frame manager manages the priority frame in the main thread.
 *
 * Once the FrameManager has started there is always an open frame in the main thread. If a model object is committed in any
 * frame then the frame manager schedules the current frame to commit with the Choreographer and a new frame is open. Any
 * model objects read during composition are recorded in an invalidations map. If they are mutated during a frame the recompose
 * scope that was active during the read is invalidated.
 */
object FrameManager {
    private var started = false
    private var commitPending = false
    private var reclaimPending = false
    private var invalidations = ObserverMap<Any, RecomposeScope>()
    private var removeCommitObserver: (() -> Unit)? = null

    private val handler by lazy { Handler(Looper.getMainLooper()) }

    fun ensureStarted() {
        if (!started) {
            started = true
            removeCommitObserver =
                registerCommitObserver(commitObserver)
            open()
        }
    }

    internal fun close() {
        synchronized(this) {
            invalidations.clear()
        }
        if (inFrame) commit()
        removeCommitObserver?.let { it() }
        started = false
        invalidations = ObserverMap()
    }

    fun <T> isolated(block: () -> T): T {
        ensureStarted()
        try {
            return block()
        } finally {
            close()
        }
    }

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
        if (started && !reclaimPending && synchronized(this) {
                if (!reclaimPending) {
                    reclaimPending = true
                    true
                } else false
            }) {
            schedule(reclaimInvalid)
        }
    }

    private val readObserver: (read: Any) -> Unit = { read ->
        currentComposer?.currentInvalidate?.let {
            synchronized(this) {
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
    }

    private val commitObserver: (committed: Set<Any>) -> Unit = { committed ->
        trace("Model:commitTransaction") {
            val currentInvalidations = synchronized(this) { invalidations[committed] }
            currentInvalidations.forEach { scope -> scope.invalidate?.invoke(false) }
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

    private inline fun schedule(crossinline block: () -> Unit) {
        handler.postAtFrontOfQueue { block() }
    }
}
