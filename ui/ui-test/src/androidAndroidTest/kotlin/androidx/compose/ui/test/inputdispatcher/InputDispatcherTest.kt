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

package androidx.compose.ui.test.inputdispatcher

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.AndroidInputDispatcher
import androidx.compose.ui.test.InputDispatcher
import androidx.compose.ui.test.InternalTestApi
import androidx.compose.ui.test.MainTestClock
import androidx.compose.ui.test.TestOwner
import androidx.compose.ui.test.createTestContext
import androidx.compose.ui.test.util.InputDispatcherTestRule
import androidx.compose.ui.test.util.MotionEventRecorder
import com.google.common.truth.Truth.assertThat
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.mock
import org.junit.After
import org.junit.Rule
import org.junit.rules.TestRule

@OptIn(InternalTestApi::class)
open class InputDispatcherTest(eventPeriodOverride: Long? = null) {

    @get:Rule
    val inputDispatcherRule: TestRule = InputDispatcherTestRule(
        eventPeriodOverride = eventPeriodOverride
    )

    internal val recorder = MotionEventRecorder()
    private val testClock: MainTestClock = mock()
    private val testOwner: TestOwner = mock {
        on { mainClock } doReturn testClock
        on { runOnUiThread(any<() -> Any>()) }.then {
            it.getArgument<() -> Any>(0).invoke()
        }
    }
    private val testContext = createTestContext(testOwner)
    internal val subject = AndroidInputDispatcher(testContext, null, recorder::recordEvent)

    @After
    fun tearDown() {
        // MotionEvents are still at the subject or in the recorder, but not both
        subject.dispose()
        recorder.disposeEvents()
    }
}

internal fun AndroidInputDispatcher.generateDownAndCheck(pointerId: Int, position: Offset) {
    enqueueDown(pointerId, position)
    assertThat(getCurrentPosition(pointerId)).isEqualTo(position)
}

internal fun AndroidInputDispatcher.movePointerAndCheck(pointerId: Int, position: Offset) {
    movePointer(pointerId, position)
    assertThat(getCurrentPosition(pointerId)).isEqualTo(position)
}

internal fun AndroidInputDispatcher.generateUpAndCheck(pointerId: Int, delay: Long? = null) {
    if (delay != null) {
        enqueueUp(pointerId, delay)
    } else {
        enqueueUp(pointerId)
    }
    assertThat(getCurrentPosition(pointerId)).isNull()
}

internal fun AndroidInputDispatcher.generateCancelAndCheck(delay: Long? = null) {
    if (delay != null) {
        enqueueCancel(delay)
    } else {
        enqueueCancel()
    }
    verifyNoGestureInProgress()
}

internal fun InputDispatcher.verifyNoGestureInProgress() {
    assertThat((this as AndroidInputDispatcher).isGestureInProgress).isFalse()
}
