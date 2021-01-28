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

import androidx.compose.runtime.mock.TestMonotonicFrameClock
import androidx.compose.runtime.snapshots.Snapshot
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runBlockingTest
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class, ExperimentalComposeApi::class)
class RecomposerTests {

    @Test
    fun recomposerRecomposesWhileOpen() = runBlockingTest {
        val testClock = TestMonotonicFrameClock(this)
        withContext(testClock) {
            val recomposer = Recomposer(coroutineContext)
            val runner = launch {
                recomposer.runRecomposeAndApplyChanges()
            }
            val composition = Composition(UnitApplier(), recomposer)
            var state by mutableStateOf(0)
            var lastRecomposedState = -1
            composition.setContent {
                lastRecomposedState = state
            }
            assertEquals(0, lastRecomposedState, "initial composition")
            Snapshot.withMutableSnapshot { state = 1 }
            assertNotNull(
                withTimeoutOrNull(3_000) { recomposer.awaitIdle() },
                "timed out waiting for recomposer idle for recomposition"
            )
            assertEquals(1, lastRecomposedState, "recomposition")
            recomposer.close()
            assertNotNull(
                withTimeoutOrNull(3_000) { recomposer.join() },
                "timed out waiting for recomposer.join"
            )
            assertNotNull(
                withTimeoutOrNull(3_000) { runner.join() },
                "timed out waiting for recomposer runner job"
            )
            Snapshot.withMutableSnapshot { state = 2 }
            assertNotNull(
                withTimeoutOrNull(3_000) {
                    recomposer.state.first { it <= Recomposer.State.PendingWork }
                },
                "timed out waiting for recomposer to not have active pending work"
            )
            assertEquals(1, lastRecomposedState, "expected no recomposition by closed recomposer")
        }
    }

    @Test
    fun recomposerRemainsOpenUntilEffectsJoin() = runBlockingTest {
        val testClock = TestMonotonicFrameClock(this)
        withContext(testClock) {
            val recomposer = Recomposer(coroutineContext)
            val runner = launch {
                recomposer.runRecomposeAndApplyChanges()
            }
            val composition = Composition(UnitApplier(), recomposer)
            val completer = Job()
            composition.setContent {
                LaunchedEffect(completer) {
                    completer.join()
                }
            }
            recomposer.awaitIdle()
            recomposer.close()
            recomposer.awaitIdle()
            assertTrue(runner.isActive, "runner is still active")
            completer.complete()
            assertNotNull(
                withTimeoutOrNull(5_000) { recomposer.join() },
                "Expected recomposer join"
            )
            assertEquals(Recomposer.State.ShutDown, recomposer.state.first(), "recomposer state")
            assertNotNull(
                withTimeoutOrNull(5_000) { runner.join() },
                "Expected runner join"
            )
        }
    }
}

private class UnitApplier : Applier<Unit> {
    override val current: Unit
        get() = Unit

    override fun down(node: Unit) {
    }

    override fun up() {
    }

    override fun insertTopDown(index: Int, instance: Unit) {
    }

    override fun insertBottomUp(index: Int, instance: Unit) {
    }

    override fun remove(index: Int, count: Int) {
    }

    override fun move(from: Int, to: Int, count: Int) {
    }

    override fun clear() {
    }
}
