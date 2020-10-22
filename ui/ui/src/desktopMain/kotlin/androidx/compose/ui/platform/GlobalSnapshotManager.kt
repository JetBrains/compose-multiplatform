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

package androidx.compose.ui.platform

import androidx.compose.runtime.ExperimentalComposeApi
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.runtime.snapshots.SnapshotWriteObserver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.swing.Swing
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Platform-specific mechanism for starting a monitor of global snapshot state writes
 * in order to schedule the periodic dispatch of snapshot apply notifications.
 * This process should remain platform-specific; it is tied to the threading and update model of
 * a particular platform and framework target.
 *
 * Composition bootstrapping mechanisms for a particular platform/framework should call
 * [ensureStarted] during setup to initialize periodic global snapshot notifications.
 * For desktop, these notifications are always sent on [Dispatchers.Swing]. Other platforms
 * may establish different policies for these notifications.
 */
internal object GlobalSnapshotManager {
    private val started = AtomicBoolean(false)
    private var commitPending = false
    private var removeWriteObserver: (() -> Unit)? = null

    private val scheduleScope = CoroutineScope(Dispatchers.Swing + SupervisorJob())

    @OptIn(ExperimentalComposeApi::class)
    fun ensureStarted() {
        if (started.compareAndSet(false, true)) {
            removeWriteObserver = Snapshot.registerGlobalWriteObserver(globalWriteObserver)
        }
    }

    @OptIn(ExperimentalComposeApi::class)
    private val globalWriteObserver: SnapshotWriteObserver = {
        // Race, but we don't care too much if we end up with multiple calls scheduled.
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
     * Guarded by [scheduledCallbacks]'s monitor lock.
     */
    private var isSynchronizeScheduled = false

    /**
     * Synchronously executes any outstanding callbacks and brings snapshots into a
     * consistent, updated state.
     */
    private fun synchronize() {
        synchronized(scheduledCallbacks) {
            scheduledCallbacks.forEach { it.invoke() }
            scheduledCallbacks.clear()
            isSynchronizeScheduled = false
        }
    }

    private fun schedule(block: () -> Unit) {
        synchronized(scheduledCallbacks) {
            scheduledCallbacks.add(block)
            if (!isSynchronizeScheduled) {
                isSynchronizeScheduled = true
                scheduleScope.launch { synchronize() }
            }
        }
    }
}
