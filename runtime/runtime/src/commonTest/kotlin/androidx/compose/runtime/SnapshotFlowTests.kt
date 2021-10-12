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

package androidx.compose.runtime

import androidx.compose.runtime.snapshots.Snapshot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.plus
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.yield
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
@Suppress("RemoveExplicitTypeArguments")
class SnapshotFlowTests {
    @Test
    fun observeBasicChanges() = _runBlocking {
        var state by mutableStateOf(1)
        var result = 0

        // Use Dispatchers.Unconfined to cause the observer to run immediately for this test,
        // both here and when we apply a change.
        val collector = snapshotFlow { state * 2 }
            .onEach { result = it }
            .launchIn(this + Dispatchers.Unconfined)

        assertEquals(2, result, "value after initial run")

        Snapshot.withMutableSnapshot {
            state = 5
        }

        assertEquals(10, result, "value after snapshot update")

        collector.cancel()
    }

    @Test
    fun coalesceChanges() = _runBlocking {
        var state by mutableStateOf(1)
        var runCount = 0

        // This test uses the runTest single-threaded dispatcher for observation, which means
        // we don't flush changes to the observer until we yield() intentionally.
        val collector = snapshotFlow { state }
            .onEach { runCount++ }
            .launchIn(this)

        assertEquals(0, runCount, "initial value - snapshot collector hasn't run yet")
        yield()
        assertEquals(1, runCount, "snapshot collector initial run")

        Snapshot.withMutableSnapshot { state++ }
        yield()

        assertEquals(2, runCount, "made one change")

        Snapshot.withMutableSnapshot { state++ }
        Snapshot.withMutableSnapshot { state++ }
        yield()

        assertEquals(3, runCount, "coalesced two changes")

        collector.cancel()
    }

    @Test
    fun ignoreUnrelatedChanges() = _runBlocking {
        val state by mutableStateOf(1)
        var unrelatedState by mutableStateOf(1)
        var runCount = 0

        // This test uses the runTest single-threaded dispatcher for observation, which means
        // we don't flush changes to the observer until we yield() intentionally.
        val collector = snapshotFlow { state }
            .onEach { runCount++ }
            .launchIn(this)
        yield()

        assertEquals(1, runCount, "initial run")

        Snapshot.withMutableSnapshot { unrelatedState++ }
        yield()

        assertEquals(1, runCount, "after changing unrelated state")

        collector.cancel()
    }
}
