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

package androidx.compose.runtime.snapshots

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlin.concurrent.thread
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SnapshotStateObserverTestsJvm {

    @Test // regression test for 192677711
    fun tryToReproduceRaceCondition() {
        var running = true
        var threadException: Exception? = null
        try {
            thread {
                try {
                    while (running) {
                        Snapshot.sendApplyNotifications()
                    }
                } catch (e: Exception) {
                    threadException = e
                }
            }

            for (i in 1..1000) {
                val state1 by mutableStateOf(0)
                var state2 by mutableStateOf(true)
                val observer = SnapshotStateObserver({}).apply {
                    start()
                }
                repeat(1000) {
                    observer.observeReads(Unit, {}) {
                        @Suppress("UNUSED_EXPRESSION")
                        state1
                        if (state2) {
                            state2 = false
                        }
                    }
                }
                assertNull(threadException)
            }
        } finally {
            running = false
        }
        assertNull(threadException)
    }

    @Test // regression test for 192677711, second case
    fun tryToReproduceSecondRaceCondtion() {
        var running = true
        var threadException: Exception? = null
        try {
            thread {
                try {
                    while (running) {
                        Snapshot.sendApplyNotifications()
                    }
                } catch (e: Exception) {
                    threadException = e
                }
            }

            for (i in 1..1000) {
                val state1 by mutableStateOf(0)
                var state2 by mutableStateOf(true)
                val observer = SnapshotStateObserver({}).apply {
                    start()
                }
                observer.observeReads(Unit, {}) {
                    repeat(1000) {
                        @Suppress("UNUSED_EXPRESSION")
                        state1
                        if (state2) {
                            state2 = false
                        }
                    }
                }
                assertNull(threadException)
            }
        } finally {
            running = false
        }
        assertNull(threadException)
    }

    @Test
    fun stateChangeTriggersUpdateWhenDerivedStateIsUsedRightAfter() {
        val data = "data"
        var changes = 0
        val derivedState = derivedStateOf { 0 }

        runSimpleTest { stateObserver, state ->
            stateObserver.observeReads(data, { _ -> changes++ }) {
                if (state.value == 0) {
                    state.value++
                }
                // derived state read internally calls notifyObjectsInitialized() which triggers
                // the first onValueChanged invocation.
                derivedState.value
            }
        }

        assertEquals(2, changes)
    }

    @Test
    fun nestedObservationIsClearingThePreviousScopesBeforeReexecuting() {
        val data1 = "data1"
        val data2 = "data2"
        var changes = 0

        val stateObserver = SnapshotStateObserver { it() }
        val state1 = mutableStateOf(true)
        val state2 = mutableStateOf(0)
        val onChanged1: (String) -> Unit = { }
        val onChanged2: (String) -> Unit = { _ -> changes++ }

        fun runObservedBlocks() {
            // we have nested observations
            stateObserver.observeReads(data1, onChanged1) {
                stateObserver.observeReads(data2, onChanged2) {
                    if (state1.value) {
                        state2.value++
                    }
                }
            }
        }

        try {
            stateObserver.start()
            Snapshot.notifyObjectsInitialized()

            runObservedBlocks()
            assertEquals(0, changes)

            state1.value = false
            Snapshot.sendApplyNotifications()
            runObservedBlocks()
            assertEquals(1, changes)

            state2.value++
            Snapshot.sendApplyNotifications()
            assertEquals(1, changes)
        } finally {
            stateObserver.stop()
        }
    }

    @Test
    fun readingNestedDerivedStateFromAnImmediatelyRerunningObserver() {
        var changes = 0

        val state = mutableStateOf(0)
        val derivedState = derivedStateOf { state.value }
        val nestedDerivedState = derivedStateOf { derivedState.value }

        val stateObserver = SnapshotStateObserver { it() }
        try {
            stateObserver.start()
            Snapshot.notifyObjectsInitialized()

            val observer = object : (Any) -> Unit {
                override fun invoke(affected: Any) {
                    assertEquals(this, affected)
                    assertEquals(0, changes)
                    changes++
                    readWithObservation()
                }

                fun readWithObservation() {
                    stateObserver.observeReads(this, this) {
                        // read the value
                        nestedDerivedState.value
                    }
                }
            }
            // read with 0
            observer.readWithObservation()
            // increase to 1
            state.value++
            Snapshot.sendApplyNotifications()

            assertEquals(1, changes)
        } finally {
            stateObserver.stop()
        }
    }

    private fun runSimpleTest(
        block: (modelObserver: SnapshotStateObserver, data: MutableState<Int>) -> Unit
    ) {
        val stateObserver = SnapshotStateObserver { it() }
        val state = mutableStateOf(0)
        try {
            stateObserver.start()
            Snapshot.notifyObjectsInitialized()
            block(stateObserver, state)
            state.value++
            Snapshot.sendApplyNotifications()
        } finally {
            stateObserver.stop()
        }
    }
}
