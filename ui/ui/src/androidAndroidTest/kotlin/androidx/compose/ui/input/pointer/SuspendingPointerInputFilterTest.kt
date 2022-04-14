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

package androidx.compose.ui.input.pointer

import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.testutils.TestViewConfiguration
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.InspectableValue
import androidx.compose.ui.platform.ValueElement
import androidx.compose.ui.platform.isDebugInspectorInfoEnabled
import androidx.compose.ui.test.TestActivity
import androidx.compose.ui.unit.IntSize
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.yield
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@SmallTest
@RunWith(AndroidJUnit4::class)
@OptIn(ExperimentalCoroutinesApi::class)
class SuspendingPointerInputFilterTest {
    @After
    fun after() {
        // some tests may set this
        isDebugInspectorInfoEnabled = false
    }

    private fun runTestUnconfined(test: suspend TestScope.() -> Unit) =
        runTest(UnconfinedTestDispatcher()) {
            test()
        }

    @Test
    fun testAwaitSingleEvent(): Unit = runTestUnconfined {
        val filter = SuspendingPointerInputFilter(TestViewConfiguration())

        val result = CompletableDeferred<PointerEvent>()
        launch {
            with(filter) {
                awaitPointerEventScope {
                    result.complete(awaitPointerEvent())
                }
            }
        }

        val emitter = PointerInputChangeEmitter()
        val expectedChange = emitter.nextChange(Offset(5f, 5f))

        filter.onPointerEvent(
            expectedChange.toPointerEvent(),
            PointerEventPass.Main,
            IntSize(10, 10)
        )

        val receivedEvent = withTimeout(200) {
            result.await()
        }

        assertEquals(expectedChange, receivedEvent.firstChange)
    }

    @Test
    fun testAwaitSeveralEvents(): Unit = runTestUnconfined {
        val filter = SuspendingPointerInputFilter(TestViewConfiguration())
        val results = Channel<PointerEvent>(Channel.UNLIMITED)
        launch {
            with(filter) {
                awaitPointerEventScope {
                    repeat(3) {
                        results.trySend(awaitPointerEvent())
                    }
                    results.close()
                }
            }
        }

        val emitter = PointerInputChangeEmitter()
        val expected = listOf(
            emitter.nextChange(Offset(5f, 5f)),
            emitter.nextChange(Offset(10f, 5f)),
            emitter.nextChange(Offset(10f, 10f))
        )

        val bounds = IntSize(20, 20)
        expected.forEach {
            filter.onPointerEvent(it.toPointerEvent(), PointerEventPass.Main, bounds)
        }
        val received = withTimeout(200) {
            results.receiveAsFlow()
                .map { it.firstChange }
                .toList()
        }

        assertEquals(expected, received)
    }

    @Test
    fun testSyntheticCancelEvent(): Unit = runTestUnconfined {
        var currentEventAtEnd: PointerEvent? = null
        val filter = SuspendingPointerInputFilter(TestViewConfiguration())
        val results = Channel<PointerEvent>(Channel.UNLIMITED)
        launch {
            with(filter) {
                awaitPointerEventScope {
                    try {
                        repeat(3) {
                            results.trySend(awaitPointerEvent())
                        }
                        results.close()
                    } finally {
                        currentEventAtEnd = currentEvent
                    }
                }
            }
        }

        val bounds = IntSize(50, 50)
        val emitter1 = PointerInputChangeEmitter(0)
        val emitter2 = PointerInputChangeEmitter(1)
        val expectedEvents = listOf(
            PointerEvent(
                listOf(
                    emitter1.nextChange(Offset(5f, 5f)),
                    emitter2.nextChange(Offset(10f, 10f))
                )
            ),
            PointerEvent(
                listOf(
                    emitter1.nextChange(Offset(6f, 6f)),
                    emitter2.nextChange(Offset(10f, 10f), down = false)
                )
            ),
            // Synthetic cancel should look like this;
            // Both pointers are there, but only the with the pressed = true is changed to false,
            // and the down change is consumed.
            PointerEvent(
                listOf(
                    PointerInputChange(
                        PointerId(0),
                        0,
                        Offset(6f, 6f),
                        false,
                        0,
                        Offset(6f, 6f),
                        true,
                        isInitiallyConsumed = true
                    ),
                    PointerInputChange(
                        PointerId(1),
                        0,
                        Offset(10f, 10f),
                        false,
                        0,
                        Offset(10f, 10f),
                        false,
                        isInitiallyConsumed = false
                    )
                )
            )
        )

        expectedEvents.take(expectedEvents.size - 1).forEach {
            filter.onPointerEvent(it, PointerEventPass.Initial, bounds)
            filter.onPointerEvent(it, PointerEventPass.Main, bounds)
            filter.onPointerEvent(it, PointerEventPass.Final, bounds)
        }
        filter.onCancel()

        val received = withTimeout(200) {
            results.receiveAsFlow().toList()
        }

        assertThat(expectedEvents).hasSize(received.size)

        expectedEvents.forEachIndexed { index, expectedEvent ->
            val actualEvent = received[index]
            PointerEventSubject.assertThat(actualEvent).isStructurallyEqualTo(expectedEvent)
        }
        assertThat(currentEventAtEnd).isNotNull()
        PointerEventSubject.assertThat(currentEventAtEnd!!)
            .isStructurallyEqualTo(expectedEvents.last())
    }

    @Test
    fun testNoSyntheticCancelEventWhenPressIsFalse(): Unit = runTestUnconfined {
        var currentEventAtEnd: PointerEvent? = null
        val filter = SuspendingPointerInputFilter(TestViewConfiguration())
        val results = Channel<PointerEvent>(Channel.UNLIMITED)
        launch {
            with(filter) {
                awaitPointerEventScope {
                    try {
                        repeat(3) {
                            withTimeout(200) {
                                results.trySend(awaitPointerEvent())
                            }
                        }
                    } finally {
                        currentEventAtEnd = currentEvent
                        results.close()
                    }
                }
            }
        }

        val bounds = IntSize(50, 50)
        val emitter1 = PointerInputChangeEmitter(0)
        val emitter2 = PointerInputChangeEmitter(1)
        val expectedEvents = listOf(
            PointerEvent(
                listOf(
                    emitter1.nextChange(Offset(5f, 5f)),
                    emitter2.nextChange(Offset(10f, 10f))
                )
            ),
            PointerEvent(
                listOf(
                    emitter1.nextChange(Offset(6f, 6f), down = false),
                    emitter2.nextChange(Offset(10f, 10f), down = false)
                )
            )
            // Unlike when a pointer is down, there is no cancel event sent
            // when there aren't any pressed pointers. There's no event stream to cancel.
        )

        expectedEvents.forEach {
            filter.onPointerEvent(it, PointerEventPass.Initial, bounds)
            filter.onPointerEvent(it, PointerEventPass.Main, bounds)
            filter.onPointerEvent(it, PointerEventPass.Final, bounds)
        }
        filter.onCancel()

        withTimeout(400) {
            while (!results.isClosedForSend) {
                yield()
            }
        }

        val received = results.receiveAsFlow().toList()

        assertThat(received).hasSize(expectedEvents.size)

        expectedEvents.forEachIndexed { index, expectedEvent ->
            val actualEvent = received[index]
            PointerEventSubject.assertThat(actualEvent).isStructurallyEqualTo(expectedEvent)
        }
        assertThat(currentEventAtEnd).isNotNull()
        PointerEventSubject.assertThat(currentEventAtEnd!!)
            .isStructurallyEqualTo(expectedEvents.last())
    }

    @Test
    fun testCancelledHandlerBlock() = runTestUnconfined {
        val filter = SuspendingPointerInputFilter(TestViewConfiguration())
        val counter = TestCounter()
        val handler = launch {
            with(filter) {
                try {
                    awaitPointerEventScope {
                        try {
                            counter.expect(1, "about to call awaitPointerEvent")
                            awaitPointerEvent()
                            fail("awaitPointerEvent returned; should have thrown for cancel")
                        } finally {
                            counter.expect(3, "inner finally block running")
                        }
                    }
                } finally {
                    counter.expect(4, "outer finally block running; inner finally should have run")
                }
            }
        }

        counter.expect(2, "before cancelling handler; awaitPointerEvent should be suspended")
        handler.cancel()
        counter.expect(5, "after cancelling; finally blocks should have run")
    }

    @Test
    fun testInspectorValue() = runBlocking<Unit> {
        isDebugInspectorInfoEnabled = true
        val block: suspend PointerInputScope.() -> Unit = {}
        val modifier = Modifier.pointerInput(Unit, block) as InspectableValue

        assertThat(modifier.nameFallback).isEqualTo("pointerInput")
        assertThat(modifier.valueOverride).isNull()
        assertThat(modifier.inspectableElements.asIterable()).containsExactly(
            ValueElement("key1", Unit),
            ValueElement("block", block)
        )
    }

    @Test
    @LargeTest
    fun testRestartPointerInput() = runBlocking {
        var toAdd by mutableStateOf("initial")
        val result = mutableListOf<String>()
        val latch = CountDownLatch(2)
        ActivityScenario.launch(TestActivity::class.java).use { scenario ->
            scenario.moveToState(Lifecycle.State.CREATED)
            scenario.onActivity {
                it.setContent {
                    // Read the value in composition to change the lambda capture below
                    val toCapture = toAdd
                    Box(
                        Modifier.pointerInput(toCapture) {
                            result += toCapture
                            latch.countDown()
                            suspendCancellableCoroutine<Unit> {}
                        }
                    )
                }
            }
            scenario.moveToState(Lifecycle.State.STARTED)
            Snapshot.withMutableSnapshot {
                toAdd = "secondary"
            }
            assertTrue("waiting for relaunch timed out", latch.await(3, TimeUnit.SECONDS))
            assertEquals(
                listOf("initial", "secondary"),
                result
            )
        }
    }

    @Test(expected = PointerEventTimeoutCancellationException::class)
    fun testWithTimeout() = runTestUnconfined {
        val filter = SuspendingPointerInputFilter(TestViewConfiguration())
        filter.coroutineScope = this
        with(filter) {
            awaitPointerEventScope {
                withTimeout(10) {
                    awaitPointerEvent()
                }
            }
        }
    }

    @Test
    fun testWithTimeoutOrNull() = runTestUnconfined {
        val filter = SuspendingPointerInputFilter(TestViewConfiguration())
        filter.coroutineScope = this
        val result: PointerEvent? = with(filter) {
            awaitPointerEventScope {
                withTimeoutOrNull(10) {
                    awaitPointerEvent()
                }
            }
        }
        assertThat(result).isNull()
    }
}

private fun PointerInputChange.toPointerEvent() = PointerEvent(listOf(this))

private val PointerEvent.firstChange get() = changes.first()

private class PointerInputChangeEmitter(id: Int = 0) {
    val pointerId = PointerId(id.toLong())
    var previousTime = 0L
    var previousPosition = Offset.Zero
    var previousPressed = false

    fun nextChange(
        position: Offset = Offset.Zero,
        down: Boolean = true,
        time: Long = 0
    ): PointerInputChange {
        return PointerInputChange(
            id = pointerId,
            time,
            position,
            down,
            previousTime,
            previousPosition,
            previousPressed,
            isInitiallyConsumed = false
        ).also {
            previousTime = time
            previousPosition = position
            previousPressed = down
        }
    }
}

private class TestCounter {
    private var count = 0

    fun expect(checkpoint: Int, message: String = "(no message)") {
        val expected = count + 1
        if (checkpoint != expected) {
            fail("out of order event $checkpoint, expected $expected, $message")
        }
        count = expected
    }
}
