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

package androidx.compose.runtime.snapshots

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.runtime.snapshots.SnapshotStateObserver
import kotlin.concurrent.thread
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SnapshotStateObserverTests {

    @Test
    fun stateChangeTriggersCallback() {
        val data = "Hello World"
        var changes = 0

        val state = mutableStateOf(0)
        val stateObserver = SnapshotStateObserver { it() }
        try {
            stateObserver.start()

            val onChangeListener: (String) -> Unit = { affected ->
                assertEquals(data, affected)
                assertEquals(0, changes)
                changes++
            }

            stateObserver.observeReads(data, onChangeListener) {
                // read the value
                state.value
            }

            Snapshot.notifyObjectsInitialized()
            state.value++
            Snapshot.sendApplyNotifications()

            assertEquals(1, changes)
        } finally {
            stateObserver.stop()
        }
    }

    @Test
    fun multipleStagesWorksTogether() {
        val strStage1 = "Stage1"
        val strStage2 = "Stage2"
        val strStage3 = "Stage3"
        var stage1Changes = 0
        var stage2Changes = 0
        var stage3Changes = 0
        val stage1Model = mutableStateOf(0)
        val stage2Model = mutableStateOf(0)
        val stage3Model = mutableStateOf(0)

        val onChangeStage1: (String) -> Unit = { affectedData ->
            assertEquals(strStage1, affectedData)
            assertEquals(0, stage1Changes)
            stage1Changes++
        }
        val onChangeStage2: (String) -> Unit = { affectedData ->
            assertEquals(strStage2, affectedData)
            assertEquals(0, stage2Changes)
            stage2Changes++
        }
        val onChangeStage3: (String) -> Unit = { affectedData ->
            assertEquals(strStage3, affectedData)
            assertEquals(0, stage3Changes)
            stage3Changes++
        }
        val stateObserver = SnapshotStateObserver { it() }
        try {
            stateObserver.start()

            stateObserver.observeReads(strStage1, onChangeStage1) {
                stage1Model.value
            }

            stateObserver.observeReads(strStage2, onChangeStage2) {
                stage2Model.value
            }

            stateObserver.observeReads(strStage3, onChangeStage3) {
                stage3Model.value
            }

            Snapshot.notifyObjectsInitialized()

            stage1Model.value++
            stage2Model.value++
            stage3Model.value++

            Snapshot.sendApplyNotifications()

            assertEquals(1, stage1Changes)
            assertEquals(1, stage2Changes)
            assertEquals(1, stage3Changes)
        } finally {
            stateObserver.stop()
        }
    }

    @Test
    fun enclosedStagesCorrectlyObserveChanges() {
        val stage1Info = "stage 1"
        val stage2Info1 = "stage 1 - value 1"
        val stage2Info2 = "stage 2 - value 2"
        var stage1Changes = 0
        var stage2Changes1 = 0
        var stage2Changes2 = 0
        val stage1Data = mutableStateOf(0)
        val stage2Data1 = mutableStateOf(0)
        val stage2Data2 = mutableStateOf(0)

        val onChangeStage1Listener: (String) -> Unit = { affected ->
            assertEquals(affected, stage1Info)
            assertEquals(stage1Changes, 0)
            stage1Changes++
        }
        val onChangeState2Listener: (String) -> Unit = { affected ->
            when (affected) {
                stage2Info1 -> {
                    assertEquals(0, stage2Changes1)
                    stage2Changes1++
                }
                stage2Info2 -> {
                    assertEquals(0, stage2Changes2)
                    stage2Changes2++
                }
                stage1Info -> {
                    error("stage 1 called in stage 2")
                }
            }
        }

        val stateObserver = SnapshotStateObserver { it() }
        try {
            stateObserver.start()

            stateObserver.observeReads(stage2Info1, onChangeState2Listener) {
                stage2Data1.value
                stateObserver.observeReads(stage2Info2, onChangeState2Listener) {
                    stage2Data2.value
                    stateObserver.observeReads(stage1Info, onChangeStage1Listener) {
                        stage1Data.value
                    }
                }
            }

            Snapshot.notifyObjectsInitialized()

            stage2Data1.value++
            stage2Data2.value++
            stage1Data.value++

            Snapshot.sendApplyNotifications()

            assertEquals(1, stage1Changes)
            assertEquals(1, stage2Changes1)
            assertEquals(1, stage2Changes2)
        } finally {
            stateObserver.stop()
        }
    }

    @Test
    fun stateReadTriggersCallbackAfterSwitchingAdvancingGlobalWithinObserveReads() {
        val info = "Hello"
        var changes = 0

        val state = mutableStateOf(0)
        val onChangeListener: (String) -> Unit = { _ ->
            assertEquals(0, changes)
            changes++
        }

        val stateObserver = SnapshotStateObserver { it() }
        try {
            stateObserver.start()

            stateObserver.observeReads(info, onChangeListener) {
                // Create a sub-snapshot
                // this will be done by subcomposition, for example.
                val snapshot = Snapshot.takeMutableSnapshot()
                try {
                    // read the value
                    snapshot.enter { state.value }
                    snapshot.apply().check()
                } finally {
                    snapshot.dispose()
                }
            }

            state.value++

            Snapshot.sendApplyNotifications()

            assertEquals(1, changes)
        } finally {
            stateObserver.stop()
        }
    }

    @Test
    fun pauseStopsObserving() {
        val data = "data"
        var changes = 0

        runSimpleTest { stateObserver, state ->
            stateObserver.observeReads(data, { _ -> changes++ }) {
                stateObserver.withNoObservations {
                    state.value
                }
            }
        }

        assertEquals(0, changes)
    }

    @Test
    fun nestedPauseStopsObserving() {
        val data = "data"
        var changes = 0

        runSimpleTest { stateObserver, state ->
            stateObserver.observeReads(data, { _ -> changes++ }) {
                stateObserver.withNoObservations {
                    stateObserver.withNoObservations {
                        state.value
                    }
                    state.value
                }
            }
        }

        assertEquals(0, changes)
    }

    @Test
    fun simpleObserving() {
        val data = "data"
        var changes = 0

        runSimpleTest { stateObserver, state ->
            stateObserver.observeReads(data, { _ -> changes++ }) {
                state.value
            }
        }

        assertEquals(1, changes)
    }

    @Test
    fun observeWithinPause() {
        val data = "data"
        var changes1 = 0
        var changes2 = 0

        runSimpleTest { stateObserver, state ->
            stateObserver.observeReads(data, { _ -> changes1++ }) {
                stateObserver.withNoObservations {
                    stateObserver.observeReads(data, { _ -> changes2++ }) {
                        state.value
                    }
                }
            }
        }
        assertEquals(0, changes1)
        assertEquals(1, changes2)
    }

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

            state.value++
            observer.readWithObservation()

            Snapshot.notifyObjectsInitialized()
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