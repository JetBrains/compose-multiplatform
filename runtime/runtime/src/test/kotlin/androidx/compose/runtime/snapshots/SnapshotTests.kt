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

@file:OptIn(InternalComposeApi::class, ExperimentalComposeApi::class)

package androidx.compose.runtime.snapshots

import androidx.compose.runtime.ExperimentalComposeApi
import androidx.compose.runtime.InternalComposeApi
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.neverEqualPolicy
import androidx.compose.runtime.referentialEqualityPolicy
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.Snapshot.Companion.openSnapshotCount
import androidx.compose.runtime.structuralEqualityPolicy
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SnapshotTests {
    @Test
    fun aSnapshotCanBeCreated() {
        val snapshot = takeSnapshot()
        snapshot.dispose()
    }

    @Test
    fun aMutableStateCanBeCreated() {
        mutableStateOf(0)
    }

    @Test
    fun aMutableStateCanBeReadOutsideASnapshot() {
        val state by mutableStateOf(0)
        assertEquals(0, state)
    }

    @Test
    fun aMutableStateCanBeWrittenToOutsideASnapshot() {
        var state by mutableStateOf(0)
        assertEquals(0, state)
        state = 1
        assertEquals(1, state)
    }

    @Test
    fun snapshotsAreIsolatedFromGlobalChanges() {
        var state by mutableStateOf(0)
        val snapshot = takeSnapshot()
        try {
            state = 1
            assertEquals(1, state)
            assertEquals(0, snapshot.enter { state })
        } finally {
            snapshot.dispose()
        }
    }

    @Test
    fun mutableSnapshotsCanBeApplied() {
        var state by mutableStateOf(0)
        val snapshot = takeMutableSnapshot()
        try {
            snapshot.enter {
                assertEquals(0, state)
                state = 1
                assertEquals(1, state)
            }
            assertEquals(0, state)
            snapshot.apply().check()
            assertEquals(1, state)
        } finally {
            snapshot.dispose()
        }

        // The same thing can be done with an atomic block
        atomic {
            assertEquals(1, state)
            state = 2
            assertEquals(2, state)
        }
        assertEquals(2, state)
    }

    @Test
    fun multipleSnapshotsAreIsolatedAndCanBeApplied() {
        val count = 2
        val state = MutableList(count) { mutableStateOf(0) }

        // Create count snapshots
        val snapshots = MutableList(count) { takeMutableSnapshot() }
        try {
            snapshots.forEachIndexed() { index, snapshot ->
                snapshot.enter { state[index].value = index }
            }

            // Ensure the modifications in snapshots are not visible to global
            repeat(count) {
                assertEquals(0, state[it].value)
            }

            // Ensure snapshots can see their own value but no other changes
            repeat(count) { index ->
                snapshots[index].enter {
                    repeat(count) {
                        if (it != index) assertEquals(0, state[it].value)
                        else assertEquals(it, state[it].value)
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
            }
        } finally {
            // Dispose the snapshots
            snapshots.forEach { it.dispose() }
        }
    }

    @Test
    fun applyingASnapshotThatCollidesWithAGlobalChangeWillFail() {
        var state by mutableStateOf(0)

        val snapshot = snapshot { state = 1 }
        try {
            state = 2
            assertTrue(snapshot.apply() is SnapshotApplyResult.Failure)
            assertEquals(2, state)
        } finally {
            snapshot.dispose()
        }
    }

    @Test
    fun applyingCollidingSnapshotsWillFail() {
        var state by mutableStateOf(0)
        val snapshot1 = snapshot { state = 1 }
        val snapshot2 = snapshot { state = 2 }
        try {
            assertEquals(0, state)
            snapshot1.apply().check()
            assertEquals(1, state)
            assertTrue(snapshot2.apply() is SnapshotApplyResult.Failure)
            assertEquals(1, state)
        } finally {
            snapshot1.dispose()
            snapshot2.dispose()
        }
    }

    @Test
    fun stateReadsCanBeObserved() {
        val state = mutableStateOf(0)

        val readStates = mutableListOf<Any>()
        val snapshot = takeSnapshot {
            readStates.add(it)
        }
        try {

            val result = snapshot.enter { state.value }

            assertEquals(0, result)
            assertEquals(1, readStates.size)
            assertEquals(state, readStates[0])
        } finally {
            snapshot.dispose()
        }
    }

    @Test
    fun stateWritesCanBeObserved() {
        val state = mutableStateOf(0)
        val writtenStates = mutableListOf<Any>()
        val snapshot = takeMutableSnapshot { write ->
            writtenStates.add(write)
        }
        try {
            snapshot.enter {
                assertEquals(0, writtenStates.size)
                state.value = 2
                assertEquals(1, writtenStates.size)
            }
        } finally {
            snapshot.dispose()
        }
        assertEquals(1, writtenStates.size)
        assertEquals(state, writtenStates[0])
    }

    @Test
    fun appliesCanBeObserved() {
        val state = mutableStateOf(0)
        var observedSnapshot: Snapshot? = null
        val unregister = Snapshot.registerApplyObserver { changed, snapshot ->
            assertTrue(state in changed)
            observedSnapshot = snapshot
        }
        val snapshot = takeMutableSnapshot()
        try {
            snapshot.enter {
                state.value = 2
            }
            assertEquals(null, observedSnapshot)
            snapshot.apply().check()
            assertEquals(snapshot, observedSnapshot)
        } finally {
            snapshot.dispose()
            unregister()
        }
    }

    @Test
    fun globalChangesCanBeObserved() {
        val state = mutableStateOf(0)

        Snapshot.notifyObjectsInitialized()

        var applyObserved = false
        val unregister = Snapshot.registerApplyObserver { changed, _ ->
            assertTrue(state in changed)
            applyObserved = true
        }
        try {
            state.value = 2

            // Nothing should have been observed yet.
            assertFalse(applyObserved)

            // Advance the global snapshot to send apply notifications
            Snapshot.sendApplyNotifications()

            assertTrue(applyObserved)
        } finally {
            unregister()
        }
    }

    @Test
    fun aNestedSnapshotCanBeTaken() {
        val state = mutableStateOf(0)

        val snapshot = takeSnapshot()
        try {
            val nested = snapshot.takeNestedSnapshot()
            try {
                state.value = 1

                assertEquals(0, nested.enter { state.value })
            } finally {
                nested.dispose()
            }
        } finally {
            snapshot.dispose()
        }
    }

    @Test
    fun aNestedMutableSnapshotCanBeTaken() {
        val state = mutableStateOf(0)
        val snapshot = takeMutableSnapshot()
        try {
            snapshot.enter { state.value = 1 }
            val nested = snapshot.takeNestedMutableSnapshot()
            try {
                nested.enter { state.value = 2 }

                assertEquals(0, state.value)
                assertEquals(1, snapshot.enter { state.value })
                assertEquals(2, nested.enter { state.value })
            } finally {
                nested.dispose()
            }
        } finally {
            snapshot.dispose()
        }
    }

    @Test
    fun aNestedSnapshotOfAMutableSnapshotCanBeTaken() {
        val state = mutableStateOf(0)
        val snapshot = takeMutableSnapshot()
        try {
            snapshot.enter { state.value = 1 }
            val nested = snapshot.takeNestedSnapshot()
            try {
                snapshot.enter { state.value = 2 }

                assertEquals(0, state.value)
                assertEquals(2, snapshot.enter { state.value })
                assertEquals(1, nested.enter { state.value })
            } finally {
                nested.dispose()
            }
        } finally {
            snapshot.dispose()
        }
    }

    @Test
    fun aNestedMutableSnapshotCanBeAppliedToItsParent() {
        val state = mutableStateOf(0)
        val snapshot = takeMutableSnapshot()
        try {
            snapshot.enter { state.value = 1 }
            val nested = snapshot.takeNestedMutableSnapshot()
            try {

                nested.enter { state.value = 2 }
                assertEquals(0, state.value)
                assertEquals(1, snapshot.enter { state.value })
                assertEquals(2, nested.enter { state.value })

                nested.apply().check()
            } finally {
                nested.dispose()
            }
            assertEquals(0, state.value)
            assertEquals(2, snapshot.enter { state.value })

            snapshot.apply().check()
        } finally {
            snapshot.dispose()
        }

        assertEquals(2, state.value)
    }

    @Test
    fun aParentSnapshotCanAccessAStatObjectedCreateByANestedSnapshot() {
        val snapshot = takeMutableSnapshot()
        val state = try {
            val nested = snapshot.takeNestedMutableSnapshot()
            val state = try {
                nested.notifyObjectsInitialized()
                val state = nested.enter { mutableStateOf(1) }
                assertEquals(1, nested.enter { state.value })
                nested.apply().check()
                state
            } finally {
                nested.dispose()
            }
            assertEquals(1, snapshot.enter { state.value })
            snapshot.apply().check()
            state
        } finally {
            snapshot.dispose()
        }
        assertEquals(1, state.value)
    }

    @Test
    fun atomicChangesNest() {
        val state = mutableStateOf(0)
        atomic {
            state.value = 1
            atomic {
                state.value = 2

                assertEquals(0, Snapshot.global { state.value })
            }
            assertEquals(2, state.value)
            assertEquals(0, Snapshot.global { state.value })
        }
        assertEquals(2, state.value)
    }

    @Test
    fun siblingNestedMutableSnapshotsAreIsolatedFromEachOther() {
        val state = mutableStateOf(0)
        val snapshot = takeMutableSnapshot()
        try {
            snapshot.enter { state.value = 10 }

            val nested1 = snapshot.takeNestedMutableSnapshot()
            try {
                nested1.enter { state.value = 1 }
                val nested2 = snapshot.takeNestedMutableSnapshot()
                try {
                    nested2.enter { state.value = 2 }

                    assertEquals(0, state.value)
                    assertEquals(10, snapshot.enter { state.value })
                    assertEquals(1, nested1.enter { state.value })
                    assertEquals(2, nested2.enter { state.value })
                } finally {
                    nested2.dispose()
                }
            } finally {
                nested1.dispose()
            }
        } finally {
            snapshot.dispose()
        }
        assertEquals(0, state.value)
    }

    @Test
    fun readingInANestedSnapshotNotifiesTheParent() {
        val state = mutableStateOf(0)
        val read = HashSet<Any>()
        val snapshot = takeSnapshot { read.add(it) }
        try {
            val nested = snapshot.takeNestedSnapshot()
            try {
                assertEquals(0, nested.enter { state.value })
            } finally {
                nested.dispose()
            }
        } finally {
            snapshot.dispose()
        }
        assertTrue(read.contains(state))
    }

    @Test
    fun readingInANestedSnapshotNotifiesNestedAndItsParent() {
        val state = mutableStateOf(0)
        val parentRead = HashSet<Any>()
        val nestedRead = HashSet<Any>()
        val snapshot = takeSnapshot { parentRead.add(it) }
        try {
            val nested = snapshot.takeNestedSnapshot { nestedRead.add(it) }
            try {
                assertEquals(0, nested.enter { state.value })
            } finally {
                nested.dispose()
            }
        } finally {
            snapshot.dispose()
        }
        assertTrue(parentRead.contains(state))
        assertTrue(nestedRead.contains(state))
    }

    @Test
    fun writingToANestedSnapshotNotifiesTheParent() {
        val state = mutableStateOf(0)
        val written = HashSet<Any>()
        val snapshot = takeMutableSnapshot { written.add(it) }
        try {
            val nested = snapshot.takeNestedMutableSnapshot()
            try {
                nested.enter { state.value = 2 }
            } finally {
                nested.dispose()
            }
        } finally {
            snapshot.dispose()
        }
        assertTrue(written.contains(state))
    }

    @Test
    fun writingToANestedSnapshotNotifiesNestedAndItsParent() {
        val state = mutableStateOf(0)
        val parentWritten = HashSet<Any>()
        val nestedWritted = HashSet<Any>()
        val snapshot = takeMutableSnapshot { parentWritten.add(it) }
        try {
            val nested = snapshot.takeNestedMutableSnapshot { nestedWritted.add(it) }
            try {
                nested.enter { state.value = 2 }
            } finally {
                nested.dispose()
            }
        } finally {
            snapshot.dispose()
        }
        assertTrue(parentWritten.contains(state))
        assertTrue(nestedWritted.contains(state))
    }

    @Test
    fun creatingAStateInANestedSnapshotAndMutatingInParentApplies() {
        val states = mutableListOf<MutableState<Int>>()
        val snapshot = takeMutableSnapshot()
        try {
            val nested = snapshot.takeNestedMutableSnapshot()
            try {
                nested.enter {
                    val state = mutableStateOf(0)
                    states.add(state)
                }
                nested.apply()
            } finally {
                nested.dispose()
            }
            snapshot.enter {
                for (state in states) {
                    state.value++
                }
            }
            snapshot.apply()
        } finally {
            snapshot.dispose()
        }
        for (state in states) {
            assertEquals(1, state.value)
        }
    }

    @Test
    fun snapshotsChangesCanMerge() {
        val state = mutableStateOf(0)
        val snapshot1 = takeMutableSnapshot()
        val snapshot2 = takeMutableSnapshot()
        try {
            // Change the state to the same value in both snapshots
            snapshot1.enter { state.value = 1 }
            snapshot2.enter { state.value = 1 }

            // Still 0 until one of the snapshots is applied
            assertEquals(0, state.value)

            // Apply snapshot 1 should change the value to 1
            snapshot1.apply().check()
            assertEquals(1, state.value)

            // Applying snapshot 2 should succeed because it changed the value to the same value.
            snapshot2.apply().check()
            assertEquals(1, state.value)
        } finally {
            snapshot1.dispose()
            snapshot2.dispose()
        }
    }

    @Test
    fun mergedSnapshotsDoNotRepeatChangeNotifications() {
        val state = mutableStateOf(0)
        val snapshot1 = takeMutableSnapshot()
        val snapshot2 = takeMutableSnapshot()
        try {
            val changes = changesOf(state) {
                snapshot1.enter { state.value = 1 }
                snapshot2.enter { state.value = 1 }
                snapshot1.apply().check()
                snapshot2.apply().check()
            }
            assertEquals(1, changes)
        } finally {
            snapshot1.dispose()
            snapshot2.dispose()
        }
    }

    @Test
    fun statesWithStructuralEqualityPolicyMerge() {
        data class Value(val v1: Int, val v2: Int)
        val state = mutableStateOf(Value(1, 2), structuralEqualityPolicy())
        val snapshot1 = takeMutableSnapshot()
        val snapshot2 = takeMutableSnapshot()
        try {
            snapshot1.enter { state.value = Value(3, 4) }
            snapshot2.enter { state.value = Value(3, 4) }
            snapshot1.apply().check()
            snapshot2.apply().check()
        } finally {
            snapshot1.dispose()
            snapshot2.dispose()
        }
    }

    @Test(expected = SnapshotApplyConflictException::class)
    fun stateUsingNeverEqualPolicyCannotBeMerged() {
        val state = mutableStateOf(0, neverEqualPolicy())
        val snapshot1 = takeMutableSnapshot()
        val snapshot2 = takeMutableSnapshot()
        try {
            snapshot1.enter { state.value = 1 }
            snapshot2.enter { state.value = 1 }
            snapshot1.apply().check()
            snapshot2.apply().check()
        } finally {
            snapshot1.dispose()
            snapshot2.dispose()
        }
    }

    @Test
    fun changingAnEqualityPolicyStateToItsCurrentValueIsNotConsideredAChange() {
        val state = mutableStateOf(0, referentialEqualityPolicy())
        val changes = changesOf(state) {
            state.value = 0
        }
        assertEquals(0, changes)
    }

    @Test
    fun changingANeverEqualPolicyStateToItsCurrentValueIsConsideredAChange() {
        val state = mutableStateOf(0, neverEqualPolicy())
        val changes = changesOf(state) {
            state.value = 0
        }
        assertEquals(1, changes)
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

internal fun <T> changesOf(state: State<T>, block: () -> Unit): Int {
    var changes = 0
    val removeObserver = Snapshot.registerApplyObserver { states, _ ->
        if (states.contains(state)) changes++
    }
    try {
        block()
        Snapshot.sendApplyNotifications()
    } finally {
        removeObserver()
    }
    return changes
}

internal inline fun <T> atomic(block: () -> T): T {
    val snapshot = takeMutableSnapshot()
    val result: T
    try {
        result = snapshot.enter {
            block()
        }
        snapshot.apply().check()
    } finally {
        snapshot.dispose()
    }
    return result
}

internal inline fun snapshot(block: () -> Unit): MutableSnapshot {
    val snapshot = takeMutableSnapshot()
    snapshot.enter(block)
    return snapshot
}
