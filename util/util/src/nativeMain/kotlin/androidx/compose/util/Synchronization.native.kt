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

package androidx.compose.util

internal actual class SynchronizedObject : kotlinx.atomicfu.locks.SynchronizedObject()

internal actual fun createSynchronizedObject() = SynchronizedObject()

// When we decide to switch to Experimental Memory Model,
// we need to change the synchronized implementation:
//
// internal actual inline fun <R> synchronized(lock: SynchronizedObject, block: () -> R): R =
//    kotlinx.atomicfu.locks.synchronized(lock, block)

@PublishedApi
internal actual inline fun <R> synchronized(lock: SynchronizedObject, block: () -> R): R = block()
