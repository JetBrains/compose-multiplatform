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

@file:OptIn(InternalComposeApi::class)

package androidx.compose.runtime.snapshots

import androidx.compose.runtime.InternalComposeApi
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.Snapshot.Companion.openSnapshotCount
import kotlin.test.Ignore
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class DerivedSnapshotStateTests {

    @Test
    fun aStateFreeCalculationCanBeUsed() {
        val a = derivedStateOf { 10 }
        assertEquals(10, a.value)
    }

    @Test
    fun theCalculationIsCached() {
        var runs = 0
        var i = 0
        val a = derivedStateOf { runs++; i }
        assertEquals(0, runs, "The calculation is run only when the value is first requested")
        i++
        assertEquals(1, a.value, "The calculation is run only when the value is first requested")
        i++
        assertEquals(1, a.value, "The calculation is run only once")
    }

    @Test
    fun statesCanBeUsedInGlobalSnapshot() {
        val a = mutableStateOf(1)
        val b = mutableStateOf(10)
        val c = derivedStateOf { a.value + b.value }
        assertEquals(11, c.value)
        a.value += 1
        assertEquals(12, c.value)
        b.value += 10
        assertEquals(22, c.value)
    }

    @Test
    fun statesCanBeUsedInSnapshot() {
        val a = mutableStateOf(1)
        val b = mutableStateOf(10)
        val c = derivedStateOf { a.value + b.value }
        val snapshot = Snapshot.takeMutableSnapshot()
        try {
            assertEquals(11, c.value)
            a.value += 1
            assertEquals(12, c.value)
            b.value += 10
            assertEquals(22, c.value)
        } finally {
            snapshot.dispose()
        }
    }

    @Test
    fun snapshotsAreIsolatedFromGlobalChanges() {
        var state by mutableStateOf(0)
        val derived by derivedStateOf { state }
        val snapshot = Snapshot.takeSnapshot()
        try {
            state = 1
            assertEquals(1, state)
            assertEquals(1, derived)
            assertEquals(0, snapshot.enter { state })
            assertEquals(0, snapshot.enter { derived })
        } finally {
            snapshot.dispose()
        }
    }

    @Test
    fun mutableSnapshotsCanBeApplied() {
        var state by mutableStateOf(0)
        val derived by derivedStateOf { state }
        val snapshot = Snapshot.takeMutableSnapshot()
        try {
            snapshot.enter {
                assertEquals(0, state)
                assertEquals(0, derived)
                state = 1
                assertEquals(1, state)
                assertEquals(1, derived)
            }
            assertEquals(0, state)
            assertEquals(0, derived)
            snapshot.apply().check()
            assertEquals(1, state)
            assertEquals(1, derived)
        } finally {
            snapshot.dispose()
        }

        // The same thing can be done with an atomic block
        atomic {
            assertEquals(1, state)
            assertEquals(1, derived)
            state = 2
            assertEquals(2, state)
            assertEquals(2, derived)
        }
        assertEquals(2, state)
        assertEquals(2, derived)
    }

    @Test
    @Ignore // "b/169406779: Flaky test"
    fun multipleSnapshotsAreIsolatedAndCanBeApplied() {
        val count = 2
        val state = MutableList(count) { mutableStateOf(0) }
        val derived = state.map { derivedStateOf { it.value } }

        // Create count snapshots
        val snapshots = MutableList(count) { Snapshot.takeMutableSnapshot() }
        try {
            repeat(count) {
                assertEquals(0, state[it].value)
                assertEquals(0, derived[it].value)
            }

            snapshots.forEachIndexed { index, snapshot ->
                snapshot.enter { state[index].value = index }
            }

            // Ensure the modifications in snapshots are not visible to global
            repeat(count) {
                assertEquals(0, state[it].value)
                assertEquals(0, derived[it].value)
            }

            // Ensure snapshots can see their own value but no other changes
            repeat(count) { index ->
                snapshots[index].enter {
                    repeat(count) {
                        if (it != index) assertEquals(0, state[it].value)
                        else assertEquals(it, state[it].value)
                        if (it != index) assertEquals(0, derived[it].value)
                        else assertEquals(it, derived[it].value)
                    }
                }
            }

            // Apply all the snapshots
            repeat(count) {
                snapshots[it].apply().check()
            }

            // Global should now be able to see all changes
            repeat(count) {
                assertEquals(it, state[it].value)
                assertEquals(it, derived[it].value)
            }
        } finally {
            // Dispose the snapshots
            snapshots.forEach { it.dispose() }
        }
    }

    @Test
    fun stateReadsCanBeObserved() {
        val state = mutableStateOf(0)
        val derived = derivedStateOf { state.value }

        var readCount = 0
        val readStates = mutableSetOf<Any>()
        val snapshot = Snapshot.takeSnapshot {
            readCount++
            readStates.add(it)
        }
        try {

            val result = snapshot.enter { derived.value }

            assertEquals(0, result)
            // 1 for derived, 1 for state
            assertEquals(2, readStates.size)
            // NOTE: the first calculation will cause two reads of all dependencies: one for the
            // calculation, and one for the hash calculation, 3 reads total
            assertEquals(3, readCount)
            assertEquals(true, readStates.contains(state))
            assertEquals(true, readStates.contains(derived))
        } finally {
            snapshot.dispose()
        }
    }

    @Test
    fun stateReadsCanBeObservedEvenIfCached() {
        val state = mutableStateOf(0)
        val derived = derivedStateOf { state.value }
        assertEquals(0, derived.value)

        val readStates = mutableListOf<Any>()
        val snapshot = Snapshot.takeSnapshot {
            readStates.add(it)
        }
        try {

            val result = snapshot.enter { derived.value }

            assertEquals(0, result)
            // 1 for derived, 1 for state
            assertEquals(2, readStates.size)
            assertEquals(derived, readStates[0])
            assertEquals(state, readStates[1])
        } finally {
            snapshot.dispose()
        }
    }

    @Test
    fun nullResultIsCached() {
        var runs = 0
        val a = derivedStateOf { runs++; null }
        assertNull(a.value)
        assertEquals(1, runs)
        assertNull(a.value)
        assertEquals(1, runs)
    }

    private var count = 0

    @BeforeTest
    fun recordOpenSnapshots() {
        count = openSnapshotCount()
    }

    // Validate that the tests do not change the number of open snapshots
    @AfterTest
    fun validateOpenSnapshots() {
        assertEquals(count, openSnapshotCount())
    }
}