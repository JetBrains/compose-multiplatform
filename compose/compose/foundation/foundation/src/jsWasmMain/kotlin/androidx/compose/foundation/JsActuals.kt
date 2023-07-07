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

package androidx.compose.foundation
/**
 * A simplified, non-thread-safe AtomicLong implementation for usage
 * in a single-threaded environment (k/js and/or k/wasm - browser apps).
 * It is not intended for multi-threaded use!
 *
 * Its purpose is to provide a corresponding API 
 * to match AtomicLong implementations for other kotlin platforms.
 */
actual class AtomicLong actual constructor(value: Long) {

    private var atomic = value

    actual fun get(): Long = atomic

    actual fun set(value: Long): Unit {
        atomic = value
    }

    actual fun getAndIncrement(): Long {
        val original = atomic
        atomic++
        return original
    }
}

