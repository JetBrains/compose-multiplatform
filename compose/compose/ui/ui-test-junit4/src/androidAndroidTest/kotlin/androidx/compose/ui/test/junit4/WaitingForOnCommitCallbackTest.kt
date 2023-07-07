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

package androidx.compose.ui.test.junit4

import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.runComposeUiTest
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
@OptIn(ExperimentalTestApi::class)
class WaitingForOnCommitCallbackTest {

    @Test
    fun setContentAndWaitForIdleReleasesAfterOnCommitCallback() = runComposeUiTest {
        val atomicBoolean = AtomicBoolean(false)
        var switch by mutableStateOf(true)
        setContent {
            DisposableEffect(switch) {
                atomicBoolean.set(switch)
                onDispose { }
            }
        }

        assertThat(atomicBoolean.get()).isTrue()

        runOnIdle {
            switch = false
        }
        waitForIdle()

        assertThat(atomicBoolean.get()).isFalse()
    }

    @LargeTest
    @Test
    fun cascadingOnCommits() = runComposeUiTest {
        // Collect unique values (markers) at each step during the process and
        // at the end verify that they were collected in the right order
        val values = mutableListOf<Int>()

        // Use a latch to make sure all collection events have occurred, to avoid
        // concurrent modification exceptions when checking the collected values,
        // in case some values still need to be collected due to a bug.
        var latch = CountDownLatch(0)

        var switch1 by mutableStateOf(true)
        var switch2 by mutableStateOf(true)
        var switch3 by mutableStateOf(true)
        var switch4 by mutableStateOf(true)
        setContent {
            DisposableEffect(switch1) {
                values.add(2)
                switch2 = switch1
                onDispose { }
            }
            DisposableEffect(switch2) {
                values.add(3)
                switch3 = switch2
                onDispose { }
            }
            DisposableEffect(switch3) {
                values.add(4)
                switch4 = switch3
                onDispose { }
            }
            DisposableEffect(switch4) {
                values.add(5)
                latch.countDown()
                onDispose { }
            }
        }

        runOnIdle {
            latch = CountDownLatch(1)
            values.clear()

            // Kick off the cascade
            values.add(1)
            switch1 = false
        }

        waitForIdle()
        // Mark the end
        values.add(6)

        // Make sure all writes into the list are complete
        latch.await()

        // And check if all was in the right order
        assertThat(values).containsExactly(1, 2, 3, 4, 5, 6).inOrder()
    }

    @Test
    fun cascadingOnCommits_suspendedWait_defaultDispatcher() = runComposeUiTest {
        runBlocking {
            // Collect unique values (markers) at each step during the process and
            // at the end verify that they were collected in the right order
            val values = mutableListOf<Int>()

            // Use a latch to make sure all collection events have occurred, to avoid
            // concurrent modification exceptions when checking the collected values,
            // in case some values still need to be collected due to a bug.
            // Start locked so we can reliably await the first composition.
            val mutex = Mutex(locked = true)

            var switch1 by mutableStateOf(true)
            var switch2 by mutableStateOf(true)
            var switch3 by mutableStateOf(true)
            var switch4 by mutableStateOf(true)
            setContent {
                DisposableEffect(switch1) {
                    values.add(2)
                    switch2 = switch1
                    onDispose { }
                }
                DisposableEffect(switch2) {
                    values.add(3)
                    switch3 = switch2
                    onDispose { }
                }
                DisposableEffect(switch3) {
                    values.add(4)
                    switch4 = switch3
                    onDispose { }
                }
                DisposableEffect(switch4) {
                    values.add(5)
                    mutex.unlock()
                    onDispose { }
                }
            }

            // Await the first composition and reset all values
            awaitIdle()
            mutex.lock()
            values.clear()

            // Kick off the cascade
            values.add(1)
            switch1 = false

            awaitIdle()
            // Mark the end
            values.add(6)

            // Make sure all writes into the list are complete
            mutex.withLock {
                // And check if all was in the right order
                assertThat(values).containsExactly(1, 2, 3, 4, 5, 6).inOrder()
            }
        }
    }
}
