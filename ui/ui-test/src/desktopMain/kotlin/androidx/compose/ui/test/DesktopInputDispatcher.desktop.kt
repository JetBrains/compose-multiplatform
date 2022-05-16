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

@file:OptIn(InternalComposeUiApi::class)

package androidx.compose.ui.test

import androidx.compose.ui.InternalComposeUiApi
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.TestPointerInputEventData
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.node.RootForTest
import androidx.compose.ui.platform.SkiaRootForTest

internal actual fun createInputDispatcher(
    testContext: TestContext,
    root: RootForTest
): InputDispatcher {
    return DesktopInputDispatcher(testContext, root as SkiaRootForTest)
}

internal class DesktopInputDispatcher(
    testContext: TestContext,
    val root: SkiaRootForTest
) : InputDispatcher(testContext, root) {
    companion object {
        var gesturePointerId = 0L
    }

    private var isMousePressed = false

    private var batchedEvents = mutableListOf<List<TestPointerInputEventData>>()

    override fun PartialGesture.enqueueDown(pointerId: Int) {
        isMousePressed = true
        enqueueEvent(pointerInputEvent(isMousePressed))
    }
    override fun PartialGesture.enqueueMove() {
        enqueueEvent(pointerInputEvent(isMousePressed))
    }

    override fun PartialGesture.enqueueMoves(
        relativeHistoricalTimes: List<Long>,
        historicalCoordinates: List<List<Offset>>
    ) {
        // TODO: add support for historical events
        enqueueMove()
    }

    override fun PartialGesture.enqueueUp(pointerId: Int) {
        isMousePressed = false
        enqueueEvent(pointerInputEvent(isMousePressed))
        gesturePointerId += 1
    }

    override fun PartialGesture.enqueueCancel() {
        println("PartialGesture.sendCancel")
    }

    override fun MouseInputState.enqueuePress(buttonId: Int) {
        TODO("Not yet implemented")
    }

    override fun MouseInputState.enqueueMove() {
        TODO("Not yet implemented")
    }

    override fun MouseInputState.enqueueRelease(buttonId: Int) {
        TODO("Not yet implemented")
    }

    override fun MouseInputState.enqueueEnter() {
        TODO("Not yet implemented")
    }

    override fun MouseInputState.enqueueExit() {
        TODO("Not yet implemented")
    }

    override fun MouseInputState.enqueueCancel() {
        TODO("Not yet implemented")
    }

    @OptIn(ExperimentalTestApi::class)
    override fun MouseInputState.enqueueScroll(delta: Float, scrollWheel: ScrollWheel) {
        TODO("Not yet implemented")
    }

    // TODO(b/233199964): Implement key injection for desktop
    override fun KeyInputState.enqueueDown(key: Key) = TODO("Not yet implemented")

    // TODO(b/233199964): Implement key injection for desktop
    override fun KeyInputState.enqueueUp(key: Key) = TODO("Not yet implemented")

    override fun RotaryInputState.enqueueRotaryScrollHorizontally(horizontalScrollPixels: Float) {
        TODO("Not yet implemented")
    }

    override fun RotaryInputState.enqueueRotaryScrollVertically(verticalScrollPixels: Float) {
        TODO("Not yet implemented")
    }

    private fun enqueueEvent(event: List<TestPointerInputEventData>) {
        batchedEvents.add(event)
    }

    private fun PartialGesture.pointerInputEvent(down: Boolean): List<TestPointerInputEventData> {
        val time = currentTime
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

    override fun flush() {
        val copy = batchedEvents.toList()
        batchedEvents.clear()
        copy.forEach {
            val eventTime = it.first().uptime
            root.processPointerInput(eventTime, it)
        }
    }

    override fun onDispose() {
        batchedEvents.clear()
    }
}