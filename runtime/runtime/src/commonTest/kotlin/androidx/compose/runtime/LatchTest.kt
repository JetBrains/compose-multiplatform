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

package androidx.compose.runtime

import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeout
import kotlin.test.Test
import kotlin.test.assertTrue

@ExperimentalCoroutinesApi
class LatchTest {
    @Test
    fun openDoesntSuspend() = runTest {
        val latch = Latch()
        assertTrue(latch.isOpen, "latch open after construction")

        val awaiter = launch(start = CoroutineStart.UNDISPATCHED) { latch.await() }
        assertTrue(awaiter.isCompleted, "await did not suspend")
    }

    @Test
    fun closedSuspendsReleasesAll() = runTest {
        val latch = Latch()
        latch.closeLatch()
        assertTrue(!latch.isOpen, "latch.isOpen after close")

        val awaiters = (1..5).map { launch(start = CoroutineStart.UNDISPATCHED) { latch.await() } }
        assertTrue("all awaiters still active") { awaiters.all { it.isActive } }

        latch.openLatch()
        withTimeout(500) {
            awaiters.map { it.join() }
        }
    }
}
