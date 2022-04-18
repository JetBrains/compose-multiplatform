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

import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.platform.GlobalSnapshotManager.ensureStarted
import androidx.compose.util.createSynchronizedObject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.launch
import org.jetbrains.skiko.MainUIDispatcher
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Platform-specific mechanism for starting a monitor of global snapshot state writes
 * in order to schedule the periodic dispatch of snapshot apply notifications.
 * This process should remain platform-specific; it is tied to the threading and update model of
 * a particular platform and framework target.
 *
 * Composition bootstrapping mechanisms for a particular platform/framework should call
 * [ensureStarted] during setup to initialize periodic global snapshot notifications.
 * For desktop, these notifications are always sent on [MainUIDispatcher]. Other platforms
 * may establish different policies for these notifications.
 */
internal actual object GlobalSnapshotManager {
    internal actual val sync = createSynchronizedObject()
    private val started = AtomicBoolean(false)

    actual fun ensureStarted() {
        if (started.compareAndSet(false, true)) {
            val channel = Channel<Unit>(Channel.CONFLATED)
            Dispatchers.IO
            CoroutineScope(MainUIDispatcher).launch {
                channel.consumeEach {
                    // TODO(https://github.com/JetBrains/compose-jb/issues/1854) get rid of synchronized
                    synchronized(sync) {
                        Snapshot.sendApplyNotifications()
                    }
                }
            }
            Snapshot.registerGlobalWriteObserver {
                channel.trySend(Unit)
            }
        }
    }
}
