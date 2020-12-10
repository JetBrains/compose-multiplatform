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

import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.InspectableValue
import androidx.compose.ui.platform.ValueElement
import androidx.compose.ui.platform.ViewConfiguration
import androidx.compose.ui.platform.isDebugInspectorInfoEnabled
import androidx.compose.ui.unit.Duration
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.Uptime
import androidx.compose.ui.unit.milliseconds
import androidx.test.ext.junit.runners.AndroidJUnit4
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
import kotlinx.coroutines.test.runBlockingTest
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith

@SmallTest
@RunWith(AndroidJUnit4::class)
@OptIn(ExperimentalCoroutinesApi::class)
class SuspendingPointerInputFilterTest {
    @After
    fun after() {
        // some tests may set this
        isDebugInspectorInfoEnabled = false
    }

    @Test
    fun testAwaitSingleEvent(): Unit = runBlockingTest {
        val filter = SuspendingPointerInputFilter(FakeViewConfiguration())

        val result = CompletableDeferred<PointerEvent>()
        launch {
            with(filter) {
                handlePointerInput {
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
    fun testAwaitSeveralEvents(): Unit = runBlockingTest {
        val filter = SuspendingPointerInputFilter(FakeViewConfiguration())
        val results = Channel<PointerEvent>(Channel.UNLIMITED)
        launch {
            with(filter) {
                handlePointerInput {
                    repeat(3) {
                        results.offer(awaitPointerEvent())
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
    fun testSyntheticCancelEvent(): Unit = runBlockingTest {
        val filter = SuspendingPointerInputFilter(FakeViewConfiguration())
        val results = Channel<PointerEvent>(Channel.UNLIMITED)
        launch {
            with(filter) {
                handlePointerInput {
                    repeat(3) {
                        results.offer(awaitPointerEvent())
                    }
                    results.close()
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
            // only one pointer since the previous event's second pointer changed to up,
            // the old position unchanged, 'down' changed from true to false, and the downChange
            // marked as consumed.
            PointerEvent(
                listOf(
                    PointerInputChange(
                        PointerId(0),
                        current = PointerInputData(
                            uptime = Uptime.Boot,
                            position = Offset(6f, 6f),
                            down = false
                        ),
                        previous = PointerInputData(
                            uptime = Uptime.Boot,
                            position = Offset(6f, 6f),
                            down = true
                        ),
                        consumed = ConsumedData(downChange = true)
                    )
                )
            )
        )

        expectedEvents.take(expectedEvents.size - 1).forEach {
            filter.onPointerEvent(it, PointerEventPass.Main, bounds)
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
    }

    @Test
    fun testCancelledHandlerBlock() = runBlockingTest {
        val filter = SuspendingPointerInputFilter(FakeViewConfiguration())
        val counter = TestCounter()
        val handler = launch {
            with(filter) {
                try {
                    handlePointerInput {
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
        val modifier = Modifier.pointerInput(block) as InspectableValue

        assertThat(modifier.nameFallback).isEqualTo("pointerInput")
        assertThat(modifier.valueOverride).isNull()
        assertThat(modifier.inspectableElements.asIterable()).containsExactly(
            ValueElement("block", block)
        )
    }
}

private fun PointerInputChange.toPointerEvent() = PointerEvent(listOf(this))

private val PointerEvent.firstChange get() = changes.first()

private class PointerInputChangeEmitter(id: Int = 0) {
    val pointerId = PointerId(id.toLong())
    var previousData = PointerInputData(
        uptime = Uptime.Boot,
        position = Offset.Zero,
        down = false
    )

    fun nextChange(
        position: Offset = Offset.Zero,
        down: Boolean = true,
        time: Uptime = Uptime.Boot
    ): PointerInputChange {
        val current = PointerInputData(
            position = position,
            down = down,
            uptime = time
        )

        return PointerInputChange(
            id = pointerId,
            current = current,
            previous = previousData,
            consumed = ConsumedData()
        ).also { previousData = current }
    }
}

private class FakeViewConfiguration : ViewConfiguration {
    override val longPressTimeout: Duration
        get() = 500.milliseconds
    override val doubleTapTimeout: Duration
        get() = 300.milliseconds
    override val doubleTapMinTime: Duration
        get() = 40.milliseconds
    override val touchSlop: Float
        get() = 18f
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
