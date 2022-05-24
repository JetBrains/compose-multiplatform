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
import java.lang.ref.WeakReference
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.test.fail
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.ObsoleteCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.newSingleThreadContext
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.Test

class RecomposerTestsJvm {

    @ExperimentalCoroutinesApi
    private fun runTestUnconfined(testBody: suspend TestScope.() -> Unit) {
        runTest(UnconfinedTestDispatcher(), testBody = testBody)
    }

    @Test
    @OptIn(ExperimentalComposeApi::class, ExperimentalCoroutinesApi::class)
    fun concurrentRecompositionOffMainThread() = runTestUnconfined {
        val dispatcher = testScheduler
        withContext(dispatcher) {
            val clock = TestMonotonicFrameClock(this)
            withContext(clock) {
                val recomposer = Recomposer(coroutineContext)
                launch {
                    recomposer.runRecomposeConcurrentlyAndApplyChanges(Dispatchers.Default)
                }

                val composition = Composition(UnitApplier(), recomposer)
                val threadLog = Channel<Thread>(Channel.BUFFERED)
                lateinit var recomposeScope: RecomposeScope
                composition.setContent {
                    threadLog.trySend(Thread.currentThread())
                    val scope = currentRecomposeScope
                    SideEffect {
                        recomposeScope = scope
                    }
                }

                val firstCompositionThread = threadLog.receive()

                recomposeScope.invalidate()
                dispatcher.advanceUntilIdle()

                val secondCompositionThread = threadLog.receive()
                assertNotEquals(firstCompositionThread, secondCompositionThread)

                recomposer.close()
                dispatcher.advanceUntilIdle()
            }
        }
    }

    @Test
    @OptIn(ExperimentalComposeApi::class, ExperimentalCoroutinesApi::class)
    fun concurrentRecompositionInvalidationDuringComposition() = runTestUnconfined {
        val dispatcher = testScheduler
        val clock = AutoTestFrameClock()
        withContext(dispatcher + clock) {
            val recomposer = Recomposer(coroutineContext)
            launch {
                recomposer.runRecomposeConcurrentlyAndApplyChanges(Dispatchers.Default)
            }

            val composition = Composition(UnitApplier(), recomposer)
            var longRecomposition by mutableStateOf(false)
            val longRecompositionLatch = CountDownLatch(1)
            val applyCount = AtomicInteger(0)
            val recomposeLatch = CountDownLatch(2)
            composition.setContent {
                recomposeLatch.countDown()
                if (longRecomposition) {
                    longRecompositionLatch.await()
                }
                SideEffect {
                    applyCount.incrementAndGet()
                }
            }

            assertEquals(1, applyCount.get(), "applyCount after initial composition")

            Snapshot.withMutableSnapshot {
                longRecomposition = true
            }

            assertTrue(recomposeLatch.await(5, TimeUnit.SECONDS), "recomposeLatch await timed out")
            assertEquals(1, applyCount.get(), "applyCount after starting long recomposition")

            longRecompositionLatch.countDown()
            recomposer.awaitIdle()

            assertEquals(2, applyCount.get(), "applyCount after long recomposition")

            recomposer.close()
        }
    }

    @Test
    @OptIn(ExperimentalComposeApi::class, ObsoleteCoroutinesApi::class)
    fun concurrentRecompositionOnCompositionSpecificContext() = runBlocking(AutoTestFrameClock()) {
        val recomposer = Recomposer(coroutineContext)
        launch {
            recomposer.runRecomposeConcurrentlyAndApplyChanges(Dispatchers.Default)
        }

        @OptIn(DelicateCoroutinesApi::class)
        newSingleThreadContext("specialThreadPool").use { pool ->
            val composition = Composition(UnitApplier(), recomposer, pool)
            var recomposition by mutableStateOf(false)
            val recompositionThread = Channel<Thread>(1)
            composition.setContent {
                if (recomposition) {
                    recompositionThread.trySend(Thread.currentThread())
                }
            }

            Snapshot.withMutableSnapshot {
                recomposition = true
            }

            assertTrue(
                withTimeoutOrNull(3_000) {
                    recompositionThread.receive()
                }?.name?.contains("specialThreadPool") == true,
                "recomposition did not occur on expected thread"
            )

            recomposer.close()
        }
    }

    @Test
    fun recomposerCancelReportsShuttingDownImmediately() = runBlocking(AutoTestFrameClock()) {
        val recomposer = Recomposer(coroutineContext)
        launch(start = CoroutineStart.UNDISPATCHED) {
            recomposer.runRecomposeAndApplyChanges()
        }

        // Create a composition with a LaunchedEffect that will need to be resumed for cancellation
        // before the recomposer can fully join.
        Composition(UnitApplier(), recomposer).setContent {
            LaunchedEffect(Unit) {
                awaitCancellation()
            }
        }

        recomposer.cancel()
        // runBlocking will not dispatch resumed continuations for cancellation yet;
        // read the current state immediately.
        val state = recomposer.currentState.value
        assertTrue(
            state <= Recomposer.State.ShuttingDown,
            "recomposer state $state but expected <= ShuttingDown"
        )
    }

    @Test
    fun disposedInvalidatedCompositionDoesNotLeak(): Unit = runBlocking {
        val recomposer = Recomposer(coroutineContext)

        // Sent to when a frame is requested by recomposer
        val frameRequestCh = Channel<Unit>(Channel.CONFLATED)

        // Run recompositions with a clock that will never produce a frame, thereby leaving
        // invalidations unhandled. Launch undispatched to get things moving before we proceed.
        launch(
            BroadcastFrameClock { frameRequestCh.trySend(Unit) },
            start = CoroutineStart.UNDISPATCHED
        ) {
            recomposer.runRecomposeAndApplyChanges()
        }

        // Used to invalidate the composition below
        var state by mutableStateOf(0)

        // Create the composition to test in a function rather than directly, otherwise
        // we end up with a hard reference from the stack sticking around preventing gc
        fun createWeakComposition() = WeakReference(
            Composition(UnitApplier(), recomposer).apply {
                setContent {
                    // This state read will invalidate the composition
                    @Suppress("UNUSED_VARIABLE")
                    val readme = state
                }
            }
        )

        // Hold only a weak reference to this created composition for the test
        val weakRef = createWeakComposition()

        // Ensure the recomposer is idle and ready to receive invalidations before we commit
        // a snapshot that includes one
        recomposer.currentState.first { it == Recomposer.State.Idle }

        // Invalidate the composition
        Snapshot.withMutableSnapshot { state++ }

        withTimeoutOrNull(1000) {
            frameRequestCh.receive()
        } ?: fail("never requested a frame from recomposer")

        // Bug 209497244 tracked the Recomposer keeping this composition
        // in an invalidation list after disposal; confirm below that this becomes unreachable
        weakRef.get()?.dispose() ?: fail("composition prematurely collected")

        Runtime.getRuntime().gc()

        assertNull(weakRef.get(), "composition was not collected after disposal")

        recomposer.cancel()
    }
}

private class AutoTestFrameClock : MonotonicFrameClock {
    private val time = AtomicLong(0)

    override suspend fun <R> withFrameNanos(onFrame: (frameTimeNanos: Long) -> R): R {
        return onFrame(time.getAndAdd(16_000_000))
    }
}
