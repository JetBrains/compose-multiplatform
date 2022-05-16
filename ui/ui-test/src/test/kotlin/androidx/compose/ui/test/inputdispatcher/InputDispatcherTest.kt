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

import android.view.View
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.ViewRootForTest
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsOwner
import androidx.compose.ui.test.AndroidInputDispatcher
import androidx.compose.ui.test.InternalTestApi
import androidx.compose.ui.test.MainTestClock
import androidx.compose.ui.test.TestOwner
import androidx.compose.ui.test.createTestContext
import androidx.compose.ui.test.util.InputEventRecorder
import androidx.compose.ui.test.util.assertNoTouchGestureInProgress
import com.google.common.truth.Truth.assertThat
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.mock
import org.junit.After

@OptIn(InternalTestApi::class)
open class InputDispatcherTest {

    internal val recorder = InputEventRecorder()

    private val testOwner: TestOwner = mock {
        val testClock: MainTestClock = mock()
        on { mainClock } doReturn testClock
        on { runOnUiThread(any<() -> Any>()) }.then {
            it.getArgument<() -> Any>(0).invoke()
        }
    }

    private val testContext = createTestContext(testOwner)

    private val viewRootForTest: ViewRootForTest = mock {
        val mockView: View = mock {
            on { getLocationOnScreen(any()) }.then {
                it.getArgument<IntArray>(0).fill(0)
                null
            }
        }
        on { view } doReturn mockView

        val mockSemanticsOwner: SemanticsOwner = mock {
            val mockSemanticsNode: SemanticsNode = mock {
                val rootBounds = Rect(Offset.Zero, Size(1000f, 1000f))
                on { boundsInRoot } doReturn rootBounds
            }
            on { rootSemanticsNode } doReturn mockSemanticsNode
        }
        on { semanticsOwner } doReturn mockSemanticsOwner
    }

    internal val subject = AndroidInputDispatcher(
        testContext,
        viewRootForTest,
        recorder::recordEvent
    )

    @After
    fun tearDown() {
        // MotionEvents are still at the subject or in the recorder, but not both
        subject.dispose()
        recorder.disposeEvents()
    }
}

internal fun AndroidInputDispatcher.generateTouchDownAndCheck(pointerId: Int, position: Offset) {
    enqueueTouchDown(pointerId, position)
    assertThat(getCurrentTouchPosition(pointerId)).isEqualTo(position)
}

internal fun AndroidInputDispatcher.updateTouchPointerAndCheck(pointerId: Int, position: Offset) {
    updateTouchPointer(pointerId, position)
    assertThat(getCurrentTouchPosition(pointerId)).isEqualTo(position)
}

internal fun AndroidInputDispatcher.generateTouchUpAndCheck(pointerId: Int, delay: Long? = null) {
    if (delay != null) {
        advanceEventTime(delay)
    }
    enqueueTouchUp(pointerId)
    assertThat(getCurrentTouchPosition(pointerId)).isNull()
}

internal fun AndroidInputDispatcher.generateTouchCancelAndCheck(delay: Long? = null) {
    if (delay != null) {
        advanceEventTime(delay)
    }
    enqueueTouchCancel()
    assertNoTouchGestureInProgress()
}
