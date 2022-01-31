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

package androidx.compose.ui.test.junit4

/**
 * A strategy to wait for idleness. This is typically implemented by different test frameworks,
 * to allow each framework to await idleness in their own unique way. For example, a framework
 * could sleep until the system is idle, or a framework could take control of the system and
 * execute all pending work immediately.
 *
 * Normally one does not need to touch this, ever.
 */
internal interface IdlingStrategy {
    /**
     * Whether or not [runUntilIdle] of this strategy can be called from the main thread. If this
     * returns `false`, attempts to synchronize on the main thread will throw an exception.
     */
    val canSynchronizeOnUiThread: Boolean

    /**
     * Should block until the system is idle. A strategy may actively push the system towards an
     * idle state, but doesn't necessarily have to do that. For example, it could just poll the
     * system until it is idle and simply sleep in between.
     */
    fun runUntilIdle()

    /**
     * Should suspend until the system is idle. A strategy may actively push the system towards
     * an idle state, but doesn't necessarily have to do that. Default implementation calls
     * [runUntilIdle] without suspending.
     */
    suspend fun awaitIdle() = runUntilIdle()

    /**
     * Runs the [block] while giving implementations the option to perform setup and
     * tear down work.
     */
    fun <R> withStrategy(block: () -> R): R = block()
}
