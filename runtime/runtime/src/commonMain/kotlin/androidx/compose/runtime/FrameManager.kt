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

package androidx.compose.runtime

import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.runtime.snapshots.SnapshotWriteObserver
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
@Deprecated(
    "Platform/framework-specific code should schedule Snapshot.sendApplyNotifications dispatch in" +
        " response to a Snapshot globalWriteObserver in a platform-appropriate manner"
)
object FrameManager {
    private var started = false
    private var commitPending = false
    private var removeWriteObserver: (() -> Unit)? = null

    /**
     * TODO: This will be merged later with the scopes used by [Recomposer]
     */
    private val scheduleScope = CoroutineScope(
        EmbeddingContext().mainThreadCompositionContext() + SupervisorJob()
    )

    @OptIn(ExperimentalComposeApi::class)
    fun ensureStarted() {
        if (!started) {
            started = true
            removeWriteObserver = Snapshot.registerGlobalWriteObserver(globalWriteObserver)
        }
    }

    internal fun close() {
        removeWriteObserver?.invoke()
        started = false
    }

    @OptIn(ExperimentalComposeApi::class)
    private val globalWriteObserver: SnapshotWriteObserver = {
        if (!commitPending) {
            commitPending = true
            schedule {
                commitPending = false
                Snapshot.sendApplyNotifications()
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
    private fun synchronize() {
        synchronized(scheduledCallbacks) {
            scheduledCallbacks.forEach { it.invoke() }
            scheduledCallbacks.clear()
            callbackRunner?.cancel()
            callbackRunner = null
        }
    }

    internal fun schedule(block: () -> Unit) {
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
