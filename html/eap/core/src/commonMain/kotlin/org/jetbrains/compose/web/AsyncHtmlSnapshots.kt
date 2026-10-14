/*
 * Copyright 2020-2026 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.web

import androidx.compose.runtime.snapshots.ObserverHandle
import androidx.compose.runtime.snapshots.Snapshot
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Shares snapshot apply notifications across active renders and external loaders. */
internal object AsyncHtmlSnapshots {
    private val mutex = Mutex()
    private var users = 0
    private var observer: ObserverHandle? = null
    private var notifications: Channel<Unit>? = null
    private var worker: Job? = null

    suspend fun acquire() = mutex.withLock {
        if (users++ == 0) {
            val channel = Channel<Unit>(Channel.CONFLATED)
            notifications = channel
            observer = Snapshot.registerGlobalWriteObserver { channel.trySend(Unit) }
            worker = CoroutineScope(Dispatchers.Default).launch {
                while (channel.receiveCatching().isSuccess) Snapshot.sendApplyNotifications()
            }
            channel.trySend(Unit)
        }
    }

    suspend fun release() = mutex.withLock {
        if (--users == 0) {
            observer?.dispose()
            observer = null
            notifications?.close()
            notifications = null
            worker?.join()
            worker = null
        }
    }
}
