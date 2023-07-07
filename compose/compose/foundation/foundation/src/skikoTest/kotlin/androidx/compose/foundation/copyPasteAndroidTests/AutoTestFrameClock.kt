/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.compose.foundation.copyPasteAndroidTests

import androidx.compose.foundation.AtomicLong
import androidx.compose.runtime.MonotonicFrameClock
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.internal.SynchronizedObject
import kotlinx.coroutines.internal.synchronized

@OptIn(InternalCoroutinesApi::class)
class AutoTestFrameClock : MonotonicFrameClock {
    private val time = AtomicLong(0)
    private val synchronizedObject = SynchronizedObject()
    override suspend fun <R> withFrameNanos(onFrame: (frameTimeNanos: Long) -> R): R {
        val get = synchronized(synchronizedObject) {
            val get = time.get()
            time.set(get + 16_000_000)
            get
        }

        return onFrame(get)
    }
}
