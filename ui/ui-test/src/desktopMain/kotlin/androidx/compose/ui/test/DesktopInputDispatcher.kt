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

package androidx.compose.ui.test

import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.TestPointerInputEventData
import androidx.compose.ui.node.RootForTest
import androidx.compose.ui.platform.DesktopOwner

internal actual fun createInputDispatcher(
    testContext: TestContext,
    root: RootForTest
): InputDispatcher {
    return DesktopInputDispatcher(testContext, root as DesktopOwner)
}

internal class DesktopInputDispatcher(
    testContext: TestContext,
    val root: DesktopOwner
) : InputDispatcher(testContext, root) {
    companion object {
        var gesturePointerId = 0L
    }

    override val now: Long get() = System.nanoTime() / 1_000_000

    private var isMousePressed = false

    private var batchedEvents = mutableListOf<List<TestPointerInputEventData>>()

    override fun PartialGesture.enqueueDown(pointerId: Int) {
        isMousePressed = true
        enqueueEvent(pointerInputEvent(isMousePressed))
    }
    override fun PartialGesture.enqueueMove() {
        enqueueEvent(pointerInputEvent(isMousePressed))
    }

    override fun PartialGesture.enqueueUp(pointerId: Int) {
        isMousePressed = false
        enqueueEvent(pointerInputEvent(isMousePressed))
        gesturePointerId += 1
    }

    override fun PartialGesture.enqueueCancel() {
        println("PartialGesture.sendCancel")
    }

    private fun enqueueEvent(event: List<TestPointerInputEventData>) {
        batchedEvents.add(event)
    }

    private fun PartialGesture.pointerInputEvent(down: Boolean): List<TestPointerInputEventData> {
        val time = lastEventTime
        val offset = lastPositions[lastPositions.keys.sorted()[0]]!!
        val event = listOf(
            TestPointerInputEventData(
                PointerId(gesturePointerId),
                time,
                offset,
                down
            )
        )
        return event
    }

    override fun sendAllSynchronous() {
        val copy = batchedEvents.toList()
        batchedEvents.clear()
        copy.forEach {
            val eventTime = it.first().uptime
            if (dispatchInRealTime) {
                val delayMs = eventTime - now
                if (delayMs > 0) {
                    Thread.sleep(delayMs)
                }
            }
            root.processPointerInput(eventTime, it)
        }
    }

    override fun onDispose() {
        batchedEvents.clear()
    }
}