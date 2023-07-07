/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.compose.runtime.snapshots

import androidx.compose.runtime.AtomicInt
import androidx.compose.runtime.AtomicReference
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.postIncrement
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.concurrent.thread
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertNull

class SnapshotTestsJvm {

    @Test
    fun testMultiThreadedReadingAndWritingOfGlobalScope() {
        val running = AtomicBoolean(true)
        val reads = AtomicInt(0)
        val writes = AtomicInt(0)
        val lowNumberSeen = AtomicInt(0)
        val exception = AtomicReference<Throwable?>(null)
        try {
            val state = mutableStateOf(0)
            Snapshot.notifyObjectsInitialized()

            // Create 100 reader threads of state
            repeat(100) {
                thread {
                    try {
                        while (running.get()) {
                            reads.postIncrement()
                            if (state.value < 1000) lowNumberSeen.postIncrement()
                        }
                    } catch (e: Throwable) {
                        exception.set(e)
                        running.set(false)
                    }
                }
            }

            // Create 10 writer threads
            repeat(10) {
                thread {
                    while (running.get()) {
                        writes.postIncrement()
                        state.value = Random.nextInt(10000)
                        Snapshot.sendApplyNotifications()
                    }
                }
            }

            while (running.get() && writes.get() < 10000) {
                Thread.sleep(0)
            }
        } finally {
            running.set(false)
        }

        exception.get()?.let { throw it }
        assertNull(exception.get())
    }
}