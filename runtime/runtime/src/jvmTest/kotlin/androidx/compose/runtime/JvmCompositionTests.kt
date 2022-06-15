/*
 * Copyright 2019 The Android Open Source Project
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
package androidx.compose.runtime

import androidx.compose.runtime.mock.EmptyApplier
import androidx.compose.runtime.mock.compositionTest
import androidx.compose.runtime.mock.Text
import androidx.compose.runtime.mock.View
import androidx.compose.runtime.mock.ViewApplier
import androidx.compose.runtime.mock.expectChanges
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.runtime.snapshots.SnapshotStateObserver
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.concurrent.thread
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlinx.coroutines.delay
import kotlin.test.Test
import kotlin.test.assertEquals

@Stable
@OptIn(InternalComposeApi::class)
@Suppress("unused")
class JvmCompositionTests {
    // Regression test for b/202967533
    // Test taken from the bug report; reformatted to conform to lint rules.
    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun avoidsDeadlockInRecomposerComposerDispose() {
        val thread = thread {
            while (!Thread.interrupted()) {
                // -> synchronized(stateLock) -> recordComposerModificationsLocked
                // -> composition.recordModificationsOf -> synchronized(lock)
                Snapshot.sendApplyNotifications()
            }
        }

        for (i in 1..1000) {
            runTest(UnconfinedTestDispatcher()) {
                localRecomposerTest {
                    @Suppress("ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE")
                    var value by mutableStateOf(0)
                    val snapshotObserver = SnapshotStateObserver {}
                    snapshotObserver.start()
                    @Suppress("UNUSED_VALUE")
                    value = 4
                    val composition = Composition(EmptyApplier(), it)
                    composition.setContent {}

                    // -> synchronized(lock) -> parent.unregisterComposition(this)
                    // -> synchronized(stateLock)
                    composition.dispose()
                    snapshotObserver.stop()
                }
            }
        }

        thread.interrupt()
    }

    @Test
    @OptIn(ExperimentalCoroutinesApi::class)
    fun avoidRaceConditionWhenInvalidating() = compositionTest {
        var scope: RecomposeScope? = null
        var count = 0
        var threadException: Exception? = null
        val thread = thread {
            try {
                while (!Thread.interrupted()) {
                    scope?.invalidate()
                    count++
                }
            } catch (e: Exception) {
                threadException = e
            }
        }

        compose {
            scope = currentRecomposeScope
            Text("Some text")
            Text("Count $count")
        }

        repeat(20) {
            advance(ignorePendingWork = true)
            delay(1)
        }

        thread.interrupt()
        @Suppress("BlockingMethodInNonBlockingContext")
        thread.join()
        delay(10)
        threadException?.let { throw it }
    }

    @Test
    @OptIn(ExperimentalCoroutinesApi::class)
    fun avoidRaceConditionWhenApplyingSnapshotsInAThread() = compositionTest {
        val count = mutableStateOf(0)
        var threadException: Exception? = null

        compose {
            Text("Some text")
            Text("Count ${count.value}")
        }

        val thread = thread {
            try {
                while (!Thread.interrupted()) {
                    Snapshot.withMutableSnapshot {
                        count.value++
                    }
                }
            } catch (e: Exception) {
                threadException = e
            }
        }

        repeat(200) {
            advance(ignorePendingWork = true)
            delay(1)
        }

        thread.interrupt()
        @Suppress("BlockingMethodInNonBlockingContext")
        thread.join()
        delay(10)
        threadException?.let { throw it }
    }

    @Test // b/197064250 and others
    fun canInvalidateDuringApplyChanges() = compositionTest {
        var value by mutableStateOf(0)
        compose {
            Wrap {
                val scope = currentRecomposeScope
                ComposeNode<View, ViewApplier>(
                    factory = { View().also { it.name = "linear" } },
                    update = {
                        set(value) {
                            scope.invalidate()
                            this.attributes["value"] = value.toString()
                        }
                    }
                )
            }
        }

        value = 2
        expectChanges()
    }

    private var count = 0
    @BeforeTest fun saveSnapshotCount() {
        count = Snapshot.openSnapshotCount()
    }

    @AfterTest fun checkSnapshotCount() {
        val afterCount = Snapshot.openSnapshotCount()
        assertEquals(count, afterCount, "A snapshot was left open after the test")
    }
}
