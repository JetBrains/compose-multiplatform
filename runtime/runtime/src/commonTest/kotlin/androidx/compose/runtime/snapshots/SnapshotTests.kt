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

import androidx.compose.runtime.ExperimentalComposeApi
import androidx.compose.runtime.InternalComposeApi
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.neverEqualPolicy
import androidx.compose.runtime.referentialEqualityPolicy
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.Snapshot.Companion.current
import androidx.compose.runtime.snapshots.Snapshot.Companion.openSnapshotCount
import androidx.compose.runtime.snapshots.Snapshot.Companion.takeMutableSnapshot
import androidx.compose.runtime.snapshots.Snapshot.Companion.takeSnapshot
import androidx.compose.runtime.structuralEqualityPolicy
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotSame
import kotlin.test.assertSame
import kotlin.test.assertTrue
import kotlin.test.fail

class SnapshotTests {
    @Test
    fun aSnapshotCanBeCreated() {
        val snapshot = Snapshot.takeSnapshot()
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
        val snapshot = Snapshot.takeSnapshot()
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
        val snapshot = Snapshot.takeMutableSnapshot()
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
        val snapshots = MutableList(count) { Snapshot.takeMutableSnapshot() }
        try {
            snapshots.forEachIndexed { index, snapshot ->
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
        val snapshot = Snapshot.takeSnapshot {
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
        val snapshot = Snapshot.takeMutableSnapshot { write ->
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
        val snapshot = Snapshot.takeMutableSnapshot()
        try {
            snapshot.enter {
                state.value = 2
            }
            assertEquals(null, observedSnapshot)
            snapshot.apply().check()
            assertEquals(snapshot, observedSnapshot)
        } finally {
            snapshot.dispose()
            unregister.dispose()
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
            unregister.dispose()
        }
    }

    @Test
    fun aNestedSnapshotCanBeTaken() {
        val state = mutableStateOf(0)

        val snapshot = Snapshot.takeSnapshot()
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
        val snapshot = Snapshot.takeMutableSnapshot()
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
        val snapshot = Snapshot.takeMutableSnapshot()
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
        val snapshot = Snapshot.takeMutableSnapshot()
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
        val snapshot = Snapshot.takeMutableSnapshot()
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
        val snapshot = Snapshot.takeMutableSnapshot()
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
        val snapshot = Snapshot.takeSnapshot { read.add(it) }
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
        val snapshot = Snapshot.takeSnapshot { parentRead.add(it) }
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
        val snapshot = Snapshot.takeMutableSnapshot { written.add(it) }
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
        val nestedWritten = HashSet<Any>()
        val snapshot = Snapshot.takeMutableSnapshot { parentWritten.add(it) }
        try {
            val nested = snapshot.takeNestedMutableSnapshot { nestedWritten.add(it) }
            try {
                nested.enter { state.value = 2 }
            } finally {
                nested.dispose()
            }
        } finally {
            snapshot.dispose()
        }
        assertTrue(parentWritten.contains(state))
        assertTrue(nestedWritten.contains(state))
    }

    @Test
    fun creatingAStateInANestedSnapshotAndMutatingInParentApplies() {
        val states = mutableListOf<MutableState<Int>>()
        val snapshot = Snapshot.takeMutableSnapshot()
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
        val snapshot1 = Snapshot.takeMutableSnapshot()
        val snapshot2 = Snapshot.takeMutableSnapshot()
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
        val snapshot1 = Snapshot.takeMutableSnapshot()
        val snapshot2 = Snapshot.takeMutableSnapshot()
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
        val snapshot1 = Snapshot.takeMutableSnapshot()
        val snapshot2 = Snapshot.takeMutableSnapshot()
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

    @Test
    fun stateUsingNeverEqualPolicyCannotBeMerged() {
        assertFailsWith(SnapshotApplyConflictException::class) {
            val state = mutableStateOf(0, neverEqualPolicy())
            val snapshot1 = Snapshot.takeMutableSnapshot()
            val snapshot2 = Snapshot.takeMutableSnapshot()
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

    @Test
    fun toStringOfMutableStateDoesNotTriggerReadObserver() {
        val state = mutableStateOf(0)
        val normalReads = readsOf {
            state.value
        }
        assertEquals(1, normalReads)
        val toStringReads = readsOf {
            state.toString()
        }
        assertEquals(0, toStringReads)
    }

    @Test
    fun toStringOfDerivedStateDoesNotTriggerReadObservers() {
        val state = mutableStateOf(0)
        val derived = derivedStateOf { state.value + 1 }
        val toStringReads = readsOf {
            derived.toString()
        }
        assertEquals(0, toStringReads)
    }

    @Test
    fun toStringValueOfMutableState() {
        val state = mutableStateOf(10)
        assertEquals("MutableState(value=10)@${state.hashCode()}", state.toString())
        state.value = 20
        assertEquals("MutableState(value=20)@${state.hashCode()}", state.toString())
    }

    @Test
    fun toStringValueOfDerivedState() {
        val state = mutableStateOf(10)
        val derivedState = derivedStateOf { state.value + 10 }
        val hash = derivedState.hashCode()
        assertEquals("DerivedState(value=<Not calculated>)@$hash", derivedState.toString())
        assertEquals(20, derivedState.value)
        assertEquals("DerivedState(value=20)@$hash", derivedState.toString())
        state.value = 20
        assertEquals("DerivedState(value=<Not calculated>)@$hash", derivedState.toString())
        assertEquals(30, derivedState.value)
        assertEquals("DerivedState(value=30)@$hash", derivedState.toString())
    }

    @Test // Regression test for b/181162478
    fun nestedSnapshotsAreIsolated() {
        var state1 by mutableStateOf(0)
        var state2 by mutableStateOf(0)
        val parent = Snapshot.takeMutableSnapshot()
        parent.enter { state1 = 1 }
        Snapshot.withMutableSnapshot { state2 = 2 }
        val snapshot = parent.takeNestedSnapshot()
        parent.apply().check()
        parent.dispose()
        snapshot.enter {
            // Should se the change of state1
            assertEquals(1, state1)

            // But not the state change of state2
            assertEquals(0, state2)
        }
        snapshot.dispose()
    }

    @Test // Regression test for b/181159260
    fun readOnlySnapshotValidAfterParentDisposed() {
        var state by mutableStateOf(0)
        val parent = Snapshot.takeMutableSnapshot()
        parent.enter { state = 1 }
        val child = parent.takeNestedSnapshot()
        parent.apply().check()
        parent.dispose()
        child.enter { assertEquals(1, state) }
        val reads = mutableListOf<Any>()
        val nestedChild = child.takeNestedSnapshot { reads.add(it) }
        nestedChild.enter { assertEquals(1, state) }
        child.dispose()
        nestedChild.dispose()
    }

    @Test // Regression test for b/193006595
    fun transparentSnapshotAdvancesCorrectly() {
        val state = Snapshot.observe({}) {
            // In a transparent snapshot, advance the global snapshot
            Snapshot.notifyObjectsInitialized()

            // Create an apply an object in a snapshot
            val state = atomic {
                mutableStateOf(0)
            }

            // Ensure that the object can be accessed in the observer
            assertEquals(0, state.value)

            state
        }

        // Ensure that the object can be accessed globally.
        assertEquals(0, state.value)
    }

    // Regression test for b/199921314
    // This test lifted directly from the bug reported by chrnie@foxmail.com, modified and formatted
    // to avoid lint warnings.
    @Test
    fun testTakeSnapshotNested() {
        assertFailsWith<IllegalStateException> {
            Snapshot.withMutableSnapshot {
                val expectReadonlySnapshot = Snapshot.takeSnapshot()
                try {
                    expectReadonlySnapshot.enter {
                        var state by mutableStateOf(0)

                        // expect throw IllegalStateException:Cannot modify a state object in a
                        // read-only snapshot
                        state = 1

                        assertEquals(1, state)
                    }
                } finally {
                    expectReadonlySnapshot.dispose()
                }
            }
        }
    }

    @Test // Regression test for b/200575924
    // Test copied from b/200575924 bu chrnie@foxmail.com
    fun nestedMutableSnapshotCanNotSeeOtherSnapshotChange() {
        var state by mutableStateOf(0)

        val snapshot1 = Snapshot.takeMutableSnapshot()
        val snapshot2 = Snapshot.takeMutableSnapshot()
        try {
            snapshot2.enter {
                state = 1
            }

            snapshot1.enter {
                Snapshot.withMutableSnapshot {
                    assertEquals(0, state)
                }
            }
        } finally {
            snapshot1.dispose()
            snapshot2.dispose()
        }
    }

    @Test // Regression test for b/200575924
    // Test copied from b/200575924 by chrnie@foxmail.com
    fun nestedSnapshotCanNotSeeOtherSnapshotChange() {
        var state by mutableStateOf(0)

        val snapshot1 = Snapshot.takeMutableSnapshot()
        val snapshot2 = Snapshot.takeMutableSnapshot()
        try {
            snapshot2.enter {
                state = 1
            }

            snapshot1.enter {
                val nestedSnapshot = Snapshot.takeSnapshot()
                try {
                    nestedSnapshot.enter {
                        assertEquals(0, state)
                    }
                } finally {
                    nestedSnapshot.dispose()
                }
            }
        } finally {
            snapshot1.dispose()
            snapshot2.dispose()
        }
    }

    @Test
    fun canTakeNestedSnapshotsFromApplyObserver() {
        var takenSnapshot: Snapshot? = null
        val observer = Snapshot.registerApplyObserver { _, snapshot ->
            if (takenSnapshot != null) error("already took a nested snapshot")
            takenSnapshot = snapshot.takeNestedSnapshot()
        }

        try {
            var state by mutableStateOf("initial")
            Snapshot.withMutableSnapshot {
                state = "before observer snapshot"
            }

            state = "after observer snapshot"

            val observerSnapshot = takenSnapshot ?: fail("snapshot was not taken by observer")

            observerSnapshot.enter {
                assertEquals("before observer snapshot", state)
            }
        } finally {
            observer.dispose()
            takenSnapshot?.dispose()
        }
    }

    @Test
    fun canTakeNestedMutableSnapshotsFromApplyObserver() {
        var takenSnapshot: MutableSnapshot? = null
        val observer = Snapshot.registerApplyObserver { _, snapshot ->
            if (takenSnapshot != null) error("already took a nested snapshot")
            takenSnapshot = (snapshot as? MutableSnapshot)
                ?.takeNestedMutableSnapshot()
                ?: error("Applied snapshot was not mutable")
        }

        try {
            var state by mutableStateOf("initial")
            Snapshot.withMutableSnapshot {
                state = "before observer snapshot"
            }

            state = "after observer snapshot"

            val observerSnapshot = takenSnapshot ?: fail("snapshot was not taken by observer")

            observerSnapshot.enter {
                assertEquals("before observer snapshot", state)
                state = "change made by observer snapshot"
            }

            assertFalse(observerSnapshot.apply().succeeded,
                "applying observer snapshot with conflicting change")
        } finally {
            observer.dispose()
            takenSnapshot?.dispose()
        }
    }

    @Test
    fun cannotTakeSnapshotOfClosedSnapshotAfterApplyReturns() {
        val snapshot = takeMutableSnapshot()
        @Suppress("ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE")
        var state by mutableStateOf("initial")

        try {
            snapshot.enter { state = "mutated" }
            snapshot.apply().check()
            snapshot.takeNestedSnapshot().dispose()
            fail("taking a nested snapshot of an applied snapshot did not throw")
        } catch (ise: IllegalStateException) {
            // expected
        } finally {
            snapshot.dispose()
        }
    }

    @Test
    fun testRecordsAreReusedCorrectly() {
        val value = mutableStateOf(0)
        Snapshot.withMutableSnapshot { value.value++ }
        val mutable1 = takeMutableSnapshot()
        val readable1 = takeSnapshot()
        mutable1.enter { value.value++ }
        mutable1.apply().check()
        Snapshot.withMutableSnapshot { value.value++ }
        val v = readable1.enter { value.value }
        assertEquals(v, 1)
        readable1.dispose()
        mutable1.dispose()
    }

    @OptIn(ExperimentalComposeApi::class)
    @Test
    fun testUnsafeSnapshotEnterAndLeave() {
        val snapshot = takeSnapshot()
        try {
            val oldSnapshot = snapshot.unsafeEnter()
            try {
                assertSame(snapshot, current, "expected taken snapshot to be current")
            } finally {
                snapshot.unsafeLeave(oldSnapshot)
            }
            assertNotSame(snapshot, current, "expected taken snapshot not to be current")
        } finally {
            snapshot.dispose()
        }
    }

    @OptIn(ExperimentalComposeApi::class)
    @Test
    fun testUnsafeSnapshotLeaveThrowsIfNotCurrent() {
        val snapshot = takeSnapshot()
        try {
            try {
                snapshot.unsafeLeave(null)
                fail("unsafeLeave should have thrown")
            } catch (ise: IllegalStateException) {
                // expected
            }
        } finally {
            snapshot.dispose()
        }
    }

    private var count = 0

    @BeforeTest
    fun recordOpenSnapshots() {
        count = openSnapshotCount()
    }

    // Validate that the tests do not change the number of open snapshots
    @AfterTest
    fun validateOpenSnapshots() {
        assertEquals(count, openSnapshotCount(), "A snapshot was not disposed correctly")
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
        removeObserver.dispose()
    }
    return changes
}

internal fun readsOf(block: () -> Unit): Int {
    var reads = 0
    val snapshot = Snapshot.takeSnapshot(readObserver = { reads++ })
    try {
        snapshot.enter(block)
    } finally {
        snapshot.dispose()
    }
    return reads
}

internal inline fun <T> atomic(block: () -> T): T {
    val snapshot = Snapshot.takeMutableSnapshot()
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
    val snapshot = Snapshot.takeMutableSnapshot()
    snapshot.enter(block)
    return snapshot
}
