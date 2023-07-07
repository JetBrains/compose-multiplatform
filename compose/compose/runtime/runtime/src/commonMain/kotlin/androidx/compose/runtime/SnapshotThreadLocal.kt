/*
 * Copyright 2023 The Android Open Source Project
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

import androidx.compose.runtime.internal.ThreadMap
import androidx.compose.runtime.internal.emptyThreadMap

/**
 * This is similar to a `java.lang.ThreadLocal` but has lower overhead because it avoids a weak reference.
 * This should only be used when the writes are delimited by a try...finally call that will clean
 * up the reference such as [androidx.compose.runtime.snapshots.Snapshot.enter] else the reference
 * could get pinned by the thread local causing a leak.
 */
internal class SnapshotThreadLocal<T> {
    private val map = AtomicReference<ThreadMap>(emptyThreadMap)
    private val writeMutex = SynchronizedObject()

    @Suppress("UNCHECKED_CAST")
    fun get(): T? = map.get().get(getCurrentThreadId()) as T?

    fun set(value: T?) {
        val key = getCurrentThreadId()
        synchronized(writeMutex) {
            val current = map.get()
            if (current.trySet(key, value)) return
            map.set(current.newWith(key, value))
        }
    }
}
