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

package androidx.compose.runtime.dispatch

import kotlinx.coroutines.yield

// TODO(demin): how to implement this clock in case we have multiple windows on different
//  monitors? This clock is using by Recomposer to sync composition with frame rendering.
//  Also this clock is available to use in coroutines in client code.
actual val DefaultMonotonicFrameClock: MonotonicFrameClock by lazy {
    object : MonotonicFrameClock {
        override suspend fun <R> withFrameNanos(
            onFrame: (Long) -> R
        ): R {
            yield()
            return onFrame(System.nanoTime())
        }
    }
}